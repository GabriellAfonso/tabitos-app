package com.ethos.app.core.habits.data

import androidx.room.withTransaction
import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.tasks.data.TaskDatabase
import com.ethos.app.core.tasks.domain.TaskClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomHabitRepository(private val database: TaskDatabase, private val clock: TaskClock) : HabitRepository {
    override fun observe(): Flow<HabitSnapshot> = database.invalidationTracker.createFlow(
        "habits", "habit_rules", "habit_intervals", "habit_overrides", "habit_records", "habit_reviews",
        "habit_audits", emitInitialState = true,
    ).map { database.withTransaction { read() } }

    private suspend fun read(): HabitSnapshot {
        val dao = database.habits()
        return HabitSnapshot(dao.habits().map { it.value }, dao.rules().map { it.value },
            dao.intervals().map { it.value }, dao.overrides().map { it.value }, dao.records().map { it.value },
            dao.reviews().map { it.value }, dao.audits().map { it.value })
    }
    override suspend fun execute(action: HabitAction) {
        database.withTransaction {
            val lists = database.tasks().lists().filterNot { it.deleted }.map { it.id }.toSet()
            val s = HabitRules.apply(read(), action, clock.today(), clock.nowMillis(), lists)
            val dao = database.habits()
            dao.habits(s.habits.map { HabitEntity(it) })
            dao.removeReplacedRules(s.rules.map { it.id })
            dao.rules(s.rules.map { HabitRuleEntity(it) })
            dao.intervals(s.intervals.map { HabitIntervalEntity(it) })
            dao.overrides(s.overrides.map { HabitOverrideEntity(it) })
            dao.records(s.records.map { HabitRecordEntity(it) })
            dao.reviews(s.reviews.map { HabitReviewEntity(it) })
            dao.audits(s.audits.map { HabitAuditEntity(it) })
        }
    }
}
