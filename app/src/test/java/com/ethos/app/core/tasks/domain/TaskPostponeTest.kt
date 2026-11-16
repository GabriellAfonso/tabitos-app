package com.ethos.app.core.tasks.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskPostponeTest {
    private val today = LocalDate.of(2026, 11, 16)
    private val dayTask = TaskFields(title = "Pagar conta", deadline = DeadlineKind.DAY, date = today, minute = 18 * 60)
    private fun apply(s: TaskSnapshot, a: TaskAction, on: LocalDate = today) = TaskRules.apply(s, a, on, 1L)
    private fun create(fields: TaskFields = dayTask, rule: RepeatRule = RepeatRule(start = fields.date ?: today),
                       on: LocalDate = today) = apply(TaskSnapshot(), TaskAction.Create("a", fields, rule), on)
    private fun problem(block: () -> Unit) = assertThrows(TaskRuleException::class.java, block).problem

    @Test fun postponeMovesOnlyTheDayAndKeepsTheTime() {
        val s = apply(create(), TaskAction.Postpone("a:1", today.plusDays(1)))
        val o = s.occurrences.single()
        assertEquals(today.plusDays(1), o.dueDate)
        assertEquals(18 * 60, o.fields.minute)
        assertEquals(TaskStatus.OPEN, o.status)
        assertFalse(s.history.any { it.kind == AuditKind.MISSED_DEADLINE })
    }

    @Test fun postponeRejectsPastDaysAndTheCurrentDeadline() {
        val s = create()
        assertEquals(TaskProblem.INVALID_DATE, problem { apply(s, TaskAction.Postpone("a:1", today.minusDays(1))) })
        assertEquals(TaskProblem.INVALID_DATE, problem { apply(s, TaskAction.Postpone("a:1", today)) })
    }

    @Test fun overduePostponeCountsAsReviewAndKeepsTheOriginalDeadline() {
        val yesterday = today.minusDays(1)
        val s = apply(create(dayTask.copy(date = yesterday), on = yesterday), TaskAction.Postpone("a:1", today))
        assertFalse(s.occurrences.single().needsReview(today))
        assertEquals(yesterday, s.history.single { it.kind == AuditKind.MISSED_DEADLINE }.originalDeadline)
    }

    @Test fun recurringPostponeLeavesTheNextOccurrencesOnTheirDates() {
        val s = create(rule = RepeatRule(RepeatKind.FIXED_INTERVAL, today, interval = 2))
        val moved = apply(s, TaskAction.Postpone("a:1", today.plusDays(5)), today.plusDays(2))
        assertEquals(today.plusDays(5), moved.occurrences.first { it.id == "a:1" }.dueDate)
        assertEquals(today.plusDays(2), moved.occurrences.first { it.id == "a:2" }.dueDate)
        assertEquals(today, moved.occurrences.first { it.id == "a:1" }.plannedDate)
    }

    @Test fun onlyOpenTasksWithASpecificDayCanBePostponed() {
        val week = create(dayTask.copy(deadline = DeadlineKind.WEEK, minute = null))
        assertEquals(TaskProblem.INVALID_INPUT, problem { apply(week, TaskAction.Postpone("a:1", today.plusDays(9))) })
        val noDate = create(TaskFields(title = "Sem data"))
        assertEquals(TaskProblem.INVALID_INPUT, problem { apply(noDate, TaskAction.Postpone("a:1", today.plusDays(1))) })
        val done = apply(create(), TaskAction.Resolve("a:1", true, today))
        assertEquals(TaskProblem.NOT_OPEN, problem { apply(done, TaskAction.Postpone("a:1", today.plusDays(1))) })
    }

    @Test fun undoRestoresAPastDeadlineAndOnlyAppendsHistory() {
        val yesterday = today.minusDays(1)
        val postponed = apply(create(dayTask.copy(date = yesterday), on = yesterday),
            TaskAction.Postpone("a:1", today.plusDays(1)))
        val undone = apply(postponed, TaskAction.UndoPostpone("a:1", yesterday))
        assertEquals(yesterday, undone.occurrences.single().dueDate)
        assertTrue(undone.history.containsAll(postponed.history))
        assertTrue(undone.history.size > postponed.history.size)
    }
}
