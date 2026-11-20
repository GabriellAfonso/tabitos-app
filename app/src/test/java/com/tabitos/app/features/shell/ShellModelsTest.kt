package com.tabitos.app.features.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        assertEquals(ShellDestination.SETTINGS, ShellDestination.EARNINGS.parent)
    }

    @Test fun earningsPageExistsOnlyWithGameMode() {
        assertTrue(ShellDestination.EARNINGS.isGameOnly)
        assertFalse(ShellDestination.EXCLUDED_HABITS.isGameOnly)
    }
}
