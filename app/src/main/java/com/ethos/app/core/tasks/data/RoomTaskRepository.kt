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
            val before = read()
            val after = TaskRules.apply(before, action, clock.today(), clock.nowMillis())
            if (before != after) write(after)
            removeErased(before, after)
            if (action is TaskAction.Postpone || action is TaskAction.UndoPostpone) moveReminders(before, after, action)
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

    /** Fixed-date reminders move with a postponed task, in the same transaction (spec 021). */
    private suspend fun moveReminders(before: TaskSnapshot, after: TaskSnapshot, action: TaskAction) {
        val id = when (action) {
            is TaskAction.Postpone -> action.id
            is TaskAction.UndoPostpone -> action.id
            else -> return
        }
        val old = before.occurrences.first { it.id == id }
        val new = after.occurrences.first { it.id == id }
        val days = ChronoUnit.DAYS.between(old.fields.date ?: return, new.fields.date ?: return)
        if (days == 0L) return
        val recurring = after.definitions.first { it.id == old.taskId }.rule.kind != RepeatKind.NONE
        val dao = database.reminders()
        ReminderRules.postponeChosen(dao.definitions().map { it.domain() }, old, days, recurring)
            .forEach { dao.definition(it.entity()) }
    }

    private suspend fun read(): TaskSnapshot {
        val dao = database.tasks()
        val checks = dao.checks().groupBy { it.ownerId }
        return TaskSnapshot(dao.lists().map { it.domain() },
            dao.definitions().map { it.domain(checks["template:${it.id}"].orEmpty()) },
            dao.occurrences().map { it.domain(checks[it.id].orEmpty()) }, dao.history().map { it.domain() })
    }

    private suspend fun write(s: TaskSnapshot) {
        val dao = database.tasks()
        dao.lists(s.lists.map { it.entity() })
        dao.definitions(s.definitions.map { it.entity() })
        dao.occurrences(s.occurrences.map { it.entity() })
        s.definitions.forEach { d ->
            val owner = "template:${d.id}"
            dao.clearChecks(owner)
            dao.checks(d.fields.checklist.mapIndexed { i, label ->
                ChecklistEntity("$owner:$i", owner, i, label, false)
            })
        }
        s.occurrences.forEach { o ->
            dao.clearChecks(o.id)
            dao.checks(o.checks.mapIndexed { i, c -> ChecklistEntity("${o.id}:$i", o.id, i, c.label, c.checked) })
        }
        dao.history(s.history.map { it.entity() })
    }
}
