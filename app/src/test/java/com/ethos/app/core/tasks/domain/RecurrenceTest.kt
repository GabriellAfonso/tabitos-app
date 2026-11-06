package com.ethos.app.core.tasks.domain

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class RecurrenceTest {
    private val start = LocalDate.of(2024, 1, 31)
    private fun create(rule: RepeatRule, today: LocalDate = start): TaskSnapshot = TaskRules.apply(
        TaskSnapshot(), TaskAction.Create("r", TaskFields(title = "Repeat", deadline = DeadlineKind.DAY,
            date = start, checklist = listOf("Check")), rule), today, 10,
    )
    private fun act(s: TaskSnapshot, a: TaskAction, today: LocalDate = start) = TaskRules.apply(s, a, today, 20)

    @Test fun monthlyClampsThenReturnsToOriginalDayAndHonorsInclusiveEnd() {
        val s = create(RepeatRule(RepeatKind.MONTHLY, start, monthDay = 31,
            until = LocalDate.of(2024, 3, 31)), LocalDate.of(2024, 4, 1))
        assertEquals(listOf(start, LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 31)),
            s.activeOccurrences.map { it.dueDate })
    }

    @Test fun missedDailySlotsAreIndividualAndRefreshIdempotent() {
        val today = start.plusDays(3)
        val s = create(RepeatRule(RepeatKind.DAILY, start, count = 3), today)
        assertEquals(3, s.activeOccurrences.count { it.needsReview(today) })
        assertEquals(s, act(s, TaskAction.Refresh, today))
    }

    @Test fun repeatedRefreshDoesNotGenerateUnboundedFutureWork() {
        val s = create(RepeatRule(RepeatKind.DAILY, start))
        assertEquals(2, s.occurrences.size)
        var refreshed = s
        repeat(10) { refreshed = act(refreshed, TaskAction.Refresh) }
        assertEquals(s, refreshed)
    }

    @Test fun afterUndoingLaterOutcomePredecessorCanBeReopenedWithoutLosingHistory() {
        var s = create(RepeatRule(RepeatKind.AFTER_COMPLETION, start, interval = 2))
        s = act(s, TaskAction.Resolve("r:1", true, start), start.plusDays(5))
        s = act(s, TaskAction.Resolve("r:2", true, start.plusDays(2)), start.plusDays(5))
        assertThrows(TaskRuleException::class.java) { act(s, TaskAction.Undo("r:1"), start.plusDays(5)) }
        s = act(s, TaskAction.Undo("r:2"), start.plusDays(5))
        s = act(s, TaskAction.Undo("r:1"), start.plusDays(5))
        assertEquals(1, s.activeOccurrences.size)
        assertEquals(2, s.history.count { it.kind == AuditKind.UNDONE })
        s = act(s, TaskAction.Resolve("r:1", true, start.plusDays(1)), start.plusDays(5))
        assertEquals(start.plusDays(3), s.activeOccurrences.first { it.ordinal == 2 }.dueDate)
        assertEquals(2, s.activeOccurrences.size)
    }

    @Test fun singleExceptionDoesNotShiftCadenceAndFutureEditRetainsConsumedLimit() {
        var s = create(RepeatRule(RepeatKind.DAILY, start, count = 4))
        val o = s.occurrences.first()
        s = act(s, TaskAction.Edit(o.id, o.fields.copy(date = start.plusDays(10)),
            s.definitions.single().rule, ChangeScope.ONLY_THIS))
        assertEquals(start.plusDays(1), s.occurrences.first { it.ordinal == 2 }.dueDate)
        // Edit from the next untouched slot; the first exception and its identity are preserved.
        val second = s.occurrences.first { it.ordinal == 2 }
        s = act(s, TaskAction.Edit(second.id, second.fields.copy(title = "New cadence"),
            s.definitions.single().rule.copy(interval = 2, kind = RepeatKind.FIXED_INTERVAL),
            ChangeScope.THIS_AND_FUTURE))
        s = act(s, TaskAction.Refresh, start.plusDays(20))
        assertEquals(4, s.occurrences.size)
        assertEquals(start.plusDays(10), s.occurrences.first { it.ordinal == 1 }.dueDate)
        assertEquals(start.plusDays(5), s.occurrences.first { it.ordinal == 4 }.dueDate)
    }

    @Test fun movedExceptionDoesNotPauseGenerationOfInterveningDays() {
        var s = create(RepeatRule(RepeatKind.DAILY, start))
        val first = s.occurrences.first()
        s = act(s, TaskAction.Edit(first.id, first.fields.copy(date = start.plusDays(30)),
            s.definitions.single().rule, ChangeScope.ONLY_THIS))
        s = act(s, TaskAction.Refresh, start.plusDays(3))
        assertEquals(listOf(2, 3, 4), s.occurrences.filter { it.ordinal in 2..4 }.map { it.ordinal })
        assertEquals(start.plusDays(2), s.occurrences.first { it.ordinal == 3 }.dueDate)
    }

    @Test fun switchingFutureToRelativePreservesEarlierReviewWithoutBlockingCurrentSuccessor() {
        var s = create(RepeatRule(RepeatKind.DAILY, start), start.plusDays(2))
        val current = s.occurrences.first { it.ordinal == 3 }
        s = act(s, TaskAction.Edit(current.id, current.fields,
            s.definitions.single().rule.copy(kind = RepeatKind.AFTER_COMPLETION, interval = 2),
            ChangeScope.THIS_AND_FUTURE), start.plusDays(2))
        s = act(s, TaskAction.Resolve(current.id, true, start.plusDays(2)), start.plusDays(2))
        assertEquals(start.plusDays(4), s.activeOccurrences.first { it.ordinal == 4 }.dueDate)
        s = act(s, TaskAction.Resolve("r:1", false, start.plusDays(2)), start.plusDays(2))
        assertEquals(start.plusDays(4), s.activeOccurrences.first { it.ordinal == 4 }.dueDate)
        assertEquals(1, s.activeOccurrences.count { it.ordinal >= 4 })
    }

    @Test fun intervalCompletionUsesActualDateAndUndoGuardsTouchedSuccessor() {
        var s = create(RepeatRule(RepeatKind.AFTER_COMPLETION, start, interval = 2))
        assertEquals(1, s.activeOccurrences.size)
        s = act(s, TaskAction.Resolve("r:1", true, start), start.plusDays(5))
        assertEquals(start.plusDays(2), s.occurrences.first { it.ordinal == 2 }.dueDate)
        assertFalse(s.occurrences.first { it.ordinal == 2 }.checks.single().checked)
        val untouched = act(s, TaskAction.Undo("r:1"), start.plusDays(5))
        assertEquals(1, untouched.activeOccurrences.size)
        s = act(s, TaskAction.Check("r:2", 0, true), start.plusDays(5))
        assertThrows(TaskRuleException::class.java) { act(s, TaskAction.Undo("r:1"), start.plusDays(5)) }
    }

    @Test fun notDoneUsesPlannedAnchorEvenWhenNextIsOverdue() {
        val s = create(RepeatRule(RepeatKind.AFTER_COMPLETION, start, interval = 2))
        val result = act(s, TaskAction.Resolve("r:1", false, start.plusDays(10)), start.plusDays(10))
        val next = result.occurrences.first { it.ordinal == 2 }
        assertEquals(start.plusDays(2), next.dueDate)
        assertTrue(next.needsReview(start.plusDays(10)))
    }

    @Test fun deletedExceptionStillConsumesFiniteSlot() {
        var s = create(RepeatRule(RepeatKind.DAILY, start, count = 3))
        s = act(s, TaskAction.Delete("r:1", ChangeScope.ONLY_THIS))
        s = act(s, TaskAction.Refresh, start.plusDays(10))
        assertEquals(3, s.occurrences.size)
        assertEquals(2, s.activeOccurrences.size)
    }

    @Test fun allWeekdayAndFixedPatternsAreFunctional() {
        val monday = LocalDate.of(2026, 11, 2)
        val cases = listOf(
            RepeatRule(RepeatKind.WEEKDAYS, monday, count = 6) to monday.plusDays(7),
            RepeatRule(RepeatKind.WEEKLY, monday, weekdays = 1, count = 2) to monday.plusDays(7),
            RepeatRule(RepeatKind.CUSTOM, monday, weekdays = 1 or 4, count = 3) to monday.plusDays(7),
            RepeatRule(RepeatKind.FIXED_INTERVAL, monday, interval = 3, count = 3) to monday.plusDays(6),
        )
        cases.forEach { (rule, last) ->
            val s = create(rule, last)
            assertEquals(last, s.occurrences.last().dueDate)
            assertEquals(rule.count, s.occurrences.size)
        }
    }

    @Test fun weeklyPeriodCanRepeatWithoutAssigningAFixedCompletionDay() {
        val monday = LocalDate.of(2026, 11, 2)
        val s = TaskRules.apply(TaskSnapshot(), TaskAction.Create("weekly",
            TaskFields(title = "Weekly work", deadline = DeadlineKind.WEEK, date = monday),
            RepeatRule(RepeatKind.WEEKLY, monday, weekdays = 1, count = 2)), monday, 10)
        val first = s.occurrences.first()
        assertEquals(monday.plusDays(6), first.dueDate)
        assertTrue(first.inDay(monday.plusDays(3)))
        assertFalse(first.needsReview(monday.plusDays(6)))
        assertTrue(first.needsReview(monday.plusDays(7)))
        assertEquals(monday.plusDays(7), s.occurrences.last().periodStart)
    }
}
