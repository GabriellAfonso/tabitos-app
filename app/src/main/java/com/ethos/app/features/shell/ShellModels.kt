package com.ethos.app.features.shell

import androidx.annotation.StringRes
import com.ethos.app.R

enum class ShellDestination(val route: String, @param:StringRes val title: Int, val symbol: String) {
    TODAY("today", R.string.today, "◉"),
    TASKS("tasks", R.string.tasks, "☰"),
    HABITS("habits", R.string.habits, "◇"),
    CALENDAR("calendar", R.string.calendar, "▦"),
    REVIEW("review", R.string.review, "◷"),
    REWARDS("rewards", R.string.rewards, "◇"),
    HISTORY("history", R.string.history, "↔"),
    SETTINGS("settings", R.string.settings, "⚙"),
    ;

    /** Exists only with game mode on (spec 017); off, requests for it land on Hoje. */
    val isGameOnly: Boolean get() = this == REWARDS || this == HISTORY

    companion object {
        val bottomBar = listOf(TODAY, TASKS, HABITS, CALENDAR)
        private val drawer = listOf(TODAY, REVIEW, REWARDS, HISTORY)

        fun drawerFor(gameMode: Boolean): List<ShellDestination> = drawer.filter { gameMode || !it.isGameOnly }
    }
}
