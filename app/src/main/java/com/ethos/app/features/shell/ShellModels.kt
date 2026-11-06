package com.ethos.app.features.shell

import androidx.annotation.StringRes
import com.ethos.app.R

enum class ShellDestination(val route: String, @param:StringRes val title: Int, val symbol: String) {
    TODAY("today", R.string.today, "◉"),
    TASKS("tasks", R.string.tasks, "✓"),
    HABITS("habits", R.string.habits, "↻"),
    CALENDAR("calendar", R.string.calendar, "▦"),
    REVIEW("review", R.string.review, "◷"),
    REWARDS("rewards", R.string.rewards, "◇"),
    HISTORY("history", R.string.history, "↔"),
    SETTINGS("settings", R.string.settings, "⚙"),
}

enum class ContentFilter(@param:StringRes val title: Int) {
    ALL(R.string.all), TASKS(R.string.tasks), HABITS(R.string.habits),
}

data class ShellItem(
    val id: String,
    val type: ContentFilter,
    @param:StringRes val title: Int,
    @param:StringRes val detail: Int,
)

data class ShellPresentation(
    val todayItems: List<ShellItem> = emptyList(),
    val pendingItems: List<ShellItem> = emptyList(),
    @param:StringRes val balance: Int = R.string.unavailable,
    @param:StringRes val level: Int = R.string.unavailable,
)

fun List<ShellItem>.matching(filter: ContentFilter): List<ShellItem> =
    if (filter == ContentFilter.ALL) this else filter { it.type == filter }
