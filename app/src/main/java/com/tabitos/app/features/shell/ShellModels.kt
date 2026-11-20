package com.tabitos.app.features.shell

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tabitos.app.R

/**
 * Pages of the app. There is no bottom bar: the drawer reaches the main pages and lists, Settings reaches
 * its sub-pages. [parent] marks a sub-page whose header shows Back instead of the menu.
 */
enum class ShellDestination(
    val route: String,
    @param:StringRes val title: Int,
    @param:DrawableRes val icon: Int,
    val parent: ShellDestination? = null,
) {
    TODAY("today", R.string.today, R.drawable.ic_today),
    TASKS("tasks", R.string.tasks, R.drawable.ic_checklist),
    CALENDAR("calendar", R.string.calendar, R.drawable.ic_calendar_month),
    REVIEW("review", R.string.review, R.drawable.ic_pending_actions),
    REWARDS("rewards", R.string.rewards, R.drawable.ic_storefront),
    STATISTICS("statistics", R.string.statistics, R.drawable.ic_bar_chart),
    SETTINGS("settings", R.string.settings, R.drawable.ic_settings),
    EXCLUDED_HABITS("excluded-habits", R.string.habit_excluded, R.drawable.ic_self_improvement, parent = SETTINGS),
    EARNINGS("earnings", R.string.earnings_setting, R.drawable.ic_toll, parent = SETTINGS),
    ;

    /** Exists only with game mode on (spec 017); off, requests for it land on Hoje. */
    val isGameOnly: Boolean get() = this == REWARDS || this == EARNINGS

    /** The header's settings shortcut: absent inside Settings itself and on its sub-pages (which show Back). */
    val showsSettingsButton: Boolean get() = this != SETTINGS && parent == null

    companion object {
        private val drawer = listOf(TODAY, CALENDAR, REVIEW, REWARDS, STATISTICS)

        fun drawerFor(gameMode: Boolean): List<ShellDestination> = drawer.filter { gameMode || !it.isGameOnly }
    }
}
