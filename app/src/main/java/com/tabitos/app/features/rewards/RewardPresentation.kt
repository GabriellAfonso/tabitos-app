package com.tabitos.app.features.rewards

import androidx.annotation.StringRes
import com.tabitos.app.R
import com.tabitos.app.core.rewards.domain.Purchase
import com.tabitos.app.core.rewards.domain.PurchaseIntent
import com.tabitos.app.core.rewards.domain.RewardRules
import com.tabitos.app.core.rewards.domain.RewardSnapshot

/** What the store row shows instead of the buy button when a reward cannot be bought now. */
sealed interface RewardBlock {
    data object None : RewardBlock
    data class Missing(val coins: Long) : RewardBlock
    data class Exhausted(@param:StringRes val renews: Int) : RewardBlock
}

/** A reached limit wins over a short balance: buying would stay impossible even with more coins. */
fun RewardCard.block(): RewardBlock = when {
    available -> RewardBlock.None
    exhausted -> RewardBlock.Exhausted(renewsLabel(reward.period))
    else -> RewardBlock.Missing(missing)
}

/** Why an open purchase cannot be confirmed; a removed reward wins over limit and balance. */
enum class PurchaseBlock(@param:StringRes val label: Int) {
    REMOVED(R.string.reward_removed), EXHAUSTED(R.string.reward_exhausted), INSUFFICIENT(R.string.reward_insufficient),
}

/**
 * Inline confirmation under the store row (option C of prototypes/confirmar-compra): balance before → after,
 * the purchase count after this one, and the running timer a timed purchase pauses ([pausing]).
 */
data class PurchaseStrip(
    val cost: Long, val minutes: Int?, val period: String, val limit: Int?, val balanceBefore: Long,
    val balanceAfter: Long?, val usedAfter: Int?, val pausing: String?, val block: PurchaseBlock?,
)

/** [unavailable] is the repository's verdict on the last confirm; it blocks even when the preview allowed it. */
fun purchaseStrip(intent: PurchaseIntent, runningTimer: String?, unavailable: Boolean): PurchaseStrip {
    val preview = purchasePresentation(intent)
    val reward = intent.reward
    val block = when {
        !unavailable && preview.available -> null
        reward.deleted -> PurchaseBlock.REMOVED
        preview.exhausted -> PurchaseBlock.EXHAUSTED
        else -> PurchaseBlock.INSUFFICIENT
    }
    return PurchaseStrip(reward.cost, reward.minutes, reward.period, reward.limit, intent.balance,
        preview.balanceAfter, reward.limit?.let { intent.used + 1 },
        runningTimer?.takeIf { reward.minutes != null }, block)
}

/** The purchase already recorded for the open intent; while it exists the store shows the short receipt. */
fun RewardSnapshot.receipt(): Purchase? = intent?.let { open -> purchases.firstOrNull { it.id == open.id } }

/** The open, not yet purchased intent shown as a strip under [rewardId]'s row. */
fun RewardSnapshot.stripIntent(rewardId: String): PurchaseIntent? =
    intent?.takeIf { it.reward.id == rewardId && receipt() == null }

@StringRes
fun renewsLabel(period: String): Int = when (period) {
    WEEK -> R.string.reward_renews_week
    MONTH -> R.string.reward_renews_month
    else -> R.string.reward_renews_day
}

/** "1 de 2 por semana", used in the store row of a limited reward. */
@StringRes
fun limitUsedLabel(period: String): Int = when (period) {
    WEEK -> R.string.reward_limit_used_week
    MONTH -> R.string.reward_limit_used_month
    else -> R.string.reward_limit_used_day
}

/** "2× por semana", the limit chip and row value in the editor. */
@StringRes
fun limitValueLabel(period: String): Int = when (period) {
    WEEK -> R.string.reward_limit_week_value
    MONTH -> R.string.reward_limit_month_value
    else -> R.string.reward_limit_day_value
}

@StringRes
fun limitOptionLabel(period: String?): Int = when (period) {
    null -> R.string.reward_no_limit
    WEEK -> R.string.reward_limit_per_week
    MONTH -> R.string.reward_limit_per_month
    else -> R.string.reward_limit_per_day
}

@StringRes
fun limitTimesLabel(period: String): Int = when (period) {
    WEEK -> R.string.reward_limit_times_week
    MONTH -> R.string.reward_limit_times_month
    else -> R.string.reward_limit_times_day
}

/** Limit sheet options: no limit, then each calendar period. */
val limitOptions: List<String?> = listOf<String?>(null) + RewardRules.periods

/** Choosing a period keeps the current count; a reward without a limit starts at once per period. */
fun RewardDraft.withLimitPeriod(period: String?): RewardDraft =
    if (period == null) copy(limit = null) else copy(period = period, limit = limit ?: 1)

/** Switching to timed use starts at [DEFAULT_REWARD_MINUTES]; immediate use clears the duration. */
fun RewardDraft.withTimed(timed: Boolean): RewardDraft =
    copy(minutes = if (timed) minutes ?: DEFAULT_REWARD_MINUTES else null)

fun durationText(minutes: Int): String {
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    return when {
        hours == 0 -> "$rest min"
        rest == 0 -> "$hours h"
        else -> "$hours h $rest min"
    }
}

val COST_SHORTCUTS = listOf(10L, 25L, 50L, 100L, 200L)
val COST_RANGE = 1..99_999
const val COST_STEP = 5
val LIMIT_COUNT_RANGE = 1..99
val REWARD_MINUTES_RANGE = 5..1440
const val REWARD_MINUTES_STEP = 5
const val DEFAULT_REWARD_MINUTES = 30
private const val MINUTES_PER_HOUR = 60
private const val WEEK = "WEEK"
private const val MONTH = "MONTH"
