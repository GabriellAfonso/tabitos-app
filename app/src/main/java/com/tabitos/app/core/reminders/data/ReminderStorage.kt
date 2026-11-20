package com.tabitos.app.core.reminders.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tabitos.app.core.reminders.domain.*
import java.time.LocalDate

@Entity(tableName = "reminder_definitions")
data class ReminderEntity(
    @PrimaryKey val id: String, val groupId: String, val kind: String, val sourceId: String,
    val occurrenceId: String?, val fromOrdinal: Int, val enabled: Boolean, val mode: String,
    val chosenDate: String?, val anchorDay: Long, val minute: Int, val weekdays: Int,
    val offsetMinutes: Long, val referenceMinute: Int?, val createdAt: Long,
) {
    fun domain() = ReminderDefinition(id, groupId, ReminderKind.valueOf(kind), sourceId, occurrenceId,
        fromOrdinal, enabled, ReminderMode.valueOf(mode), chosenDate?.let(LocalDate::parse), anchorDay,
        minute, weekdays, offsetMinutes, referenceMinute, createdAt)
}
fun ReminderDefinition.entity() = ReminderEntity(id, groupId, kind.name, sourceId, occurrenceId, fromOrdinal,
    enabled, mode.name, chosenDate?.toString(), anchorDay, minute, weekdays, offsetMinutes, referenceMinute, createdAt)

@Entity(tableName = "reminder_deliveries")
data class DeliveryEntity(
    @PrimaryKey val key: String, val groupId: String, val kind: String, val sourceId: String,
    val periodKey: String, val slotDate: String, val anchorDate: String?, val dueAt: Long,
    val snoozed: Boolean, val state: String,
) {
    fun domain() = ReminderDelivery(key, groupId, ReminderKind.valueOf(kind), sourceId, periodKey,
        LocalDate.parse(slotDate), anchorDate?.let(LocalDate::parse), dueAt, snoozed, DeliveryState.valueOf(state))
}
fun ReminderDelivery.entity() = DeliveryEntity(key, groupId, kind.name, sourceId, periodKey,
    slotDate.toString(), anchorDate?.toString(), dueAt, snoozed, state.name)

@Entity(tableName = "reminder_settings")
data class ReminderSettingsEntity(@PrimaryKey val id: Int = 1, val summaryMinute: Int? = null,
    val lastSummaryDate: String? = null)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder_definitions") suspend fun definitions(): List<ReminderEntity>
    @Query("SELECT * FROM reminder_deliveries") suspend fun deliveries(): List<DeliveryEntity>
    @Query("SELECT * FROM reminder_settings WHERE id = 1") suspend fun settings(): ReminderSettingsEntity?
    @Upsert suspend fun definition(row: ReminderEntity)
    @Query("DELETE FROM reminder_definitions WHERE id = :id") suspend fun remove(id: String)
    @Upsert suspend fun deliveries(rows: List<DeliveryEntity>)
    @Upsert suspend fun settings(row: ReminderSettingsEntity)
    @Query("""UPDATE reminder_settings SET lastSummaryDate = :date WHERE id = 1 AND summaryMinute = :minute
        AND (lastSummaryDate IS NULL OR lastSummaryDate < :date)""")
    suspend fun claimReview(date: String, minute: Int): Int
    @Query("DELETE FROM reminder_definitions WHERE kind = :kind AND sourceId IN (:sourceIds)")
    suspend fun removeForSources(kind: String, sourceIds: List<String>)
    @Query("DELETE FROM reminder_definitions WHERE occurrenceId IN (:occurrenceIds)")
    suspend fun removeForOccurrences(occurrenceIds: List<String>)
    @Query("DELETE FROM reminder_deliveries WHERE kind = :kind AND sourceId IN (:sourceIds)")
    suspend fun removeDeliveries(kind: String, sourceIds: List<String>)
}

val REMINDER_MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS reminder_definitions (
            id TEXT NOT NULL PRIMARY KEY, groupId TEXT NOT NULL, kind TEXT NOT NULL, sourceId TEXT NOT NULL,
            occurrenceId TEXT, fromOrdinal INTEGER NOT NULL, enabled INTEGER NOT NULL, mode TEXT NOT NULL,
            chosenDate TEXT, anchorDay INTEGER NOT NULL, minute INTEGER NOT NULL, weekdays INTEGER NOT NULL,
            offsetMinutes INTEGER NOT NULL, referenceMinute INTEGER, createdAt INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS reminder_deliveries (
            `key` TEXT NOT NULL PRIMARY KEY, groupId TEXT NOT NULL, kind TEXT NOT NULL, sourceId TEXT NOT NULL,
            periodKey TEXT NOT NULL, slotDate TEXT NOT NULL, anchorDate TEXT, dueAt INTEGER NOT NULL,
            snoozed INTEGER NOT NULL, state TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS reminder_settings (
            id INTEGER NOT NULL PRIMARY KEY, summaryMinute INTEGER, lastSummaryDate TEXT)""")
    }
}
