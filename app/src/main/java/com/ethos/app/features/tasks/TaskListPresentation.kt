package com.ethos.app.features.tasks

import com.ethos.app.core.ui.components.ContentFilter
import com.ethos.app.core.tasks.domain.DeadlineKind
import com.ethos.app.core.tasks.domain.RepeatKind
import com.ethos.app.core.tasks.domain.TaskOccurrence
import com.ethos.app.core.tasks.domain.TaskSnapshot
import com.ethos.app.core.tasks.domain.TaskStatus
import java.time.LocalDate

internal fun TaskSnapshot.completedOccurrences(page: TaskPage, today: LocalDate, listId: String?):
    List<TaskOccurrence> = activeOccurrences.filter { o ->
        o.status == TaskStatus.DONE && when (page) {
            TaskPage.LIST -> o.fields.listId == listId
            TaskPage.TODAY -> o.completedDate == today
            TaskPage.REVIEW -> o.completedDate == today && o.dueDate?.isBefore(today) == true
        }
    }.sortedWith(compareByDescending<TaskOccurrence> { it.completedDate }.thenBy { it.position }.thenBy { it.id })

internal fun TaskSnapshot.listOccurrences(listId: String?): List<TaskOccurrence> {
    val recurringIds = definitions.filter { it.rule.kind != RepeatKind.NONE }.map { it.id }.toSet()
    val currentIds = activeOccurrences.filter { it.taskId in recurringIds && it.status == TaskStatus.OPEN }
        .groupBy { it.taskId }
        .values.map { series -> series.minBy { it.ordinal }.id }.toSet()
    return activeOccurrences.filter {
        it.fields.listId == listId &&
            (it.taskId !in recurringIds || it.status != TaskStatus.OPEN || it.id in currentIds)
    }
}

/** What a task page shows; computed outside Composables so the rules are testable. */
data class TaskSections(
    val pending: List<TaskOccurrence>,
    val weekly: List<TaskOccurrence>,
    val notDone: List<TaskOccurrence>,
    val completed: List<TaskOccurrence>,
    val showsTasks: Boolean,
    val showsHabits: Boolean,
) {
    val isEmpty: Boolean get() = pending.isEmpty() && weekly.isEmpty() && notDone.isEmpty() && completed.isEmpty()
}

/**
 * [habitsTab] is the Tarefas/Hábitos switch of a list page; [filter] applies to Hoje. Revisão lists tasks
 * only (spec 014). On Hoje, work without a specific day goes to the weekly section.
 */
internal fun TaskSnapshot.sections(
    page: TaskPage, filter: ContentFilter, today: LocalDate, listId: String?, habitsTab: Boolean,
): TaskSections {
    val showsTasks = if (page == TaskPage.LIST) !habitsTab else filter.showsTasks
    val showsHabits = when (page) {
        TaskPage.LIST -> habitsTab
        TaskPage.TODAY -> filter.showsHabits
        TaskPage.REVIEW -> false
    }
    if (!showsTasks) return TaskSections(emptyList(), emptyList(), emptyList(), emptyList(), false, showsHabits)
    val visible = when (page) {
        TaskPage.LIST -> listOccurrences(listId)
        TaskPage.TODAY -> activeOccurrences.filter { it.inDay(today) }
        TaskPage.REVIEW -> activeOccurrences.filter { it.needsReview(today) }
    }.sortedWith(compareBy<TaskOccurrence> { it.position }.thenBy { it.id })
    val open = visible.filter { it.status == TaskStatus.OPEN }
    val (day, weekly) = if (page == TaskPage.TODAY) open.partition { it.fields.deadline == DeadlineKind.DAY }
        else open to emptyList()
    return TaskSections(day, weekly, visible.filter { it.status == TaskStatus.NOT_DONE },
        completedOccurrences(page, today, listId), true, showsHabits)
}
