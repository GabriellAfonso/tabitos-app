package com.ethos.app.core.tasks.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ListEntity::class, DefinitionEntity::class, OccurrenceEntity::class,
    ChecklistEntity::class, AuditEntity::class], version = 1, exportSchema = true)
@TypeConverters(TaskConverters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao
}
