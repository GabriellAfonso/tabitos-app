package com.tabitos.app.core.tasks.domain

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TaskOrderTest {
    private val day = LocalDate.of(2026, 11, 10)
    private fun apply(s: TaskSnapshot, a: TaskAction) = TaskRules.apply(s, a, day, 100)
    private fun snapshot(): TaskSnapshot {
        var s = TaskSnapshot()
        for (id in listOf("a", "hidden", "b", "c")) {
            s = apply(s, TaskAction.Create(id, TaskFields(title = id), RepeatRule(start = day)))
        }
        return s
    }

    @Test fun movingVisibleTasksPreservesHiddenSlotsAndOtherFields() {
        val before = snapshot()
        val result = apply(before, TaskAction.Order(listOf("c:1", "a:1", "b:1")))
        assertEquals(listOf("c:1", "hidden:1", "a:1", "b:1"),
            result.activeOccurrences.sortedBy { it.position }.map { it.id })
        assertEquals(before.history, result.history)
        assertEquals(before.definitions, result.definitions)
        before.occurrences.forEach { old ->
            assertEquals(old, result.occurrences.first { it.id == old.id }.copy(position = old.position))
        }
        assertEquals(result, apply(result, TaskAction.Order(listOf("c:1", "a:1", "b:1"))))
    }

    @Test fun invalidOrdersAreRejected() {
        val before = snapshot()
        for (ids in listOf(emptyList(), listOf("a:1", "a:1"), listOf("missing"))) {
            assertThrows(TaskRuleException::class.java) { apply(before, TaskAction.Order(ids)) }
        }
        val closed = apply(before, TaskAction.Resolve("a:1", true, day))
        assertThrows(TaskRuleException::class.java) {
            apply(closed, TaskAction.Order(listOf("b:1", "a:1")))
        }
        val withList = apply(before, TaskAction.SaveList(TaskList("list", "Other")))
        val moved = apply(withList, TaskAction.Move("a:1", "list"))
        assertThrows(TaskRuleException::class.java) { apply(moved, TaskAction.Order(listOf("a:1", "b:1"))) }
    }
}
