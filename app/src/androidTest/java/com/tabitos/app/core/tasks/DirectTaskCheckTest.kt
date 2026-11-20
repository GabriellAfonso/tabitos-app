package com.tabitos.app.core.tasks

import com.tabitos.app.core.preferences.FixedGameMode
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tabitos.app.core.accounting.data.RoomAccountingRepository
import com.tabitos.app.core.tasks.data.RoomTaskRepository
import com.tabitos.app.core.tasks.data.TaskDatabase
import com.tabitos.app.core.tasks.domain.*
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DirectTaskCheckTest {
    private val clock = object : TaskClock {
        override fun today(): LocalDate = LocalDate.of(2026, 11, 10)
        override fun nowMillis() = 100L
    }

    @Test fun desiredStateIsIdempotentAndRecurrenceUndoPreservesLaterActions() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        try {
            val tasks = RoomTaskRepository(db, clock, FixedGameMode(on = true))
            val wallet = RoomAccountingRepository(db, clock)
            tasks.execute(TaskAction.Create("t", TaskFields(title = "Routine", deadline = DeadlineKind.DAY,
                date = clock.today(), checklist = listOf("One")),
                RepeatRule(RepeatKind.AFTER_COMPLETION, clock.today(), interval = 1)))
            val on = TaskAction.SetCompleted("t:1", true, clock.today())
            val off = on.copy(done = false)
            tasks.execute(on); tasks.execute(on)
            assertEquals(10L, wallet.observe().first().balance)
            assertEquals(20L, wallet.observe().first().totalXp)
            assertEquals(1, tasks.observe().first().history.count { it.kind == AuditKind.COMPLETED })
            tasks.execute(off); tasks.execute(off)
            assertEquals(0L, wallet.observe().first().balance)
            assertEquals(0L, wallet.observe().first().totalXp)
            assertEquals(listOf("t:1"), tasks.observe().first().activeOccurrences
                .filter { it.status == TaskStatus.OPEN }.map { it.id })
            tasks.execute(on)
            tasks.execute(TaskAction.Check("t:2", 0, true))
            try {
                tasks.execute(off)
                fail("Later independent work must be preserved")
            } catch (e: TaskRuleException) { assertEquals(TaskProblem.DEPENDENT_ACTIONS, e.problem) }
            val committed = tasks.observe().first()
            assertEquals(TaskStatus.DONE, committed.occurrences.first { it.id == "t:1" }.status)
            assertTrue(committed.occurrences.first { it.id == "t:2" }.checks.single().checked)
            assertEquals(10L, wallet.observe().first().balance)
        } finally { db.close() }
    }
}
