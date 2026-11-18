package com.ethos.app.core.habits.data

import androidx.room.withTransaction
import com.ethos.app.core.accounting.data.AccountingStore
import com.ethos.app.core.accounting.domain.AccountingProjection
import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.history.data.HistoryErasure
import com.ethos.app.core.preferences.GameModeSource
import com.ethos.app.core.tasks.data.TaskDatabase
import com.ethos.app.core.tasks.data.changedRows
import com.ethos.app.core.tasks.domain.TaskClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RoomHabitRepository(
    private val database: TaskDatabase, private val clock: TaskClock, private val gameMode: GameModeSource,
) : HabitRepository {
    override fun observe(): Flow<HabitSnapshot> = database.invalidationTracker.createFlow(
        "habits", "habit_rules", "habit_intervals", "habit_overrides", "habit_records", "habit_reviews",
        "habit_audits", emitInitialState = true,
    ).map { database.withTransaction { read() } }

    private suspend fun read(): HabitSnapshot = read(database)

    companion object {
        /** Reads the habit snapshot inside the caller's transaction (used by tracker validation). */
        suspend fun read(database: TaskDatabase): HabitSnapshot {
            val dao = database.habits()
            return HabitSnapshot(dao.habits().map { it.value }, dao.rules().map { it.value },
                dao.intervals().map { it.value }, dao.overrides().map { it.value }, dao.records().map { it.value },
                dao.reviews().map { it.value }, dao.audits().map { it.value })
        }
    }
    override suspend fun erase(habitId: String) {
        database.withTransaction { HistoryErasure(database).habit(habitId) }
    }

    override suspend fun execute(action: HabitAction) {
        // Read before the transaction: outcomes follow the mode at write time (spec 017).
        val scoring = gameMode.enabled.first()
        database.withTransaction {
            val accounting = AccountingStore(database, clock)
            accounting.initialize()
            val lists = database.tasks().lists().filterNot { it.deleted }.map { it.id }.toSet()
            val before = read()
            val s = HabitRules.apply(before, action, clock.today(), clock.nowMillis(), lists)
            val dao = database.habits()
            // Only changed rows are written (spec 027): the closing that runs on every open then wakes no observer.
            dao.habits(changedRows(before.habits, s.habits) { it.id }.map { HabitEntity(it) })
            dao.removeReplacedRules(s.rules.map { it.id })
            dao.rules(changedRows(before.rules, s.rules) { it.id }.map { HabitRuleEntity(it) })
            dao.intervals(changedRows(before.intervals, s.intervals) { it.id }.map { HabitIntervalEntity(it) })
            dao.overrides(changedRows(before.overrides, s.overrides) { it.key }.map { HabitOverrideEntity(it) })
            dao.records(changedRows(before.records, s.records) { it.id }.map { HabitRecordEntity(it) })
            dao.reviews(changedRows(before.reviews, s.reviews) { it.key }.map { HabitReviewEntity(it) })
            dao.audits(changedRows(before.audits, s.audits) { it.id }.map { HabitAuditEntity(it) })
            if (action == HabitAction.Settle) {
                // Automatic closings (avoid/limit successes) reach the ledger without any user action.
                val owners = s.habits.map { "habit:${it.id}" }.toSet()
                accounting.reconcile(accounting.habitOutcomes(s).filter { it.owner in owners }, owners,
                    scoring = scoring)
            } else if (action !is HabitAction.Status || (action.status != HabitStatus.EXCLUDED &&
                    before.overrides != s.overrides)) {
                val id = when (action) {
                    is HabitAction.Record -> action.habitId
                    is HabitAction.SetPractice -> action.habitId
                    is HabitAction.Correct -> before.records.first { it.id == action.id }.habitId
                    is HabitAction.Review -> s.audits.last().habitId
                    is HabitAction.Save -> action.habit.id
                    is HabitAction.Status -> action.id
                    HabitAction.Settle -> return@withTransaction
                }
                // Presentation-only edits never create financial transitions.
                if (action !is HabitAction.Save || before.rules != s.rules || before.overrides != s.overrides) {
                    accounting.reconcile(accounting.habitOutcomes(s)
                        .filter { it.owner == "habit:$id" }, setOf("habit:$id"),
                        AccountingProjection.practiceOrigins(before, s, action, clock.today()), scoring)
                }
            }
        }
    }
}
