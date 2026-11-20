package com.tabitos.app

import com.tabitos.app.core.startup.DeferredSource
import com.tabitos.app.features.accounting.AccountingUiState
import com.tabitos.app.features.habits.HabitsUiState
import com.tabitos.app.features.settings.GameModeUiState
import com.tabitos.app.features.shell.ShellDestination
import com.tabitos.app.features.tasks.TasksUiState
import com.tabitos.app.features.timers.TimerUiState
import com.tabitos.app.features.trackers.TrackerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TodayReadinessTest {
    private val day = LocalDate.of(2026, 11, 17)

    private fun sources(
        tasks: Boolean = false, habits: Boolean = false, timers: Boolean = false, trackers: Boolean = false,
        wallet: Boolean = false, gameModeLoading: Boolean = false, gameMode: Boolean = true,
    ) = TodaySources(
        tasks = TasksUiState(today = day, loading = tasks),
        habits = HabitsUiState(today = day, loading = habits),
        timers = TimerUiState(loading = timers),
        trackers = TrackerUiState(today = day, loading = trackers),
        wallet = AccountingUiState(loading = wallet),
        gameMode = GameModeUiState(enabled = gameMode, loading = gameModeLoading),
    )

    @Test fun readyOnlyWhenEverySourceLoaded() {
        assertTrue(sources().isReady())
        assertFalse(sources(tasks = true).isReady())
        assertFalse(sources(habits = true).isReady())
        assertFalse(sources(timers = true).isReady())
        assertFalse(sources(trackers = true).isReady())
        assertFalse(sources(gameModeLoading = true).isReady())
    }

    @Test fun walletMattersOnlyInGameMode() {
        assertFalse(sources(wallet = true).isReady())
        assertTrue(sources(wallet = true, gameMode = false).isReady())
    }

    @Test fun failedSourceCountsAsLoaded() {
        val failed = sources().let {
            TodaySources(it.tasks.copy(storageError = true), it.habits, it.timers, it.trackers,
                it.wallet.copy(error = true), it.gameMode)
        }
        assertTrue(failed.isReady())
    }

    @Test fun pagesStartTheirDeferredSources() {
        assertEquals(setOf(DeferredSource.CALENDAR), ShellDestination.CALENDAR.deferredSources())
        assertEquals(setOf(DeferredSource.REWARDS), ShellDestination.REWARDS.deferredSources())
        assertEquals(setOf(DeferredSource.REMINDERS), ShellDestination.SETTINGS.deferredSources())
        assertEquals(emptySet<DeferredSource>(), ShellDestination.TODAY.deferredSources())
        assertEquals(emptySet<DeferredSource>(), ShellDestination.STATISTICS.deferredSources())
    }
}
