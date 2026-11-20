package com.tabitos.app.core.reminders

import com.tabitos.app.core.preferences.TaskReminderDefaults
import com.tabitos.app.core.habits.domain.HabitSnapshot
import com.tabitos.app.core.reminders.domain.ReminderRules
import com.tabitos.app.core.reminders.domain.ReminderSources
import com.tabitos.app.core.reminders.domain.defaultTaskReminders
import com.tabitos.app.core.tasks.domain.DeadlineKind
import com.tabitos.app.core.tasks.domain.RepeatKind
import com.tabitos.app.core.tasks.domain.RepeatRule
import com.tabitos.app.core.tasks.domain.TaskAction
import com.tabitos.app.core.tasks.domain.TaskFields
import com.tabitos.app.core.tasks.domain.TaskRules
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultTaskRemindersTest {
    private val day = LocalDate.of(2026, 11, 20)
    private val now = day.atTime(9, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private val task = TaskAction.Create("t", TaskFields(title = "Consulta", deadline = DeadlineKind.DAY,
        date = day, minute = 15 * 60), RepeatRule(start = day))
    private val both = TaskReminderDefaults(setOf(30, 1440), dateOnlyMinute = 9 * 60)

    @Test fun reminderSchedulerReceivesTheExpectedLocalTimesAndSkipsCompletedTasks() {
        val tomorrow = day.plusDays(1)
        val create = task.copy(fields = task.fields.copy(date = tomorrow))
        val reminders = defaultTaskReminders(create, TaskReminderDefaults(), now, ZoneOffset.UTC)
        val created = TaskRules.apply(TaskSnapshot(), create, day, now)
        fun deliveries(snapshot: TaskSnapshot) = ReminderRules.schedule(
            ReminderSources(snapshot, HabitSnapshot()), reminders, day, ZoneOffset.UTC,
        )
        assertEquals(tomorrow.atTime(14, 30).toInstant(ZoneOffset.UTC).toEpochMilli(),
            deliveries(created).single().dueAt)
        val moved = TaskRules.apply(created, TaskAction.Reschedule("t:1", tomorrow, day), day, now)
        assertEquals(day.atTime(14, 30).toInstant(ZoneOffset.UTC).toEpochMilli(), deliveries(moved).single().dueAt)
        val completed = TaskRules.apply(moved, TaskAction.SetCompleted("t:1", true, day), day, now)
        assertTrue(deliveries(completed).isEmpty())
    }

    @Test fun untilDateWindowIsRemindedBeforeItsEnd() {
        val end = day.plusDays(8)
        val window = TaskAction.Create("w", TaskFields(title = "Relatório", deadline = DeadlineKind.UNTIL_DATE,
            date = end, minute = 18 * 60), RepeatRule(start = day))
        val reminders = defaultTaskReminders(window, TaskReminderDefaults(), now, ZoneOffset.UTC)
        val created = TaskRules.apply(TaskSnapshot(), window, day, now)
        val delivery = ReminderRules.schedule(ReminderSources(created, HabitSnapshot()), reminders, day,
            ZoneOffset.UTC).single()
        assertEquals(end.atTime(17, 30).toInstant(ZoneOffset.UTC).toEpochMilli(), delivery.dueAt)
    }

    @Test fun skipsPastOffsetsOnSingleTaskButKeepsRulesForLaterOccurrences() {
        val single = defaultTaskReminders(task, both, now, ZoneOffset.UTC)
        assertEquals(listOf(30L), single.map { it.offsetMinutes })
        val series = task.copy(rule = RepeatRule(kind = RepeatKind.DAILY, start = day))
        val rules = defaultTaskReminders(series, both, now, ZoneOffset.UTC)
        assertEquals(setOf(30L, 1440L), rules.map { it.offsetMinutes }.toSet())
        assertEquals(2, rules.distinctBy { it.groupId }.size)
        assertTrue(rules.all { it.occurrenceId == null && it.createdAt == now })
    }

    @Test fun dateOnlyNeedsOptInAndWeekWindowsNeverGetASpecificTime() {
        val dateOnly = task.copy(fields = task.fields.copy(date = day.plusDays(1), minute = null))
        assertTrue(defaultTaskReminders(dateOnly, TaskReminderDefaults(), now, ZoneOffset.UTC).isEmpty())
        val rule = defaultTaskReminders(dateOnly, both, now, ZoneOffset.UTC).single()
        assertEquals(0L, rule.offsetMinutes)
        assertEquals(540, rule.referenceMinute)
        for (kind in listOf(DeadlineKind.WEEK, DeadlineKind.WORK_WEEK, DeadlineKind.NONE)) {
            val other = task.copy(fields = task.fields.copy(deadline = kind))
            assertTrue(defaultTaskReminders(other, both, now, ZoneOffset.UTC).isEmpty())
        }
    }
}
