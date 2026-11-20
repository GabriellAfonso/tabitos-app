package com.tabitos.app.core.ui.rewards

import androidx.annotation.DrawableRes
import com.tabitos.app.core.icons.AppIcon
import com.tabitos.app.core.ui.components.option

/** Catalog icon of a persisted reward icon key; unknown keys fall back to the gift. */
fun rewardAppIcon(icon: String): AppIcon = AppIcon.fromKey(icon) ?: AppIcon.GIFT

/** Drawing for a persisted reward icon key. */
@DrawableRes
fun rewardIcon(icon: String): Int = rewardAppIcon(icon).option.icon
