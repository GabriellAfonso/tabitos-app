package com.tabitos.app.core.habits

import com.tabitos.app.core.preferences.FixedGameMode
import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.tabitos.app.core.habits.data.*
import com.tabitos.app.core.habits.domain.*
import com.tabitos.app.core.tasks.data.*
import com.tabitos.app.core.tasks.domain.*
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class HabitDatabaseTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val clock = object : TaskClock {
        override fun today(): LocalDate = LocalDate.of(2026, 11, 6)
        override fun nowMillis(): Long = 100
    }
    @Test fun exportedV1MigrationPreservesAllTaskTablesAndReopensHabits() = runBlocking {
        val name = "habit-migration-${System.nanoTime()}.db"
        val asset = "com.tabitos.app.core.tasks.data.TaskDatabase/1.json"
        val text = InstrumentationRegistry.getInstrumentation().context.assets.open(asset)
            .bufferedReader().use { it.readText() }
        val schema = JSONObject(text).getJSONObject("database")
        val old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        try {
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                val table = e.getString("tableName")
                old.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                val fields = e.getJSONArray("fields")
                val values = ContentValues()
                val strings = mapOf("id" to "saved", "taskId" to "saved", "ownerId" to "saved",
                    "occurrenceId" to "saved", "repeatKind" to "NONE", "field_deadline" to "NONE",
                    "status" to "OPEN", "kind" to "CREATED", "field_title" to "Preservada",
                    "field_description" to "Descrição anterior", "name" to "Lista anterior", "label" to "Conferir")
                for (j in 0 until fields.length()) {
                    val f = fields.getJSONObject(j)
                    val column = f.getString("columnName")
                    if (!f.optBoolean("notNull", false)) values.putNull(column)
                    else if (f.getString("affinity") == "TEXT") values.put(column, strings[column] ?: "saved")
                    else values.put(column,
                        if (column in listOf("created", "start")) clock.today().toEpochDay() else 1L)
                }
                // Keep list visible and one-off task open.
                if (table == "task_lists") values.put("deleted", 0)
                if (table == "task_definitions") values.put("stopped", 0)
                old.insertOrThrow(table, null, values)
                val indices = e.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) old.execSQL(
                    indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            old.version = 1
        } finally { old.close() }
        var db = Room.databaseBuilder(context, TaskDatabase::class.java, name)
            .addMigrations(HABIT_MIGRATION_1_2,
                com.tabitos.app.core.accounting.data.ACCOUNTING_MIGRATION_2_3,
                com.tabitos.app.core.rewards.data.REWARD_MIGRATION_3_4,
                com.tabitos.app.core.reminders.data.REMINDER_MIGRATION_4_5,
                com.tabitos.app.core.timers.data.TIMER_MIGRATION_5_6,
                com.tabitos.app.core.trackers.data.TRACKER_MIGRATION_6_7).build()
        try {
            val tasks = RoomTaskRepository(db, clock, FixedGameMode(on = true)).observe().first()
            assertEquals("Lista anterior", tasks.lists.single().name)
            assertEquals("Preservada", tasks.occurrences.single().fields.title)
            assertEquals("Conferir", tasks.occurrences.single().checks.single().label)
            assertEquals(1, tasks.history.size)
            val habits = RoomHabitRepository(db, clock, FixedGameMode(on = true))
            habits.execute(HabitAction.Save(Habit("h", clock.today(), "Leitura"),
                HabitRule("r", "h", clock.today())))
            habits.execute(HabitAction.Record("a", "h", clock.today()))
            val before = habits.observe().first()
            db.close()
            db = Room.databaseBuilder(context, TaskDatabase::class.java, name)
                .addMigrations(HABIT_MIGRATION_1_2,
                com.tabitos.app.core.accounting.data.ACCOUNTING_MIGRATION_2_3,
                com.tabitos.app.core.rewards.data.REWARD_MIGRATION_3_4,
                com.tabitos.app.core.reminders.data.REMINDER_MIGRATION_4_5,
                com.tabitos.app.core.timers.data.TIMER_MIGRATION_5_6,
                com.tabitos.app.core.trackers.data.TRACKER_MIGRATION_6_7).build()
            assertEquals(before, RoomHabitRepository(db, clock, FixedGameMode(on = true)).observe().first())
            assertEquals(tasks, RoomTaskRepository(db, clock, FixedGameMode(on = true)).observe().first())
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun listDeletionIsAtomicAndRetainsHabitHistory() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        try {
            val tasks = RoomTaskRepository(db, clock, FixedGameMode(on = true))
            val habits = RoomHabitRepository(db, clock, FixedGameMode(on = true))
            tasks.execute(TaskAction.SaveList(TaskList("list", "Rotina")))
            habits.execute(HabitAction.Save(Habit("h", clock.today(), "Ler", listId = "list"),
                HabitRule("r", "h", clock.today())))
            habits.execute(HabitAction.Record("a", "h", clock.today()))
            try {
                db.withTransaction { tasks.execute(TaskAction.DeleteList("list")); error("rollback") }
            } catch (_: IllegalStateException) { /* intentional */ }
            assertEquals(HabitStatus.ACTIVE, habits.observe().first().habits.single().status)
            assertFalse(tasks.observe().first().lists.single().deleted)
            tasks.execute(TaskAction.DeleteList("list"))
            val s = habits.observe().first()
            assertEquals(HabitStatus.EXCLUDED, s.habits.single().status)
            assertEquals(1, s.records.size)
            assertNotNull(s.intervals.single().end)
        } finally { db.close() }
    }
}
