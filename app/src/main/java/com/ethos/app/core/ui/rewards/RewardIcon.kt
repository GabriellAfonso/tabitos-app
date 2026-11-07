package com.ethos.app.core.ui.rewards

fun rewardSymbol(icon: String): String = when (icon) {
    "GAME" -> "🎮"
    "FOOD" -> "🍎"
    "REST" -> "☕"
    else -> "🎁"
}
