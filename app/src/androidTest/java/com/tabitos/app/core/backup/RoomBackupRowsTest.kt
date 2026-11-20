package com.tabitos.app.core.backup

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.tabitos.app.core.accounting.domain.Price
import com.tabitos.app.core.tasks.data.TaskDatabase
import com.tabitos.app.core.timers.domain.TimerNow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomBackupRowsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val now = TimerNow(wall = 5_000_000L, elapsed = 800_000L, boot = 4)

    private suspend fun <R> withMemory(block: suspend (TaskDatabase) -> R): R {
        val database = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        return try { block(database) } finally { database.close() }
    }

    @Test fun snapshotPausesOnlyCopiedSessionAndPreviewLeavesDestinationUntouched() = runBlocking {
        withMemory { source ->
            val bytes = source.withTransaction {
                val db = source.openHelper.writableDatabase
                db.execSQL("INSERT INTO task_lists VALUES ('list', 'Estudos', 0, 0, 0, 0)")
                db.execSQL(
                    """INSERT INTO timer_sessions(id, kind, sourceId, title, state, period, created, reached)
                        VALUES ('session','TASK','source','Estudar','RUNNING','TOTAL',1000,0)"""
                )
                db.execSQL(
                    """INSERT INTO timer_segments(id, sessionId, startWall, startElapsed, bootCount,
                        checkpointElapsed, manual, active, created)
                        VALUES ('segment','session',1000,80000,4,90000,0,1,1000)"""
                )
                RoomBackupRows.capture(db, now)
            }
            assertEquals("RUNNING", source.timers().sessions().single().state)
            assertEquals(null, source.timers().segments().single().endWall)
            withMemory { dest ->
                dest.withTransaction {
                    val sql = dest.openHelper.writableDatabase
                    sql.execSQL("INSERT INTO task_lists VALUES ('existing','Terça',0,0,0,0)")
                    assertEquals(BackupCounts(1, 0, 0), RoomBackupRows.inspect(bytes, sql))
                }
                assertEquals("Terça", dest.tasks().lists().single().name)
                repeat(2) {
                    dest.withTransaction { RoomBackupRows.replace(dest.openHelper.writableDatabase, bytes) }
                    assertEquals("Estudos", dest.tasks().lists().single().name)
                    assertEquals("PAUSED", dest.timers().sessions().single().state)
                    assertEquals(721_000L, dest.timers().segments().single().endWall)
                }
            }
        }
    }

    @Test fun rejectsTruncatedContentAndRecognizesSeedPrices() = runBlocking {
        withMemory { room ->
            room.withTransaction {
                val db = room.openHelper.writableDatabase
                db.execSQL("INSERT INTO accounting_activation VALUES (1, 12345)")
                Price.defaults().forEach { price ->
                    db.execSQL("INSERT INTO accounting_prices VALUES (?,?,?,?)",
                        arrayOf<Any>(price.category, price.coins, price.xp, price.penalty))
                }
                assertFalse(RoomBackupRows.hasPersonalData(db))
                db.execSQL("UPDATE accounting_prices SET coins=101 WHERE category=0")
                assertTrue(RoomBackupRows.hasPersonalData(db))
                val good = RoomBackupRows.capture(db, now)
                val rejected = runCatching { RoomBackupRows.inspect(good.copyOf(good.size - 1), db) }
                assertTrue(rejected.isFailure)
            }
            assertEquals(101L, room.accounting().prices().single { it.category == 0 }.coins)
        }
    }

    @Test fun restoredRowsMatchJournalFingerprintAndStaleRemindersStayQuiet() = runBlocking {
        withMemory { room ->
            room.withTransaction {
                val db = room.openHelper.writableDatabase
                db.execSQL(
                    """INSERT INTO reminder_deliveries(key, groupId, kind, sourceId, periodKey, slotDate, dueAt,
                        snoozed, state) VALUES ('old','g','TASK','t','p','2026-11-18',1000,0,'PENDING'),
                        ('next','g','TASK','t','p','2026-11-20',9000000,0,'PENDING')"""
                )
                val bytes = RoomBackupRows.capture(db, now)
                RoomBackupRows.replace(db, bytes)
                RoomBackupRows.retireOverdueReminders(db, now.wall)
                val written = RoomBackupRows.fingerprint(db, now)
                assertTrue(written.contentEquals(RoomBackupRows.fingerprint(db, now.copy(wall = now.wall + 60_000))))
            }
            val states = room.reminders().deliveries().associate { it.key to it.state }
            assertEquals(mapOf("old" to "CANCELLED", "next" to "PENDING"), states)
        }
    }
}
