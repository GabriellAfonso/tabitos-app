package com.tabitos.app

import com.tabitos.app.core.preferences.FixedGameMode
import com.tabitos.app.core.preferences.TaskReminderDefaults
import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.tabitos.app.core.tasks.data.*
import com.tabitos.app.core.tasks.domain.*
import com.tabitos.app.core.reminders.data.*
import com.tabitos.app.core.reminders.domain.*
import com.tabitos.app.core.habits.data.HABIT_MIGRATION_1_2
import com.tabitos.app.core.accounting.data.ACCOUNTING_MIGRATION_2_3
import com.tabitos.app.core.rewards.data.REWARD_MIGRATION_3_4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ReminderIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val today = LocalDate.of(2026, 11, 2)
    private val clock = object : TaskClock {
        override fun today() = this@ReminderIntegrationTest.today
        override fun nowMillis() = 1L
    }
    private fun open(name: String) = Room.databaseBuilder(context, TaskDatabase::class.java, name)
        .addMigrations(HABIT_MIGRATION_1_2, ACCOUNTING_MIGRATION_2_3, REWARD_MIGRATION_3_4,
            REMINDER_MIGRATION_4_5,
                com.tabitos.app.core.timers.data.TIMER_MIGRATION_5_6,
                com.tabitos.app.core.trackers.data.TRACKER_MIGRATION_6_7).build()

    @Test fun migration4PreservesEveryColumnIncludingPurchases() {
        val name = "reminder-migration-${System.nanoTime()}.db"
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.tabitos.app.core.tasks.data.TaskDatabase/4.json").bufferedReader().use { it.readText() })
            .getJSONObject("database")
        val expected = mutableMapOf<String, List<String?>>()
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                val table = e.getString("tableName")
                old.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                val values = ContentValues()
                val fields = e.getJSONArray("fields")
                for (j in 0 until fields.length()) {
                    val f = fields.getJSONObject(j)
                    val column = f.getString("columnName")
                    if (!f.optBoolean("notNull", false)) values.putNull(column)
                    else if (f.getString("affinity") == "TEXT") values.put(column, "preserved-$column")
                    else values.put(column, 1L)
                }
                old.insertOrThrow(table, null, values)
                val indices = e.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) old.execSQL(
                    indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                old.rawQuery("SELECT * FROM `$table`", null).use { c ->
                    assertTrue(c.moveToFirst())
                    expected[table] = (0 until c.columnCount).map { c.getString(it) }
                }
            }
            old.version = 4
        }
        val db = open(name)
        try {
            expected.forEach { (table, row) -> db.openHelper.readableDatabase.query("SELECT * FROM `$table`").use {
                assertTrue(it.moveToFirst())
                assertEquals(table, row, row.indices.map(it::getString))
                assertFalse(it.moveToNext())
            } }
            for (table in listOf("reminder_definitions", "reminder_deliveries", "reminder_settings")) {
                db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
                    assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
                }
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun multipleDefinitionsAndReplacementSnoozeSurviveColdReopenWithoutSourceMutation() = runBlocking {
        val name = "reminder-cold-${System.nanoTime()}.db"
        var db = open(name)
        try {
            val manualOnly = flowOf(TaskReminderDefaults(offsetMinutes = emptySet()))
            val tasks = RoomTaskRepository(db, clock, FixedGameMode(on = true), manualOnly)
            tasks.execute(TaskAction.Create("t", TaskFields(title = "Fictício", deadline = DeadlineKind.DAY,
                date = today, minute = 600), RepeatRule(start = today)))
            val before = tasks.observe().first()
            val repo = RoomReminderRepository(db, clock)
            val first = ReminderDefinition("a", kind = ReminderKind.TASK, sourceId = "t", occurrenceId = "t:1",
                mode = ReminderMode.RELATIVE, offsetMinutes = 30)
            repo.save(first); repo.save(first.copy(id = "b", groupId = "b", offsetMinutes = 1440))
            val row = ReminderRules.schedule(ReminderSources(before,
                com.tabitos.app.core.habits.domain.HabitSnapshot()), listOf(first), today,
                java.time.ZoneId.of("UTC")).single()
            repo.deliveries(listOf(row.copy(state = DeliveryState.POSTED)))
            repo.snooze(row.key, 2000, 1000); repo.snooze(row.key, 3000, 1000)
            repo.summary(600)
            db.close(); db = open(name)
            val restored = RoomReminderRepository(db, clock).read()
            assertEquals(setOf(first, first.copy(id = "b", groupId = "b", offsetMinutes = 1440)),
                restored.definitions.toSet())
            assertEquals(3000, restored.deliveries.single().dueAt)
            assertTrue(restored.deliveries.single().snoozed)
            assertEquals(600, restored.settings.summaryMinute)
            assertEquals(before, RoomTaskRepository(db, clock, FixedGameMode(on = true)).observe().first())
            RoomReminderRepository(db, clock).remove(first)
            assertFalse(RoomReminderRepository(db, clock).read().definitions.first { it.id == "a" }.enabled)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun evaluatedReviewCannotRepeatAfterReopeningOrChangingTheTime() = runBlocking {
        val name = "review-slot-${System.nanoTime()}.db"
        val day = today.plusYears(1)
        val minute = 21 * 60
        var db = open(name)
        try {
            val repository = RoomReminderRepository(db, clock)
            repository.summary(minute)
            assertFalse(repository.claimReview(day, minute - 1))
            assertTrue(repository.claimReview(day, minute))
            db.close()
            db = open(name)
            val restored = RoomReminderRepository(db, clock)
            assertEquals(minute, restored.read().settings.summaryMinute)
            assertFalse(restored.claimReview(day, minute))
            assertTrue(restored.claimReview(day.plusDays(1), minute))
            restored.summary(null)
            assertFalse(restored.claimReview(day.plusDays(2), minute))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun materializationUsesOriginalSeriesAndNeverRecordsCompletion() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        try {
            val repo = RoomTaskRepository(db, clock, FixedGameMode(on = true))
            repo.execute(TaskAction.Create("t", TaskFields(title = "Fictício", deadline = DeadlineKind.DAY,
                date = today), RepeatRule(RepeatKind.DAILY, today, count = 4)))
            repo.execute(TaskAction.Materialize(today.plusDays(3)))
            val once = repo.observe().first()
            repo.execute(TaskAction.Materialize(today.plusDays(3)))
            assertEquals(once, repo.observe().first())
            assertEquals(4, once.occurrences.size)
            assertTrue(once.occurrences.all { it.status == TaskStatus.OPEN })
            assertFalse(once.history.any { it.kind == AuditKind.COMPLETED })
        } finally { db.close() }
    }

    @Test fun defaultsApplyOnlyToNewTasksAndSeriesExceptionsSurviveReopen() = runBlocking {
        val name = "new-task-reminders-${System.nanoTime()}.db"
        var db = open(name)
        val defaults = MutableStateFlow(TaskReminderDefaults(offsetMinutes = emptySet()))
        fun taskRepository() = RoomTaskRepository(db, clock, FixedGameMode(on = true), defaults)
        val fields = TaskFields(title = "Consulta", deadline = DeadlineKind.DAY, date = today, minute = 900)
        val daily = RepeatRule(RepeatKind.DAILY, today, count = 3)
        val once = RepeatRule(start = today)
        val oldManual = ReminderDefinition("old-manual", kind = ReminderKind.TASK, sourceId = "old-series",
            mode = ReminderMode.RELATIVE, offsetMinutes = 15)
        val newSeries = TaskAction.Create("new-series", fields, daily)
        suspend fun effective(id: String): List<ReminderDefinition> {
            val occurrence = taskRepository().observe().first().occurrences.single { it.id == id }
            return ReminderRules.effective(RoomReminderRepository(db, clock).read().definitions, occurrence)
        }
        try {
            val tasks = taskRepository()
            val reminders = RoomReminderRepository(db, clock)
            assertFalse(tasks.createWithReminders(TaskAction.Create("old-task", fields, once)))
            tasks.execute(TaskAction.Create("old-series", fields, daily))
            reminders.save(oldManual)

            defaults.value = TaskReminderDefaults()
            tasks.execute(TaskAction.Reschedule("old-task:1", today, today.plusDays(1)))
            assertTrue(tasks.createWithReminders(newSeries))
            tasks.execute(TaskAction.Materialize(today.plusDays(2)))
            assertTrue(effective("old-task:1").isEmpty())
            assertEquals(listOf(oldManual), effective("old-series:3"))
            val inherited = effective("new-series:2").single()
            assertEquals(30L, inherited.offsetMinutes)
            val onlySecond = inherited.copy(id = "${inherited.groupId}:new-series:2", occurrenceId = "new-series:2",
                fromOrdinal = 2, offsetMinutes = 10)
            reminders.save(onlySecond)
            assertEquals(10L, effective("new-series:2").single().offsetMinutes)
            assertEquals(30L, effective("new-series:3").single().offsetMinutes)
            reminders.remove(onlySecond)

            defaults.value = TaskReminderDefaults(offsetMinutes = setOf(30, 1440))
            assertFalse(tasks.createWithReminders(newSeries))
            tasks.execute(TaskAction.Reschedule("new-series:2", today.plusDays(1), today.plusDays(4)))
            tasks.execute(TaskAction.Create("new-task", fields.copy(date = today.plusDays(5)), once))
            db.close()
            db = open(name)
            taskRepository().execute(TaskAction.Refresh)
            assertTrue(effective("new-series:2").isEmpty())
            assertEquals(listOf(inherited), effective("new-series:1"))
            assertEquals(listOf(inherited), effective("new-series:3"))
            assertEquals(listOf(30L, 1440L), effective("new-task:1").map { it.offsetMinutes }.sorted())
            assertTrue(effective("old-task:1").isEmpty())
            assertEquals(listOf(oldManual), effective("old-series:3"))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun failedReminderWriteRollsBackTaskAndCanBeRetried() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        try {
            val source = flowOf(TaskReminderDefaults(offsetMinutes = setOf(30, 1440)))
            val tasks = RoomTaskRepository(db, clock, FixedGameMode(on = true), source)
            val date = today.plusDays(3)
            val create = TaskAction.Create("retry", TaskFields(title = "Consulta", deadline = DeadlineKind.DAY,
                date = date, minute = 900), RepeatRule(start = date))
            val sql = db.openHelper.writableDatabase
            sql.execSQL("""CREATE TRIGGER fail_reminder BEFORE INSERT ON reminder_definitions
                BEGIN SELECT RAISE(ABORT, 'simulated failure'); END""")
            assertTrue(runCatching { tasks.execute(create) }.isFailure)
            assertTrue(tasks.observe().first().occurrences.isEmpty())
            assertTrue(RoomReminderRepository(db, clock).read().definitions.isEmpty())

            sql.execSQL("DROP TRIGGER fail_reminder")
            tasks.execute(create)
            assertEquals("retry:1", tasks.observe().first().occurrences.single().id)
            assertEquals(listOf(30L, 1440L), RoomReminderRepository(db, clock).read().definitions.map { it.offsetMinutes }
                .sorted())
        } finally {
            db.close()
        }
    }
    @Test fun futureScopeReplacesCurrentExceptionButPreservesEarlierDefinition() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        try {
            val repo = RoomReminderRepository(db, clock)
            val base = ReminderDefinition("g:from:1", groupId = "g", kind = ReminderKind.TASK,
                sourceId = "t", mode = ReminderMode.WEEKDAYS)
            repo.save(base)
            repo.save(base.copy(id = "g:t:2", occurrenceId = "t:2", fromOrdinal = 2, minute = 600))
            repo.save(base.copy(id = "g:from:2", fromOrdinal = 2, minute = 700))
            val defs = repo.read().definitions
            assertEquals(2, defs.size)
            assertFalse(defs.any { it.occurrenceId == "t:2" })
            assertTrue(defs.any { it.id == base.id })
        } finally { db.close() }
    }
}
