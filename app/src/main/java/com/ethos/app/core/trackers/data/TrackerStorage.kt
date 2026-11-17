package com.ethos.app.core.trackers.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ethos.app.core.trackers.domain.*
import java.time.LocalDate

@Entity(tableName = "trackers", indices = [Index(value = ["habitId"], unique = true)])
data class TrackerEntity(
    @PrimaryKey val id: String, val habitId: String, val created: LocalDate, val multiple: Boolean,
    val active: Boolean, val version: Int, val updated: Long,
) {
    fun domain() = Tracker(id, habitId, created, multiple, active, version, updated)
}
fun Tracker.entity() = TrackerEntity(id, habitId, created, multiple, active, version, updated)

@Entity(tableName = "tracker_versions", primaryKeys = ["trackerId", "number"])
data class TrackerVersionEntity(val trackerId: String, val number: Int, val created: Long)

@Entity(tableName = "tracker_questions", primaryKeys = ["trackerId", "version", "questionId"])
data class TrackerQuestionEntity(
    val trackerId: String, val version: Int, val questionId: String, val position: Int, val type: String,
    val title: String, val required: Boolean, val isPrimary: Boolean, val min: Int, val max: Int,
    val minLabel: String, val maxLabel: String, val multiple: Boolean,
)

@Entity(tableName = "tracker_options", primaryKeys = ["trackerId", "version", "optionId"])
data class TrackerOptionEntity(
    val trackerId: String, val version: Int, val optionId: String, val questionId: String, val position: Int,
    val label: String, val description: String,
)

@Entity(tableName = "tracker_entries", indices = [Index("trackerId"),
    Index(value = ["trackerId", "dayKey"], unique = true)])
data class TrackerEntryEntity(
    @PrimaryKey val id: String, val trackerId: String, val version: Int, val date: LocalDate, val dayKey: String?,
    val filledAt: Long, val updatedAt: Long,
)

@Entity(tableName = "tracker_answers", primaryKeys = ["entryId", "questionId"])
data class TrackerAnswerEntity(
    val entryId: String, val questionId: String, val scale: Int?, val choices: String, val text: String,
)

@Dao
interface TrackerDao {
    @Query("SELECT * FROM trackers") suspend fun trackers(): List<TrackerEntity>
    @Query("SELECT * FROM tracker_versions") suspend fun versions(): List<TrackerVersionEntity>
    @Query("SELECT * FROM tracker_questions ORDER BY position") suspend fun questions(): List<TrackerQuestionEntity>
    @Query("SELECT * FROM tracker_options ORDER BY position") suspend fun options(): List<TrackerOptionEntity>
    @Query("SELECT * FROM tracker_entries") suspend fun entries(): List<TrackerEntryEntity>
    @Query("SELECT * FROM tracker_answers") suspend fun answers(): List<TrackerAnswerEntity>
    @Upsert suspend fun trackers(rows: List<TrackerEntity>)
    @Insert suspend fun versions(rows: List<TrackerVersionEntity>)
    @Insert suspend fun questions(rows: List<TrackerQuestionEntity>)
    @Insert suspend fun options(rows: List<TrackerOptionEntity>)
    @Upsert suspend fun entry(row: TrackerEntryEntity)
    @Insert suspend fun answers(rows: List<TrackerAnswerEntity>)
    @Query("DELETE FROM tracker_answers WHERE entryId = :id") suspend fun removeAnswers(id: String)
    @Query("DELETE FROM tracker_entries WHERE id = :id") suspend fun removeEntry(id: String)
    @Query("SELECT id FROM trackers WHERE habitId = :habitId") suspend fun trackerIds(habitId: String): List<String>
    @Query("DELETE FROM tracker_answers WHERE entryId IN (SELECT id FROM tracker_entries WHERE trackerId = :id)")
    suspend fun removeTrackerAnswers(id: String)
    @Query("DELETE FROM tracker_entries WHERE trackerId = :id") suspend fun removeTrackerEntries(id: String)
    @Query("DELETE FROM tracker_options WHERE trackerId = :id") suspend fun removeOptions(id: String)
    @Query("DELETE FROM tracker_questions WHERE trackerId = :id") suspend fun removeQuestions(id: String)
    @Query("DELETE FROM tracker_versions WHERE trackerId = :id") suspend fun removeVersions(id: String)
    @Query("DELETE FROM trackers WHERE id = :id") suspend fun removeTracker(id: String)
}

val TRACKER_MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS `trackers` (`id` TEXT NOT NULL, `habitId` TEXT NOT NULL,
            `created` INTEGER NOT NULL, `multiple` INTEGER NOT NULL, `active` INTEGER NOT NULL,
            `version` INTEGER NOT NULL, `updated` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_trackers_habitId` ON `trackers` (`habitId`)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `tracker_versions` (`trackerId` TEXT NOT NULL,
            `number` INTEGER NOT NULL, `created` INTEGER NOT NULL, PRIMARY KEY(`trackerId`, `number`))""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `tracker_questions` (`trackerId` TEXT NOT NULL,
            `version` INTEGER NOT NULL, `questionId` TEXT NOT NULL, `position` INTEGER NOT NULL, `type` TEXT NOT NULL,
            `title` TEXT NOT NULL, `required` INTEGER NOT NULL, `isPrimary` INTEGER NOT NULL, `min` INTEGER NOT NULL,
            `max` INTEGER NOT NULL, `minLabel` TEXT NOT NULL, `maxLabel` TEXT NOT NULL, `multiple` INTEGER NOT NULL,
            PRIMARY KEY(`trackerId`, `version`, `questionId`))""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `tracker_options` (`trackerId` TEXT NOT NULL,
            `version` INTEGER NOT NULL, `optionId` TEXT NOT NULL, `questionId` TEXT NOT NULL,
            `position` INTEGER NOT NULL, `label` TEXT NOT NULL, `description` TEXT NOT NULL,
            PRIMARY KEY(`trackerId`, `version`, `optionId`))""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `tracker_entries` (`id` TEXT NOT NULL, `trackerId` TEXT NOT NULL,
            `version` INTEGER NOT NULL, `date` INTEGER NOT NULL, `dayKey` TEXT, `filledAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_entries_trackerId` ON `tracker_entries` (`trackerId`)")
        db.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_tracker_entries_trackerId_dayKey`
            ON `tracker_entries` (`trackerId`, `dayKey`)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `tracker_answers` (`entryId` TEXT NOT NULL,
            `questionId` TEXT NOT NULL, `scale` INTEGER, `choices` TEXT NOT NULL, `text` TEXT NOT NULL,
            PRIMARY KEY(`entryId`, `questionId`))""")
    }
}
