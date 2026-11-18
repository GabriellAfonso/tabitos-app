package com.ethos.app

import com.ethos.app.core.startup.DeferredSource
import com.ethos.app.features.accounting.AccountingUiState
import com.ethos.app.features.habits.HabitsUiState
import com.ethos.app.features.settings.GameModeUiState
import com.ethos.app.features.shell.ShellDestination
import com.ethos.app.features.tasks.TasksUiState
import com.ethos.app.features.timers.TimerUiState
import com.ethos.app.features.trackers.TrackerUiState

/** The states of every source Hoje draws (spec 027). */
class TodaySources(
    val tasks: TasksUiState, val habits: HabitsUiState, val timers: TimerUiState, val trackers: TrackerUiState,
    val wallet: AccountingUiState, val gameMode: GameModeUiState,
)

/**
 * Hoje is drawn complete, never in pieces: only once every source it shows finished its first load. A source that
 * failed counts as finished, since it then shows its own error and retry. The wallet is drawn only in game mode.
 */
fun TodaySources.isReady(): Boolean {
    if (gameMode.loading) return false
    val walletPending = gameMode.enabled && wallet.loading
    return !tasks.loading && !habits.loading && !timers.loading && !trackers.loading && !walletPending
}

/** Deferred sources a page draws; showing the page starts them without waiting for the preload queue. */
fun ShellDestination.deferredSources(): Set<DeferredSource> = when (this) {
    ShellDestination.CALENDAR -> setOf(DeferredSource.CALENDAR)
    ShellDestination.REWARDS -> setOf(DeferredSource.REWARDS)
    ShellDestination.SETTINGS -> setOf(DeferredSource.REMINDERS)
    ShellDestination.TODAY, ShellDestination.TASKS, ShellDestination.REVIEW, ShellDestination.STATISTICS,
    ShellDestination.EXCLUDED_HABITS, ShellDestination.EARNINGS -> emptySet()
}
