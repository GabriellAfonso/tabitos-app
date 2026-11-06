package com.ethos.app.core.tasks.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.ethos.app.core.tasks.domain.*
import java.time.LocalDate

class TaskConverters {
    @TypeConverter fun fromDate(date: LocalDate?): Long? = date?.toEpochDay()
    @TypeConverter fun toDate(day: Long?): LocalDate? = day?.let(LocalDate::ofEpochDay)
}

data class StoredFields(
    val title: String, val description: String, val listId: String?, val difficulty: Int,
    val importance: Int, val importanceManual: Boolean, val deadline: DeadlineKind,
    val date: LocalDate?, val minute: Int?,
) {
    fun domain(labels: List<String>) = TaskFields(title, description, listId, difficulty, importance,
        importanceManual, deadline, date, minute, labels)
    companion object {
        fun from(f: TaskFields) = StoredFields(f.title, f.description, f.listId, f.difficulty, f.importance,
            f.importanceManual, f.deadline, f.date, f.minute)
    }
}

@Entity(tableName = "task_lists")
data class ListEntity(
    @PrimaryKey val id: String, val name: String, val color: Int, val icon: Int,
    val position: Int, val deleted: Boolean,
)

@Entity(tableName = "task_definitions")
data class DefinitionEntity(
    @PrimaryKey val id: String, val created: LocalDate,
    @Embedded(prefix = "field_") val fields: StoredFields,
    val repeatKind: RepeatKind, val start: LocalDate, val weekdays: Int, val monthDay: Int,
    val interval: Int, val until: LocalDate?, val count: Int?, val nextDate: LocalDate?,
    val nextOrdinal: Int, val stopped: Boolean,
)

@Entity(tableName = "task_occurrences", indices = [Index(value = ["taskId", "ordinal"], unique = true)])
data class OccurrenceEntity(
    @PrimaryKey val id: String, val taskId: String, val ordinal: Int,
    @Embedded(prefix = "field_") val fields: StoredFields,
    val plannedDate: LocalDate?, val periodStart: LocalDate?, val dueDate: LocalDate?, val status: TaskStatus,
    val completedDate: LocalDate?, val activeOutcome: String?, val position: Int, val touched: Boolean,
)

@Entity(tableName = "task_checklist", indices = [Index("ownerId")])
data class ChecklistEntity(
    @PrimaryKey val id: String, val ownerId: String, val position: Int, val label: String, val checked: Boolean,
)

@Entity(tableName = "task_audit", indices = [Index("occurrenceId")])
data class AuditEntity(
    @PrimaryKey val id: String, val occurrenceId: String, val kind: AuditKind, val recordedAt: Long,
    val actualDate: LocalDate?, val originalDeadline: LocalDate?, val relatedOutcome: String?,
)

fun ListEntity.domain() = TaskList(id, name, color, icon, position, deleted)
fun TaskList.entity() = ListEntity(id, name, color, icon, position, deleted)
fun TaskDefinition.entity() = DefinitionEntity(id, created, StoredFields.from(fields), rule.kind,
    rule.start, rule.weekdays, rule.monthDay, rule.interval, rule.until, rule.count, nextDate, nextOrdinal, stopped)
fun DefinitionEntity.domain(checks: List<ChecklistEntity>) = TaskDefinition(id, created,
    fields.domain(checks.map { it.label }),
    RepeatRule(repeatKind, start, weekdays, monthDay, interval, until, count), nextDate, nextOrdinal, stopped)
fun TaskOccurrence.entity() = OccurrenceEntity(id, taskId, ordinal, StoredFields.from(fields), plannedDate,
    periodStart, dueDate, status, completedDate, activeOutcome, position, touched)
fun OccurrenceEntity.domain(checks: List<ChecklistEntity>) = TaskOccurrence(id, taskId, ordinal,
    fields.domain(checks.map { it.label }), plannedDate, periodStart, dueDate, status,
    checks.map { CheckItem(it.label, it.checked) }, completedDate, activeOutcome, position, touched)
fun TaskAudit.entity() = AuditEntity(id, occurrenceId, kind, recordedAt, actualDate, originalDeadline, relatedOutcome)
fun AuditEntity.domain() = TaskAudit(id, occurrenceId, kind, recordedAt, actualDate, originalDeadline, relatedOutcome)
