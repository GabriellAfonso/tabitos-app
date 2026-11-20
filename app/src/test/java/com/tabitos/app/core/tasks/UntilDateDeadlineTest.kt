package com.tabitos.app.core.tasks

import com.tabitos.app.core.tasks.domain.DeadlineKind
import com.tabitos.app.core.tasks.domain.RepeatKind
import com.tabitos.app.core.tasks.domain.RepeatRule
import com.tabitos.app.core.tasks.domain.ChangeScope
import com.tabitos.app.core.tasks.domain.TaskAction
import com.tabitos.app.core.tasks.domain.TaskFields
import com.tabitos.app.core.tasks.domain.TaskOccurrence
import com.tabitos.app.core.tasks.domain.TaskProblem
import com.tabitos.app.core.tasks.domain.TaskRuleException
import com.tabitos.app.core.tasks.domain.TaskRules
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Até uma data" deadline (spec 035): a single-task window from the chosen start to the chosen end. */
class UntilDateDeadlineTest {
    private val today = LocalDate.of(2026, 11, 19)
    private val end = LocalDate.of(2026, 11, 28)
    private val fields = TaskFields(title = "Entregar relatório", deadline = DeadlineKind.UNTIL_DATE, date = end)

    private fun create(start: LocalDate = today, f: TaskFields = fields, kind: RepeatKind = RepeatKind.NONE) =
        TaskRules.apply(TaskSnapshot(), TaskAction.Create("t", f, RepeatRule(kind = kind, start = start)), today, 0)

    private fun TaskSnapshot.task(): TaskOccurrence = occurrences.single()

    @Test fun windowShowsEveryDayFromStartToEndThenNeedsReview() {
        val o = create().task()
        assertEquals(today, o.periodStart)
        assertEquals(end, o.dueDate)
        assertTrue(generateSequence(today) { it.plusDays(1) }.takeWhile { it <= end }.all { o.inDay(it) })
        assertFalse(o.inDay(end.plusDays(1)))
        assertFalse(o.needsReview(end))
        assertTrue(o.needsReview(end.plusDays(1)))
    }

    @Test fun laterStartHidesTheTaskUntilThen() {
        val o = create(start = LocalDate.of(2026, 11, 24)).task()
        assertFalse(o.inDay(LocalDate.of(2026, 11, 23)))
        assertTrue(o.inDay(LocalDate.of(2026, 11, 24)))
    }

    @Test fun acceptsTimeOnTheEndButNeverRepeats() {
        assertEquals(18 * 60, create(f = fields.copy(minute = 18 * 60)).task().fields.minute)
        val problem = runCatching { create(kind = RepeatKind.DAILY) }.exceptionOrNull() as TaskRuleException
        assertEquals(TaskProblem.INVALID_INPUT, problem.problem)
    }

    @Test fun editingKeepsTheNewlyChosenStart() {
        val created = create()
        val newStart = LocalDate.of(2026, 11, 22)
        val edited = TaskRules.apply(created, TaskAction.Edit("t:1", fields, RepeatRule(start = newStart),
            ChangeScope.ONLY_THIS), today, 0).task()
        assertEquals(newStart, edited.periodStart)
    }

    @Test fun reschedulingMovesOnlyTheEndAndUndoRestoresTheWindow() {
        val created = create(start = LocalDate.of(2026, 11, 24), f = fields.copy(minute = 18 * 60))
        assertTrue(created.task().canReschedule)
        val tomorrow = today.plusDays(1)
        val moved = TaskRules.apply(created, TaskAction.Reschedule("t:1", end, tomorrow), today, 0).task()
        assertEquals(tomorrow, moved.dueDate)
        assertEquals(tomorrow, moved.periodStart)
        assertEquals(18 * 60, moved.fields.minute)
        val undone = TaskRules.apply(TaskRules.apply(created, TaskAction.Reschedule("t:1", end, tomorrow), today, 0),
            TaskAction.UndoReschedule("t:1", tomorrow, end), today, 0).task()
        assertEquals(LocalDate.of(2026, 11, 24), undone.periodStart)
        assertEquals(end, undone.dueDate)
    }

    @Test fun existingKindsKeepTheirWindows() {
        val week = create(f = fields.copy(deadline = DeadlineKind.WEEK, date = today)).task()
        assertEquals(LocalDate.of(2026, 11, 16), week.periodStart)
        assertEquals(LocalDate.of(2026, 11, 22), week.dueDate)
        val day = create(start = LocalDate.of(2026, 11, 10), f = fields.copy(deadline = DeadlineKind.DAY)).task()
        assertEquals(end, day.periodStart)
        assertEquals(end, day.dueDate)
        assertFalse(week.canReschedule)
    }
}
