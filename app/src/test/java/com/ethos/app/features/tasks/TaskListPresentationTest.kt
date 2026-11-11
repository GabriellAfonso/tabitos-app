package com.ethos.app.features.tasks

import com.ethos.app.core.tasks.domain.*
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskListPresentationTest {
    @Test fun completedSectionsRespectActualDayListAndReviewWithoutIncludingFailures() {
        val day = LocalDate.of(2026, 11, 10)
        val base = TaskOccurrence("done", "t", 1, TaskFields(title = "Done", listId = "a"),
            day.minusDays(2), day.minusDays(2), day.minusDays(2), TaskStatus.DONE, completedDate = day)
        val old = base.copy(id = "old", completedDate = day.minusDays(1))
        val other = base.copy(id = "other", fields = base.fields.copy(listId = "b"))
        val todayTask = base.copy(id = "today", dueDate = day)
        val failure = base.copy(id = "failure", status = TaskStatus.NOT_DONE, completedDate = null)
        val s = TaskSnapshot(occurrences = listOf(base, old, other, todayTask, failure))
        assertEquals(setOf("done", "old", "today"), s.completedOccurrences(TaskPage.LIST, day, "a")
            .map { it.id }.toSet())
        assertEquals(setOf("done", "other", "today"), s.completedOccurrences(TaskPage.TODAY, day, "a")
            .map { it.id }.toSet())
        assertEquals(setOf("done", "other"), s.completedOccurrences(TaskPage.REVIEW, day, null)
            .map { it.id }.toSet())
    }
    private val today = LocalDate.of(2026, 11, 10)
    private val fields = TaskFields(title = "Bible", listId = "routine", deadline = DeadlineKind.DAY)
    private val definition = TaskDefinition("bible", today, fields, RepeatRule(RepeatKind.DAILY, today))
    private fun occurrence(ordinal: Int, status: TaskStatus = TaskStatus.OPEN): TaskOccurrence {
        val day = today.plusDays(ordinal.toLong() - 1)
        return TaskOccurrence("bible:$ordinal", "bible", ordinal, fields.copy(date = day), day, day, day, status)
    }

    @Test fun currentOccurrenceAdvancesAfterResolutionAndUndoRestoresIt() {
        val first = occurrence(1)
        val next = occurrence(2)
        val snapshot = TaskSnapshot(definitions = listOf(definition), occurrences = listOf(next, first))
        assertEquals(listOf(first), snapshot.listOccurrences("routine"))
        for (status in listOf(TaskStatus.DONE, TaskStatus.NOT_DONE, TaskStatus.DELETED)) {
            val resolved = first.copy(status = status)
            val updated = snapshot.copy(occurrences = listOf(next, resolved))
            assertEquals(listOf(next), updated.listOccurrences("routine").filter { it.status == TaskStatus.OPEN })
            val undone = updated.copy(occurrences = listOf(next, resolved.copy(status = TaskStatus.OPEN)))
            assertEquals(listOf(first), undone.listOccurrences("routine"))
        }
        assertEquals(listOf(next, first), snapshot.activeOccurrences)
    }

    @Test fun overdueOccurrenceRemainsCurrentAndOtherTasksAndListsStayIndependent() {
        val overdue = occurrence(1).copy(plannedDate = today.minusDays(2), dueDate = today.minusDays(2))
        val single = occurrence(1).copy(id = "single", taskId = "single")
        val other = occurrence(1).copy(id = "other", taskId = "other", fields = fields.copy(listId = "other"))
        val snapshot = TaskSnapshot(definitions = listOf(definition),
            occurrences = listOf(occurrence(2), overdue, single, other))
        assertEquals(listOf(overdue, single), snapshot.listOccurrences("routine"))
        assertEquals(listOf(other), snapshot.listOccurrences("other"))
    }
}
