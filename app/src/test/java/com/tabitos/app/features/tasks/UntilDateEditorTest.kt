package com.tabitos.app.features.tasks

import com.tabitos.app.core.tasks.domain.DeadlineKind
import com.tabitos.app.core.tasks.domain.RepeatKind
import com.tabitos.app.core.tasks.domain.RepeatRule
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Editor choices for the "Até uma data" deadline (spec 035). */
class UntilDateEditorTest {
    private val today = LocalDate.of(2026, 11, 19)
    private val draft = TaskDraft("t", dateText = "2026-11-28", startText = "2026-11-10", timeText = "18:00")

    @Test fun offeredOnlyForSingleTasks() {
        assertTrue(DeadlineKind.UNTIL_DATE in deadlineKindsFor(draft))
        val recurring = draft.copy(rule = RepeatRule(kind = RepeatKind.DAILY, start = today))
        assertFalse(DeadlineKind.UNTIL_DATE in deadlineKindsFor(recurring))
    }

    @Test fun choosingItKeepsTheEndAndTimeAndStartsToday() {
        val window = withDeadline(draft, DeadlineKind.UNTIL_DATE, today)
        assertEquals("2026-11-28", window.dateText)
        assertEquals(today.toString(), window.startText)
        assertEquals("18:00", window.timeText)
        assertFalse(window.allowsRepeat)
    }

    @Test fun pastEndStartsTheWindowOnTheEnd() {
        val window = withDeadline(draft.copy(dateText = "2026-11-15"), DeadlineKind.UNTIL_DATE, today)
        assertEquals("2026-11-15", window.startText)
    }

    @Test fun reselectingKeepsTheChosenStart() {
        val window = withDeadline(draft, DeadlineKind.UNTIL_DATE, today).copy(startText = "2026-11-24")
        assertEquals("2026-11-24", withDeadline(window, DeadlineKind.UNTIL_DATE, today).startText)
    }

    @Test fun leavingItForAWeekDropsTheTime() {
        val window = withDeadline(draft, DeadlineKind.UNTIL_DATE, today)
        assertEquals("", withDeadline(window, DeadlineKind.WEEK, today).timeText)
        assertEquals("18:00", withDeadline(window, DeadlineKind.DAY, today).timeText)
        assertTrue(withDeadline(window, DeadlineKind.DAY, today).allowsRepeat)
    }

    @Test fun startStaysFreeWhileTheEndIsStillTheDefault() {
        val fresh = withDeadline(TaskDraft("n", dateText = today.toString(), startText = today.toString()),
            DeadlineKind.UNTIL_DATE, today)
        assertEquals(today, untilDateLimits(fresh, today).earliestStart)
        val later = withUntilDateStart(fresh, LocalDate.of(2026, 11, 24))
        assertEquals("2026-11-24", later.startText)
        assertEquals("2026-11-24", later.dateText)
        assertEquals(LocalDate.of(2026, 11, 24), untilDateLimits(later, today).earliestEnd)
    }

    @Test fun newWeekDeadlineMeansThisWeekAndNeedsNoDetails() {
        val day = withDeadline(draft, DeadlineKind.DAY, today)
        val week = withDeadline(day, DeadlineKind.WEEK, today)
        assertEquals(today.toString(), week.dateText)
        assertEquals(today, weekReference(week, DeadlineKind.WEEK, today))
        assertFalse(week.showsDeadlineDetails)
        assertTrue(week.copy(editing = true).showsDeadlineDetails)
        assertTrue(day.showsDeadlineDetails)
        assertFalse(withDeadline(day, DeadlineKind.NONE, today).showsDeadlineDetails)
    }

    @Test fun onlyEnteringUntilDateOpensItsEndPicker() {
        assertTrue(opensUntilDatePicker(DeadlineKind.DAY, DeadlineKind.UNTIL_DATE))
        assertFalse(opensUntilDatePicker(DeadlineKind.UNTIL_DATE, DeadlineKind.UNTIL_DATE))
        assertFalse(opensUntilDatePicker(DeadlineKind.NONE, DeadlineKind.DAY))
    }

    @Test fun startBeforeTheEndKeepsTheEnd() {
        val moved = withUntilDateStart(draft, LocalDate.of(2026, 11, 22))
        assertEquals("2026-11-28", moved.dateText)
        assertEquals(null, untilDateLimits(draft.copy(editing = true), today).earliestStart)
    }
}
