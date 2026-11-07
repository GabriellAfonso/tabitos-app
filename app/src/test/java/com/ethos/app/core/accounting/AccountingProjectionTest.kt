package com.ethos.app.core.accounting

import com.ethos.app.core.accounting.domain.*
import com.ethos.app.core.habits.domain.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AccountingProjectionTest {
    private val day = LocalDate.of(2026, 11, 2)
    private fun habit(type: HabitType, target: Int, records: List<HabitRecord> = emptyList()): HabitSnapshot =
        HabitSnapshot(habits = listOf(Habit("h", day, "Leitura")),
            rules = listOf(HabitRule("r", "h", day, type, HabitUnit.WEEK, target)),
            intervals = listOf(ActiveInterval("i", "h", day)), records = records)

    @Test fun distinctPracticeDaysEarnImmediatelyWithoutClosingBonus() {
        val s = habit(HabitType.PRACTICE, 3, listOf(HabitRecord("a", "h", day, 1),
            HabitRecord("b", "h", day, 2), HabitRecord("c", "h", day.plusDays(1), 3)))
        val outcomes = AccountingProjection.habits(s, day.plusDays(2)).filter { it.active }
        assertEquals(2, outcomes.size)
        val wallet = AccountingRules.reconcile(AccountingSnapshot(), outcomes, setOf("habit:h"), 10)
        assertEquals(20L, wallet.balance)
        assertEquals(40L, wallet.totalXp)
    }

    @Test fun fullPracticeRejectsMoreRecordsButCorrectionRestoresCapacity() {
        val s = habit(HabitType.PRACTICE, 1, listOf(HabitRecord("a", "h", day, 1)))
        try {
            HabitRules.apply(s, HabitAction.Record("b", "h", day.plusDays(1)), day.plusDays(1), 2, emptySet())
            fail("Must reject practice beyond target")
        } catch (e: HabitRuleException) { assertEquals(HabitProblem.CAPACITY, e.reason) }
        val corrected = HabitRules.apply(s, HabitAction.Correct("a", delete = true), day.plusDays(1), 3, emptySet())
        val resumed = HabitRules.apply(corrected, HabitAction.Record("b", "h", day.plusDays(1)),
            day.plusDays(1), 4, emptySet())
        assertEquals(1, AccountingProjection.habits(resumed, day.plusDays(1)).count { it.active })
    }

    @Test fun confirmedIncompletePracticeKeepsDayAwardAndOnePeriodPenalty() {
        var s = habit(HabitType.PRACTICE, 3, listOf(HabitRecord("a", "h", day, 1)))
        val review = HabitRules.pending(s, day.plusWeeks(1)).first()
        s = HabitRules.apply(s, HabitAction.Review(review.key), day.plusWeeks(1), 2, emptySet())
        val outcomes = AccountingProjection.habits(s, day.plusWeeks(1)).filter { it.active }
        assertEquals(2, outcomes.size)
        val wallet = AccountingRules.reconcile(AccountingSnapshot(), outcomes, setOf("habit:h"), 3)
        assertEquals(0L, wallet.balance)
        assertEquals(20L, wallet.totalXp)
    }

    @Test fun limitUsesStableChronologyAndNoExtraClosingPenalty() {
        val s = habit(HabitType.LIMIT, 1, listOf(HabitRecord("z", "h", day, 1),
            HabitRecord("b", "h", day, 2), HabitRecord("a", "h", day, 2)))
        val result = AccountingProjection.habits(s, day.plusWeeks(1)).filter { it.active }
        assertEquals(setOf("event:a", "event:b"), result.map { it.key }.toSet())
        assertTrue(result.all { !it.reward })
    }

    @Test fun noEventsDoesNotAutomaticallyAwardNegativePeriod() {
        val s = habit(HabitType.LIMIT, 1)
        assertTrue(AccountingProjection.habits(s, day.plusWeeks(1)).none { it.active })
        val review = HabitRules.pending(s, day.plusWeeks(1)).first()
        val confirmed = HabitRules.apply(s, HabitAction.Review(review.key), day.plusWeeks(1), 2, emptySet())
        assertEquals(1, AccountingProjection.habits(confirmed, day.plusWeeks(1)).count { it.active })
    }

    @Test fun reconciliationPreservesHistoricalPricesAndNoDoubleRefund() {
        val owner = setOf("habit:h")
        val outcome = FinancialOutcome("practice:h:$day", "habit:h", "Leitura", day, true, 1, true)
        var wallet = AccountingRules.reconcile(AccountingSnapshot(), listOf(outcome), owner, 1)
        wallet = wallet.copy(prices = Price.defaults().map { it.copy(coins = 100, xp = 100) })
        wallet = AccountingRules.reconcile(wallet, emptyList(), owner, 2)
        assertEquals(0L, wallet.balance)
        assertEquals(0L, wallet.totalXp)
        assertEquals(wallet, AccountingRules.reconcile(wallet, emptyList(), owner, 3))
        wallet = AccountingRules.reconcile(wallet, listOf(outcome), owner, 4)
        assertEquals(10L, wallet.balance)
        assertEquals(20L, wallet.totalXp)
        assertEquals(3, wallet.entries.size)
        assertEquals(wallet, AccountingRules.reconcile(wallet, listOf(outcome), owner, 5))
    }

    @Test fun baselineDoesNotBackPayAndNewResolutionUsesCurrentPrice() {
        val outcome = FinancialOutcome("task:t:done", "task:t", "Antiga", day, true, 2, true)
        var wallet = AccountingSnapshot(entitlements = listOf(Entitlement(outcome.key, outcome.owner,
            outcome.title, day, 0, 0, true, true, priced = false)))
        wallet = AccountingRules.reconcile(wallet, listOf(outcome), setOf(outcome.owner), 1)
        assertTrue(wallet.entries.isEmpty())
        wallet = AccountingRules.reconcile(wallet, emptyList(), setOf(outcome.owner), 2)
        assertTrue(wallet.entries.isEmpty())
        wallet = AccountingRules.reconcile(wallet, listOf(outcome), setOf(outcome.owner), 3)
        assertEquals(20L, wallet.balance)
        assertEquals(40L, wallet.totalXp)
    }

    @Test fun floorClippedAwardCanBeRepeatedWithoutMinting() {
        val o = FinancialOutcome("task:t:done", "task:t", "T", day, true, 2, true)
        var s = AccountingRules.reconcile(AccountingSnapshot(), listOf(o), setOf(o.owner), 1)
        // Models coins already consumed by a future spending integration; not a production mint/debit API.
        s = s.copy(entries = s.entries + LedgerEntry(2, "future:spent", "Spent", day, 2,
            "PENALTY", -315, -315, 0, -295, s.totalXp))
        repeat(3) {
            s = AccountingRules.reconcile(s, emptyList(), setOf(o.owner), 3)
            assertEquals(-300L, s.balance)
            s = AccountingRules.reconcile(s, listOf(o), setOf(o.owner), 4)
            assertEquals(-295L, s.balance)
            assertEquals(40L, s.totalXp)
        }
    }

    @Test fun movingPracticeDatePreservesOriginalPriceAcrossCorrections() {
        val before = habit(HabitType.PRACTICE, 3, listOf(HabitRecord("a", "h", day, 1)))
        val action = HabitAction.Correct("a", day.plusDays(1))
        val after = HabitRules.apply(before, action, day.plusDays(2), 2, emptySet())
        val owner = setOf("habit:h")
        var wallet = AccountingRules.reconcile(AccountingSnapshot(),
            AccountingProjection.habits(before, day.plusDays(2)), owner, 1)
        wallet = wallet.copy(prices = Price.defaults().map { it.copy(coins = 1000, xp = 1000) })
        wallet = AccountingRules.reconcile(wallet, AccountingProjection.habits(after, day.plusDays(2)),
            owner, 2, AccountingProjection.practiceOrigins(before, after, action, day.plusDays(2)))
        assertEquals(10L, wallet.balance)
        assertEquals(20L, wallet.totalXp)
        assertEquals(3, wallet.entries.size)
        assertEquals(day.plusDays(1), wallet.entries.last().date)
        val back = HabitAction.Correct("a", day)
        wallet = AccountingRules.reconcile(wallet, AccountingProjection.habits(before, day.plusDays(2)),
            owner, 3, AccountingProjection.practiceOrigins(after, before, back, day.plusDays(2)))
        assertEquals(10L, wallet.balance)
        assertEquals(20L, wallet.totalXp)
    }

    @Test fun correctingPenalizedEventDateRetainsChargeAndRefundProvenance() {
        val old = FinancialOutcome("event:e", "habit:h", "Evento", day, false, 1, true)
        val owner = setOf(old.owner)
        var wallet = AccountingRules.reconcile(AccountingSnapshot(), listOf(old), owner, 1)
        wallet = AccountingRules.reconcile(wallet, listOf(old.copy(date = day.plusDays(1))), owner, 2)
        assertEquals(-10L, wallet.balance)
        assertEquals("CORRECTION", wallet.entries.last().kind)
        assertEquals(day, wallet.entries.first().date)
        assertEquals(day.plusDays(1), wallet.entries.last().date)
        assertEquals(0L, wallet.entries.last().coins)
        wallet = AccountingRules.reconcile(wallet, emptyList(), owner, 3)
        assertEquals(0L, wallet.balance)
        assertEquals(10L, wallet.entries.last().coins)
        assertEquals(1L, wallet.entries.last().related)
    }
}
