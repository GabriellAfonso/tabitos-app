package com.ethos.app.core.tasks.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ethos.app.core.habits.data.*

@Database(entities = [ListEntity::class, DefinitionEntity::class, OccurrenceEntity::class,
    ChecklistEntity::class, AuditEntity::class, HabitEntity::class, HabitRuleEntity::class,
    HabitIntervalEntity::class, HabitOverrideEntity::class, HabitRecordEntity::class,
    HabitReviewEntity::class, HabitAuditEntity::class], version = 2, exportSchema = true)
@TypeConverters(TaskConverters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao
    abstract fun habits(): HabitDao
}
