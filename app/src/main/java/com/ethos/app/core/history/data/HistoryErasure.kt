package com.ethos.app.core.history.data

import com.ethos.app.core.reminders.domain.ReminderKind
import com.ethos.app.core.tasks.data.TaskDatabase
import com.ethos.app.core.timers.domain.TimerKind

/**
 * Removes every trace of erased tasks/habits outside their own tables (spec 022, research R10). Coin and XP
 * amounts are never touched: ledger and entitlement rows stay and only lose their copied title, so the balance,
 * total XP and level are exactly the same afterwards. Callers must hold the database transaction.
 */
class HistoryErasure(private val database: TaskDatabase) {
    /** [occurrenceIds] and [definitionIds] were already removed from the task tables by the caller. */
    suspend fun taskTraces(occurrenceIds: List<String>, definitionIds: List<String>) {
        if (occurrenceIds.isEmpty() && definitionIds.isEmpty()) return
        val tasks = database.tasks()
        tasks.removeHistory(occurrenceIds)
        tasks.removeChecks(occurrenceIds + definitionIds.map { "$TEMPLATE_CHECKS$it" })
        removeTimers(TimerKind.TASK, occurrenceIds)
        definitionIds.forEach { database.timers().removeGoal("$TASK_OWNER$it") }
        val reminders = database.reminders()
        reminders.removeForOccurrences(occurrenceIds)
        reminders.removeForSources(ReminderKind.TASK.name, definitionIds)
        reminders.removeDeliveries(ReminderKind.TASK.name, occurrenceIds)
        anonymize(occurrenceIds.map { "$TASK_OWNER$it" })
    }

    /** Deletes the habit itself with its records, periods, trackers, timers and reminders. */
    suspend fun habit(habitId: String) {
        val habits = database.habits()
        habits.removeRecords(habitId)
        habits.removeAudits(habitId)
        habits.removeIntervals(habitId)
        habits.removeRules(habitId)
        habits.removeOverrides("$habitId:")
        habits.removeReviews("$habitId:")
        habits.removeHabit(habitId)
        val trackers = database.trackers()
        val trackerIds = trackers.trackerIds(habitId)
        trackerIds.forEach { id ->
            trackers.removeTrackerAnswers(id)
            trackers.removeTrackerEntries(id)
            trackers.removeOptions(id)
            trackers.removeQuestions(id)
            trackers.removeVersions(id)
            trackers.removeTracker(id)
        }
        removeTimers(TimerKind.HABIT, listOf(habitId))
        database.timers().removeGoal("$HABIT_OWNER$habitId")
        val reminders = database.reminders()
        reminders.removeForSources(ReminderKind.HABIT.name, listOf(habitId))
        reminders.removeDeliveries(ReminderKind.HABIT.name, listOf(habitId))
        reminders.removeForSources(ReminderKind.TRACKER.name, trackerIds)
        reminders.removeDeliveries(ReminderKind.TRACKER.name, trackerIds)
        anonymize(listOf("$HABIT_OWNER$habitId", "$BASELINE_HABIT_OWNER$habitId"))
    }

    private suspend fun removeTimers(kind: TimerKind, sourceIds: List<String>) {
        if (sourceIds.isEmpty()) return
        val timers = database.timers()
        val sessions = timers.sessionIds(kind.name, sourceIds)
        if (sessions.isEmpty()) return
        timers.removeAudits(sessions)
        timers.removeSegments(sessions)
        timers.removeSessions(sessions)
    }

    private suspend fun anonymize(owners: List<String>) {
        if (owners.isEmpty()) return
        val accounting = database.accounting()
        accounting.clearLedgerTitles(owners)
        accounting.clearEntitlementTitles(owners)
    }

    private companion object {
        /** Owners and keys shared with the accounting, timer and task stores. */
        const val TASK_OWNER = "task:"
        const val HABIT_OWNER = "habit:"
        const val BASELINE_HABIT_OWNER = "baseline:habit:"
        const val TEMPLATE_CHECKS = "template:"
    }
}
