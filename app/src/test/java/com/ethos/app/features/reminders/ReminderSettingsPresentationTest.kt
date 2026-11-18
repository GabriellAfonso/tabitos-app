package com.ethos.app.features.reminders

import com.ethos.app.core.reminders.platform.NotificationCapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ReminderSettingsPresentationTest {
    @Test fun warnsOnlyAboutWhatAndroidBlocks() {
        assertEquals(BlockedNotifications.ALL, capability(reminders = false, summaries = false).blockedNotifications())
        assertEquals(BlockedNotifications.REMINDERS,
            capability(reminders = false, summaries = true).blockedNotifications())
        assertEquals(BlockedNotifications.SUMMARIES,
            capability(reminders = true, summaries = false).blockedNotifications())
    }

    @Test fun preciseAlarmAccessIsNotABlock() {
        assertNull(capability(reminders = true, summaries = true, exact = false).blockedNotifications())
    }

    @Test fun preciseTimeRowOnlyWhileDenied() {
        assertTrue(capability(reminders = true, summaries = true, exact = false).asksPreciseAlarms)
        assertFalse(capability(reminders = true, summaries = true, exact = true).asksPreciseAlarms)
    }

    private fun capability(reminders: Boolean, summaries: Boolean, exact: Boolean = true) =
        NotificationCapability(reminders, summaries, exact)
}
