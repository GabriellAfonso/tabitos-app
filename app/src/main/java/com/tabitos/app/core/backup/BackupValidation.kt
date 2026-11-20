package com.tabitos.app.core.backup

import androidx.sqlite.db.SupportSQLiteDatabase

/** Upper bound for any single text value; real routine fields are far smaller. */
private const val MAX_TEXT_LENGTH = 64 * 1024

/** Columns stored as Room booleans. Any other value would break the domain mappers after import. */
private val booleanColumns = mapOf(
    "task_lists" to setOf("deleted"),
    "task_definitions" to setOf("stopped", "field_importanceManual"),
    "task_occurrences" to setOf("touched", "field_importanceManual"),
    "task_checklist" to setOf("checked"),
    "habits" to setOf("importanceManual"),
    "habit_records" to setOf("deleted"),
    "habit_reviews" to setOf("invalidated"),
    "accounting_entitlements" to setOf("active", "exempt", "priced", "reward"),
    "reminder_definitions" to setOf("enabled"),
    "reminder_deliveries" to setOf("snoozed"),
    "timer_sessions" to setOf("reached"),
    "timer_segments" to setOf("manual", "active"),
    "trackers" to setOf("multiple", "active"),
    "tracker_questions" to setOf("required", "isPrimary", "multiple"),
)

/** Child column → parent table and key. A dangling reference means the file was edited or damaged. */
private data class Relation(val table: String, val column: String, val parent: String, val key: String)

private val relations = listOf(
    Relation("task_definitions", "field_listId", "task_lists", "id"),
    Relation("task_occurrences", "taskId", "task_definitions", "id"),
    Relation("task_occurrences", "field_listId", "task_lists", "id"),
    Relation("task_audit", "occurrenceId", "task_occurrences", "id"),
    Relation("habits", "listId", "task_lists", "id"),
    Relation("habit_rules", "habitId", "habits", "id"),
    Relation("habit_intervals", "habitId", "habits", "id"),
    Relation("habit_records", "habitId", "habits", "id"),
    Relation("habit_audits", "habitId", "habits", "id"),
    Relation("timer_segments", "sessionId", "timer_sessions", "id"),
    Relation("timer_audits", "sessionId", "timer_sessions", "id"),
    Relation("trackers", "habitId", "habits", "id"),
    Relation("tracker_versions", "trackerId", "trackers", "id"),
    Relation("tracker_entries", "trackerId", "trackers", "id"),
    Relation("tracker_answers", "entryId", "tracker_entries", "id"),
)

internal fun validateBackupNumber(table: String, column: String, value: Long) {
    if (booleanColumns[table]?.contains(column) == true) require(value == 0L || value == 1L)
}

internal fun validateBackupText(table: String, column: String, value: String) {
    require(value.length <= MAX_TEXT_LENGTH) { "Text too long in $table.$column" }
}

internal fun validateBackupRelations(db: SupportSQLiteDatabase) {
    relations.forEach { (table, column, parent, key) ->
        val dangling = db.hasRow(
            """SELECT 1 FROM $table c WHERE c."$column" IS NOT NULL
               AND NOT EXISTS (SELECT 1 FROM $parent p WHERE p."$key" = c."$column") LIMIT 1""",
        )
        require(!dangling) { "Dangling $table.$column" }
    }
}
