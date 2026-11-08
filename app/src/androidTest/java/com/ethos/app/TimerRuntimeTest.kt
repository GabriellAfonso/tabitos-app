package com.ethos.app

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.ethos.app.core.accounting.data.ACCOUNTING_MIGRATION_2_3
import com.ethos.app.core.habits.data.HABIT_MIGRATION_1_2
import com.ethos.app.core.habits.data.RoomHabitRepository
import com.ethos.app.core.reminders.data.REMINDER_MIGRATION_4_5
import com.ethos.app.core.rewards.data.REWARD_MIGRATION_3_4
import com.ethos.app.core.tasks.data.*
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.timers.data.RoomTimerRepository
import com.ethos.app.core.timers.data.TIMER_MIGRATION_5_6
import com.ethos.app.core.timers.domain.*
import com.ethos.app.core.timers.platform.*
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Disposable test devices only: drives the real foreground service, ongoing notification and Pause action. */
class TimerRuntimeTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        }
    }
    private fun waitFor(timeout: Long, check: () -> Boolean): Boolean {
        val end = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < end) { if (check()) return true; SystemClock.sleep(250) }
        return false
    }

    @Test fun serviceShowsOngoingSessionPauseActionPersistsAndObjectiveStopsInBackground() = runBlocking {
        shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        val db = Room.databaseBuilder(context, TaskDatabase::class.java, "ethos-tasks.db")
            .addMigrations(HABIT_MIGRATION_1_2, ACCOUNTING_MIGRATION_2_3, REWARD_MIGRATION_3_4, REMINDER_MIGRATION_4_5,
                TIMER_MIGRATION_5_6).build()
        val clock = object : TaskClock {
            override fun today() = LocalDate.now()
            override fun nowMillis() = System.currentTimeMillis()
        }
        val tasks = RoomTaskRepository(db, clock)
        val timers = RoomTimerRepository(db, AndroidTimerClock(context))
        val coordinator = TimerCoordinator(context, timers, tasks, RoomHabitRepository(db, clock),
            TimerNotifications(context))
        val id = "timer-runtime-${UUID.randomUUID()}"
        try {
            tasks.execute(TaskAction.Create(id, TaskFields(title = "Leitura de teste"),
                RepeatRule(start = clock.today())))
            val occurrence = tasks.observe().first().occurrences.first { it.taskId == id }.id
            timers.observe().first().running?.let { timers.pause(it.id) }
            assertEquals(StartResult.STARTED, timers.start(TimerStart(TimerKind.TASK, occurrence, "Leitura de teste"),
                false))
            coordinator.sync()
            assertFalse("Foreground service start was refused", coordinator.serviceBlocked.value)
            val found = waitFor(30_000) {
                manager.activeNotifications.any { it.id == TimerNotifications.ONGOING_ID &&
                    it.notification.actions?.size == 2 } }
            assertTrue("Ongoing session notification missing: " + manager.activeNotifications.map {
                "${it.id}/${it.tag}/${it.notification.actions?.size}" } + " running=" +
                timers.observe().first().running?.sourceId + " own=" + occurrence, found)
            val ongoing = manager.activeNotifications.first { it.id == TimerNotifications.ONGOING_ID }.notification
            assertTrue(ongoing.flags and Notification.FLAG_ONGOING_EVENT != 0)
            assertEquals(Notification.VISIBILITY_PRIVATE, ongoing.visibility)
            assertEquals(2, ongoing.actions.size)
            SystemClock.sleep(1500)
            // The notification Pause updates the same persisted session.
            ongoing.actions[0].actionIntent.send()
            assertTrue("Pause from notification not persisted", waitFor(10_000) {
                runBlocking { timers.observe().first().running == null } })
            val snapshot = timers.observe().first()
            val session = snapshot.open(TimerKind.TASK, occurrence)!!
            assertEquals(TimerState.PAUSED, session.state)
            assertTrue(TimerRules.sessionTotal(snapshot, session.id, AndroidTimerClock(context).now()) >= 1000)
            assertTrue("Service did not stop", waitFor(15_000) {
                manager.activeNotifications.none { it.id == TimerNotifications.ONGOING_ID } })

            // A one-minute objective stops exactly at the cap with no UI and raises one alert.
            timers.saveGoal("task:$id", 1)
            timers.start(TimerStart(TimerKind.TASK, occurrence, "Leitura de teste", "task:$id"), false)
            coordinator.sync()
            assertTrue("Objective alert missing", waitFor(90_000) {
                manager.activeNotifications.any { it.tag == "ethos:timer:${session.id}" } })
            val after = timers.observe().first()
            assertNull(after.running)
            assertEquals(60_000L, TimerRules.progress(after, TimerKind.TASK, occurrence, TimerPeriod.TOTAL,
                clock.today(), AndroidTimerClock(context).now(), AndroidTimerClock(context).zone()))
            assertEquals(1, after.audits.count { it.sessionId == session.id && it.kind == TimerAuditKind.REACHED })
            // Reaching time never resolved the task or touched the ledger.
            assertEquals(TaskStatus.OPEN, tasks.observe().first().occurrences.first { it.id == occurrence }.status)
        } finally {
            manager.cancelAll()
            db.close()
        }
    }
}
