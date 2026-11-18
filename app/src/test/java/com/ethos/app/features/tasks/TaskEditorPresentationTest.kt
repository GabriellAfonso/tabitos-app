package com.ethos.app.features.tasks

import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.ui.components.pickerMillisToDate
import com.ethos.app.core.ui.components.toPickerMillis
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskEditorPresentationTest {
    private val today = LocalDate.of(2026, 11, 10) // Tuesday

    @Test fun everyKindKeepsItsRuleWhenItsOwnChoiceIsSelected() {
        RepeatKind.entries.forEach { kind ->
            val rule = RepeatRule(kind = kind, start = today, weekdays = 0b0000110, interval = 4)
            assertEquals(kind.name, rule, ruleForChoice(rule, repeatChoiceOf(kind), today))
        }
    }

    @Test fun choicesMapOntoExistingKinds() {
        val none = RepeatRule(start = today)
        assertEquals(RepeatKind.DAILY, ruleForChoice(none, RepeatChoice.DAILY, today).kind)
        assertEquals(RepeatKind.MONTHLY, ruleForChoice(none, RepeatChoice.MONTHLY, today).kind)
        assertEquals(RepeatKind.FIXED_INTERVAL, ruleForChoice(none, RepeatChoice.INTERVAL, today).kind)
        val weekly = ruleForChoice(none, RepeatChoice.WEEKLY, today)
        assertEquals(RepeatKind.WEEKLY, weekly.kind)
        assertEquals(1 shl 1, weekly.weekdays) // Tuesday
        assertEquals(RepeatKind.NONE, ruleForChoice(weekly, RepeatChoice.NONE, today).kind)
        assertEquals(RepeatChoice.DAILY, repeatChoiceOf(RepeatKind.WEEKDAYS))
        assertEquals(RepeatChoice.WEEKLY, repeatChoiceOf(RepeatKind.CUSTOM))
        assertEquals(RepeatChoice.INTERVAL, repeatChoiceOf(RepeatKind.AFTER_COMPLETION))
    }

    @Test fun weekdaySelectionChoosesKindAndNeverEmpties() {
        assertEquals(RepeatKind.WEEKLY, weeklyKind(0b1))
        assertEquals(RepeatKind.CUSTOM, weeklyKind(0b11111))
        assertEquals(0b101, toggleWeekday(0b1, 2))
        assertEquals(0b1, toggleWeekday(0b1, 0))
        assertEquals(0b100, toggleWeekday(0b101, 0))
    }

    @Test fun sheetVisibilityFollowsVariantAndRepetition() {
        val draft = TaskDraft("d")
        assertTrue(draft.showsChecklistRow)
        assertFalse(draft.showsDurationChip)
        assertFalse(draft.showsEndingRow)
        val timed = draft.copy(timed = true, fields = TaskFields(checklist = listOf("a")))
        assertFalse(timed.showsChecklistRow)
        assertTrue(timed.showsDurationChip)
        assertTrue(timed.showsTimedChecklistWarning)
        assertFalse(draft.copy(timed = true).showsTimedChecklistWarning)
        val recurring = draft.copy(rule = RepeatRule(kind = RepeatKind.DAILY))
        assertTrue(recurring.showsEndingRow)
        assertTrue(recurring.editsStartDate)
        assertFalse(recurring.copy(editing = true).editsStartDate)
        assertEquals(DeadlineKind.entries, deadlineKindsFor(draft))
        assertFalse(DeadlineKind.NONE in deadlineKindsFor(recurring))
    }

    @Test fun deadlineChoiceKeepsDateAndTimeRules() {
        val draft = TaskDraft("d", dateText = "", timeText = "09:00")
        val day = withDeadline(draft, DeadlineKind.DAY, today)
        assertEquals(today.toString(), day.dateText)
        assertEquals("09:00", day.timeText)
        assertEquals("", withDeadline(day, DeadlineKind.WEEK, today).timeText)
        val none = withDeadline(day, DeadlineKind.NONE, today)
        assertEquals("", none.dateText)
        assertEquals(DeadlineKind.NONE, none.fields.deadline)
    }

    @Test fun repeatChoiceMovesTheChosenDayBetweenDateAndStart() {
        val friday = today.plusDays(3)
        val single = TaskDraft("d", dateText = friday.toString(), startText = today.toString())
        assertEquals(friday.toString(), withRepeatChoiceDates(single, RepeatChoice.DAILY).startText)
        assertEquals(1 shl 4, ruleForRepeatChoice(single, RepeatChoice.WEEKLY, today).weekdays) // Friday
        val recurring = TaskDraft("d", rule = RepeatRule(kind = RepeatKind.DAILY, start = friday),
            startText = friday.toString())
        assertEquals(friday.toString(), withRepeatChoiceDates(recurring, RepeatChoice.NONE).dateText)
        assertEquals(recurring, withRepeatChoiceDates(recurring.copy(), RepeatChoice.WEEKLY))
        val editing = single.copy(editing = true)
        assertEquals(editing, withRepeatChoiceDates(editing, RepeatChoice.DAILY))
    }

    @Test fun reviewConfirmationOnlyForOverdueDateChange() {
        val past = today.minusDays(2)
        val fields = TaskFields(title = "t", deadline = DeadlineKind.DAY, date = past)
        val overdue = TaskOccurrence("o", "t", 1, fields, past, past, past)
        val draft = TaskDraft("o", editing = true, fields = fields, dateText = past.toString())
        assertFalse(needsReviewConfirmation(draft, overdue, today))
        assertTrue(needsReviewConfirmation(draft.copy(dateText = today.toString()), overdue, today))
        assertTrue(needsReviewConfirmation(draft.copy(fields = fields.copy(deadline = DeadlineKind.NONE)), overdue,
            today))
        val future = overdue.copy(dueDate = today.plusDays(1), fields = fields.copy(date = today.plusDays(1)))
        assertFalse(needsReviewConfirmation(draft.copy(dateText = today.plusDays(3).toString()), future, today))
        assertFalse(needsReviewConfirmation(draft.copy(editing = false, dateText = today.toString()), overdue, today))
    }

    @Test fun checklistHelpers() {
        val items = listOf("a", "b", "c")
        assertEquals(listOf("a", "b", "c", ""), addChecklistItem(items))
        assertEquals(listOf("a", "", "b", "c"), addChecklistItem(items, 0))
        assertEquals(listOf(""), addChecklistItem(emptyList()))
        assertEquals(listOf("a", "c"), removeChecklistItem(items, 1))
    }

    @Test fun pickerMillisKeepTheCalendarDay() {
        listOf(LocalDate.of(2026, 1, 1), today, LocalDate.of(2028, 2, 29)).forEach {
            assertEquals(it, pickerMillisToDate(it.toPickerMillis()))
        }
    }
}
