package com.ethos.app.features.shell

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

    @Test fun settingsShortcutIsHiddenInsideSettings() {
        assertFalse(ShellDestination.SETTINGS.showsSettingsButton)
        assertFalse(ShellDestination.EARNINGS.showsSettingsButton)
        assertFalse(ShellDestination.EXCLUDED_HABITS.showsSettingsButton)
        assertTrue(ShellDestination.TODAY.showsSettingsButton)
        assertTrue(ShellDestination.STATISTICS.showsSettingsButton)
    }
}
