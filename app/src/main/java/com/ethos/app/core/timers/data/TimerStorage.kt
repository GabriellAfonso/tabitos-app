package com.ethos.app.core.timers.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ethos.app.core.timers.domain.*

@Entity(tableName = "timer_goals")
data class TimerGoalEntity(@PrimaryKey val sourceKey: String, val minutes: Int, val updated: Long)

@Entity(tableName = "timer_sessions")
data class TimerSessionEntity(
    @PrimaryKey val id: String, val kind: String, val sourceId: String, val title: String, val state: String,
    val period: String, val goalKey: String?, val limitMillis: Long?, val created: Long, val reached: Boolean,
) {
    fun domain() = TimerSession(id, TimerKind.valueOf(kind), sourceId, title, TimerState.valueOf(state),
        TimerPeriod.valueOf(period), goalKey, limitMillis, created, reached)
}
fun TimerSession.entity() = TimerSessionEntity(id, kind.name, sourceId, title, state.name, period.name, goalKey,
    limitMillis, created, reached)

@Entity(tableName = "timer_segments", indices = [Index("sessionId")])
data class TimerSegmentEntity(
    @PrimaryKey val id: String, val sessionId: String, val startWall: Long, val endWall: Long?,
    val startElapsed: Long?, val bootCount: Int?, val checkpointElapsed: Long?, val limitMillis: Long?,
    val manual: Boolean, val active: Boolean, val created: Long,
) {
    fun domain() = TimerSegment(id, sessionId, startWall, endWall, startElapsed, bootCount, checkpointElapsed,
        limitMillis, manual, active, created)
}
fun TimerSegment.entity() = TimerSegmentEntity(id, sessionId, startWall, endWall, startElapsed, bootCount,
    checkpointElapsed, limitMillis, manual, active, created)

@Entity(tableName = "timer_audits", indices = [Index("sessionId")])
data class TimerAuditEntity(
    @PrimaryKey val id: String, val sessionId: String, val kind: String, val at: Long, val segmentId: String?,
    val beforeStart: Long?, val beforeEnd: Long?, val afterStart: Long?, val afterEnd: Long?,
) {
    fun domain() = TimerAudit(id, sessionId, TimerAuditKind.valueOf(kind), at, segmentId, beforeStart, beforeEnd,
        afterStart, afterEnd)
}
fun TimerAudit.entity() = TimerAuditEntity(id, sessionId, kind.name, at, segmentId, beforeStart, beforeEnd,
    afterStart, afterEnd)

@Dao
interface TimerDao {
    @Query("SELECT * FROM timer_goals") suspend fun goals(): List<TimerGoalEntity>
    @Upsert suspend fun goal(row: TimerGoalEntity)
    @Query("DELETE FROM timer_goals WHERE sourceKey = :key") suspend fun removeGoal(key: String)
    @Query("SELECT * FROM timer_sessions ORDER BY created") suspend fun sessions(): List<TimerSessionEntity>
    @Query("SELECT * FROM timer_sessions WHERE id = :id") suspend fun session(id: String): TimerSessionEntity?
    @Upsert suspend fun session(row: TimerSessionEntity)
    @Query("SELECT * FROM timer_segments ORDER BY startWall") suspend fun segments(): List<TimerSegmentEntity>
    @Upsert suspend fun segment(row: TimerSegmentEntity)
    @Query("SELECT * FROM timer_audits ORDER BY at, rowid") suspend fun audits(): List<TimerAuditEntity>
    @Insert suspend fun audit(row: TimerAuditEntity)
}

val TIMER_MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf("rewards", "reward_intents", "reward_purchases").forEach {
            db.execSQL("ALTER TABLE `$it` ADD COLUMN `reward_minutes` INTEGER")
        }
        db.execSQL("""CREATE TABLE IF NOT EXISTS `timer_goals` (`sourceKey` TEXT NOT NULL, `minutes` INTEGER NOT NULL,
            `updated` INTEGER NOT NULL, PRIMARY KEY(`sourceKey`))""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `timer_sessions` (`id` TEXT NOT NULL, `kind` TEXT NOT NULL,
            `sourceId` TEXT NOT NULL, `title` TEXT NOT NULL, `state` TEXT NOT NULL, `period` TEXT NOT NULL,
            `goalKey` TEXT, `limitMillis` INTEGER, `created` INTEGER NOT NULL, `reached` INTEGER NOT NULL,
            PRIMARY KEY(`id`))""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `timer_segments` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL,
            `startWall` INTEGER NOT NULL, `endWall` INTEGER, `startElapsed` INTEGER, `bootCount` INTEGER,
            `checkpointElapsed` INTEGER, `limitMillis` INTEGER, `manual` INTEGER NOT NULL, `active` INTEGER NOT NULL,
            `created` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_timer_segments_sessionId` ON `timer_segments` (`sessionId`)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `timer_audits` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL,
            `kind` TEXT NOT NULL, `at` INTEGER NOT NULL, `segmentId` TEXT, `beforeStart` INTEGER, `beforeEnd` INTEGER,
            `afterStart` INTEGER, `afterEnd` INTEGER, PRIMARY KEY(`id`))""")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_timer_audits_sessionId` ON `timer_audits` (`sessionId`)")
    }
}
