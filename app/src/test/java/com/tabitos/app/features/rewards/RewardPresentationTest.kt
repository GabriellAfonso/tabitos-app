package com.tabitos.app.features.rewards

import com.tabitos.app.R
import com.tabitos.app.core.rewards.domain.Purchase
import com.tabitos.app.core.rewards.domain.PurchaseIntent
import com.tabitos.app.core.rewards.domain.Reward
import com.tabitos.app.core.rewards.domain.RewardSnapshot
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RewardPresentationTest {
    private val today = LocalDate.of(2026, 11, 19)

    @Test fun storeRowExplainsWhyARewardCannotBeBought() {
        val weekly = Reward("w", "Lanche", cost = 120, limit = 1, period = "WEEK")
        val bought = listOf(Purchase("p", weekly, today, 1, 1))
        assertEquals(RewardBlock.None, rewardCard(weekly, emptyList(), balance = 120, today).block())
        assertEquals(RewardBlock.Missing(20), rewardCard(weekly, emptyList(), balance = 100, today).block())
        // A reached limit wins: more coins would not help.
        assertEquals(RewardBlock.Exhausted(R.string.reward_renews_week),
            rewardCard(weekly, bought, balance = 0, today).block())
    }

    @Test fun purchaseStripShowsBalanceCountAfterAndPausesOnlyForTimedRewards() {
        val episode = Reward("e", "Episódio", cost = 40, limit = 2)
        val strip = purchaseStrip(PurchaseIntent("i", episode, today, 140, 1), "Leitura", unavailable = false)
        assertEquals(140L, strip.balanceBefore)
        assertEquals(100L, strip.balanceAfter)
        assertEquals(2, strip.usedAfter)
        assertNull(strip.pausing)
        assertNull(strip.block)
        val game = Reward("g", "Jogo", cost = 60, minutes = 60)
        val timed = purchaseStrip(PurchaseIntent("j", game, today, 140, 0), "Leitura", unavailable = false)
        assertEquals("Leitura", timed.pausing)
        assertNull(timed.usedAfter)
    }

    @Test fun purchaseStripBlocksRemovedExhaustedAndShortRewards() {
        val reward = Reward("e", "Episódio", cost = 40, limit = 1)
        fun block(r: Reward, balance: Long, used: Int, unavailable: Boolean = false) =
            purchaseStrip(PurchaseIntent("i", r, today, balance, used), null, unavailable).block
        assertEquals(PurchaseBlock.EXHAUSTED, block(reward, 140, 1))
        assertEquals(PurchaseBlock.INSUFFICIENT, block(reward, 10, 0))
        assertEquals(PurchaseBlock.REMOVED, block(reward.copy(deleted = true), 140, 0, unavailable = true))
        // The repository's verdict wins over a preview that still looked available.
        assertEquals(PurchaseBlock.INSUFFICIENT, block(reward, 140, 0, unavailable = true))
    }

    @Test fun recordedPurchaseTurnsTheStripIntoTheReceipt() {
        val reward = Reward("e", "Episódio", cost = 40)
        val open = RewardSnapshot(intent = PurchaseIntent("i", reward, today, 140, 0))
        assertEquals("i", open.stripIntent("e")?.id)
        assertNull(open.stripIntent("other"))
        assertNull(open.receipt())
        val bought = open.copy(purchases = listOf(Purchase("i", reward, today, 1, 1)))
        assertNull(bought.stripIntent("e"))
        assertEquals("i", bought.receipt()?.id)
    }

    @Test fun limitAndUseChoicesKeepOrResetTheirValues() {
        val draft = RewardDraft("r")
        val daily = draft.withLimitPeriod("DAY")
        assertEquals(1, daily.limit)
        val monthly = daily.copy(limit = 3).withLimitPeriod("MONTH")
        assertEquals(3, monthly.limit)
        assertEquals("MONTH", monthly.period)
        assertNull(monthly.withLimitPeriod(null).limit)
        assertEquals(DEFAULT_REWARD_MINUTES, draft.withTimed(true).minutes)
        assertEquals(90, draft.copy(minutes = 90).withTimed(true).minutes)
        assertNull(draft.copy(minutes = 90).withTimed(false).minutes)
    }

    @Test fun durationReadsInHoursAndMinutes() {
        assertEquals("45 min", durationText(45))
        assertEquals("2 h", durationText(120))
        assertEquals("1 h 30 min", durationText(90))
    }
}
