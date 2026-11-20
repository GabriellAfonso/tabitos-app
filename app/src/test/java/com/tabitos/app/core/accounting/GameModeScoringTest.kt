package com.tabitos.app.core.accounting

import com.tabitos.app.core.accounting.domain.AccountingRules
import com.tabitos.app.core.accounting.domain.AccountingSnapshot
import com.tabitos.app.core.accounting.domain.FinancialOutcome
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Game mode off (spec 017): outcomes never pay, paid outcomes are still reversed. */
class GameModeScoringTest {
    private val day = LocalDate.of(2026, 11, 11)
    private val task = FinancialOutcome("task:t:done", "task:t", "Relatório", day, true, MEDIUM, true)
    private val owners = setOf(task.owner)

    private fun AccountingSnapshot.apply(outcomes: List<FinancialOutcome>, scoring: Boolean, now: Long) =
        AccountingRules.reconcile(this, outcomes, owners, now, scoring = scoring)

    @Test fun outcomeRecordedWhileOffCreatesNoEntry() {
        val wallet = AccountingSnapshot().apply(listOf(task), scoring = false, now = 1)
        assertTrue(wallet.entries.isEmpty())
        assertEquals(0L, wallet.balance)
    }

    @Test fun avoidEventWhileOffChargesNoPenalty() {
        val event = FinancialOutcome("event:e", "habit:h", "Doce", day, false, MEDIUM, true)
        val wallet = AccountingRules.reconcile(AccountingSnapshot(), listOf(event), setOf(event.owner), 1,
            scoring = false)
        assertTrue(wallet.entries.isEmpty())
    }

    @Test fun outcomeFromOffIsNotPaidWhenScoringReturns() {
        var wallet = AccountingSnapshot().apply(listOf(task), scoring = false, now = 1)
        // Any later reconcile while on (e.g. another action of the same owner) must not back-pay it.
        wallet = wallet.apply(listOf(task), scoring = true, now = 2)
        assertTrue(wallet.entries.isEmpty())
    }

    @Test fun paidOutcomeUndoneWhileOffIsReversed() {
        var wallet = AccountingSnapshot().apply(listOf(task), scoring = true, now = 1)
        assertEquals(10L, wallet.balance)
        wallet = wallet.apply(emptyList(), scoring = false, now = 2)
        assertEquals(0L, wallet.balance)
        assertEquals(0L, wallet.totalXp)
        assertEquals("REVERSAL", wallet.entries.last().kind)
        wallet = wallet.apply(listOf(task), scoring = false, now = 3)
        assertEquals(2, wallet.entries.size)
        assertEquals(0L, wallet.balance)
    }

    @Test fun outcomeFromOffUndoneAndRedoneWhileOnPaysOnce() {
        var wallet = AccountingSnapshot().apply(listOf(task), scoring = false, now = 1)
        wallet = wallet.apply(emptyList(), scoring = true, now = 2)
        assertTrue(wallet.entries.isEmpty())
        wallet = wallet.apply(listOf(task), scoring = true, now = 3)
        assertEquals(10L, wallet.balance)
        assertEquals(20L, wallet.totalXp)
        assertEquals(wallet, wallet.apply(listOf(task), scoring = true, now = 4))
    }

    @Test fun movingPaidPracticeDayWhileOffKeepsItsAward() {
        val owner = setOf("habit:h")
        val old = FinancialOutcome("practice:h:$day", "habit:h", "Leitura", day, true, MEDIUM, true)
        val moved = old.copy(key = "practice:h:${day.plusDays(1)}", date = day.plusDays(1))
        var wallet = AccountingRules.reconcile(AccountingSnapshot(), listOf(old), owner, 1)
        wallet = AccountingRules.reconcile(wallet, listOf(moved), owner, 2, mapOf(moved.key to old.key),
            scoring = false)
        assertEquals(10L, wallet.balance)
        assertEquals(20L, wallet.totalXp)
    }

    private companion object {
        const val MEDIUM = 1
    }
}
