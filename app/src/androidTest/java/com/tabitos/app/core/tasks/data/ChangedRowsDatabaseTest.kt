package com.tabitos.app.core.tasks.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tabitos.app.core.habits.data.RoomHabitRepository
import com.tabitos.app.core.habits.domain.Habit
import com.tabitos.app.core.habits.domain.HabitAction
import com.tabitos.app.core.habits.domain.HabitRule
import com.tabitos.app.core.habits.domain.HabitRules
import com.tabitos.app.core.habits.domain.HabitType
import com.tabitos.app.core.habits.domain.HabitUnit
import com.tabitos.app.core.preferences.FixedGameMode
import com.tabitos.app.core.reminders.data.entity
import com.tabitos.app.core.reminders.domain.ReminderDefinition
import com.tabitos.app.core.reminders.domain.ReminderKind
import com.tabitos.app.core.tasks.domain.AuditKind
import com.tabitos.app.core.tasks.domain.ChangeScope
import com.tabitos.app.core.tasks.domain.DeadlineKind
import com.tabitos.app.core.tasks.domain.RepeatKind
import com.tabitos.app.core.tasks.domain.RepeatRule
import com.tabitos.app.core.tasks.domain.TaskAction
import com.tabitos.app.core.tasks.domain.TaskClock
import com.tabitos.app.core.tasks.domain.TaskFields
import com.tabitos.app.core.tasks.domain.TaskRules
import java.time.LocalDate
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Saves that write only changed rows (spec 027) keep the stored result and wake no observer when nothing changed. */
class ChangedRowsDatabaseTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.of(2026, 11, 17)
    private val clock = object : TaskClock {
        override fun today(): LocalDate = day
        override fun nowMillis(): Long = 1_000
    }
    private lateinit var db: TaskDatabase

    @Before fun open() { db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build() }
    @After fun close() { db.close() }

    @Test fun storedTasksMatchTheRulesAfterEachAction() = runBlocking {
        val repo = RoomTaskRepository(db, clock, FixedGameMode(on = true))
        val actions = listOf(
            TaskAction.Create("a", TaskFields(title = "Relatório", checklist = listOf("Dados", "Gráficos")),
                RepeatRule(start = day)),
            TaskAction.Create("r", TaskFields(title = "Regar", deadline = DeadlineKind.DAY, date = day),
                RepeatRule(RepeatKind.DAILY, day)),
            TaskAction.Check("a:1", 1, true),
            TaskAction.Edit("a:1", TaskFields(title = "Relatório final", checklist = listOf("Dados")),
                RepeatRule(start = day), ChangeScope.ONLY_THIS),
            TaskAction.Reschedule("r:1", day, day.plusDays(1)),
            TaskAction.Resolve("a:1", true, day),
            TaskAction.Delete("r:1", ChangeScope.THIS_AND_FUTURE, eraseHistory = true),
        )
        var expected = repo.observe().first()
        actions.forEach { action ->
            expected = TaskRules.apply(expected, action, day, clock.nowMillis())
            repo.execute(action)
            assertEquals(expected, repo.observe().first())
        }
    }

    @Test fun rescheduleAndUndoPersistAuditsAndReminderDates() = runBlocking {
        val repo = RoomTaskRepository(db, clock, FixedGameMode(on = true))
        val later = day.plusDays(2)
        repo.execute(TaskAction.Create("t", TaskFields(title = "Ler", deadline = DeadlineKind.DAY, date = later),
            RepeatRule(start = later)))
        val reminder = ReminderDefinition("r", kind = ReminderKind.TASK, sourceId = "t", occurrenceId = "t:1",
            chosenDate = later, minute = 540)
        db.reminders().definition(reminder.entity())
        suspend fun checkStored(expected: LocalDate, kind: AuditKind) {
            val snapshot = repo.observe().first()
            assertEquals(expected, snapshot.occurrences.single().dueDate)
            assertEquals(expected, snapshot.history.single { it.kind == kind }.actualDate)
            assertEquals(reminder.copy(chosenDate = expected), db.reminders().definitions().single().domain())
        }
        repo.execute(TaskAction.Reschedule("t:1", later, day))
        checkStored(day, AuditKind.ADVANCED)
        repo.execute(TaskAction.UndoReschedule("t:1", day, later))
        checkStored(later, AuditKind.ADVANCE_UNDONE)
    }

    @Test fun refreshWithoutChangesWakesNoTaskObserver() = runBlocking {
        val repo = RoomTaskRepository(db, clock, FixedGameMode(on = true))
        repo.execute(TaskAction.Create("a", TaskFields(title = "Ler"), RepeatRule(start = day)))
        repo.execute(TaskAction.Refresh)
        val invalidated = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(QUIET_MILLIS) {
                db.invalidationTracker.createFlow("task_lists", "task_definitions", "task_occurrences",
                    "task_checklist", "task_audit", emitInitialState = false).first()
            }
        }
        delay(SUBSCRIBE_MILLIS)
        repo.execute(TaskAction.Refresh)
        assertNull(invalidated.await())
    }

    @Test fun settleWithoutChangesWakesNoHabitObserver() = runBlocking {
        val repo = RoomHabitRepository(db, clock, FixedGameMode(on = true))
        repo.execute(HabitAction.Save(Habit("h", day, "Leitura"),
            HabitRule("hr", "h", day, HabitType.PRACTICE, HabitUnit.DAY, 1)))
        repo.execute(HabitAction.Settle)
        val stored = repo.observe().first()
        val invalidated = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(QUIET_MILLIS) {
                db.invalidationTracker.createFlow("habits", "habit_rules", "habit_intervals", "habit_overrides",
                    "habit_records", "habit_reviews", "habit_audits", emitInitialState = false).first()
            }
        }
        delay(SUBSCRIBE_MILLIS)
        repo.execute(HabitAction.Settle)
        assertNull(invalidated.await())
        assertEquals(HabitRules.apply(stored, HabitAction.Settle, day, clock.nowMillis(), emptySet()),
            repo.observe().first())
    }

    private companion object {
        const val QUIET_MILLIS = 1_000L
        /** Lets the invalidation observer register before the save under test. */
        const val SUBSCRIBE_MILLIS = 200L
    }
}
