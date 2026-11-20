package com.tabitos.app.features.tasks

import com.tabitos.app.core.reminders.domain.ReminderDefinition
import com.tabitos.app.core.reminders.domain.ReminderRules
import com.tabitos.app.core.tasks.domain.RepeatKind
import com.tabitos.app.core.tasks.domain.RepeatRule
import com.tabitos.app.core.tasks.domain.TaskList
import com.tabitos.app.core.tasks.domain.TaskOccurrence
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import com.tabitos.app.core.tasks.domain.TaskStatus
import java.time.LocalDate

/** Everything the task detail popup decides about one occurrence (spec 021), computed outside Composables. */
data class TaskDetailModel(
    val occurrence: TaskOccurrence,
    val isOpen: Boolean,
    val isOverdue: Boolean,
    val dates: RescheduleDates?,
    /** Repetition of the series, or null when the task does not repeat. */
    val repeat: RepeatRule?,
    val reminderCount: Int,
    /** Duration goal of a timed task, in minutes. */
    val timerMinutes: Int?,
    /** Destination list, or null for the inbox. */
    val list: TaskList?,
    /** Date of the following occurrence of the series, shown when rescheduling only this one. */
    val nextInSeries: LocalDate?,
    /** Earliest date accepted as "Concluí em outro dia". */
    val created: LocalDate,
)

data class RescheduleDates(val shortcuts: List<LocalDate>, val initial: LocalDate)

fun rescheduleDates(today: LocalDate, deadline: LocalDate): RescheduleDates {
    val tomorrow = today.plusDays(1)
    val shortcuts = listOf(today, tomorrow).filter { it != deadline }
    return RescheduleDates(shortcuts, initial = if (deadline == tomorrow) today else tomorrow)
}

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
        dates = o.fields.date?.takeIf { o.canReschedule }?.let { rescheduleDates(today, it) },
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
