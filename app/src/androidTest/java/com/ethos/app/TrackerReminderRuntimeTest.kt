package com.ethos.app

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.ethos.app.core.habits.data.*
import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.reminders.data.RoomReminderRepository
import com.ethos.app.core.reminders.domain.*
import com.ethos.app.core.reminders.platform.ReminderCoordinator
import com.ethos.app.core.reminders.platform.ReminderPlatform
import com.ethos.app.core.tasks.data.RoomTaskRepository
import com.ethos.app.core.tasks.data.TaskDatabase
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.trackers.data.RoomTrackerRepository
import com.ethos.app.core.trackers.domain.*
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class TrackerReminderRuntimeTest {
    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        }
    }

    @Test fun neutralOngoingNoticeSurvivesDismissInAppAndClearsOnSave() = runBlocking {
        val now = LocalTime.now()
        assumeTrue("needs a minute already passed today", now.hour * 60 + now.minute >= 2)
        val context = ApplicationProvider.getApplicationContext<Context>()
        shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        val today = LocalDate.now()
        val clock = object : TaskClock {
            override fun today() = LocalDate.now()
            override fun nowMillis() = System.currentTimeMillis()
        }
        val habits = RoomHabitRepository(db, clock)
        val trackers = RoomTrackerRepository(db, clock)
        val repository = RoomReminderRepository(db)
        val platform = ReminderPlatform(context)
        val coordinator = ReminderCoordinator(repository, RoomTaskRepository(db, clock), habits, trackers, platform)
        val manager = context.getSystemService(NotificationManager::class.java)
        var key: String? = null
        try {
            habits.execute(HabitAction.Save(Habit("h", today, "Nome privado do hábito"), HabitRule("r", "h", today)))
            trackers.execute(TrackerAction.SaveForm("t", "h", false, listOf(
                TrackerQuestion("u", QuestionType.SCALE, "Pergunta privada", required = true))))
            val minute = now.hour * 60 + now.minute - 1
            repository.save(ReminderDefinition("tracker:t", kind = ReminderKind.TRACKER, sourceId = "t",
                mode = ReminderMode.WEEKDAYS, minute = minute, createdAt = 0))
            coordinator.reconcile()
            val row = repository.read().deliveries.single { it.slotDate == today }
            key = row.key
            assertEquals(DeliveryState.POSTED, row.state)
            val notice = manager.activeNotifications.single { it.tag == key }.notification
            val text = notice.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
            val title = notice.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
            assertEquals("Você tem um acompanhamento para preencher", text)
            assertEquals("Ethos", title)
            assertFalse(text.contains("privad") || title.contains("privad"))
            assertTrue(notice.flags and Notification.FLAG_ONGOING_EVENT != 0)
            assertEquals(Notification.VISIBILITY_PRIVATE, notice.visibility)
            assertEquals(listOf("Preencher", "Adiar"), notice.actions.map { it.title.toString() })
            // Dismissal (simulated) is not resolution: no repost loop, pending remains in the app.
            platform.cancel(key)
            repeat(2) { coordinator.reconcile() }
            assertTrue(manager.activeNotifications.none { it.tag == key })
            val pending = TrackerRules.pending(trackers.observe().first(), habits.observe().first(),
                mapOf("t" to minute), today, now.hour * 60 + now.minute)
            assertEquals(1, pending.size)
            platform.show(row, "")
            trackers.execute(TrackerAction.SaveEntry("e", "t", today, listOf(TrackerAnswer("u", scale = 3))))
            coordinator.reconcile()
            assertEquals(DeliveryState.CANCELLED, repository.read().deliveries.single { it.key == key }.state)
            assertTrue(manager.activeNotifications.none { it.tag == key })
        } finally {
            key?.let(platform::cancel)
            platform.schedule(null)
            db.close()
        }
    }
}
