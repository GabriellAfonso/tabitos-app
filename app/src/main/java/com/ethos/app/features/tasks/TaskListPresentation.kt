package com.ethos.app.features.tasks

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
