package com.tabitos.app.features.reminders

import com.tabitos.app.core.reminders.domain.ReminderDefinition
import com.tabitos.app.core.reminders.domain.ReminderKind
import com.tabitos.app.core.reminders.domain.ReminderMode
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPresentationTest {
    private val today = LocalDate.of(2026, 11, 19)
    private val now = LocalDateTime.of(2026, 11, 19, 9, 41)
    private val timedTask = ReminderTarget(today, 18 * 60, now)
    private val dateOnlyTask = ReminderTarget(today.plusDays(1), null, now)
    private val relative = ReminderDefinition("r", kind = ReminderKind.TASK, sourceId = "t",
        mode = ReminderMode.RELATIVE, offsetMinutes = 30)

    @Test fun relativeReminderRingsBeforeTheDeadlineTime() {
        assertEquals(ReminderMoment.At(LocalDateTime.of(2026, 11, 19, 17, 30), past = false),
            timedTask.moment(relative))
    }

    @Test fun dateOnlyTaskCountsFromTheReferenceOrAsksForIt() {
        assertEquals(ReminderMoment.At(LocalDateTime.of(2026, 11, 19, 9, 0), past = true),
            dateOnlyTask.moment(relative.copy(offsetMinutes = 1440, referenceMinute = 9 * 60)))
        assertEquals(ReminderMoment.NeedsReference, dateOnlyTask.moment(relative))
        assertEquals(ReminderMoment.NeedsDeadline, ReminderTarget(now = now).moment(relative))
    }

    @Test fun onlyAPastChosenDateBlocksSaving() {
        val pastChosen = timedTask.moment(relative.copy(mode = ReminderMode.CHOSEN, chosenDate = today, minute = 540))
        assertFalse(pastChosen.canBeSaved(ReminderMode.CHOSEN))
        // A relative reminder already past for this occurrence still rings for the next ones of a series.
        val pastRelative = timedTask.moment(relative.copy(offsetMinutes = 600))
        assertTrue(pastRelative.canBeSaved(ReminderMode.RELATIVE))
        assertFalse(ReminderMoment.NeedsReference.canBeSaved(ReminderMode.RELATIVE))
    }

    @Test fun shortcutsHideOffsetsAlreadyUsedAndNeedADeadline() {
        assertEquals(listOf(0L, 60L, 1440L), timedTask.quickOffsets(listOf(relative)))
        assertEquals(emptyList<Long>(), ReminderTarget(now = now).quickOffsets(emptyList()))
    }

    @Test fun offsetsReadAsDaysHoursOrMinutes() {
        assertEquals(OffsetText.AtDeadline, offsetText(0))
        assertEquals(OffsetText.Days(2), offsetText(2880))
        assertEquals(OffsetText.Hours(1), offsetText(60))
        assertEquals(OffsetText.Minutes(90), offsetText(90))
        assertEquals(listOf(0L, 5L, 10L, 15L, 30L, 45L, 60L, 120L, 1440L, 2880L), offsetChoices(45))
    }

    @Test fun weekdayMasksStartOnMondayAndKeepOneDay() {
        assertEquals(WeekdayPreset.MONDAY_TO_FRIDAY, weekdayPreset(0b0011111))
        assertEquals(listOf(0, 2, 4), weekdayIndexes(0b0010101))
        assertEquals(0b0000100, 0b0000100.toggleWeekdayKeepingOne(2))
        assertEquals(0b0000101, 0b0000100.toggleWeekdayKeepingOne(0))
    }

    @Test fun draftSavesOnlyTheFieldsOfItsMode() {
        val draft = ReminderDraft(relative.copy(mode = ReminderMode.WEEKDAYS), date = today, minute = 21 * 60,
            offsetMinutes = 30, referenceMinute = 540)
        val saved = draft.asDefinition()
        assertEquals(null, saved.chosenDate)
        assertEquals(0L, saved.offsetMinutes)
        assertEquals(null, saved.referenceMinute)
        assertEquals(21 * 60, saved.minute)
    }
}
