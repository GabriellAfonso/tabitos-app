package com.ethos.app.core.reminders

import com.ethos.app.core.reminders.domain.*
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.habits.domain.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ReminderRulesTest {
    private val monday = LocalDate.of(2026, 11, 2)
    private val zone = ZoneId.of("UTC")
    private val task = TaskOccurrence("t:1", "t", 1,
        TaskFields(title = "Test", deadline = DeadlineKind.DAY, date = monday, minute = 600),
        monday, monday, monday)
    private fun sources(o: TaskOccurrence = task) = ReminderSources(TaskSnapshot(occurrences = listOf(o)),
        HabitSnapshot())
    private fun definition() = ReminderDefinition("r", kind = ReminderKind.TASK, sourceId = "t",
        occurrenceId = "t:1", mode = ReminderMode.RELATIVE, offsetMinutes = 30)

    @Test fun relativeKeepsDeadlineAndUsesExplicitReference() {
        val s = sources(task.copy(fields = task.fields.copy(minute = null)))
        assertTrue(ReminderRules.schedule(s, listOf(definition()), monday, zone).isEmpty())
        val rows = ReminderRules.schedule(s, listOf(definition().copy(referenceMinute = 600)), monday, zone)
        assertEquals(ReminderRules.instant(monday, 570, zone), rows.single().dueAt)
        assertNull(s.tasks.occurrences.single().fields.minute)
    }
    @Test fun scopeOverrideSuppressesOnlyOneOccurrence() {
        val base = definition().copy(occurrenceId = null)
        val override = base.copy(id = "exception", occurrenceId = "t:1", enabled = false)
        assertTrue(ReminderRules.effective(listOf(base, override), task).isEmpty())
        assertEquals(base, ReminderRules.effective(listOf(base, override), task.copy(id = "t:2", ordinal = 2)).single())
    }
    @Test fun weeklyCuesStayInsideActualPeriod() {
        val weekly = task.copy(periodStart = monday, dueDate = monday.plusDays(4),
            fields = task.fields.copy(deadline = DeadlineKind.WORK_WEEK, minute = null))
        val rows = ReminderRules.schedule(sources(weekly), listOf(definition().copy(
            mode = ReminderMode.WEEKDAYS, weekdays = 127)), monday, zone)
        assertEquals(5, rows.size)
        assertEquals(monday.plusDays(4), rows.last().slotDate)
    }
    @Test fun snoozeReplacesSameIdentityAndRejectsPast() {
        val row = ReminderRules.schedule(sources(), listOf(definition()), monday, zone).single()
        val first = ReminderRules.snooze(row.copy(state = DeliveryState.POSTED), 2000, 1000)
        val second = ReminderRules.snooze(first, 3000, 1000)
        assertEquals(row.key, second.key)
        assertEquals(3000, second.dueAt)
        assertEquals(DeliveryState.PENDING, second.state)
        assertThrows(IllegalArgumentException::class.java) { ReminderRules.snooze(row, 1000, 1000) }
    }
    @Test fun resolvedTaskNeverRelevantEvenWhenSnoozed() {
        val row = ReminderRules.schedule(sources(), listOf(definition()), monday, zone).single()
        assertFalse(ReminderRules.relevant(row.copy(snoozed = true),
            sources(task.copy(status = TaskStatus.DONE)), monday))
    }
    @Test fun dstGapAndOverlapHaveOneDeterministicInstant() {
        val berlin = ZoneId.of("Europe/Berlin")
        assertEquals(Instant.parse("2026-03-29T01:30:00Z").toEpochMilli(),
            ReminderRules.instant(LocalDate.of(2026, 3, 29), 150, berlin))
        assertEquals(Instant.parse("2026-10-25T00:30:00Z").toEpochMilli(),
            ReminderRules.instant(LocalDate.of(2026, 10, 25), 150, berlin))
    }
    private fun habits(type: HabitType, records: Int = 1): HabitSnapshot = HabitSnapshot(
        habits = listOf(Habit("h", monday, "Test")),
        rules = listOf(HabitRule("rule", "h", monday, type, HabitUnit.WEEK, 1)),
        intervals = listOf(ActiveInterval("a", "h", monday)),
        records = (1..records).map { HabitRecord("$it", "h", monday, it.toLong()) })

    @Test fun practiceReachedCancelsButLimitExceededContinues() {
        val d = ReminderDefinition("h-r", kind = ReminderKind.HABIT, sourceId = "h",
            mode = ReminderMode.WEEKDAYS)
        val practice = sources().copy(habits = habits(HabitType.PRACTICE))
        assertFalse(ReminderRules.schedule(practice, listOf(d), monday, zone).any { it.slotDate == monday })
        val limit = sources().copy(habits = habits(HabitType.LIMIT, 2))
        val row = ReminderRules.schedule(limit, listOf(d), monday, zone).first()
        assertTrue(ReminderRules.relevant(row, limit, monday))
        assertFalse(ReminderRules.relevant(row, limit.copy(habits = limit.habits.copy(habits =
            listOf(limit.habits.habits.single().copy(status = HabitStatus.PAUSED)))), monday))
    }
    @Test fun summaryUsesRealTodayAndPendingReview() {
        val s = sources().copy(tasks = TaskSnapshot(occurrences = listOf(task,
            task.copy(id = "old", dueDate = monday.minusDays(1)),
            task.copy(id = "done", status = TaskStatus.DONE))), habits = habits(HabitType.LIMIT))
        assertEquals(DaySummary(1, 1, 1), ReminderRules.summary(s, monday))
        assertEquals(DaySummary(0, 0, 0),
            ReminderRules.summary(ReminderSources(TaskSnapshot(), HabitSnapshot()), monday))
    }
    @Test fun monthlyProjectionPreservesClampAndFiniteEnd() {
        val rule = RepeatRule(RepeatKind.MONTHLY, LocalDate.of(2026, 1, 31), monthDay = 31, count = 3)
        val snapshot = TaskRules.apply(TaskSnapshot(), TaskAction.Create("monthly",
            task.fields.copy(date = rule.start), rule), rule.start, 0)
        val s = ReminderSources(snapshot, HabitSnapshot())
        val projected = ReminderRules.project(s, listOf(definition().copy(offsetMinutes = 100000)), rule.start)
        assertEquals(listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31)),
            projected.tasks.occurrences.map { it.plannedDate })
        assertEquals(snapshot, s.tasks)
    }
    @Test fun repeatedRecoveryKeepsTerminalSlotsAndDoesNotRepostBacklog() {
        val def = definition()
        val row = ReminderRules.schedule(sources(), listOf(def), monday, zone).single()
        val now = row.dueAt + 3600000
        var rows = listOf(row.copy(state = DeliveryState.RECOVERED))
        repeat(3) { rows = ReminderRules.reconcileRows(listOf(row), rows, listOf(def), sources(), now, zone) }
        assertEquals(1, rows.size)
        assertTrue(rows.none { it.state == DeliveryState.PENDING })
        val canceled = ReminderRules.reconcileRows(emptyList(), rows, emptyList(), sources(), now, zone)
        assertEquals(DeliveryState.CANCELLED, canceled.single().state)
    }
    @Test fun reopenedPracticeResumesOnlyFutureSlotsAndPreservesSnoozeInstant() {
        val d = ReminderDefinition("h-r", kind = ReminderKind.HABIT, sourceId = "h",
            mode = ReminderMode.WEEKDAYS)
        val s = sources().copy(habits = habits(HabitType.PRACTICE, 0))
        val rows = ReminderRules.schedule(s, listOf(d), monday, zone).filter { it.slotDate <= monday.plusDays(1) }
        val now = ReminderRules.instant(monday, 600, zone)
        val canceled = rows.map { it.copy(state = DeliveryState.CANCELLED) }
        val restored = ReminderRules.reconcileRows(rows, canceled, listOf(d), s, now, zone)
        assertEquals(DeliveryState.CANCELLED, restored.first { it.slotDate == monday }.state)
        assertEquals(DeliveryState.PENDING, restored.first { it.slotDate == monday.plusDays(1) }.state)
        val snoozed = rows.last().copy(snoozed = true, dueAt = now + 999999)
        val changed = rows.last().copy(dueAt = now + 888888)
        assertEquals(snoozed.dueAt, ReminderRules.reconcileRows(listOf(changed), listOf(snoozed),
            listOf(d), s, now, zone).single().dueAt)
    }
}
