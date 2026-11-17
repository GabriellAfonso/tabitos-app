package com.ethos.app.features.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellModelsTest {
    @Test fun drawerHidesRewardsOnlyWithGameModeOff() {
        assertEquals(listOf(ShellDestination.TODAY, ShellDestination.CALENDAR, ShellDestination.REVIEW,
            ShellDestination.REWARDS, ShellDestination.STATISTICS), ShellDestination.drawerFor(gameMode = true))
        assertEquals(listOf(ShellDestination.TODAY, ShellDestination.CALENDAR, ShellDestination.REVIEW,
            ShellDestination.STATISTICS), ShellDestination.drawerFor(gameMode = false))
    }

    @Test fun settingsSubPagesReturnToSettings() {
        assertEquals(ShellDestination.SETTINGS, ShellDestination.EXCLUDED_HABITS.parent)
    }
}
