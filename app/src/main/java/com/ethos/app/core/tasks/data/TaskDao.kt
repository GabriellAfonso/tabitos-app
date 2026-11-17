package com.ethos.app.core.tasks.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface TaskDao {
    @Query("SELECT * FROM task_lists ORDER BY position, id") suspend fun lists(): List<ListEntity>
    @Query("SELECT * FROM task_definitions ORDER BY id") suspend fun definitions(): List<DefinitionEntity>
    @Query("SELECT * FROM task_occurrences ORDER BY position, id") suspend fun occurrences(): List<OccurrenceEntity>
    @Query("SELECT * FROM task_checklist ORDER BY position") suspend fun checks(): List<ChecklistEntity>
    @Query("SELECT * FROM task_audit ORDER BY recordedAt, rowid") suspend fun history(): List<AuditEntity>
    @Upsert suspend fun lists(rows: List<ListEntity>)
    @Upsert suspend fun definitions(rows: List<DefinitionEntity>)
    @Upsert suspend fun occurrences(rows: List<OccurrenceEntity>)
    @Upsert suspend fun checks(rows: List<ChecklistEntity>)
    @Upsert suspend fun history(rows: List<AuditEntity>)
    @Query("DELETE FROM task_checklist WHERE ownerId = :owner") suspend fun clearChecks(owner: String)
    @Query("DELETE FROM task_checklist WHERE ownerId IN (:owners)") suspend fun removeChecks(owners: List<String>)
    @Query("DELETE FROM task_audit WHERE occurrenceId IN (:ids)") suspend fun removeHistory(ids: List<String>)
    @Query("DELETE FROM task_occurrences WHERE id IN (:ids)") suspend fun removeOccurrences(ids: List<String>)
    @Query("DELETE FROM task_definitions WHERE id IN (:ids)") suspend fun removeDefinitions(ids: List<String>)
}
