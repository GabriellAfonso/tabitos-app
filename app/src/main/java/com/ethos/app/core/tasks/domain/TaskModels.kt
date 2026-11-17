package com.ethos.app.core.tasks.domain

import java.time.LocalDate

enum class DeadlineKind { NONE, DAY, WEEK, WORK_WEEK }
enum class RepeatKind { NONE, DAILY, WEEKDAYS, WEEKLY, CUSTOM, MONTHLY, FIXED_INTERVAL, AFTER_COMPLETION }
enum class TaskStatus { OPEN, DONE, NOT_DONE, DELETED }
enum class ChangeScope { ONLY_THIS, THIS_AND_FUTURE }
/**
 * Persisted by name. POSTPONED and POSTPONE_UNDONE keep the deadline before the change in
 * [TaskAudit.originalDeadline] and the new date in [TaskAudit.actualDate], so the history can say
 * "postponed from X to Y" without confusing a postpone with an ordinary edit.
 */
enum class AuditKind {
    CREATED, COMPLETED, NOT_DONE, UNDONE, MISSED_DEADLINE, EDITED, DELETED, CHECKED, POSTPONED, POSTPONE_UNDONE;

    /** Changes the person made to this occurrence itself, which keep it from being silently regenerated. */
    val isIndependentChange: Boolean get() = when (this) {
        EDITED, MISSED_DEADLINE, POSTPONED, POSTPONE_UNDONE -> true
        CREATED, COMPLETED, NOT_DONE, UNDONE, DELETED, CHECKED -> false
    }
}
enum class TaskProblem { INVALID_INPUT, INVALID_DATE, REVIEW_REQUIRED, DEPENDENT_ACTIONS, NOT_OPEN }

class TaskRuleException(val problem: TaskProblem) : IllegalArgumentException(problem.name)

data class TaskList(
    val id: String, val name: String, val color: Int = 0, val icon: Int = 0,
    val position: Int = 0, val deleted: Boolean = false,
) : java.io.Serializable {
    companion object {
        /** Sizes of the color and icon choices shown in the list editor; saved indices must stay inside them. */
        const val COLOR_COUNT = 7
        const val ICON_COUNT = 6
    }
}

data class TaskFields(
    val title: String = "", val description: String = "", val listId: String? = null,
    val difficulty: Int = 1, val importance: Int = 1, val importanceManual: Boolean = false,
    val deadline: DeadlineKind = DeadlineKind.NONE, val date: LocalDate? = null, val minute: Int? = null,
    val checklist: List<String> = emptyList(),
) : java.io.Serializable

data class RepeatRule(
    val kind: RepeatKind = RepeatKind.NONE, val start: LocalDate = LocalDate.now(),
    val weekdays: Int = 1, val monthDay: Int = 1, val interval: Int = 1,
    val until: LocalDate? = null, val count: Int? = null,
) : java.io.Serializable

data class TaskDefinition(
    val id: String, val created: LocalDate, val fields: TaskFields, val rule: RepeatRule,
    val nextDate: LocalDate? = null, val nextOrdinal: Int = 1, val stopped: Boolean = false,
)

data class CheckItem(val label: String, val checked: Boolean = false)

data class TaskOccurrence(
    val id: String, val taskId: String, val ordinal: Int, val fields: TaskFields,
    val plannedDate: LocalDate?, val periodStart: LocalDate?, val dueDate: LocalDate?,
    val status: TaskStatus = TaskStatus.OPEN, val checks: List<CheckItem> = emptyList(),
    val completedDate: LocalDate? = null, val activeOutcome: String? = null,
    val position: Int = 0, val touched: Boolean = false,
) {
    fun needsReview(today: LocalDate): Boolean = status == TaskStatus.OPEN && dueDate?.isBefore(today) == true
    /** Only an open task due on a specific day can be postponed to another day (spec 021). */
    val canPostpone: Boolean get() = status == TaskStatus.OPEN && fields.deadline == DeadlineKind.DAY &&
        fields.date != null
    fun inDay(day: LocalDate): Boolean = status == TaskStatus.OPEN && when (fields.deadline) {
        DeadlineKind.NONE -> false
        DeadlineKind.DAY -> dueDate == day
        else -> periodStart != null && dueDate != null && day >= periodStart && day <= dueDate
    }
}

data class TaskAudit(
    val id: String, val occurrenceId: String, val kind: AuditKind, val recordedAt: Long,
    val actualDate: LocalDate? = null, val originalDeadline: LocalDate? = null,
    val relatedOutcome: String? = null,
)

data class TaskSnapshot(
    val lists: List<TaskList> = emptyList(), val definitions: List<TaskDefinition> = emptyList(),
    val occurrences: List<TaskOccurrence> = emptyList(), val history: List<TaskAudit> = emptyList(),
) {
    val activeLists get() = lists.filterNot { it.deleted }.sortedBy { it.position }
    val activeOccurrences get() = occurrences.filter { it.status != TaskStatus.DELETED }
}

sealed interface TaskAction {
    data class SaveList(val list: TaskList) : TaskAction
    data class DeleteList(val id: String) : TaskAction
    /** Full order of the active lists, as left by a drag. */
    data class OrderLists(val ids: List<String>) : TaskAction
    data class Create(val id: String, val fields: TaskFields, val rule: RepeatRule) : TaskAction
    data class Edit(
        val id: String, val fields: TaskFields, val rule: RepeatRule, val scope: ChangeScope,
        val reviewedDeadline: Boolean = false,
    ) : TaskAction
    /** Moves only this occurrence to [to] (today or later), keeping its time; counts as the review (spec 021). */
    data class Postpone(val id: String, val to: LocalDate) : TaskAction
    /** Puts a just-postponed occurrence back on [to], which may be in the past. */
    data class UndoPostpone(val id: String, val to: LocalDate) : TaskAction
    data class Check(val id: String, val index: Int, val checked: Boolean) : TaskAction
    data class Resolve(val id: String, val done: Boolean, val actualDate: LocalDate) : TaskAction
    data class Undo(val id: String) : TaskAction
    data class SetCompleted(val id: String, val done: Boolean, val actualDate: LocalDate) : TaskAction
    /**
     * With [eraseHistory] the deleted occurrences leave no trace: "Só esta" erases this occurrence, "Todas" the
     * whole series including past occurrences (spec 022). Coins already earned are never touched.
     */
    data class Delete(val id: String, val scope: ChangeScope, val eraseHistory: Boolean = false) : TaskAction
    /** Erases the history of a deleted occurrence: every deleted occurrence of its series (spec 022). */
    data class EraseHistory(val id: String) : TaskAction
    data class Move(val id: String, val listId: String?) : TaskAction
    data class Reorder(val id: String, val direction: Int) : TaskAction
    data class Order(val ids: List<String>) : TaskAction
    data object Refresh : TaskAction
    data class Materialize(val through: LocalDate) : TaskAction
}
