package com.ethos.app.features.tasks

import com.ethos.app.core.tasks.domain.*
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

    @Test fun advancedOptionsOnlyForNonDefaultFields() {
        val draft = TaskDraft("d")
        assertFalse(hasAdvancedOptions(draft))
        assertTrue(hasAdvancedOptions(draft.copy(fields = TaskFields(description = "x"))))
        assertTrue(hasAdvancedOptions(draft.copy(fields = TaskFields(checklist = listOf("a")))))
        assertTrue(hasAdvancedOptions(draft.copy(fields = TaskFields(difficulty = 2))))
        assertTrue(hasAdvancedOptions(draft.copy(rule = RepeatRule(kind = RepeatKind.DAILY))))
        assertTrue(hasAdvancedOptions(draft.copy(ending = 2)))
        assertFalse(hasAdvancedOptions(draft.copy(fields = TaskFields(checklist = listOf(" ")))))
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
        assertEquals(listOf("a", "c", "b"), moveChecklistItem(items, 2, -1))
        assertEquals(items, moveChecklistItem(items, 0, -1))
    }

    @Test fun pickerMillisKeepTheCalendarDay() {
        listOf(LocalDate.of(2026, 1, 1), today, LocalDate.of(2028, 2, 29)).forEach {
            assertEquals(it, pickerMillisToDate(it.toPickerMillis()))
        }
    }
}
