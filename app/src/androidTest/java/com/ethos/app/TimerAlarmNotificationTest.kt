package com.ethos.app

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.ethos.app.core.timers.domain.TimerAuditKind
import com.ethos.app.core.timers.domain.TimerKind
import com.ethos.app.core.timers.domain.TimerSession
import com.ethos.app.core.timers.domain.TimerState
import com.ethos.app.core.timers.platform.TimerNotifications
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec 026: a ringing timer alert can be stopped from the notification, including the lock-screen version. */
class TimerAlarmNotificationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private val notifications = TimerNotifications(context)

    private fun grantNotifications() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
            .use { descriptor -> java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() } }
    }

    private fun posted(sessionId: String): Notification? =
        manager.activeNotifications.firstOrNull { it.tag == "ethos:timer:$sessionId" }?.notification

    private fun waitFor(check: () -> Boolean): Boolean {
        val end = SystemClock.elapsedRealtime() + TIMEOUT_MILLIS
        while (SystemClock.elapsedRealtime() < end) { if (check()) return true; SystemClock.sleep(POLL_MILLIS) }
        return false
    }

    private fun session() = TimerSession("alarm-${UUID.randomUUID()}", TimerKind.TASK, "source", "Leitura de teste",
        TimerState.PAUSED, created = 0)

    @Test fun reachedGoalWithAlarmOffersStopAndOpenAndStopRemovesIt() {
        grantNotifications()
        notifications.channels()
        val session = session()
        assertTrue(notifications.alert(session, TimerAuditKind.REACHED, alarm = true))
        assertTrue("Alarm notification missing", waitFor { posted(session.id) != null })
        val alert = posted(session.id)!!
        val stop = context.getString(R.string.timer_alarm_stop)
        assertEquals(listOf(stop, context.getString(R.string.timer_open)), alert.actions.map { it.title.toString() })
        assertEquals(listOf(stop), alert.publicVersion.actions.map { it.title.toString() })
        assertTrue(alert.deleteIntent != null)

        notifications.cancelAlert(session.id)
        assertTrue("Stopping should remove the ringing notification", waitFor { posted(session.id) == null })
    }

    @Test fun silentAlertsKeepNoActions() {
        grantNotifications()
        notifications.channels()
        val session = session()
        assertTrue(notifications.alert(session, TimerAuditKind.REACHED, alarm = false))
        assertTrue(waitFor { posted(session.id) != null })
        assertTrue(posted(session.id)!!.actions.isNullOrEmpty())
        notifications.cancelAlert(session.id)
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
        const val POLL_MILLIS = 100L
    }
}
