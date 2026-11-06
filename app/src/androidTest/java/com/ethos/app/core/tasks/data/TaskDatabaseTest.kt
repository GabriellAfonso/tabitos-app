package com.ethos.app.core.tasks.data

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.ethos.app.core.tasks.domain.*
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TaskDatabaseTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val clock = object : TaskClock {
        override fun today(): LocalDate = LocalDate.of(2026, 11, 6)
        override fun nowMillis(): Long = 10
    }

    @Test fun fileBackedReopenRetainsIndependentOccurrencesAndCorrections() = runBlocking {
        val name = "task-test-${System.nanoTime()}.db"
        var db = Room.databaseBuilder(context, TaskDatabase::class.java, name).build()
        try {
            var repo = RoomTaskRepository(db, clock)
            repo.execute(TaskAction.SaveList(TaskList("l", "Work", 2, 1)))
            repo.execute(TaskAction.Create("a", TaskFields(title = "Persist", listId = "l",
                deadline = DeadlineKind.DAY, date = clock.today(), checklist = listOf("Check")),
                RepeatRule(RepeatKind.DAILY, clock.today(), count = 3)))
            repo.execute(TaskAction.Check("a:1", 0, true))
            repo.execute(TaskAction.Resolve("a:1", true, clock.today()))
            repo.execute(TaskAction.Undo("a:1"))
            db.close()
            db = Room.databaseBuilder(context, TaskDatabase::class.java, name).build()
            repo = RoomTaskRepository(db, clock)
            val s = repo.observe().first()
            assertEquals("Work", s.activeLists.single().name)
            assertTrue(s.occurrences.first { it.ordinal == 1 }.checks.single().checked)
            assertFalse(s.occurrences.first { it.ordinal == 2 }.checks.single().checked)
            assertEquals(TaskStatus.OPEN, s.occurrences.first { it.ordinal == 1 }.status)
            assertTrue(s.history.any { it.kind == AuditKind.UNDONE && it.relatedOutcome != null })
            repo.execute(TaskAction.Refresh)
            assertEquals(s, repo.observe().first())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun transactionFailureRollsBackAndInvalidMutationDoesNotEraseData() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        try {
            val repo = RoomTaskRepository(db, clock)
            repo.execute(TaskAction.Create("a", TaskFields(title = "Keep"), RepeatRule(start = clock.today())))
            val before = repo.observe().first()
            try {
                db.withTransaction {
                    db.tasks().lists(listOf(ListEntity("temporary", "Temporary", 0, 0, 0, false)))
                    error("Injected transaction failure")
                }
                fail("Expected rollback")
            } catch (_: IllegalStateException) { /* deliberate rollback */ }
            assertEquals(before, repo.observe().first())
            try {
                repo.execute(TaskAction.Resolve("a:1", true, clock.today().plusDays(1)))
                fail("Expected invalid date")
            } catch (_: TaskRuleException) { /* validation must leave data intact */ }
            assertEquals(before, repo.observe().first())
        } finally { db.close() }
    }
}
