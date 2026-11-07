package com.ethos.app.core.accounting

import com.ethos.app.core.accounting.domain.*
import org.junit.Assert.*
import org.junit.Test

class AccountingRulesTest {
    @Test fun exactLinearThresholdsAndLargeValues() {
        listOf(0L to 1L, 99L to 1L, 100L to 2L, 249L to 2L, 250L to 3L,
            450L to 4L, 252450L to 100L).forEach { (xp, level) ->
            assertEquals(level, AccountingRules.progress(xp).level)
        }
        assertEquals(5000L, AccountingRules.progress(247450).required)
        assertTrue(AccountingRules.progress(Long.MAX_VALUE).level > 100)
    }

    @Test fun effectivePenaltyRefundAndZeroImpactIncident() {
        val p = AccountingRules.change(-295, 0, -20, 0)
        assertEquals(-5L, p.coins)
        assertEquals(-300L, p.balance)
        assertEquals(0L, AccountingRules.change(-300, 0, -20, 0).coins)
        assertEquals(-295L, AccountingRules.change(p.balance, 0, -p.coins, 0).balance)
    }

    @Test fun defaultsSeparateDifficultyAndImportance() {
        val prices = Price.defaults()
        assertEquals(listOf(5L, 10L, 20L), prices.map { it.coins })
        assertEquals(listOf(10L, 20L, 40L), prices.map { it.xp })
        assertEquals(listOf(5L, 10L, 20L), prices.map { it.penalty })
    }

    @Test fun clippedAwardReapplicationCannotMintCoinsAndRetainsXp() {
        val original = AccountingRules.change(0, 0, 20, 40)
        val reversal = AccountingRules.change(-295, original.totalXp, -20, -40)
        val reapplied = AccountingRules.change(reversal.balance, reversal.totalXp, -reversal.coins, 40)
        assertEquals(-295L, reapplied.balance)
        assertEquals(40L, reapplied.totalXp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidPriceRejected() { Price(0, -1, 0, 0).validate() }

    @Test fun zeroAndMaximumPricesAllowed() {
        Price(0, 0, 0, 0).validate()
        Price(2, 10000, 10000, 10000).validate()
    }
}
