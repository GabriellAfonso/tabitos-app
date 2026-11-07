package com.ethos.app.core.accounting

import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.ethos.app.core.accounting.data.*
import com.ethos.app.core.accounting.domain.*
import com.ethos.app.core.tasks.data.*
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.habits.data.*
import com.ethos.app.core.habits.domain.*
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AccountingDatabaseTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private var day = LocalDate.of(2026, 11, 2)
    private var time = 100L
    private val clock = object : TaskClock {
        override fun today() = day
        override fun nowMillis() = time++
    }
    private suspend fun create(tasks: TaskRepository, id: String, difficulty: Int = 1, importance: Int = 1) {
        tasks.execute(TaskAction.Create(id, TaskFields(title = id, difficulty = difficulty, importance = importance),
            RepeatRule(start = day)))
    }
    private fun memory() = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()

    @Test fun taskAwardsOriginalPricesRollbackAndSourceDeletion() = runBlocking {
        val db = memory()
        try {
            val tasks = RoomTaskRepository(db, clock)
            val wallet = RoomAccountingRepository(db, clock)
            assertEquals(0L, wallet.observe().first().balance)
            create(tasks, "t", 0, 2)
            val id = tasks.observe().first().occurrences.single().id
            try {
                db.withTransaction { tasks.execute(TaskAction.Resolve(id, true, day)); error("rollback") }
                fail("Expected rollback")
            } catch (_: IllegalStateException) { }
            assertEquals(TaskStatus.OPEN, tasks.observe().first().occurrences.single().status)
            assertTrue(wallet.observe().first().entries.isEmpty())
            tasks.execute(TaskAction.Resolve(id, true, day))
            assertEquals(5L, wallet.observe().first().balance)
            wallet.savePrices(Price.defaults().map { it.copy(coins = 999, xp = 999, penalty = 999) })
            tasks.execute(TaskAction.Undo(id))
            assertEquals(0L, wallet.observe().first().totalXp)
            tasks.execute(TaskAction.Resolve(id, true, day))
            val s = wallet.observe().first()
            assertEquals(5L, s.balance)
            assertEquals(10L, s.totalXp)
            assertEquals(3, s.entries.size)
            tasks.execute(TaskAction.Refresh)
            tasks.execute(TaskAction.Delete(id, ChangeScope.ONLY_THIS))
            assertEquals(s, wallet.observe().first())
            assertEquals("t", wallet.observe().first().entries.first().title)
        } finally { db.close() }
    }

    @Test fun floorClippingRefundsOnlyEffectiveDeductionAndRetainsZeroIncident() = runBlocking {
        val db = memory()
        try {
            val tasks = RoomTaskRepository(db, clock)
            val wallet = RoomAccountingRepository(db, clock)
            wallet.savePrices(Price.defaults().map { if (it.category == 0) it.copy(penalty = 295) else it })
            create(tasks, "first", importance = 0)
            create(tasks, "second", importance = 2)
            create(tasks, "third", importance = 2)
            val ids = tasks.observe().first().occurrences.associate { it.fields.title to it.id }
            tasks.execute(TaskAction.Resolve(ids.getValue("first"), false, day))
            assertEquals(-295L, wallet.observe().first().balance)
            tasks.execute(TaskAction.Resolve(ids.getValue("second"), false, day))
            val clipped = wallet.observe().first().entries.last()
            assertEquals(-20L, clipped.nominalCoins)
            assertEquals(-5L, clipped.coins)
            assertEquals(-300L, clipped.balance)
            tasks.execute(TaskAction.Resolve(ids.getValue("third"), false, day))
            assertEquals(0L, wallet.observe().first().entries.last().coins)
            tasks.execute(TaskAction.Undo(ids.getValue("third")))
            assertEquals(-300L, wallet.observe().first().balance)
            tasks.execute(TaskAction.Undo(ids.getValue("second")))
            assertEquals(-295L, wallet.observe().first().balance)
            assertEquals(0L, wallet.observe().first().totalXp)
        } finally { db.close() }
    }

    @Test fun habitReclassificationAndConfirmedPeriodsAreIdempotent() = runBlocking {
        val db = memory()
        try {
            val habits = RoomHabitRepository(db, clock)
            val wallet = RoomAccountingRepository(db, clock)
            habits.execute(HabitAction.Save(Habit("h", day, "Limite"),
                HabitRule("r", "h", day, HabitType.LIMIT, HabitUnit.WEEK, 1)))
            habits.execute(HabitAction.Record("a", "h", day))
            habits.execute(HabitAction.Record("b", "h", day))
            assertEquals(-10L, wallet.observe().first().balance)
            habits.execute(HabitAction.Record("b", "h", day))
            assertEquals(1, wallet.observe().first().entries.size)
            habits.execute(HabitAction.Correct("a", delete = true))
            assertEquals(0L, wallet.observe().first().balance)
            day = day.plusWeeks(1)
            val key = HabitRules.pending(habits.observe().first(), day).first().key
            habits.execute(HabitAction.Review(key))
            assertEquals(10L, wallet.observe().first().balance)
            assertEquals(20L, wallet.observe().first().totalXp)
            val size = wallet.observe().first().entries.size
            try { habits.execute(HabitAction.Review(key)); fail("Review must not duplicate")
            } catch (_: HabitRuleException) { }
            assertEquals(size, wallet.observe().first().entries.size)
            habits.execute(HabitAction.Record("forgotten", "h", day.minusWeeks(1)))
            assertEquals(-10L, wallet.observe().first().balance)
            assertEquals(0L, wallet.observe().first().totalXp)
            assertTrue(habits.observe().first().reviews.single().invalidated)
            habits.execute(HabitAction.Review(key))
            assertEquals(-10L, wallet.observe().first().balance)
        } finally { db.close() }
    }

    @Test fun practiceRewardsDistinctDaysAndRejectsExtraWithAtomicRollback() = runBlocking {
        val db = memory()
        try {
            val habits = RoomHabitRepository(db, clock)
            val wallet = RoomAccountingRepository(db, clock)
            habits.execute(HabitAction.Save(Habit("h", day, "Prática"),
                HabitRule("r", "h", day, HabitType.PRACTICE, HabitUnit.WEEK, 2)))
            habits.execute(HabitAction.Record("a", "h", day))
            habits.execute(HabitAction.Record("duplicate-day", "h", day))
            assertEquals(10L, wallet.observe().first().balance)
            day = day.plusDays(1)
            habits.execute(HabitAction.Record("b", "h", day))
            val before = wallet.observe().first()
            try { habits.execute(HabitAction.Record("extra", "h", day)); fail("Cap required")
            } catch (_: HabitRuleException) { }
            assertEquals(before, wallet.observe().first())
            assertEquals(3, habits.observe().first().records.size)
            habits.execute(HabitAction.Correct("b", delete = true))
            assertEquals(10L, wallet.observe().first().balance)
            habits.execute(HabitAction.Record("replacement", "h", day))
            assertEquals(20L, wallet.observe().first().balance)
            assertEquals(40L, wallet.observe().first().totalXp)
            wallet.savePrices(Price.defaults().map { it.copy(coins = 999, xp = 999) })
            day = day.plusDays(1)
            habits.execute(HabitAction.Correct("replacement", date = day))
            assertEquals(20L, wallet.observe().first().balance)
            assertEquals(40L, wallet.observe().first().totalXp)
        } finally { db.close() }
    }

    @Test fun exportedV2MigrationPreservesEveryTableAndExcludesOldAwards() = runBlocking {
        val name = "accounting-migration-${System.nanoTime()}.db"
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.ethos.app.core.tasks.data.TaskDatabase/2.json").bufferedReader().use { it.readText() })
            .getJSONObject("database")
        val old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        val tables = mutableListOf<String>()
        try {
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                val table = e.getString("tableName")
                tables += table
                old.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                val fields = e.getJSONArray("fields")
                val values = ContentValues()
                val strings = mapOf("repeatKind" to "NONE", "field_deadline" to "NONE",
                    "status" to if (table == "habits") "ACTIVE" else "DONE", "kind" to "COMPLETED",
                    "type" to "PRACTICE", "unit" to "DAY", "outcome" to "SUCCESS",
                    "name" to "Antigo", "field_title" to "Tarefa antiga")
                for (j in 0 until fields.length()) {
                    val f = fields.getJSONObject(j)
                    val column = f.getString("columnName")
                    if (!f.optBoolean("notNull", false)) values.putNull(column)
                    else if (f.getString("affinity") == "TEXT") values.put(column, strings[column] ?: "saved")
                    else values.put(column, when (column) {
                        "created", "start", "effective", "date" -> day.toEpochDay()
                        "deleted", "stopped", "invalidated" -> 0L
                        "weekdays" -> 127L
                        else -> 1L
                    })
                }
                old.insertOrThrow(table, null, values)
                val indices = e.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) old.execSQL(
                    indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            old.version = 2
        } finally { old.close() }
        var db = Room.databaseBuilder(context, TaskDatabase::class.java, name)
            .addMigrations(ACCOUNTING_MIGRATION_2_3).build()
        try {
            val wallet = RoomAccountingRepository(db, clock)
            assertEquals(0L, wallet.observe().first().balance)
            assertEquals(0L, wallet.observe().first().totalXp)
            for (table in tables) db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$table`").use {
                assertTrue(it.moveToFirst()); assertEquals("Preserve $table", 1, it.getInt(0))
            }
            val tasks = RoomTaskRepository(db, clock)
            tasks.execute(TaskAction.Refresh)
            assertTrue(wallet.observe().first().entries.isEmpty())
            tasks.execute(TaskAction.Undo("saved"))
            assertTrue(wallet.observe().first().entries.isEmpty())
            tasks.execute(TaskAction.Resolve("saved", true, day))
            assertEquals(10L, wallet.observe().first().balance)
            day = day.plusDays(1)
            RoomHabitRepository(db, clock).execute(HabitAction.Correct("saved", date = day))
            assertEquals(10L, wallet.observe().first().balance)
            val expected = wallet.observe().first()
            db.close()
            db = Room.databaseBuilder(context, TaskDatabase::class.java, name)
                .addMigrations(ACCOUNTING_MIGRATION_2_3).build()
            assertEquals(expected, RoomAccountingRepository(db, clock).observe().first())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
