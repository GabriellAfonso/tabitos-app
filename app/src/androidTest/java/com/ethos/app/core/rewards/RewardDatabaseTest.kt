package com.ethos.app.core.rewards

import android.content.Context
import android.content.ContentValues
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.ethos.app.core.accounting.data.RoomAccountingRepository
import com.ethos.app.core.rewards.data.*
import com.ethos.app.core.rewards.domain.*
import com.ethos.app.core.tasks.data.*
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.habits.data.RoomHabitRepository
import com.ethos.app.core.habits.domain.*
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class RewardDatabaseTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private var day = LocalDate.of(2026, 11, 7)
    private val clock = object : TaskClock {
        override fun today() = day
        override fun nowMillis() = 1000L
    }
    private val reward = Reward("r", "Pausa", "Descrição original", "REST", cost = 5)
    private fun memory() = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
    private suspend fun RoomRewardRepository.confirm(id: String): PurchaseResult =
        confirm(observe().first().intent!!.copy(id = id))
    private suspend fun earn(db: TaskDatabase, id: String = "work") : String {
        val tasks = RoomTaskRepository(db, clock)
        tasks.execute(TaskAction.Create(id, TaskFields(title = id), RepeatRule(start = day)))
        val occurrence = tasks.observe().first().occurrences.first { it.taskId == id }.id
        tasks.execute(TaskAction.Resolve(occurrence, true, day))
        return occurrence
    }

    @Test fun exactBalanceCancelRetryLegitimateRepeatAndNoXpChange() = runBlocking {
        val db = memory()
        try {
            val repo = RoomRewardRepository(db, clock)
            earn(db)
            repo.save(reward)
            repo.preview("r")
            val cancelled = repo.observe().first().intent!!
            repo.close(cancelled.id)
            assertEquals(10L, repo.observe().first().wallet.balance)
            repo.preview("r")
            val id = repo.observe().first().intent!!.id
            assertEquals(PurchaseResult.PURCHASED, repo.confirm(id))
            assertEquals(PurchaseResult.PURCHASED, repo.confirm(id))
            assertEquals(1, repo.observe().first().purchases.size)
            repo.close(id)
            repo.preview("r")
            val second = repo.observe().first().intent!!.id
            assertNotEquals(id, second)
            assertEquals(PurchaseResult.PURCHASED, repo.confirm(second))
            val s = repo.observe().first()
            assertEquals(0L, s.wallet.balance)
            assertEquals(20L, s.wallet.totalXp)
            assertEquals(2, s.purchases.size)
            repo.close(second)
            repo.preview("r")
            assertEquals(PurchaseResult.UNAVAILABLE, repo.confirm(repo.observe().first().intent!!.id))
            assertEquals(2, repo.observe().first().purchases.size)
        } finally { db.close() }
    }

    @Test fun stalePriceBalanceDateDeletionAndAtomicRollback() = runBlocking {
        val db = memory()
        try {
            val repo = RoomRewardRepository(db, clock)
            earn(db)
            repo.save(reward)
            repo.preview("r")
            var id = repo.observe().first().intent!!.id
            repo.save(reward.copy(cost = 6))
            assertEquals(PurchaseResult.STALE, repo.confirm(id))
            assertEquals(10L, repo.observe().first().wallet.balance)
            day = day.plusDays(1)
            assertEquals(PurchaseResult.STALE, repo.confirm(id))
            try {
                db.withTransaction { repo.confirm(id); error("rollback") }
                fail("Rollback expected")
            } catch (_: IllegalStateException) { }
            assertTrue(repo.observe().first().purchases.isEmpty())
            assertEquals(10L, repo.observe().first().wallet.balance)
            assertEquals(PurchaseResult.PURCHASED, repo.confirm(id))
            repo.close(id)
            repo.preview("r")
            id = repo.observe().first().intent!!.id
            repo.delete("r")
            assertEquals(PurchaseResult.UNAVAILABLE, repo.confirm(id))
            assertEquals(4L, repo.observe().first().wallet.balance)
        } finally { db.close() }
    }

    @Test fun limitsCountExistingPurchasesAfterEditsAndPeriods() = runBlocking {
        val db = memory()
        try {
            val repo = RoomRewardRepository(db, clock)
            earn(db)
            repo.save(reward.copy(cost = 1))
            repeat(2) {
                repo.preview("r")
                val id = repo.observe().first().intent!!.id
                assertEquals(PurchaseResult.PURCHASED, repo.confirm(id))
                repo.close(id)
            }
            repo.save(reward.copy(cost = 1, limit = 1, period = "MONTH"))
            repo.preview("r")
            var intent = repo.observe().first().intent!!
            assertEquals(2, intent.used)
            assertEquals(PurchaseResult.UNAVAILABLE, repo.confirm(intent.id))
            repo.close(intent.id)
            day = day.withDayOfMonth(1).plusMonths(1)
            repo.preview("r")
            intent = repo.observe().first().intent!!
            assertEquals(0, intent.used)
            assertEquals(PurchaseResult.PURCHASED, repo.confirm(intent.id))
            repo.close(intent.id)
            repo.save(reward.copy(id = "other", cost = 1, limit = 1))
            repo.preview("other")
            assertEquals(PurchaseResult.PURCHASED, repo.confirm(repo.observe().first().intent!!.id))
            assertEquals(4, repo.observe().first().purchases.size)
        } finally { db.close() }
    }

    @Test fun concurrentConfirmationsCannotDoubleChargeOrOverspend() = runBlocking {
        val db = memory()
        try {
            val repo = RoomRewardRepository(db, clock)
            earn(db)
            repo.save(reward.copy(cost = 10, limit = 1))
            repo.preview("r")
            val id = repo.observe().first().intent!!.id
            val outcomes = coroutineScope { (1..8).map { async(Dispatchers.Default) { repo.confirm(id) } }.awaitAll() }
            assertTrue(outcomes.all { it == PurchaseResult.PURCHASED })
            assertEquals(1, repo.observe().first().purchases.size)
            assertEquals(0L, repo.observe().first().wallet.balance)
        } finally { db.close() }
    }

    @Test fun historySurvivesEditDeleteAndTaskCorrectionAndColdReopen() = runBlocking {
        val name = "rewards-${System.nanoTime()}.db"
        var db = Room.databaseBuilder(context, TaskDatabase::class.java, name).build()
        try {
            var repo = RoomRewardRepository(db, clock)
            val occurrence = earn(db)
            repo.save(reward)
            repo.preview("r")
            val id = repo.observe().first().intent!!.id
            db.close()
            db = Room.databaseBuilder(context, TaskDatabase::class.java, name).build()
            repo = RoomRewardRepository(db, clock)
            assertEquals(id, repo.observe().first().intent!!.id)
            assertEquals(PurchaseResult.PURCHASED, repo.confirm(id))
            repo.save(reward.copy(name = "Novo", description = "Alterada", cost = 99, icon = "GAME"))
            repo.delete("r")
            RoomTaskRepository(db, clock).execute(TaskAction.Undo(occurrence))
            RoomTaskRepository(db, clock).execute(TaskAction.Refresh)
            assertEquals(-5L, repo.observe().first().wallet.balance)
            db.close()
            db = Room.databaseBuilder(context, TaskDatabase::class.java, name).build()
            repo = RoomRewardRepository(db, clock)
            assertEquals(PurchaseResult.PURCHASED, repo.confirm(id))
            val s = repo.observe().first()
            assertEquals("Pausa", s.purchases.single().reward.name)
            assertEquals("Descrição original", s.purchases.single().reward.description)
            assertEquals("REST", s.purchases.single().reward.icon)
            assertTrue(s.rewards.isEmpty())
            assertEquals(1, s.wallet.entries.count { it.kind == "PURCHASE" })
            assertEquals(0L, s.wallet.totalXp)
            assertNotNull(s.intent)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun migrationFromSchema3PreservesEveryOldColumnAndTable() {
        val name = "reward-migration-${System.nanoTime()}.db"
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.ethos.app.core.tasks.data.TaskDatabase/3.json").bufferedReader().use { it.readText() })
            .getJSONObject("database")
        val expected = mutableMapOf<String, List<String?>>()
        val old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        try {
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val values = ContentValues()
                val fields = entity.getJSONArray("fields")
                for (j in 0 until fields.length()) {
                    val field = fields.getJSONObject(j)
                    val column = field.getString("columnName")
                    if (!field.optBoolean("notNull", false)) values.putNull(column)
                    else if (field.getString("affinity") == "TEXT") values.put(column, "preserved-$column")
                    else values.put(column, 1L)
                }
                old.insertOrThrow(table, null, values)
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) old.execSQL(
                    indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                old.rawQuery("SELECT * FROM `$table`", null).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    expected[table] = (0 until cursor.columnCount).map { cursor.getString(it) }
                }
            }
            old.version = 3
        } finally { old.close() }
        val db = Room.databaseBuilder(context, TaskDatabase::class.java, name)
            .addMigrations(REWARD_MIGRATION_3_4, com.ethos.app.core.reminders.data.REMINDER_MIGRATION_4_5,
                com.ethos.app.core.timers.data.TIMER_MIGRATION_5_6).build()
        try {
            expected.forEach { (table, row) ->
                db.openHelper.readableDatabase.query("SELECT * FROM `$table`").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(table, row, row.indices.map { cursor.getString(it) })
                    assertFalse(cursor.moveToNext())
                }
            }
            for (table in listOf("rewards", "reward_intents", "reward_purchases")) {
                db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$table`").use {
                    assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
                }
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun concurrentDistinctIntentsAndStaleRenderedPreviewRequireReconfirmation() = runBlocking {
        val db = memory()
        try {
            val repo = RoomRewardRepository(db, clock)
            earn(db)
            repo.save(reward)
            repo.preview("r")
            val old = repo.observe().first().intent!!
            repo.save(reward.copy(cost = 6))
            assertEquals(PurchaseResult.STALE, repo.confirm(old))
            assertEquals(PurchaseResult.STALE, repo.confirm(old))
            assertTrue(repo.observe().first().purchases.isEmpty())
            val refreshed = repo.observe().first().intent!!
            // Independent durable intent fixtures model two confirmations from the same disclosed state.
            val competitor = refreshed.copy(id = "competitor")
            db.rewards().intent(competitor.entity())
            val results = coroutineScope {
                listOf(refreshed, competitor).map { async(Dispatchers.Default) { repo.confirm(it) } }.awaitAll()
            }
            assertEquals(1, results.count { it == PurchaseResult.PURCHASED })
            assertEquals(1, results.count { it == PurchaseResult.STALE })
            assertEquals(4L, repo.observe().first().wallet.balance)
            assertEquals(1, repo.observe().first().purchases.size)
        } finally { db.close() }
    }

    @Test fun crudOrderingAndNegativeWalletCannotPurchase() = runBlocking {
        val db = memory()
        try {
            val repo = RoomRewardRepository(db, clock)
            val tasks = RoomTaskRepository(db, clock)
            val occurrence = earn(db)
            tasks.execute(TaskAction.Undo(occurrence))
            tasks.execute(TaskAction.Resolve(occurrence, false, day))
            repo.save(reward)
            repo.save(reward.copy(id = "second", name = "Segundo"))
            repo.move("second", -1)
            assertEquals(listOf("second", "r"), repo.observe().first().rewards.map { it.id })
            repo.save(reward.copy(name = "Editado"))
            assertEquals(listOf("second", "r"), repo.observe().first().rewards.map { it.id })
            repo.preview("r")
            assertEquals(PurchaseResult.UNAVAILABLE, repo.confirm(repo.observe().first().intent!!))
            assertEquals(-10L, repo.observe().first().wallet.balance)
            assertTrue(repo.observe().first().purchases.isEmpty())
        } finally { db.close() }
    }

    @Test fun habitCorrectionPreservesPurchaseAndOriginalDebit() = runBlocking {
        day = LocalDate.of(2026, 11, 2) // Full civil week; no partial-period target fixture needed.
        val db = memory()
        try {
            val repo = RoomRewardRepository(db, clock)
            val habits = RoomHabitRepository(db, clock)
            habits.execute(HabitAction.Save(Habit("h", day, "Prática fictícia"),
                HabitRule("rule", "h", day, HabitType.PRACTICE, HabitUnit.WEEK, 2)))
            habits.execute(HabitAction.Record("record", "h", day))
            repo.save(reward)
            repo.preview("r")
            assertEquals(PurchaseResult.PURCHASED, repo.confirm(repo.observe().first().intent!!))
            val before = repo.observe().first().wallet.entries.single { it.kind == "PURCHASE" }
            habits.execute(HabitAction.Correct("record", delete = true))
            val after = repo.observe().first()
            assertEquals(-5L, after.wallet.balance)
            assertEquals(0L, after.wallet.totalXp)
            assertEquals(before, after.wallet.entries.single { it.kind == "PURCHASE" })
            assertEquals(1, after.purchases.size)
        } finally { db.close() }
    }
}
