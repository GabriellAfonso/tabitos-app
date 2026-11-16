package com.ethos.app.core.reminders

import com.ethos.app.core.habits.domain.HabitSnapshot
import com.ethos.app.core.reminders.domain.ReminderDefinition
import com.ethos.app.core.reminders.domain.ReminderKind
import com.ethos.app.core.reminders.domain.ReminderMode
import com.ethos.app.core.reminders.domain.ReminderRules
import com.ethos.app.core.reminders.domain.ReminderSources
import com.ethos.app.core.tasks.domain.DeadlineKind
import com.ethos.app.core.tasks.domain.TaskFields
import com.ethos.app.core.tasks.domain.TaskOccurrence
import com.ethos.app.core.tasks.domain.TaskSnapshot
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPostponeTest {
    private val monday = LocalDate.of(2026, 11, 16)
    private val zone = ZoneId.of("UTC")
    private val task = TaskOccurrence("t:3", "t", 3,
        TaskFields(title = "Regar", deadline = DeadlineKind.DAY, date = monday, minute = 600),
        monday, monday, monday)
    private val fixed = ReminderDefinition("g:t:3", groupId = "g", kind = ReminderKind.TASK, sourceId = "t",
        occurrenceId = "t:3", fromOrdinal = 3, mode = ReminderMode.CHOSEN, chosenDate = monday, minute = 9 * 60)
    private val series = fixed.copy(id = "g:from:1", occurrenceId = null, fromOrdinal = 1, chosenDate = null,
        anchorDay = -1, minute = 20 * 60)

    @Test fun occurrenceFixedReminderMovesByTheSameDaysKeepingTheTime() {
        val moved = ReminderRules.postponeChosen(listOf(fixed), task, 2, recurring = false).single()
        assertEquals(monday.plusDays(2), moved.chosenDate)
        assertEquals(9 * 60, moved.minute)
        assertEquals(fixed.id, moved.id)
    }

    @Test fun recurringSeriesRuleGetsAnOccurrenceCopyKeepingItsDistance() {
        val copy = ReminderRules.postponeChosen(listOf(series), task, 2, recurring = true).single()
        assertEquals("g:t:3", copy.id)
        assertEquals("t:3", copy.occurrenceId)
        assertEquals(monday.plusDays(1), copy.chosenDate)
        val postponed = task.copy(fields = task.fields.copy(date = monday.plusDays(2)), dueDate = monday.plusDays(2))
        val rows = ReminderRules.schedule(ReminderSources(TaskSnapshot(occurrences = listOf(postponed)),
            HabitSnapshot()), listOf(series, copy), monday, zone)
        assertEquals(ReminderRules.instant(monday.plusDays(1), 20 * 60, zone), rows.single().dueAt)
    }

    @Test fun nonRecurringSeriesRelativeAndWeekdayRemindersAreLeftAlone() {
        val relative = fixed.copy(id = "r", groupId = "r", mode = ReminderMode.RELATIVE, chosenDate = null)
        val weekdays = fixed.copy(id = "w", groupId = "w", mode = ReminderMode.WEEKDAYS, chosenDate = null)
        assertTrue(ReminderRules.postponeChosen(listOf(series, relative, weekdays), task, 2, recurring = false)
            .isEmpty())
    }

    @Test fun undoShiftsBack() {
        val moved = ReminderRules.postponeChosen(listOf(fixed), task, 3, recurring = false).single()
        val back = ReminderRules.postponeChosen(listOf(moved), task, -3, recurring = false).single()
        assertEquals(monday, back.chosenDate)
    }
}
