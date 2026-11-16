package com.ethos.app.features.tasks

import com.ethos.app.core.reminders.domain.ReminderDefinition
import com.ethos.app.core.reminders.domain.ReminderRules
import com.ethos.app.core.tasks.domain.RepeatKind
import com.ethos.app.core.tasks.domain.RepeatRule
import com.ethos.app.core.tasks.domain.TaskList
import com.ethos.app.core.tasks.domain.TaskOccurrence
import com.ethos.app.core.tasks.domain.TaskSnapshot
import com.ethos.app.core.tasks.domain.TaskStatus
import java.time.LocalDate

/** Everything the task detail popup decides about one occurrence (spec 021), computed outside Composables. */
data class TaskDetailModel(
    val occurrence: TaskOccurrence,
    val isOpen: Boolean,
    val isOverdue: Boolean,
    val canPostpone: Boolean,
    /** Repetition of the series, or null when the task does not repeat. */
    val repeat: RepeatRule?,
    val reminderCount: Int,
    /** Duration goal of a timed task, in minutes. */
    val timerMinutes: Int?,
    /** Destination list, or null for the inbox. */
    val list: TaskList?,
    /** Date of the following occurrence of the series, shown when postponing only this one. */
    val nextInSeries: LocalDate?,
    /** Earliest date accepted as "Concluí em outro dia". */
    val created: LocalDate,
)

/** Moment the user postponed a task, kept so "Desfazer" can put it back on [from]. */
data class PostponeFeedback(val occurrenceId: String, val from: LocalDate, val to: LocalDate)

fun taskDetailModel(
    snapshot: TaskSnapshot, o: TaskOccurrence, today: LocalDate, reminders: List<ReminderDefinition>,
    durations: Map<String, Int>,
): TaskDetailModel {
    val definition = snapshot.definitions.firstOrNull { it.id == o.taskId }
    val repeat = definition?.rule?.takeIf { it.kind != RepeatKind.NONE }
    return TaskDetailModel(
        occurrence = o,
        isOpen = o.status == TaskStatus.OPEN,
        isOverdue = o.needsReview(today),
        canPostpone = o.canPostpone,
        repeat = repeat,
        reminderCount = ReminderRules.effective(reminders, o).size,
        timerMinutes = durations[o.taskId],
        list = snapshot.activeLists.firstOrNull { it.id == o.fields.listId },
        nextInSeries = if (repeat == null) null else nextInSeries(snapshot, o),
        created = definition?.created ?: today,
    )
}

/** Next not-deleted occurrence after [o] in its series, or the series' next planned date. */
fun nextInSeries(snapshot: TaskSnapshot, o: TaskOccurrence): LocalDate? =
    snapshot.occurrences.filter { it.taskId == o.taskId && it.ordinal > o.ordinal && it.status != TaskStatus.DELETED }
        .minByOrNull { it.ordinal }?.dueDate
        ?: snapshot.definitions.firstOrNull { it.id == o.taskId }?.takeIf { !it.stopped }?.nextDate
