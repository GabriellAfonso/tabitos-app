package com.tabitos.app

import com.tabitos.app.core.preferences.FixedGameMode
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.tabitos.app.core.tasks.data.*
import com.tabitos.app.core.tasks.domain.*
import com.tabitos.app.core.habits.data.RoomHabitRepository
import com.tabitos.app.core.habits.data.HABIT_MIGRATION_1_2
import com.tabitos.app.core.accounting.data.ACCOUNTING_MIGRATION_2_3
import com.tabitos.app.core.rewards.data.REWARD_MIGRATION_3_4
import com.tabitos.app.core.reminders.data.*
import com.tabitos.app.core.reminders.domain.*
import com.tabitos.app.core.reminders.platform.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import java.time.*
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

/** Runs only on disposable test devices; fixture is created through normal domain repositories. */
class ReminderRuntimeTest {
    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        }
    }
    private fun channelEnabled(enabled: Boolean) {
        shell("am start -W -a android.settings.CHANNEL_NOTIFICATION_SETTINGS " +
            "--es android.provider.extra.APP_PACKAGE com.tabitos.app " +
            "--es android.provider.extra.CHANNEL_ID tabitos_reminders")
        fun toggle(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.className == "android.widget.Button" && node.text?.toString() in setOf("Wait", "Aguardar")) {
                var clickable: AccessibilityNodeInfo? = node
                while (clickable != null && !clickable.isClickable) clickable = clickable.parent
                clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return null
            }
            if (node.isCheckable && node.className?.contains("Switch") == true) return node
            for (index in 0 until node.childCount) toggle(node.getChild(index))?.let { return it }
            return null
        }
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val end = SystemClock.elapsedRealtime() + 15000
        while (SystemClock.elapsedRealtime() < end) {
            val node = toggle(automation.rootInActiveWindow)
            if (node != null) {
                if (node.isChecked == enabled) return
                var clickable: AccessibilityNodeInfo? = node
                while (clickable != null && !clickable.isClickable) clickable = clickable.parent
                clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                SystemClock.sleep(500)
            } else SystemClock.sleep(250)
        }
        error("Notification channel toggle was not available")
    }
    @Test fun realAlarmReceiverPostsPrivateActionsAndCompletionCancelsSnooze() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        shell("appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow")
        val db = Room.databaseBuilder(context, TaskDatabase::class.java, "tabitos-tasks.db")
            .addMigrations(HABIT_MIGRATION_1_2, ACCOUNTING_MIGRATION_2_3, REWARD_MIGRATION_3_4,
                REMINDER_MIGRATION_4_5,
                com.tabitos.app.core.timers.data.TIMER_MIGRATION_5_6,
                com.tabitos.app.core.trackers.data.TRACKER_MIGRATION_6_7).build()
        val clock = object : TaskClock {
            override fun today() = LocalDate.now()
            override fun nowMillis() = System.currentTimeMillis()
        }
        val tasks = RoomTaskRepository(db, clock, FixedGameMode(on = true))
        val repository = RoomReminderRepository(db, clock)
        val platform = ReminderPlatform(context)
        val coordinator = ReminderCoordinator(repository, tasks, RoomHabitRepository(db, clock, FixedGameMode(on = true)),
            com.tabitos.app.core.trackers.data.RoomTrackerRepository(db, clock), platform, clock)
        val id = "runtime-${UUID.randomUUID()}"
        val at = ZonedDateTime.now().plusMinutes(1).withSecond(0).withNano(0)
            .let { if (it.toInstant().toEpochMilli() - System.currentTimeMillis() < 15000) it.plusMinutes(1) else it }
        val definition = ReminderDefinition(id, kind = ReminderKind.TASK, sourceId = id, occurrenceId = "$id:1",
            chosenDate = at.toLocalDate(), minute = at.hour * 60 + at.minute, createdAt = System.currentTimeMillis())
        val manager = context.getSystemService(NotificationManager::class.java)
        var key: String? = null
        try {
            tasks.execute(TaskAction.Create(id, TaskFields(title = "Teste local de lembrete",
                deadline = DeadlineKind.DAY,
                date = at.toLocalDate()), RepeatRule(start = at.toLocalDate())))
            repository.save(definition)
            coordinator.reconcile()
            key = repository.read().deliveries.first { it.groupId == id }.key
            assertTrue(platform.capability().exact)
            val timeout = SystemClock.elapsedRealtime() + 150000
            while (manager.activeNotifications.none { it.tag == key } && SystemClock.elapsedRealtime() < timeout)
                SystemClock.sleep(250)
            val notice = manager.activeNotifications.firstOrNull { it.tag == key }
            assertNotNull("Actual AlarmManager receiver notification was not observed", notice)
            val notification = notice!!.notification
            assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
            assertNotNull(notification.publicVersion)
            assertEquals(3, notification.actions.size)
            assertEquals(listOf("Concluir", "Adiar", "Abrir"), notification.actions.map { it.title.toString() })
            val future = System.currentTimeMillis() + 600000
            coordinator.snooze(key, future)
            coordinator.reconcile()
            assertEquals(future, repository.read().deliveries.first { it.key == key }.dueAt)
            assertTrue(manager.activeNotifications.none { it.tag == key })
            tasks.execute(TaskAction.Resolve("$id:1", true, LocalDate.now()))
            coordinator.reconcile()
            assertEquals(DeliveryState.CANCELLED, repository.read().deliveries.first { it.key == key }.state)
            assertTrue(manager.activeNotifications.none { it.tag == key })
        } finally {
            repository.remove(definition)
            tasks.execute(TaskAction.Delete("$id:1", ChangeScope.ONLY_THIS))
            key?.let(platform::cancel)
            coordinator.reconcileSafely(false)
            db.close()
        }
    }

    @Test fun blockedDeliveryRecoversOnceWithoutMutatingTaskAndResolvedSummaryDisappears() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        val clock = object : TaskClock {
            override fun today() = LocalDate.now()
            override fun nowMillis() = System.currentTimeMillis()
        }
        val tasks = RoomTaskRepository(db, clock, FixedGameMode(on = true))
        val repository = RoomReminderRepository(db, clock)
        val platform = ReminderPlatform(context)
        val coordinator = ReminderCoordinator(repository, tasks, RoomHabitRepository(db, clock, FixedGameMode(on = true)),
            com.tabitos.app.core.trackers.data.RoomTrackerRepository(db, clock), platform, clock)
        val missed = ZonedDateTime.now().minusMinutes(10)
        val id = "missed-${UUID.randomUUID()}"
        try {
            platform.cancel(ReminderPlatform.RECOVERY_TAG)
            tasks.execute(TaskAction.Create(id, TaskFields(title = "Fictício de recuperação",
                deadline = DeadlineKind.DAY, date = missed.toLocalDate()), RepeatRule(start = missed.toLocalDate())))
            repository.save(ReminderDefinition(id, kind = ReminderKind.TASK, sourceId = id,
                occurrenceId = "$id:1", chosenDate = missed.toLocalDate(), minute = missed.hour * 60 + missed.minute))
            platform.channels()
            channelEnabled(false)
            assertFalse(platform.capability().reminders)
            coordinator.reconcile()
            assertEquals(DeliveryState.PENDING, repository.read().deliveries.single().state)
            channelEnabled(true)
            coordinator.reconcile()
            assertEquals(DeliveryState.RECOVERED, repository.read().deliveries.single().state)
            repeat(3) { coordinator.reconcile() }
            val manager = context.getSystemService(NotificationManager::class.java)
            assertEquals(1, manager.activeNotifications.count { it.tag == ReminderPlatform.RECOVERY_TAG })
            assertEquals(TaskStatus.OPEN, tasks.observe().first().occurrences.single().status)
            platform.cancel(ReminderPlatform.RECOVERY_TAG) // Simulate dismiss; reconciliation must not repost.
            coordinator.reconcile()
            assertTrue(manager.activeNotifications.none { it.tag == ReminderPlatform.RECOVERY_TAG })
            tasks.execute(TaskAction.Resolve("$id:1", true, LocalDate.now()))
            coordinator.reconcile()
            assertEquals(DeliveryState.CANCELLED, repository.read().deliveries.single().state)
        } finally {
            channelEnabled(true)
            platform.cancel(ReminderPlatform.RECOVERY_TAG)
            platform.schedule(null)
            db.close()
        }
    }
}
