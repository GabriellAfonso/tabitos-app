package com.ethos.app.core.ui.rewards

import androidx.annotation.DrawableRes
import com.ethos.app.R

/** Drawing for a persisted reward icon key (RewardRules.icons); unknown keys fall back to the gift. */
@DrawableRes
fun rewardIcon(icon: String): Int = when (icon) {
    "GAME" -> R.drawable.ic_sports_esports
    "FOOD" -> R.drawable.ic_restaurant
    "REST" -> R.drawable.ic_coffee
    else -> R.drawable.ic_redeem
}
