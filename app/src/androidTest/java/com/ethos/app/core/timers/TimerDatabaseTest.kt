package com.ethos.app.core.timers

import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.ethos.app.core.accounting.data.ACCOUNTING_MIGRATION_2_3
import com.ethos.app.core.habits.data.HABIT_MIGRATION_1_2
import com.ethos.app.core.reminders.data.REMINDER_MIGRATION_4_5
import com.ethos.app.core.rewards.data.REWARD_MIGRATION_3_4
import com.ethos.app.core.rewards.data.RoomRewardRepository
import com.ethos.app.core.rewards.domain.*
import com.ethos.app.core.tasks.data.*
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.timers.data.RoomTimerRepository
import com.ethos.app.core.timers.data.TIMER_MIGRATION_5_6
import com.ethos.app.core.timers.domain.*
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TimerDatabaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val day = LocalDate.of(2026, 11, 7)
    private val minute = 60_000L
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val taskClock = object : TaskClock {
        override fun today() = day
        override fun nowMillis() = 1000L
    }
    private class Clock(var wall: Long, var elapsed: Long, var boot: Int?, val zone: ZoneId) : TimerClock {
        override fun wallMillis() = wall
        override fun elapsedMillis() = elapsed
        override fun bootCount() = boot
        override fun zone() = zone
        fun advance(ms: Long) { wall += ms; elapsed += ms }
    }
    private fun clock() = Clock(day.atTime(10, 0).atZone(zone).toInstant().toEpochMilli(), 100_000, 4, zone)
    private fun memory() = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
    private fun task(id: String) = TimerStart(TimerKind.TASK, id, "Tarefa $id", "task:$id")

    @Test fun migration5PreservesEveryRowAndAddsNullDurations() {
        val name = "timer-migration-${System.nanoTime()}.db"
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.ethos.app.core.tasks.data.TaskDatabase/5.json").bufferedReader().use { it.readText() })
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
            old.version = 5
        }
        val db = Room.databaseBuilder(context, TaskDatabase::class.java, name).addMigrations(HABIT_MIGRATION_1_2,
            ACCOUNTING_MIGRATION_2_3, REWARD_MIGRATION_3_4, REMINDER_MIGRATION_4_5, TIMER_MIGRATION_5_6).build()
        try {
            expected.forEach { (table, row) -> db.openHelper.readableDatabase.query("SELECT * FROM `$table`").use {
                assertTrue(it.moveToFirst())
                assertEquals(table, row, row.indices.map(it::getString))
                assertFalse(it.moveToNext())
            } }
            for (table in listOf("rewards", "reward_intents", "reward_purchases")) {
                db.openHelper.readableDatabase.query("SELECT reward_minutes FROM `$table`").use {
                    assertTrue(it.moveToFirst()); assertTrue(it.isNull(0))
                }
            }
            for (table in listOf("timer_goals", "timer_sessions", "timer_segments", "timer_audits")) {
                db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
                    assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
                }
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun oneRunningTimerConflictPauseResumeAndRebootRecovery() = runBlocking {
        val db = memory()
        try {
            val clock = clock()
            val repo = RoomTimerRepository(db, clock)
            assertEquals(StartResult.STARTED, repo.start(task("a"), false))
            assertEquals(StartResult.STARTED, repo.start(task("a"), false))
            clock.advance(5 * minute)
            assertEquals(StartResult.CONFLICT, repo.start(task("b"), false))
            assertEquals("a", repo.observe().first().running!!.sourceId)
            assertEquals(StartResult.STARTED, repo.start(task("b"), true))
            var s = repo.observe().first()
            assertEquals(1, s.sessions.count { it.state == TimerState.RUNNING })
            val a = s.open(TimerKind.TASK, "a")!!
            assertEquals(5 * minute, TimerRules.sessionTotal(s, a.id, TimerNow(clock.wall, clock.elapsed, 4)))
            clock.advance(10 * minute)
            repo.pause(s.running!!.id)
            // Paused gap adds nothing; resuming keeps the same session identity.
            clock.advance(30 * minute)
            assertEquals(StartResult.STARTED, repo.start(task("a"), false))
            assertEquals(a.id, repo.observe().first().running!!.id)
            clock.advance(2 * minute)
            repo.checkpoint()
            // Device off for an hour, then a new boot with a reset monotonic clock.
            clock.wall += 60 * minute; clock.elapsed = 5_000; clock.boot = 5
            repo.sync()
            s = repo.observe().first()
            assertNull(s.running)
            val now = TimerNow(clock.wall, clock.elapsed, 5)
            assertEquals(7 * minute, TimerRules.sessionTotal(s, a.id, now))
            assertTrue(s.audits.any { it.kind == TimerAuditKind.RECOVERED })
        } finally { db.close() }
    }

    @Test fun goalCapsPausesAlertsOnceAndRefusesOverrun() = runBlocking {
        val db = memory()
        try {
            val clock = clock()
            val repo = RoomTimerRepository(db, clock)
            repo.saveGoal("task:g", 1)
            repo.start(task("g"), false)
            clock.advance(3 * minute)
            assertEquals(1, repo.sync().alerts.size)
            assertEquals(0, repo.sync().alerts.size)
            val s = repo.observe().first()
            val session = s.open(TimerKind.TASK, "g")!!
            assertTrue(session.reached)
            assertEquals(minute, TimerRules.sessionTotal(s, session.id, TimerNow(clock.wall, clock.elapsed, 4)))
            assertEquals(StartResult.UNAVAILABLE, repo.start(task("g"), false))
            assertNull(repo.observe().first().running)
        } finally { db.close() }
    }

    @Test fun manualAddOverlapCorrectionAndDeletionAreAudited() = runBlocking {
        val db = memory()
        try {
            val clock = clock()
            val repo = RoomTimerRepository(db, clock)
            val bounds = TimerBounds(0)
            val start = clock.wall - 120 * minute
            repo.addManual(task("m"), start, start + 30 * minute, bounds)
            val overlap = runCatching { repo.addManual(task("n"), start + 10 * minute, start + 40 * minute, bounds) }
            assertEquals(TimerProblem.OVERLAP, (overlap.exceptionOrNull() as TimerRuleException).problem)
            val segment = repo.observe().first().segments.single()
            repo.correct(segment.id, start, start + 10 * minute, bounds)
            var s = repo.observe().first()
            assertEquals(10 * minute, TimerRules.progress(s, TimerKind.TASK, "m", TimerPeriod.TOTAL, day,
                TimerNow(clock.wall, clock.elapsed, 4), zone))
            val corrected = s.audits.single { it.kind == TimerAuditKind.CORRECTED }
            assertEquals(start + 30 * minute, corrected.beforeEnd)
            assertEquals(start + 10 * minute, corrected.afterEnd)
            repo.delete(s.segments.single { it.active }.id)
            s = repo.observe().first()
            assertTrue(s.segments.none { it.active })
            assertEquals(3, s.audits.size)
        } finally { db.close() }
    }

    @Test fun timedPurchaseAsksBeforeChargeIsIdempotentAndResumesWithoutRepurchase() = runBlocking {
        val db = memory()
        try {
            val clock = clock()
            val timers = RoomTimerRepository(db, clock)
            val rewards = RoomRewardRepository(db, taskClock, clock)
            val tasks = RoomTaskRepository(db, taskClock)
            tasks.execute(TaskAction.Create("w", TaskFields(title = "w"), RepeatRule(start = day)))
            tasks.execute(TaskAction.Resolve(tasks.observe().first().occurrences.first().id, true, day))
            rewards.save(Reward("r", "Jogo", cost = 5, limit = 1, minutes = 2))
            timers.start(task("t"), false)
            rewards.preview("r")
            val intent = rewards.observe().first().intent!!
            assertEquals(PurchaseResult.RUNNING_CONFLICT, rewards.confirm(intent))
            var r = rewards.observe().first()
            assertEquals(10L, r.wallet.balance)
            assertTrue(r.purchases.isEmpty())
            assertEquals("t", timers.observe().first().running!!.sourceId)
            assertEquals(PurchaseResult.PURCHASED, rewards.confirm(intent, true))
            assertEquals(PurchaseResult.PURCHASED, rewards.confirm(intent, true))
            r = rewards.observe().first()
            assertEquals(5L, r.wallet.balance)
            assertEquals(1, r.purchases.size)
            assertEquals(1, r.wallet.entries.count { it.kind == "PURCHASE" })
            var s = timers.observe().first()
            val reward = s.running!!
            assertEquals(TimerKind.REWARD, reward.kind)
            assertEquals(1, s.sessions.count { it.kind == TimerKind.REWARD })
            clock.advance(minute)
            timers.pause(reward.id)
            clock.advance(10 * minute)
            assertEquals(StartResult.STARTED,
                timers.start(TimerStart(TimerKind.REWARD, reward.sourceId, "Jogo"), false))
            assertEquals(1, rewards.observe().first().purchases.size)
            assertEquals(5L, rewards.observe().first().wallet.balance)
            clock.advance(5 * minute)
            val sync = timers.sync()
            assertEquals(TimerAuditKind.EXHAUSTED, sync.alerts.single().kind)
            s = timers.observe().first()
            assertEquals(2 * minute, TimerRules.sessionTotal(s, reward.id, TimerNow(clock.wall, clock.elapsed, 4)))
            assertEquals(StartResult.UNAVAILABLE, timers.start(TimerStart(TimerKind.REWARD, reward.sourceId, "Jogo"),
                false))
            assertEquals(5L, rewards.observe().first().wallet.balance)
        } finally { db.close() }
    }
}
