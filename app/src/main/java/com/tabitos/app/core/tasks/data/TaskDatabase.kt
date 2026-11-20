package com.tabitos.app.core.tasks.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tabitos.app.core.habits.data.*
import com.tabitos.app.core.accounting.data.*
import com.tabitos.app.core.rewards.data.*
import com.tabitos.app.core.reminders.data.*
import com.tabitos.app.core.timers.data.*
import com.tabitos.app.core.trackers.data.*

@Database(entities = [ListEntity::class, DefinitionEntity::class, OccurrenceEntity::class,
    ChecklistEntity::class, AuditEntity::class, HabitEntity::class, HabitRuleEntity::class,
    HabitIntervalEntity::class, HabitOverrideEntity::class, HabitRecordEntity::class,
    HabitReviewEntity::class, HabitAuditEntity::class, ActivationEntity::class, PriceEntity::class,
    EntitlementEntity::class, LedgerEntity::class, RewardEntity::class,
    PurchaseIntentEntity::class, PurchaseEntity::class, ReminderEntity::class, DeliveryEntity::class,
    ReminderSettingsEntity::class, TimerGoalEntity::class, TimerSessionEntity::class, TimerSegmentEntity::class,
    TimerAuditEntity::class, TrackerEntity::class, TrackerVersionEntity::class, TrackerQuestionEntity::class,
    TrackerOptionEntity::class, TrackerEntryEntity::class, TrackerAnswerEntity::class], version = 7, exportSchema = true)
@TypeConverters(TaskConverters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao
    abstract fun habits(): HabitDao
    abstract fun accounting(): AccountingDao
    abstract fun rewards(): RewardDao
    abstract fun reminders(): ReminderDao
    abstract fun timers(): TimerDao
    abstract fun trackers(): TrackerDao
}
