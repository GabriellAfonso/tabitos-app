package com.ethos.app.features.tasks

import com.ethos.app.core.tasks.domain.*
import java.time.LocalDate
import java.time.ZoneId
import com.ethos.app.core.ui.components.ContentFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskListPresentationTest {
    @Test fun sectionsSplitHojeAndFollowFilterAndListTab() {
        val day = LocalDate.of(2026, 11, 10)
        val dayTask = TaskOccurrence("d", "d", 1, TaskFields(title = "Dia", deadline = DeadlineKind.DAY),
            day, day, day, position = 1)
        val weekTask = TaskOccurrence("w", "w", 1, TaskFields(title = "Semana", deadline = DeadlineKind.WEEK),
            day, day.minusDays(1), day.plusDays(5), position = 0)
        val s = TaskSnapshot(occurrences = listOf(dayTask, weekTask))
        val today = s.sections(TaskPage.TODAY, ContentFilter.ALL, day, null, habitsTab = false)
        assertEquals(listOf("d"), today.pending.map { it.id })
        assertEquals(listOf("w"), today.weekly.map { it.id })
        assertTrue(today.showsHabits)
        val habitsOnly = s.sections(TaskPage.TODAY, ContentFilter.HABITS, day, null, habitsTab = false)
        assertTrue(habitsOnly.isEmpty)
        assertFalse(habitsOnly.showsTasks)
        assertFalse(s.sections(TaskPage.TODAY, ContentFilter.TASKS, day, null, false).showsHabits)
        val list = s.sections(TaskPage.LIST, ContentFilter.ALL, day, null, habitsTab = false)
        assertEquals(listOf("w", "d"), list.pending.map { it.id })
        assertTrue(list.weekly.isEmpty())
        assertTrue(s.sections(TaskPage.LIST, ContentFilter.ALL, day, null, habitsTab = true).isEmpty)
        assertFalse(s.sections(TaskPage.REVIEW, ContentFilter.ALL, day, null, false).showsHabits)
    }

    @Test fun completedSectionsIncludeNotDoneAndRespectResolutionDayListAndReview() {
        val day = LocalDate.of(2026, 11, 10)
        val base = TaskOccurrence("done", "t", 1, TaskFields(title = "Done", listId = "a"),
            day.minusDays(2), day.minusDays(2), day.minusDays(2), TaskStatus.DONE, completedDate = day)
        val old = base.copy(id = "old", completedDate = day.minusDays(1))
        val other = base.copy(id = "other", fields = base.fields.copy(listId = "b"))
        val todayTask = base.copy(id = "today", dueDate = day)
        val failure = base.copy(id = "failure", status = TaskStatus.NOT_DONE, completedDate = null,
            activeOutcome = "failure-outcome")
        val oldFailure = failure.copy(id = "old-failure", activeOutcome = "old-outcome")
        val recordedAt = day.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val s = TaskSnapshot(occurrences = listOf(base, old, other, todayTask, failure, oldFailure),
            history = listOf(
                TaskAudit("failure-outcome", failure.id, AuditKind.NOT_DONE, recordedAt),
                TaskAudit("old-outcome", oldFailure.id, AuditKind.NOT_DONE,
                    day.minusDays(1).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()),
            ))
        assertEquals(setOf("done", "old", "today", "failure", "old-failure"),
            s.completedOccurrences(TaskPage.LIST, day, "a")
            .map { it.id }.toSet())
        assertEquals(setOf("done", "other", "today", "failure"), s.completedOccurrences(TaskPage.TODAY, day, "a")
            .map { it.id }.toSet())
        assertEquals(setOf("done", "other", "failure"), s.completedOccurrences(TaskPage.REVIEW, day, null)
            .map { it.id }.toSet())
        val sections = s.sections(TaskPage.TODAY, ContentFilter.TASKS, day, "a", false)
        assertTrue(sections.pending.isEmpty())
        assertEquals(setOf("done", "other", "today", "failure"), sections.completed.map { it.id }.toSet())
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
