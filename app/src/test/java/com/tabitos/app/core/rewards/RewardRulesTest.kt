package com.tabitos.app.core.rewards

import com.tabitos.app.core.rewards.domain.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class RewardRulesTest {
    private val day = LocalDate.of(2026, 11, 7)
    private val reward = Reward("r", "Pausa", cost = 5)

    @Test fun rejectsInvalidDefinitions() {
        listOf(reward.copy(name = " "), reward.copy(cost = 0), reward.copy(cost = -1),
            reward.copy(limit = 0), reward.copy(icon = "UNKNOWN"), reward.copy(period = "ROLLING"))
            .forEach { value ->
                try { RewardRules.validate(value); fail("Invalid definition accepted") }
                catch (_: IllegalArgumentException) { }
            }
        RewardRules.validate(reward.copy(cost = Long.MAX_VALUE, limit = Int.MAX_VALUE))
        listOf("GIFT", "GAME", "FOOD", "REST", "PETS").forEach { RewardRules.validate(reward.copy(icon = it)) }
    }

    @Test fun civilWeeksMonthsAndIndependentUsage() {
        val purchases = listOf(day.minusDays(6), day.minusDays(5), day, day.plusDays(1)).mapIndexed { i, date ->
            Purchase("$i", reward, date, i.toLong(), i.toLong())
        } + Purchase("other", reward.copy(id = "other"), day, 9, 9)
        assertEquals(1, RewardRules.used(reward, purchases, day))
        assertEquals(3, RewardRules.used(reward.copy(period = "WEEK"), purchases, day))
        assertEquals(4, RewardRules.used(reward.copy(period = "MONTH"), purchases, day))
        assertEquals(0, RewardRules.used(reward.copy(period = "WEEK"), purchases, day.plusDays(2)))
        assertEquals(0, RewardRules.used(reward.copy(period = "MONTH"), purchases, LocalDate.of(2027, 1, 1)))
    }

    @Test fun exactBalanceAndReducedLimit() {
        assertTrue(RewardRules.available(reward, 5, 0))
        assertFalse(RewardRules.available(reward, 4, 0))
        assertFalse(RewardRules.available(reward, -300, 0))
        assertTrue(RewardRules.available(reward, 100, Int.MAX_VALUE))
        assertFalse(RewardRules.available(reward.copy(limit = 1), 100, 2))
        assertFalse(RewardRules.available(reward.copy(deleted = true), 100, 0))
    }
}
