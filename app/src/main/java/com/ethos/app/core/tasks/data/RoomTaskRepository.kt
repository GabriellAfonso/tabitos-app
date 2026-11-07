package com.ethos.app.core.tasks.data

import androidx.room.withTransaction
import com.ethos.app.core.accounting.data.AccountingStore
import com.ethos.app.core.accounting.domain.AccountingProjection
import com.ethos.app.core.tasks.domain.TaskAction
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.tasks.domain.TaskRepository
import com.ethos.app.core.tasks.domain.TaskRules
import com.ethos.app.core.tasks.domain.TaskSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTaskRepository(private val database: TaskDatabase, private val clock: TaskClock) : TaskRepository {
    override fun observe(): Flow<TaskSnapshot> = database.invalidationTracker.createFlow(
        "task_lists", "task_definitions", "task_occurrences", "task_checklist", "task_audit",
        emitInitialState = true,
    ).map { database.withTransaction { read() } }

    override suspend fun execute(action: TaskAction) {
        database.withTransaction {
            val accounting = AccountingStore(database, clock)
            accounting.initialize()
            val before = read()
            val after = TaskRules.apply(before, action, clock.today(), clock.nowMillis())
            if (before != after) write(after)
            if (action is TaskAction.Resolve || action is TaskAction.Undo) {
                val id = when (action) {
                    is TaskAction.Resolve -> action.id
                    is TaskAction.Undo -> action.id
                }
                accounting.reconcile(AccountingProjection.tasks(after).filter { it.owner == "task:$id" },
                    setOf("task:$id"))
            }
            if (action is TaskAction.DeleteList) {
                database.habits().closeListIntervals(action.id, clock.today())
                database.habits().excludeList(action.id)
            }
        }
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
