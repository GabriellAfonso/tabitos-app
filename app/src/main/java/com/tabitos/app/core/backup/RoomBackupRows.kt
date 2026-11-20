package com.tabitos.app.core.backup

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tabitos.app.core.accounting.domain.Price
import com.tabitos.app.core.reminders.domain.DeliveryState
import com.tabitos.app.core.timers.domain.TimerAuditKind
import com.tabitos.app.core.timers.domain.TimerNow
import com.tabitos.app.core.timers.domain.TimerRules
import com.tabitos.app.core.timers.domain.TimerSegment
import com.tabitos.app.core.timers.domain.TimerState
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest

data class BackupCounts(val lists: Int, val tasks: Int, val habits: Int)

/** The caller holds the same Room write transaction for every operation and any coordinated preference snapshot. */
object RoomBackupRows {
    private const val SCHEMA = 7
    private const val NULL = 0
    private const val INTEGER = 1
    private const val TEXT = 2

    private val tables = listOf(
        "task_lists", "task_definitions", "task_occurrences", "task_checklist", "task_audit",
        "habits", "habit_rules", "habit_intervals", "habit_overrides", "habit_records", "habit_reviews", "habit_audits",
        "accounting_activation", "accounting_prices", "accounting_entitlements", "accounting_ledger",
        "rewards", "reward_intents", "reward_purchases", "reminder_definitions", "reminder_deliveries",
        "reminder_settings", "timer_goals", "timer_sessions", "timer_segments", "timer_audits",
        "trackers", "tracker_versions", "tracker_questions", "tracker_options", "tracker_entries", "tracker_answers",
    )

    private data class Column(val name: String, val type: Int, val required: Boolean)

    fun capture(db: SupportSQLiteDatabase, now: TimerNow): ByteArray {
        check(db.inTransaction() && db.version == SCHEMA)
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeInt(SCHEMA)
            tables.forEach { table ->
                val columns = columns(db, table)
                output.writeUTF(table)
                output.writeInt(columns.size)
                columns.forEach { output.writeUTF(it.name) }
                val names = columns.joinToString { quote(it.name) }
                db.query("SELECT $names FROM $table ORDER BY rowid").use { cursor ->
                    output.writeInt(cursor.count)
                    while (cursor.moveToNext()) writeRow(output, table, columns, cursor, now)
                }
            }
        }
        return bytes.toByteArray()
    }

    fun inspect(data: ByteArray, db: SupportSQLiteDatabase): BackupCounts {
        check(db.inTransaction())
        db.execSQL("SAVEPOINT backup_preview")
        return try {
            replace(db, data)
            BackupCounts(db.count("task_lists"), db.count("task_definitions"), db.count("habits"))
        } finally {
            // Android 24–27 intercepts statements starting with ROL as a full rollback.
            db.execSQL(";ROLLBACK TO backup_preview")
            db.execSQL("RELEASE backup_preview")
        }
    }

    fun replace(db: SupportSQLiteDatabase, data: ByteArray) {
        check(db.inTransaction() && db.version == SCHEMA)
        DataInputStream(data.inputStream()).use { input ->
            require(input.readInt() == SCHEMA)
            tables.asReversed().forEach { db.execSQL("DELETE FROM $it") }
            tables.forEach { table -> readTable(db, input, table) }
            require(input.read() == -1)
        }
        validateBackupRelations(db)
        // A pending completion belongs to the original installation, which may still be running.
        val reached = TimerAuditKind.REACHED.name
        val consumed = TimerAuditKind.TRANSFERRED.name
        val confirmed = TimerAuditKind.CONFIRMED.name
        db.execSQL(
            """INSERT INTO timer_audits(id, sessionId, kind, at)
               SELECT 'transfer:' || r.id, r.sessionId, ?, r.at FROM timer_audits r
               WHERE r.kind = ? AND NOT EXISTS (SELECT 1 FROM timer_audits c
               WHERE c.sessionId = r.sessionId AND c.kind IN (?, ?) AND c.at >= r.at)""",
            arrayOf(consumed, reached, confirmed, consumed),
        )
    }

    /** Digest of the stored routine, used to tell whether an interrupted restore reached its commit. */
    fun fingerprint(db: SupportSQLiteDatabase, now: TimerNow): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(capture(db, now))

    /**
     * Reminders that were already due when the copy was restored belong to the original installation: they are
     * closed so the new installation does not post a burst of stale notices. Future slots stay pending.
     */
    fun retireOverdueReminders(db: SupportSQLiteDatabase, now: Long) {
        check(db.inTransaction())
        db.execSQL(
            "UPDATE reminder_deliveries SET state = ? WHERE state = ? AND dueAt <= ?",
            arrayOf<Any>(DeliveryState.CANCELLED.name, DeliveryState.PENDING.name, now),
        )
    }

    fun hasPersonalData(db: SupportSQLiteDatabase): Boolean {
        check(db.inTransaction())
        val automatic = setOf("accounting_activation", "accounting_prices", "reminder_settings")
        if (tables.any { it !in automatic && db.hasRow("SELECT 1 FROM $it LIMIT 1") }) return true
        if (db.hasRow("SELECT 1 FROM reminder_settings WHERE summaryMinute IS NOT NULL OR lastSummaryDate IS NOT NULL")) {
            return true
        }
        val prices = db.query("SELECT category, coins, xp, penalty FROM accounting_prices ORDER BY category").use {
            buildList {
                while (it.moveToNext()) add(Price(it.getInt(0), it.getLong(1), it.getLong(2), it.getLong(3)))
            }
        }
        return prices.isNotEmpty() && prices != Price.defaults()
    }

    private fun columns(db: SupportSQLiteDatabase, table: String): List<Column> =
        db.query("PRAGMA table_info($table)").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    val type = when (cursor.getString(cursor.getColumnIndexOrThrow("type"))) {
                        "INTEGER" -> INTEGER
                        "TEXT" -> TEXT
                        else -> error("Unsupported local database type")
                    }
                    add(Column(name, type, cursor.getInt(cursor.getColumnIndexOrThrow("notnull")) != 0))
                }
            }.sortedBy { it.name }.also { check(it.isNotEmpty()) }
        }

    private fun writeRow(
        output: DataOutputStream,
        table: String,
        columns: List<Column>,
        row: Cursor,
        now: TimerNow,
    ) {
        val segment = if (table == "timer_segments" && row.number("endWall") == null) {
            TimerSegment(
                id = row.string("id"),
                sessionId = row.string("sessionId"),
                startWall = requireNotNull(row.number("startWall")),
                startElapsed = row.number("startElapsed"),
                bootCount = row.number("bootCount")?.toInt(),
                checkpointElapsed = row.number("checkpointElapsed"),
                limitMillis = row.number("limitMillis"),
                created = requireNotNull(row.number("created")),
            )
        } else null
        columns.forEachIndexed { index, column ->
            when {
                segment != null && column.name == "endWall" -> output.integer(TimerRules.endWall(segment, now))
                segment?.startElapsed != null && column.name == "checkpointElapsed" ->
                    output.integer(segment.startElapsed + TimerRules.duration(segment, now))
                table == "timer_sessions" && column.name == "state" && row.getString(index) == TimerState.RUNNING.name ->
                    output.string(TimerState.PAUSED.name)
                row.isNull(index) -> output.writeByte(NULL)
                row.getType(index) == Cursor.FIELD_TYPE_INTEGER && column.type == INTEGER ->
                    output.integer(row.getLong(index))
                row.getType(index) == Cursor.FIELD_TYPE_STRING && column.type == TEXT ->
                    output.string(row.getString(index))
                else -> error("Unsupported stored value")
            }
        }
    }

    private fun readTable(db: SupportSQLiteDatabase, input: DataInputStream, table: String) {
        val columns = columns(db, table)
        require(input.readUTF() == table && input.readInt() == columns.size)
        columns.forEach { require(input.readUTF() == it.name) }
        val rowCount = input.readInt()
        require(rowCount in 0..input.available() / columns.size)
        val names = columns.joinToString { quote(it.name) }
        val placeholders = columns.joinToString { "?" }
        db.compileStatement("INSERT INTO $table ($names) VALUES ($placeholders)").use { stmt ->
            repeat(rowCount) {
                columns.forEachIndexed { index, column ->
                    val type = input.readUnsignedByte()
                    require(type == column.type || (type == NULL && !column.required))
                    when (type) {
                        INTEGER -> {
                            val value = input.readLong()
                            validateBackupNumber(table, column.name, value)
                            stmt.bindLong(index + 1, value)
                        }
                        TEXT -> {
                            val length = input.readInt()
                            require(length in 0..input.available())
                            val bytes = ByteArray(length).also(input::readFully)
                            val value = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()
                            validateBackupText(table, column.name, value)
                            stmt.bindString(index + 1, value)
                        }
                        NULL -> stmt.bindNull(index + 1)
                    }
                }
                stmt.executeInsert()
            }
        }
    }

    private fun quote(name: String) = "\"" + name.replace("\"", "\"\"") + "\""

    private fun SupportSQLiteDatabase.count(table: String) = query("SELECT COUNT(*) FROM $table").use {
        check(it.moveToFirst())
        it.getInt(0)
    }

    private fun Cursor.string(name: String): String = getString(getColumnIndexOrThrow(name))
    private fun Cursor.number(name: String): Long? {
        val index = getColumnIndexOrThrow(name)
        return if (isNull(index)) null else getLong(index)
    }

    private fun DataOutputStream.integer(value: Long) {
        writeByte(INTEGER)
        writeLong(value)
    }

    private fun DataOutputStream.string(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeByte(TEXT)
        writeInt(bytes.size)
        write(bytes)
    }
}

internal fun SupportSQLiteDatabase.hasRow(sql: String): Boolean = query(sql).use { it.moveToFirst() }
