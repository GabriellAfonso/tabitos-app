package com.tabitos.app.core.tasks.data

import com.tabitos.app.core.tasks.domain.ChangeScope
import com.tabitos.app.core.tasks.domain.DeadlineKind
import com.tabitos.app.core.tasks.domain.RepeatKind
import com.tabitos.app.core.tasks.domain.RepeatRule
import com.tabitos.app.core.tasks.domain.TaskAction
import com.tabitos.app.core.tasks.domain.TaskFields
import com.tabitos.app.core.tasks.domain.TaskList
import com.tabitos.app.core.tasks.domain.TaskRules
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Saving only changed rows must leave every task table exactly as the former full rewrite did (spec 027). The
 * tables are modelled as keyed maps with the same upsert and "clear owner, insert" semantics as the DAO.
 */
class ChangedRowsTest {
    private val day = LocalDate.of(2026, 11, 17)

    private class Tables(rows: TaskRows) {
        val lists = rows.lists.associateBy { it.id }.toMutableMap()
        val definitions = rows.definitions.associateBy { it.id }.toMutableMap()
        val occurrences = rows.occurrences.associateBy { it.id }.toMutableMap()
        val checks = rows.checks.associateBy { it.id }.toMutableMap()
        val history = rows.history.associateBy { it.id }.toMutableMap()

        fun replaceChecklist(owner: String, items: List<ChecklistEntity>) {
            checks.values.removeAll { it.ownerId == owner }
            items.forEach { checks[it.id] = it }
        }

        /** What `write()` did before spec 027: upsert everything and replace every owner's checklist. */
        fun fullRewrite(s: TaskSnapshot) {
            s.lists.forEach { lists[it.id] = it.entity() }
            s.definitions.forEach { definitions[it.id] = it.entity() }
            s.occurrences.forEach { occurrences[it.id] = it.entity() }
            s.checklistRows().forEach { (owner, items) -> replaceChecklist(owner, items) }
            s.history.forEach { history[it.id] = it.entity() }
        }

        fun apply(w: TaskWrites) {
            w.lists.forEach { lists[it.id] = it }
            w.definitions.forEach { definitions[it.id] = it }
            w.occurrences.forEach { occurrences[it.id] = it }
            w.checklists.forEach { (owner, items) -> replaceChecklist(owner, items) }
            w.history.forEach { history[it.id] = it }
        }

        fun rows() = TaskRows(lists.values.toList(), definitions.values.toList(), occurrences.values.toList(),
            checks.values.toList(), history.values.toList())

        fun contents() = listOf(lists, definitions, occurrences, checks, history).map { it.toMap() }
    }

    /** Stores [before] with a full rewrite, then saves [action]'s result both ways and compares the tables. */
    private fun assertSameTables(before: TaskSnapshot, action: TaskAction, today: LocalDate = day): TaskWrites {
        val stored = Tables(TaskRows(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
            .apply { fullRewrite(before) }
        val after = TaskRules.apply(stored.rows().snapshot(), action, today, 500L)
        val full = Tables(stored.rows()).apply { fullRewrite(after) }
        val writes = TaskWrites.between(stored.rows(), after)
        val changed = Tables(stored.rows()).apply { apply(writes) }
        assertEquals(full.contents(), changed.contents())
        return writes
    }

    private fun base(): TaskSnapshot {
        var s = TaskRules.apply(TaskSnapshot(), TaskAction.SaveList(TaskList("work", "Trabalho")), day, 1L)
        s = TaskRules.apply(s, TaskAction.Create("a", TaskFields(title = "Relatório", listId = "work",
            checklist = listOf("Dados", "Gráficos")), RepeatRule(start = day)), day, 2L)
        s = TaskRules.apply(s, TaskAction.Create("b", TaskFields(title = "Ler", listId = "work"),
            RepeatRule(start = day)), day, 3L)
        return TaskRules.apply(s, TaskAction.Create("r", TaskFields(title = "Regar", deadline = DeadlineKind.DAY,
            date = day), RepeatRule(RepeatKind.DAILY, day)), day, 4L)
    }

    @Test fun storedRowsReadBackAsTheSameSnapshot() {
        val s = base()
        val stored = Tables(TaskRows(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
            .apply { fullRewrite(s) }
        assertEquals(s.copy(occurrences = s.occurrences.sortedBy { it.id }),
            stored.rows().snapshot().let { it.copy(occurrences = it.occurrences.sortedBy { o -> o.id }) })
    }

    @Test fun everyActionLeavesTheSameTables() {
        val s = base()
        val a = s.occurrences.first { it.taskId == "a" }.id
        val b = s.occurrences.first { it.taskId == "b" }.id
        val r = s.occurrences.first { it.taskId == "r" }.id
        listOf(
            TaskAction.Create("c", TaskFields(title = "Nova", checklist = listOf("Um")), RepeatRule(start = day)),
            TaskAction.Edit(a, TaskFields(title = "Relatório final", listId = "work", checklist = listOf("Dados")),
                RepeatRule(start = day), ChangeScope.ONLY_THIS),
            TaskAction.Check(a, 1, true),
            TaskAction.Resolve(b, true, day),
            TaskAction.SetCompleted(r, true, day),
            TaskAction.Reschedule(r, day, day.plusDays(1)),
            TaskAction.Order(listOf(b, a)),
            TaskAction.Move(b, null),
            TaskAction.Delete(a, ChangeScope.ONLY_THIS),
            TaskAction.Delete(r, ChangeScope.THIS_AND_FUTURE, eraseHistory = true),
            TaskAction.DeleteList("work"),
        ).forEach { assertSameTables(s, it) }
        assertSameTables(s, TaskAction.Refresh, day.plusDays(3))
    }

    @Test fun onlyTheChangedRowsAreWritten() {
        val s = base()
        val b = s.occurrences.first { it.taskId == "b" }.id
        val writes = assertSameTables(s, TaskAction.Resolve(b, true, day))
        assertEquals(listOf(b), writes.occurrences.map { it.id })
        assertTrue(writes.lists.isEmpty() && writes.definitions.isEmpty() && writes.checklists.isEmpty())
        assertEquals(1, writes.history.size)
    }

    @Test fun nothingChangedWritesNothing() {
        assertTrue(assertSameTables(base(), TaskAction.Refresh).isEmpty)
    }

    @Test fun changedRowsKeepNewAndDifferentRowsOnly() {
        val stored = listOf("a" to 1, "b" to 2)
        val after = listOf("a" to 1, "b" to 3, "c" to 4)
        assertEquals(listOf("b" to 3, "c" to 4), changedRows(stored, after) { it.first })
    }
}
