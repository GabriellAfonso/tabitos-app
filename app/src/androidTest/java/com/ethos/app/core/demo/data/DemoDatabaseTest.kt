package com.ethos.app.core.demo.data

import com.ethos.app.core.preferences.FixedGameMode
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ethos.app.R
import com.ethos.app.core.accounting.data.RoomAccountingRepository
import com.ethos.app.core.demo.domain.DemoTimerRunningException
import com.ethos.app.core.habits.data.RoomHabitRepository
import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.tasks.data.*
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.timers.data.RoomTimerRepository
import com.ethos.app.core.timers.domain.*
import com.ethos.app.core.trackers.data.RoomTrackerRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DemoDatabaseTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val clock = object : TaskClock {
        override fun today(): LocalDate = LocalDate.of(2026, 11, 10)
        override fun nowMillis(): Long = today().atTime(20, 0).atZone(ZoneId.of("America/Sao_Paulo"))
            .toInstant().toEpochMilli()
    }
    private val timerClock = object : TimerClock {
        override fun wallMillis() = clock.nowMillis()
        override fun elapsedMillis() = 1000L
        override fun bootCount() = 1
        override fun zone(): ZoneId = ZoneId.of("America/Sao_Paulo")
    }

    @Test fun restoreIsRepeatableAndFailedPopulationRollsBack() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        try {
            val text = context.resources.getStringArray(R.array.demo_content)
            RoomDemoRepository.restoreContent(db, clock, timerClock, text)
            val tasks = RoomTaskRepository(db, clock, FixedGameMode(on = true))
            val habits = RoomHabitRepository(db, clock, FixedGameMode(on = true))
            val accounting = RoomAccountingRepository(db, clock)
            val beforeTasks = tasks.observe().first()
            val beforeHabits = habits.observe().first()
            val beforeWallet = accounting.observe().first()
            assertEquals(4, beforeTasks.lists.size)
            assertEquals(6, beforeHabits.habits.size)
            assertTrue(beforeWallet.balance > 0)
            assertTrue(beforeWallet.totalXp > 0)
            assertEquals(1, db.rewards().purchases().size)
            assertTrue(RoomTrackerRepository(db, clock).observe().first().entries.isNotEmpty())
            assertTrue(db.reminders().definitions().isEmpty())
            assertTrue(db.timers().sessions().isEmpty())
            try {
                RoomDemoRepository.restoreContent(db, clock, timerClock, emptyArray())
                fail("Invalid fixture must fail")
            } catch (_: IndexOutOfBoundsException) { /* injected failure after content deletion */ }
            assertEquals(beforeTasks, tasks.observe().first())
            assertEquals(beforeHabits, habits.observe().first())
            assertEquals(beforeWallet, accounting.observe().first())
            tasks.execute(TaskAction.SaveList(TaskList("extra", "Extra")))
            RoomDemoRepository.restoreContent(db, clock, timerClock, text)
            assertEquals(beforeTasks.occurrences.size, tasks.observe().first().occurrences.size)
            assertEquals(beforeTasks.lists, tasks.observe().first().lists)
            assertEquals(beforeHabits.records.size, habits.observe().first().records.size)
            assertEquals(beforeWallet.balance, accounting.observe().first().balance)
            assertEquals(beforeWallet.totalXp, accounting.observe().first().totalXp)
            assertEquals(1, db.rewards().purchases().size)
            val timers = RoomTimerRepository(db, timerClock)
            assertEquals(StartResult.STARTED, timers.start(
                TimerStart(TimerKind.TASK, "demo-proposal:1", "Teste"), false))
            try {
                RoomDemoRepository.restoreContent(db, clock, timerClock, text)
                fail("Running timer must block reset")
            } catch (_: DemoTimerRunningException) { }
            assertEquals(beforeTasks.lists, tasks.observe().first().lists)
            assertEquals(1, db.timers().sessions().size)
        } finally { db.close() }
    }

    @Test fun desiredPracticeStateReversesMoneyAndRemovesLegacyDuplicateDays() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        try {
            val repo = RoomHabitRepository(db, clock, FixedGameMode(on = true))
            val wallet = RoomAccountingRepository(db, clock)
            repo.execute(HabitAction.Save(Habit("h", clock.today(), "Ler"),
                HabitRule("r", "h", clock.today(), unit = HabitUnit.WEEK, target = 3), partialTarget = 3))
            repo.execute(HabitAction.Record("legacy-a", "h", clock.today()))
            repo.execute(HabitAction.Record("legacy-b", "h", clock.today()))
            val earned = wallet.observe().first()
            assertTrue(earned.balance > 0)
            val off = HabitAction.SetPractice("off", "h", clock.today(), false)
            repo.execute(off); repo.execute(off)
            assertTrue(repo.observe().first().records.all { it.deleted })
            assertEquals(0L, wallet.observe().first().balance)
            assertEquals(0L, wallet.observe().first().totalXp)
            val on = HabitAction.SetPractice("on", "h", clock.today(), true)
            repo.execute(on); repo.execute(on)
            assertEquals(1, repo.observe().first().records.count { !it.deleted })
            assertEquals(earned.balance, wallet.observe().first().balance)
            assertEquals(earned.totalXp, wallet.observe().first().totalXp)
        } finally { db.close() }
    }
}
