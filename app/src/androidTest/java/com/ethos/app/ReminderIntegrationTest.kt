package com.ethos.app

import com.ethos.app.core.preferences.FixedGameMode
import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.ethos.app.core.tasks.data.*
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.reminders.data.*
import com.ethos.app.core.reminders.domain.*
import com.ethos.app.core.habits.data.HABIT_MIGRATION_1_2
import com.ethos.app.core.accounting.data.ACCOUNTING_MIGRATION_2_3
import com.ethos.app.core.rewards.data.REWARD_MIGRATION_3_4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
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
                com.ethos.app.core.timers.data.TIMER_MIGRATION_5_6,
                com.ethos.app.core.trackers.data.TRACKER_MIGRATION_6_7).build()

    @Test fun migration4PreservesEveryColumnIncludingPurchases() {
        val name = "reminder-migration-${System.nanoTime()}.db"
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.ethos.app.core.tasks.data.TaskDatabase/4.json").bufferedReader().use { it.readText() })
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
            val tasks = RoomTaskRepository(db, clock, FixedGameMode(on = true))
            tasks.execute(TaskAction.Create("t", TaskFields(title = "Fictício", deadline = DeadlineKind.DAY,
                date = today, minute = 600), RepeatRule(start = today)))
            val before = tasks.observe().first()
            val repo = RoomReminderRepository(db)
            val first = ReminderDefinition("a", kind = ReminderKind.TASK, sourceId = "t", occurrenceId = "t:1",
                mode = ReminderMode.RELATIVE, offsetMinutes = 30)
            repo.save(first); repo.save(first.copy(id = "b", groupId = "b", offsetMinutes = 1440))
            val row = ReminderRules.schedule(ReminderSources(before,
                com.ethos.app.core.habits.domain.HabitSnapshot()), listOf(first), today,
                java.time.ZoneId.of("UTC")).single()
            repo.deliveries(listOf(row.copy(state = DeliveryState.POSTED)))
            repo.snooze(row.key, 2000, 1000); repo.snooze(row.key, 3000, 1000)
            repo.summary(600)
            db.close(); db = open(name)
            val restored = RoomReminderRepository(db).read()
            assertEquals(2, restored.definitions.size)
            assertEquals(3000, restored.deliveries.single().dueAt)
            assertTrue(restored.deliveries.single().snoozed)
            assertEquals(600, restored.settings.summaryMinute)
            assertEquals(before, RoomTaskRepository(db, clock, FixedGameMode(on = true)).observe().first())
            RoomReminderRepository(db).remove(first)
            assertFalse(RoomReminderRepository(db).read().definitions.first { it.id == "a" }.enabled)
        } finally { db.close(); context.deleteDatabase(name) }
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
    @Test fun futureScopeReplacesCurrentExceptionButPreservesEarlierDefinition() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        try {
            val repo = RoomReminderRepository(db)
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
