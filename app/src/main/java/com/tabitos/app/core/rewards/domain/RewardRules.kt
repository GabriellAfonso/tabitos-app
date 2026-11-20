package com.tabitos.app.core.rewards.domain

import com.tabitos.app.core.icons.AppIcon
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object RewardRules {
    val periods = listOf("DAY", "WEEK", "MONTH")

    fun validate(reward: Reward) {
        require(reward.id.isNotBlank() && reward.name.isNotBlank() && reward.cost > 0)
        require(AppIcon.fromKey(reward.icon) != null && reward.period in periods)
        require(reward.limit == null || reward.limit > 0)
        require(reward.minutes == null || reward.minutes in 1..1440)
    }

    fun used(reward: Reward, purchases: List<Purchase>, today: LocalDate): Int {
        val start = when (reward.period) {
            "WEEK" -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            "MONTH" -> today.withDayOfMonth(1)
            else -> today
        }
        val end = when (reward.period) {
            "WEEK" -> start.plusDays(6)
            "MONTH" -> start.with(TemporalAdjusters.lastDayOfMonth())
            else -> today
        }
        return purchases.count { it.reward.id == reward.id && it.date >= start && it.date <= end }
    }

    fun available(reward: Reward, balance: Long, used: Int): Boolean =
        !reward.deleted && reward.cost > 0 && balance >= reward.cost && (reward.limit == null || used < reward.limit)
}
