package com.ethos.app.core.trackers

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.ethos.app.core.accounting.data.ACCOUNTING_MIGRATION_2_3
import com.ethos.app.core.habits.data.*
import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.reminders.data.REMINDER_MIGRATION_4_5
import com.ethos.app.core.rewards.data.REWARD_MIGRATION_3_4
import com.ethos.app.core.tasks.data.TaskDatabase
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.timers.data.TIMER_MIGRATION_5_6
import com.ethos.app.core.trackers.data.*
import com.ethos.app.core.trackers.domain.*
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TrackerDatabaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val day = LocalDate.of(2026, 11, 7)
    private val clock = object : TaskClock {
        override fun today() = day
        override fun nowMillis() = 1000L
    }
    private val migrations = arrayOf(HABIT_MIGRATION_1_2, ACCOUNTING_MIGRATION_2_3, REWARD_MIGRATION_3_4,
        REMINDER_MIGRATION_4_5, TIMER_MIGRATION_5_6, TRACKER_MIGRATION_6_7)

    @Test fun migration6PreservesEveryRowAndCreatesEmptyTrackerTables() {
        val name = "tracker-migration-${System.nanoTime()}.db"
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.ethos.app.core.tasks.data.TaskDatabase/6.json").bufferedReader().use { it.readText() })
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
            old.version = 6
        }
        val db = Room.databaseBuilder(context, TaskDatabase::class.java, name).addMigrations(*migrations).build()
        try {
            expected.forEach { (table, row) -> db.openHelper.readableDatabase.query("SELECT * FROM `$table`").use {
                assertTrue(it.moveToFirst())
                assertEquals(table, row, row.indices.map(it::getString))
                assertFalse(it.moveToNext())
            } }
            for (table in listOf("trackers", "tracker_versions", "tracker_questions", "tracker_options",
                    "tracker_entries", "tracker_answers")) {
                db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
                    assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
                }
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }

    private fun count(db: TaskDatabase, table: String) =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }

    @Test fun versionsEntriesAndUniqueDayAreStoredWithoutTouchingHabitsOrLedger() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        try {
            val habits = RoomHabitRepository(db, clock)
            habits.execute(HabitAction.Save(Habit("h", day, "Hábito"), HabitRule("r", "h", day)))
            val before = habits.observe().first()
            val ledger = count(db, "accounting_ledger")
            val repo = RoomTrackerRepository(db, clock)
            val scale = TrackerQuestion("u", QuestionType.SCALE, "Vontade", required = true, primary = true)
            val choice = TrackerQuestion("c", QuestionType.CHOICE, "Gatilhos", multiple = true,
                options = listOf(TrackerOption("a", "Tédio", "Sem nada para fazer"), TrackerOption("b", "Cansaço")))
            repo.execute(TrackerAction.SaveForm("t", "h", false, listOf(choice, scale)))
            repo.execute(TrackerAction.SaveEntry("e1", "t", day, listOf(TrackerAnswer("u", scale = 8),
                TrackerAnswer("c", choices = listOf("a", "b")))))
            repo.execute(TrackerAction.SaveForm("t", "h", false, listOf(scale,
                choice.copy(options = choice.options.take(1)))))
            var s = repo.observe().first()
            assertEquals(2, s.tracker("t")!!.version)
            assertEquals(listOf("c", "u"), s.version("t", 1)!!.questions.map { it.id })
            assertEquals(listOf("a", "b"), s.version("t", 1)!!.questions.first().options.map { it.id })
            assertEquals("Sem nada para fazer", s.version("t", 1)!!.questions.first().options.first().description)
            assertEquals(listOf("u", "c"), s.current("t")!!.questions.map { it.id })
            val e = s.entries.single()
            assertEquals(1, e.version)
            assertEquals(listOf("a", "b"), e.answers.first { it.questionId == "c" }.choices)
            // Storage-level guarantee of one entry per date in single mode.
            try {
                db.openHelper.writableDatabase.execSQL("INSERT INTO tracker_entries (id, trackerId, version, date, " +
                    "dayKey, filledAt, updatedAt) VALUES ('dup', 't', 2, ${day.toEpochDay()}, '$day', 1, 1)")
                fail("unique day index")
            } catch (_: SQLiteConstraintException) { }
            try {
                repo.execute(TrackerAction.SaveEntry("e2", "t", day, listOf(TrackerAnswer("u", scale = 1))))
                fail("duplicate rule")
            } catch (ex: TrackerRuleException) { assertEquals(TrackerProblem.DUPLICATE, ex.problem) }
            repo.execute(TrackerAction.DeleteEntry("e1"))
            s = repo.observe().first()
            assertTrue(s.entries.isEmpty())
            assertEquals(0, count(db, "tracker_answers"))
            assertEquals(before, habits.observe().first())
            assertEquals(ledger, count(db, "accounting_ledger"))
        } finally { db.close() }
    }
}
