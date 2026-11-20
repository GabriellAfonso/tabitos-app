package com.tabitos.app.core.ui.components

import com.tabitos.app.core.tasks.domain.TaskAction
import com.tabitos.app.core.tasks.domain.TaskList
import com.tabitos.app.core.tasks.domain.TaskRuleException
import com.tabitos.app.core.tasks.domain.TaskRules
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ReorderTest {
    @Test fun movedPlacesItemAtTargetAndIgnoresInvalidIndexes() {
        val items = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "a", "d"), items.moved(0, 2))
        assertEquals(listOf("d", "a", "b", "c"), items.moved(3, 0))
        assertEquals(items, items.moved(1, 1))
        assertEquals(items, items.moved(-1, 2))
        assertEquals(items, items.moved(0, 4))
    }

    @Test fun orderListsAppliesFullOrderAndRejectsStaleOrders() {
        val day = LocalDate.of(2026, 11, 11)
        var s = TaskSnapshot()
        listOf("a", "b", "c").forEach { s = TaskRules.apply(s, TaskAction.SaveList(TaskList(it, it.uppercase())), day, 1) }
        s = TaskRules.apply(s, TaskAction.OrderLists(listOf("c", "a", "b")), day, 2)
        assertEquals(listOf("c", "a", "b"), s.activeLists.map { it.id })
        try {
            TaskRules.apply(s, TaskAction.OrderLists(listOf("a", "b")), day, 3)
            fail("partial order accepted")
        } catch (_: TaskRuleException) { }
    }
}
