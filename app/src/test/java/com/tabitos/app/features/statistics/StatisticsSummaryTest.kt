package com.tabitos.app.features.statistics

import com.tabitos.app.core.accounting.domain.AccountingSnapshot
import com.tabitos.app.core.accounting.domain.LedgerEntry
import com.tabitos.app.core.habits.domain.Habit
import com.tabitos.app.core.habits.domain.HabitAction
import com.tabitos.app.core.habits.domain.HabitRule
import com.tabitos.app.core.habits.domain.HabitRules
import com.tabitos.app.core.habits.domain.HabitSnapshot
import com.tabitos.app.core.habits.domain.HabitStatus
import com.tabitos.app.core.habits.domain.HabitType
import com.tabitos.app.core.tasks.domain.AuditKind
import com.tabitos.app.core.tasks.domain.DeadlineKind
import com.tabitos.app.core.tasks.domain.TaskAudit
import com.tabitos.app.core.tasks.domain.TaskFields
import com.tabitos.app.core.tasks.domain.TaskOccurrence
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import com.tabitos.app.core.tasks.domain.TaskStatus
import com.tabitos.app.core.timers.domain.TimerKind
import com.tabitos.app.core.timers.domain.TimerSegment
import com.tabitos.app.core.timers.domain.TimerSession
import com.tabitos.app.core.timers.domain.TimerSnapshot
import com.tabitos.app.core.timers.domain.TimerState
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatisticsSummaryTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 18)
    private val week = StatisticsPeriod.current(PeriodKind.WEEK, today).range(today, today)
    private fun millis(day: LocalDate, hour: Int = 12) = day.atTime(hour, 0).toInstant(zone).toEpochMilli()

    private fun task(
        id: String, done: LocalDate?, due: LocalDate?, deadline: DeadlineKind = DeadlineKind.DAY,
        status: TaskStatus = if (done != null) TaskStatus.DONE else TaskStatus.OPEN,
    ) = TaskOccurrence(id, id, 1, TaskFields(title = id, deadline = if (due == null) DeadlineKind.NONE else deadline,
        date = due), due, due, due, status, completedDate = done)

    @Test fun completedAndOnTimeCountOnlyTasksWithADeadline() {
        val monday = week.start
        val tasks = TaskSnapshot(occurrences = listOf(
            task("early", done = monday, due = monday.plusDays(1)),
            task("late", done = monday.plusDays(2), due = monday),
            task("weekWindow", done = monday.plusDays(1), due = monday.plusDays(6), deadline = DeadlineKind.WEEK),
            task("noDate", done = monday, due = null),
            task("deleted", done = monday, due = monday, status = TaskStatus.DELETED),
            task("open", done = null, due = monday),
        ))
        val summary = taskSummary(tasks, week, BucketUnit.DAY, today, zone)
        assertEquals(4, summary.completed)
        assertEquals(66, summary.onTimePercent)
    }

    @Test fun noCompletedTaskWithDeadlineHasNoPercentage() {
        val tasks = TaskSnapshot(occurrences = listOf(task("noDate", done = today, due = null)))
        assertNull(taskSummary(tasks, week, BucketUnit.DAY, today, zone).onTimePercent)
    }

    @Test fun postponedCountsOnlyPostponeRecordsOfThePeriod() {
        val history = listOf(
            TaskAudit("1", "x", AuditKind.POSTPONED, millis(today)),
            TaskAudit("2", "x", AuditKind.POSTPONED, millis(week.start.minusDays(1))),
            TaskAudit("3", "x", AuditKind.EDITED, millis(today)),
            TaskAudit("4", "x", AuditKind.POSTPONE_UNDONE, millis(today)),
        )
        assertEquals(1, taskSummary(TaskSnapshot(history = history), week, BucketUnit.DAY, today, zone).postponed)
    }

    @Test fun weekChartHasSevenDaysWithFutureDaysMarked() {
        val tasks = TaskSnapshot(occurrences = listOf(task("a", done = today, due = today)))
        val buckets = taskSummary(tasks, week, BucketUnit.DAY, today, zone).buckets
        assertEquals(7, buckets.size)
        assertEquals(1, buckets.single { it.start == today }.count)
        assertEquals(listOf(false, false, false, true, true, true, true), buckets.map { it.future })
    }

    @Test fun yearChartGroupsByMonth() {
        val year = StatisticsPeriod.current(PeriodKind.YEAR, today).range(today, today)
        val tasks = TaskSnapshot(occurrences = listOf(task("a", today, today), task("b", today.withDayOfMonth(2),
            today.withDayOfMonth(2)), task("c", LocalDate.of(2026, 2, 3), LocalDate.of(2026, 2, 3))))
        val buckets = taskSummary(tasks, year, BucketUnit.MONTH, today, zone).buckets
        assertEquals(12, buckets.size)
        assertEquals(2, buckets.single { it.start == LocalDate.of(2026, 11, 1) }.count)
        assertEquals(1, buckets.single { it.start == LocalDate.of(2026, 2, 1) }.count)
    }

    private fun habits(): HabitSnapshot {
        val start = today.minusDays(20)
        var s = HabitRules.apply(HabitSnapshot(), HabitAction.Save(Habit("h", start, "Ler"),
            HabitRule("r", "h", start)), start, 0, emptySet())
        s = HabitRules.apply(s, HabitAction.Save(Habit("bad", start, "Refrigerante"),
            HabitRule("r2", "bad", start, type = HabitType.AVOID)), start, 0, emptySet())
        (0L..20L step 2).forEach { offset ->
            val day = start.plusDays(offset)
            s = HabitRules.apply(s, HabitAction.Record("rec$offset", "h", day), day, 0, emptySet())
        }
        return s
    }

    @Test fun habitNumbersUseTheCardRules() {
        val s = habits()
        val all = StatisticsPeriod.current(PeriodKind.ALL, today).range(today.minusDays(20), today)
        val summary = habitSummary(s, all, today)
        val reading = summary.items.single { it.id == "h" }
        assertEquals(HabitRules.successRate(s, "h", today), reading.ratePercent)
        assertEquals(HabitRules.streak(s, "h", today).current, reading.streak)
        assertEquals(true, summary.items.single { it.id == "bad" }.bad)
    }

    @Test fun excludedHabitsAreNotListed() {
        val s = HabitRules.apply(habits(), HabitAction.Status("h", HabitStatus.EXCLUDED),
            today, 0, emptySet())
        assertEquals(listOf("bad"), habitSummary(s, week, today).items.map { it.id })
    }

    @Test fun timeCountsClosedTaskAndHabitStretchesButNotRewards() {
        val sessions = listOf(
            TimerSession("t", TimerKind.TASK, "task:1", "Estudar", TimerState.PAUSED, created = 0),
            TimerSession("r", TimerKind.REWARD, "reward", "Série", TimerState.FINISHED, created = 0),
            TimerSession("h", TimerKind.HABIT, "habit", "Ler", TimerState.RUNNING, created = 0),
        )
        val segments = listOf(
            TimerSegment("1", "t", millis(today, 10), millis(today, 11), created = 0),
            TimerSegment("2", "r", millis(today, 12), millis(today, 13), created = 0),
            TimerSegment("3", "h", millis(today, 14), null, startElapsed = 0, created = 0),
            TimerSegment("4", "t", millis(today, 15), millis(today, 16), active = false, created = 0),
        )
        val summary = timeSummary(creditedTime(TimerSnapshot(sessions, segments), zone), week)
        assertEquals(3_600_000L, summary.totalMillis)
        assertEquals(listOf("Estudar"), summary.items.map { it.title })
    }

    @Test fun timeAcrossMidnightIsSplitBetweenDays() {
        val session = TimerSession("t", TimerKind.TASK, "task:1", "Estudar", TimerState.PAUSED, created = 0)
        val segment = TimerSegment("1", "t", millis(today, 23), millis(today.plusDays(1), 1), created = 0)
        val credited = creditedTime(TimerSnapshot(listOf(session), listOf(segment)), zone)
        assertEquals(listOf(3_600_000L, 3_600_000L), credited.map { it.millis })
    }

    @Test fun coinsEarnedAreGainsAndSpentArePurchases() {
        fun entry(seq: Long, kind: String, coins: Long, day: LocalDate = today) = LedgerEntry(seq, "k$seq", "t",
            day, millis(day), kind, coins, coins, 0, 0, 0)
        val wallet = AccountingSnapshot(entries = listOf(entry(1, "GAIN", 10), entry(2, "REVERSAL", -10),
            entry(3, "REAPPLY", 10), entry(4, "PURCHASE", -30), entry(5, "GAIN", 50, week.start.minusDays(1)),
            entry(6, "PENALTY", -5)))
        val summary = coinSummary(wallet, week, zone)
        assertEquals(20L, summary.earned)
        assertEquals(30L, summary.spent)
    }
}
