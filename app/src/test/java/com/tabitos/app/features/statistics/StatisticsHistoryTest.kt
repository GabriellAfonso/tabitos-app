package com.tabitos.app.features.statistics

import com.tabitos.app.core.accounting.domain.LedgerEntry
import com.tabitos.app.core.habits.domain.HabitSnapshot
import com.tabitos.app.core.rewards.domain.Purchase
import com.tabitos.app.core.rewards.domain.Reward
import com.tabitos.app.core.tasks.domain.AuditKind
import com.tabitos.app.core.tasks.domain.DeadlineKind
import com.tabitos.app.core.tasks.domain.TaskAudit
import com.tabitos.app.core.tasks.domain.TaskFields
import com.tabitos.app.core.tasks.domain.TaskOccurrence
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import com.tabitos.app.core.tasks.domain.TaskStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsHistoryTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 18)
    private val week = StatisticsPeriod.current(PeriodKind.WEEK, today).range(today, today)
    private fun millis(day: LocalDate, hour: Int = 12) = day.atTime(hour, 0).toInstant(zone).toEpochMilli()
    private val moment = millis(today, 9)

    private fun occurrence(id: String, status: TaskStatus = TaskStatus.DONE) = TaskOccurrence(id, "def", 1,
        TaskFields(title = "Pagar conta", deadline = DeadlineKind.DAY, date = today), today, today, today, status)

    private fun audit(id: String, kind: AuditKind, at: Long = moment, occurrence: String = "o:1",
                      actual: LocalDate? = null, original: LocalDate? = null) =
        TaskAudit(id, occurrence, kind, at, actual, original)

    private fun ledger(seq: Long, key: String, kind: String, coins: Long, at: Long = moment, title: String = "Pagar") =
        LedgerEntry(seq, key, title, today, at, kind, coins, coins, if (coins > 0) 20 else 0, 0, 0)

    private fun items(
        tasks: TaskSnapshot, ledger: List<LedgerEntry> = emptyList(), gameMode: Boolean = true,
        filter: HistoryFilter = HistoryFilter.ALL, purchases: List<Purchase> = emptyList(),
    ) = PreparedHistory.of(HistorySources(tasks, HabitSnapshot(), emptyList(), ledger, purchases), today, zone,
        gameMode).items(week, filter)

    @Test fun showsResultsAndPostponesButNotCreationsEditsOrChecks() {
        val tasks = TaskSnapshot(occurrences = listOf(occurrence("o:1")), history = listOf(
            audit("1", AuditKind.CREATED), audit("2", AuditKind.EDITED), audit("3", AuditKind.CHECKED),
            audit("4", AuditKind.POSTPONED, actual = today.plusDays(2), original = today),
            audit("5", AuditKind.COMPLETED, actual = today), audit("6", AuditKind.DELETED),
        ))
        val shown = items(tasks)
        assertEquals(setOf(HistoryKind.TASK_POSTPONED, HistoryKind.TASK_COMPLETED), shown.map { it.kind }.toSet())
        val postponed = shown.single { it.kind == HistoryKind.TASK_POSTPONED }
        assertEquals(today, postponed.from)
        assertEquals(today.plusDays(2), postponed.to)
    }

    @Test fun advanceAndItsCorrectionAreTaskHistoryAndDoNotCountAsPostponed() {
        val later = today.plusDays(2)
        val tasks = TaskSnapshot(occurrences = listOf(occurrence("o:1")), history = listOf(
            audit("advance", AuditKind.ADVANCED, actual = today, original = later),
            audit("undo", AuditKind.ADVANCE_UNDONE, at = moment + 1, actual = later, original = today),
        ))
        val (undo, advance) = items(tasks, filter = HistoryFilter.TASKS)
        assertEquals(HistoryKind.TASK_ADVANCED, advance.kind)
        assertEquals(later, advance.from)
        assertEquals(today, advance.to)
        assertEquals(HistoryKind.TASK_ADVANCE_UNDONE, undo.kind)
        assertEquals(later, undo.to)
        assertTrue(undo.adjustment)
        assertEquals(0, taskSummary(tasks, week, BucketUnit.DAY, today, zone).postponed)
    }

    @Test fun undoOfPostponeIsADiscreetAdjustment() {
        val tasks = TaskSnapshot(occurrences = listOf(occurrence("o:1")),
            history = listOf(audit("1", AuditKind.POSTPONE_UNDONE, actual = today)))
        assertTrue(items(tasks).single().adjustment)
    }

    @Test fun ledgerEntriesAttachToTheResultTheyPaid() {
        val tasks = TaskSnapshot(occurrences = listOf(occurrence("o:1")),
            history = listOf(
                audit("a", AuditKind.ADVANCED, at = moment - 1, actual = today, original = today.plusDays(1)),
                audit("1", AuditKind.COMPLETED, actual = today),
            ))
        val entries = listOf(ledger(1, "task:o:1:done", "GAIN", 10, at = moment + 40))
        val shown = items(tasks, entries)
        assertNull(shown.single { it.kind == HistoryKind.TASK_ADVANCED }.coins)
        val item = shown.single { it.kind == HistoryKind.TASK_COMPLETED }
        assertEquals(10L, item.coins)
        assertEquals(20L, item.xp)
    }

    @Test fun entriesFarFromAnyRecordStayAsTheirOwnCoinItems() {
        val tasks = TaskSnapshot(occurrences = listOf(occurrence("o:1")),
            history = listOf(audit("1", AuditKind.COMPLETED, actual = today)))
        val late = ledger(1, "task:o:1:done", "REVERSAL", -10, at = moment + 3_600_000)
        val shown = items(tasks, listOf(late))
        assertNull(shown.single { it.kind == HistoryKind.TASK_COMPLETED }.coins)
        val coin = shown.single { it.kind == HistoryKind.COINS }
        assertTrue(coin.adjustment)
    }

    @Test fun coinsFilterListsEveryLedgerEntryWithItsValues() {
        val entries = listOf(ledger(1, "task:o:1:done", "GAIN", 10), ledger(2, "purchase:p", "PURCHASE", -30,
            title = "Cinema"), ledger(3, "task:o:1:done", "REVERSAL", -10))
        val purchase = Purchase("p", Reward("cinema", "Cinema", cost = 30), today, moment, 2)
        val shown = items(TaskSnapshot(occurrences = listOf(occurrence("o:1"))), entries,
            filter = HistoryFilter.COINS, purchases = listOf(purchase))
        assertEquals(3, shown.size)
        assertEquals(setOf(10L, -30L, -10L), shown.map { it.coins }.toSet())
        assertEquals(HistorySubject.Reward("cinema"), shown.single { it.ledgerKind == "PURCHASE" }.subject)
    }

    @Test fun gameModeOffHidesCoinsAndTheCoinsFilter() {
        val tasks = TaskSnapshot(occurrences = listOf(occurrence("o:1")),
            history = listOf(audit("1", AuditKind.COMPLETED, actual = today)))
        val entries = listOf(ledger(1, "task:o:1:done", "GAIN", 10), ledger(2, "purchase:p", "PURCHASE", -30))
        val shown = items(tasks, entries, gameMode = false)
        assertEquals(listOf(HistoryKind.TASK_COMPLETED), shown.map { it.kind })
        assertNull(shown.single().coins)
        assertEquals(listOf(HistoryFilter.ALL, HistoryFilter.TASKS, HistoryFilter.HABITS, HistoryFilter.BAD_HABITS,
            HistoryFilter.TIME),
            historyFilters(gameMode = false))
        assertTrue(items(tasks, entries, gameMode = false, filter = HistoryFilter.COINS).isEmpty())
    }

    @Test fun deletedTasksAndErasedEntriesAreMarked() {
        val tasks = TaskSnapshot(occurrences = listOf(occurrence("o:1", TaskStatus.DELETED)),
            history = listOf(audit("1", AuditKind.COMPLETED, actual = today)))
        assertEquals(HistorySubject.Task("o:1", "def", exists = false), items(tasks).single().subject)
        val erased = ledger(1, "task:gone:1:done", "GAIN", 10, title = "")
        assertEquals(HistorySubject.Erased(task = true),
            items(TaskSnapshot(), listOf(erased), filter = HistoryFilter.COINS).single().subject)
    }

    @Test fun itemsOutsideThePeriodAreLeftOut() {
        val tasks = TaskSnapshot(occurrences = listOf(occurrence("o:1")), history = listOf(
            audit("1", AuditKind.COMPLETED, at = millis(week.start.minusDays(1)), actual = today)))
        assertTrue(items(tasks).isEmpty())
    }

    @Test fun rowsGroupByDayNewestFirstWithMonthHeadersWhenAsked() {
        fun item(id: String, day: LocalDate, hour: Int) = HistoryItem(id, millis(day, hour), day,
            HistoryKind.TASK_COMPLETED, HistorySubject.None, id)
        val oct = LocalDate.of(2026, 10, 31)
        val rows = historyRows(listOf(item("a", oct, 9), item("b", today, 8), item("c", today, 18)), true)
        assertEquals(listOf(HistoryRow.MonthHeader(YearMonth.of(2026, 11)), HistoryRow.DayHeader(today)),
            rows.take(2))
        assertEquals(listOf("c", "b", "a"), rows.filterIsInstance<HistoryRow.Item>().map { it.item.id })
        assertTrue(historyRows(listOf(item("a", oct, 9)), false).none { it is HistoryRow.MonthHeader })
    }
}
