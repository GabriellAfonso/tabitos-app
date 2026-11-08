package com.ethos.app.core.trackers.data

import androidx.room.withTransaction
import com.ethos.app.core.habits.data.RoomHabitRepository
import com.ethos.app.core.habits.domain.HabitSnapshot
import com.ethos.app.core.tasks.data.TaskDatabase
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.trackers.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Writes only tracker tables; habit, accounting, reward, task and timer data are read-only here. */
class RoomTrackerRepository(private val db: TaskDatabase, private val clock: TaskClock) : TrackerRepository {
    override fun observe(): Flow<TrackerSnapshot> = db.invalidationTracker.createFlow(
        "trackers", "tracker_versions", "tracker_questions", "tracker_options", "tracker_entries", "tracker_answers",
        emitInitialState = true,
    ).map { db.withTransaction { read() } }

    private suspend fun read(): TrackerSnapshot {
        val dao = db.trackers()
        val questions = dao.questions().groupBy { it.trackerId to it.version }
        val options = dao.options().groupBy { Triple(it.trackerId, it.version, it.questionId) }
        val versions = dao.versions().map { v ->
            TrackerVersion(v.trackerId, v.number, v.created, questions[v.trackerId to v.number].orEmpty()
                .sortedBy { it.position }.map { q ->
                    TrackerQuestion(q.questionId, QuestionType.valueOf(q.type), q.title, q.required, q.isPrimary,
                        q.min, q.max, q.minLabel, q.maxLabel, q.multiple,
                        options[Triple(q.trackerId, q.version, q.questionId)].orEmpty().sortedBy { it.position }
                            .map { TrackerOption(it.optionId, it.label, it.description) })
                })
        }
        val answers = dao.answers().groupBy { it.entryId }
        val entries = dao.entries().map { e ->
            TrackerEntry(e.id, e.trackerId, e.version, e.date, e.filledAt, e.updatedAt, answers[e.id].orEmpty().map {
                TrackerAnswer(it.questionId, it.scale, it.choices.split('\n').filter(String::isNotEmpty), it.text)
            }, e.dayKey)
        }
        return TrackerSnapshot(dao.trackers().map { it.domain() }, versions, entries)
    }

    private suspend fun habits(): HabitSnapshot = RoomHabitRepository.read(db)

    override suspend fun execute(action: TrackerAction) {
        db.withTransaction {
            val before = read()
            val after = TrackerRules.apply(before, habits(), action, clock.today(), clock.nowMillis())
            val dao = db.trackers()
            dao.trackers(after.trackers.filter { it !in before.trackers }.map { it.entity() })
            val newVersions = after.versions.filter { v ->
                before.versions.none { it.trackerId == v.trackerId && it.number == v.number } }
            dao.versions(newVersions.map { TrackerVersionEntity(it.trackerId, it.number, it.created) })
            dao.questions(newVersions.flatMap { v -> v.questions.mapIndexed { i, q ->
                TrackerQuestionEntity(v.trackerId, v.number, q.id, i, q.type.name, q.title, q.required, q.primary,
                    q.min, q.max, q.minLabel, q.maxLabel, q.multiple) } })
            dao.options(newVersions.flatMap { v -> v.questions.flatMap { q -> q.options.mapIndexed { i, o ->
                TrackerOptionEntity(v.trackerId, v.number, o.id, q.id, i, o.label, o.description) } } })
            val kept = after.entries.associateBy { it.id }
            before.entries.filter { it.id !in kept }.forEach {
                dao.removeAnswers(it.id); dao.removeEntry(it.id)
            }
            after.entries.filter { it !in before.entries }.forEach { e ->
                dao.entry(TrackerEntryEntity(e.id, e.trackerId, e.version, e.date, e.dayKey, e.filledAt, e.updatedAt))
                dao.removeAnswers(e.id)
                dao.answers(e.answers.map {
                    TrackerAnswerEntity(e.id, it.questionId, it.scale, it.choices.joinToString("\n"), it.text) })
            }
        }
    }
}
