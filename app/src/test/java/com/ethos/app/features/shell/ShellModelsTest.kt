package com.ethos.app.features.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellModelsTest {
    @Test fun drawerHidesRewardsAndHistoryOnlyWithGameModeOff() {
        assertEquals(listOf(ShellDestination.TODAY, ShellDestination.REVIEW, ShellDestination.REWARDS,
            ShellDestination.HISTORY), ShellDestination.drawerFor(gameMode = true))
        assertEquals(listOf(ShellDestination.TODAY, ShellDestination.REVIEW),
            ShellDestination.drawerFor(gameMode = false))
    }
}
