package com.tabitos.app.core.tasks.data

import androidx.room.withTransaction
import com.tabitos.app.core.accounting.data.AccountingStore
import com.tabitos.app.core.accounting.domain.AccountingProjection
import com.tabitos.app.core.history.data.HistoryErasure
import com.tabitos.app.core.preferences.GameModeSource
import com.tabitos.app.core.preferences.TaskReminderDefaults
import com.tabitos.app.core.reminders.domain.defaultTaskReminders
import com.tabitos.app.core.tasks.domain.DeadlineKind
import com.tabitos.app.core.reminders.data.entity
import com.tabitos.app.core.reminders.domain.ReminderRules
import com.tabitos.app.core.tasks.domain.RepeatKind
import com.tabitos.app.core.tasks.domain.TaskAction
import com.tabitos.app.core.tasks.domain.TaskClock
import com.tabitos.app.core.tasks.domain.TaskRepository
import com.tabitos.app.core.tasks.domain.TaskRules
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.temporal.ChronoUnit
import java.time.ZoneId

class RoomTaskRepository(
    private val database: TaskDatabase, private val clock: TaskClock, private val gameMode: GameModeSource,
    private val reminderDefaults: Flow<TaskReminderDefaults> = flowOf(TaskReminderDefaults()),
) : TaskRepository {
    override fun observe(): Flow<TaskSnapshot> = database.invalidationTracker.createFlow(
        "task_lists", "task_definitions", "task_occurrences", "task_checklist", "task_audit",
        emitInitialState = true,
    ).map { database.withTransaction { read() } }

    override suspend fun execute(action: TaskAction) {
        commit(action)
    }

    override suspend fun createWithReminders(action: TaskAction.Create): Boolean = commit(action)

    private suspend fun commit(action: TaskAction): Boolean {
        // Read before the transaction: outcomes follow the mode at write time (spec 017).
        val scoring = gameMode.enabled.first()
        val defaults = if (action is TaskAction.Create && action.fields.deadline.hasDayTime) {
            reminderDefaults.first()
        } else null
        return database.withTransaction {
            val accounting = AccountingStore(database, clock)
            accounting.initialize()
            val stored = readRows()
            val before = stored.snapshot()
            val now = clock.nowMillis()
            val after = TaskRules.apply(before, action, clock.today(), now)
            if (before != after) write(TaskWrites.between(stored, after))
            removeErased(before, after)
            if (action is TaskAction.Reschedule || action is TaskAction.UndoReschedule) {
                moveReminders(before, after, action)
            }
            if (action is TaskAction.Resolve || action is TaskAction.Undo || action is TaskAction.SetCompleted) {
                val id = when (action) {
                    is TaskAction.Resolve -> action.id
                    is TaskAction.Undo -> action.id
                    is TaskAction.SetCompleted -> action.id
                }
                accounting.reconcile(AccountingProjection.tasks(after).filter { it.owner == "task:$id" },
                    setOf("task:$id"), scoring = scoring)
            }
            if (action is TaskAction.DeleteList) {
                database.habits().closeListIntervals(action.id, clock.today())
                database.habits().excludeList(action.id)
            }
            if (action is TaskAction.Create && defaults != null && before.definitions.none { it.id == action.id }) {
                val definitions = defaultTaskReminders(action, defaults, now, ZoneId.systemDefault())
                definitions.forEach { database.reminders().definition(it.entity()) }
                definitions.isNotEmpty()
            } else false
        }
    }

    /** Rows the rules erased (spec 022) are deleted here, with their traces in other tables. */
    private suspend fun removeErased(before: TaskSnapshot, after: TaskSnapshot) {
        val occurrences = before.occurrences.map { it.id } - after.occurrences.map { it.id }.toSet()
        val definitions = before.definitions.map { it.id } - after.definitions.map { it.id }.toSet()
        if (occurrences.isEmpty() && definitions.isEmpty()) return
        val dao = database.tasks()
        dao.removeOccurrences(occurrences)
        dao.removeDefinitions(definitions)
        HistoryErasure(database).taskTraces(occurrences, definitions)
    }

    /** Fixed-date reminders move with a rescheduled task, in the same transaction (spec 021). */
    private suspend fun moveReminders(before: TaskSnapshot, after: TaskSnapshot, action: TaskAction) {
        val id = when (action) {
            is TaskAction.Reschedule -> action.id
            is TaskAction.UndoReschedule -> action.id
            else -> return
        }
        val old = before.occurrences.first { it.id == id }
        val new = after.occurrences.first { it.id == id }
        val days = ChronoUnit.DAYS.between(old.fields.date ?: return, new.fields.date ?: return)
        if (days == 0L) return
        val recurring = after.definitions.first { it.id == old.taskId }.rule.kind != RepeatKind.NONE
        val dao = database.reminders()
        ReminderRules.rescheduleChosen(dao.definitions().map { it.domain() }, old, days, recurring)
            .forEach { dao.definition(it.entity()) }
    }

    private suspend fun read(): TaskSnapshot = readRows().snapshot()

    private suspend fun readRows(): TaskRows {
        val dao = database.tasks()
        return TaskRows(dao.lists(), dao.definitions(), dao.occurrences(), dao.checks(), dao.history())
    }

    /** Writes only what changed (spec 027); unchanged rows are left alone, so their observers are not woken. */
    private suspend fun write(w: TaskWrites) {
        val dao = database.tasks()
        if (w.lists.isNotEmpty()) dao.lists(w.lists)
        if (w.definitions.isNotEmpty()) dao.definitions(w.definitions)
        if (w.occurrences.isNotEmpty()) dao.occurrences(w.occurrences)
        w.checklists.forEach { (owner, rows) ->
            dao.clearChecks(owner)
            dao.checks(rows)
        }
        if (w.history.isNotEmpty()) dao.history(w.history)
    }
}
