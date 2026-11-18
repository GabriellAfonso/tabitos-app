package com.ethos.app.core.tasks.data

import androidx.room.withTransaction
import com.ethos.app.core.accounting.data.AccountingStore
import com.ethos.app.core.accounting.domain.AccountingProjection
import com.ethos.app.core.history.data.HistoryErasure
import com.ethos.app.core.preferences.GameModeSource
import com.ethos.app.core.reminders.data.entity
import com.ethos.app.core.reminders.domain.ReminderRules
import com.ethos.app.core.tasks.domain.RepeatKind
import com.ethos.app.core.tasks.domain.TaskAction
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.tasks.domain.TaskRepository
import com.ethos.app.core.tasks.domain.TaskRules
import com.ethos.app.core.tasks.domain.TaskSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.temporal.ChronoUnit

class RoomTaskRepository(
    private val database: TaskDatabase, private val clock: TaskClock, private val gameMode: GameModeSource,
) : TaskRepository {
    override fun observe(): Flow<TaskSnapshot> = database.invalidationTracker.createFlow(
        "task_lists", "task_definitions", "task_occurrences", "task_checklist", "task_audit",
        emitInitialState = true,
    ).map { database.withTransaction { read() } }

    override suspend fun execute(action: TaskAction) {
        // Read before the transaction: outcomes follow the mode at write time (spec 017).
        val scoring = gameMode.enabled.first()
        database.withTransaction {
            val accounting = AccountingStore(database, clock)
            accounting.initialize()
            val stored = readRows()
            val before = stored.snapshot()
            val after = TaskRules.apply(before, action, clock.today(), clock.nowMillis())
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
