package com.tabitos.app.features.update

import com.tabitos.app.core.update.domain.AppUpdatePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdatePresentationTest {
    @Test fun readyOffersRestartOnlyWithoutACountingTimer() {
        assertEquals(UpdateNotice.READY, updateNotice(AppUpdatePhase.READY, timerRunning = false))
        assertEquals(UpdateNotice.WAITING_TIMER, updateNotice(AppUpdatePhase.READY, timerRunning = true))
    }

    @Test fun otherPhasesShowNoStrip() {
        AppUpdatePhase.entries.filter { it != AppUpdatePhase.READY }.forEach { phase ->
            assertNull(updateNotice(phase, timerRunning = false))
            assertNull(updateNotice(phase, timerRunning = true))
        }
    }
}
