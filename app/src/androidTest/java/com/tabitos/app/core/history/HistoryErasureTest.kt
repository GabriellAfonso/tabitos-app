package com.tabitos.app.core.history

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tabitos.app.core.accounting.data.RoomAccountingRepository
import com.tabitos.app.core.habits.data.RoomHabitRepository
import com.tabitos.app.core.habits.domain.Habit
import com.tabitos.app.core.habits.domain.HabitAction
import com.tabitos.app.core.habits.domain.HabitRule
import com.tabitos.app.core.preferences.FixedGameMode
import com.tabitos.app.core.tasks.data.RoomTaskRepository
import com.tabitos.app.core.tasks.data.TaskDatabase
import com.tabitos.app.core.tasks.domain.ChangeScope
import com.tabitos.app.core.tasks.domain.RepeatRule
import com.tabitos.app.core.tasks.domain.TaskAction
import com.tabitos.app.core.tasks.domain.TaskClock
import com.tabitos.app.core.tasks.domain.TaskFields
import com.tabitos.app.core.timers.data.TimerSessionEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Erasing history removes the rows of a task/habit everywhere but never changes coins or XP (spec 022). */
class HistoryErasureTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.of(2026, 11, 17)
    private var time = 100L
    private val clock = object : TaskClock {
        override fun today() = day
        override fun nowMillis() = time++
    }
    private fun memory() = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()

    @Test fun erasingADoneTaskKeepsBalanceAndClearsItsTitles() = runBlocking {
        val db = memory()
        try {
            val tasks = RoomTaskRepository(db, clock, FixedGameMode(on = true))
            val wallet = RoomAccountingRepository(db, clock)
            tasks.execute(TaskAction.Create("t", TaskFields(title = "Consulta"), RepeatRule(start = day)))
            tasks.execute(TaskAction.Resolve("t:1", true, day))
            db.timers().session(TimerSessionEntity("s", "TASK", "t:1", "Consulta", "PAUSED", "TOTAL", null, null,
                0, false))
            val before = wallet.observe().first()
            tasks.execute(TaskAction.Delete("t:1", ChangeScope.ONLY_THIS, eraseHistory = true))
            val after = wallet.observe().first()
            assertEquals(before.balance, after.balance)
            assertEquals(before.totalXp, after.totalXp)
            assertEquals(before.entries.size, after.entries.size)
            assertTrue(after.entries.all { it.title.isEmpty() })
            val snapshot = tasks.observe().first()
            assertTrue(snapshot.occurrences.isEmpty() && snapshot.history.isEmpty() && snapshot.definitions.isEmpty())
            assertTrue(db.timers().sessions().isEmpty())
        } finally { db.close() }
    }

    @Test fun deletingWithoutEraseKeepsHistory() = runBlocking {
        val db = memory()
        try {
            val tasks = RoomTaskRepository(db, clock, FixedGameMode(on = true))
            tasks.execute(TaskAction.Create("t", TaskFields(title = "Consulta"), RepeatRule(start = day)))
            tasks.execute(TaskAction.Delete("t:1", ChangeScope.ONLY_THIS))
            val snapshot = tasks.observe().first()
            assertEquals(1, snapshot.occurrences.size)
            assertTrue(snapshot.history.isNotEmpty())
        } finally { db.close() }
    }

    @Test fun erasingAHabitRemovesItAndKeepsTheWallet() = runBlocking {
        val db = memory()
        try {
            val habits = RoomHabitRepository(db, clock, FixedGameMode(on = true))
            val wallet = RoomAccountingRepository(db, clock)
            habits.execute(HabitAction.Save(Habit("h", day, "Ler"), HabitRule("r", "h", day)))
            habits.execute(HabitAction.SetPractice("p", "h", day, true))
            val before = wallet.observe().first()
            habits.erase("h")
            val after = wallet.observe().first()
            assertEquals(before.balance, after.balance)
            assertEquals(before.totalXp, after.totalXp)
            assertTrue(after.entries.all { it.title.isEmpty() })
            val snapshot = habits.observe().first()
            assertTrue(snapshot.habits.isEmpty() && snapshot.records.isEmpty() && snapshot.rules.isEmpty() &&
                snapshot.intervals.isEmpty() && snapshot.audits.isEmpty())
        } finally { db.close() }
    }
}
