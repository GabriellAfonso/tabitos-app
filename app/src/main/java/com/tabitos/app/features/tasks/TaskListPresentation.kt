package com.tabitos.app.features.tasks

import com.tabitos.app.core.ui.components.ContentFilter
import com.tabitos.app.core.tasks.domain.DeadlineKind
import com.tabitos.app.core.tasks.domain.RepeatKind
import com.tabitos.app.core.tasks.domain.TaskOccurrence
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import com.tabitos.app.core.tasks.domain.TaskStatus
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

/** "Concluídas" on every page shows only what was resolved today; older outcomes stay in the history. */
internal fun TaskSnapshot.completedOccurrences(page: TaskPage, today: LocalDate, listId: String?):
    List<TaskOccurrence> {
    val outcomeDates = history.associate { it.id to Instant.ofEpochMilli(it.recordedAt)
        .atZone(ZoneId.systemDefault()).toLocalDate() }
    fun resolvedDate(o: TaskOccurrence): LocalDate? = o.completedDate ?: outcomeDates[o.activeOutcome]
    return activeOccurrences.filter { o ->
        (o.status == TaskStatus.DONE || o.status == TaskStatus.NOT_DONE) && when (page) {
            TaskPage.LIST -> o.fields.listId == listId && resolvedDate(o) == today
            TaskPage.TODAY -> resolvedDate(o) == today
            TaskPage.REVIEW -> resolvedDate(o) == today && o.dueDate?.isBefore(today) == true
        }
    }.sortedWith(compareByDescending<TaskOccurrence> { resolvedDate(it) }.thenBy { it.position }.thenBy { it.id })
}

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

/** Count on the Tarefas tab of a list page: the same open tasks its list shows. */
internal fun TaskSnapshot.openListTaskCount(listId: String?): Int =
    listOccurrences(listId).count { it.status == TaskStatus.OPEN }

/** What a task page shows; computed outside Composables so the rules are testable. */
data class TaskSections(
    val pending: List<TaskOccurrence>,
    val weekly: List<TaskOccurrence>,
    val completed: List<TaskOccurrence>,
    val showsTasks: Boolean,
    val showsHabits: Boolean,
) {
    val isEmpty: Boolean get() = pending.isEmpty() && weekly.isEmpty() && completed.isEmpty()
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
    if (!showsTasks) return TaskSections(emptyList(), emptyList(), emptyList(), false, showsHabits)
    val visible = when (page) {
        TaskPage.LIST -> listOccurrences(listId)
        TaskPage.TODAY -> activeOccurrences.filter { it.inDay(today) }
        TaskPage.REVIEW -> activeOccurrences.filter { it.needsReview(today) }
    }.sortedWith(compareBy<TaskOccurrence> { it.position }.thenBy { it.id })
    val open = visible.filter { it.status == TaskStatus.OPEN }
    val (day, weekly) = if (page == TaskPage.TODAY) open.partition { it.fields.deadline == DeadlineKind.DAY }
        else open to emptyList()
    return TaskSections(day, weekly,
        completedOccurrences(page, today, listId), true, showsHabits)
}
