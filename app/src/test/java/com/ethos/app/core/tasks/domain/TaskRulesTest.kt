package com.ethos.app.core.tasks.domain

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TaskRulesTest {
    private val day = LocalDate.of(2026, 11, 6)
    private fun apply(s: TaskSnapshot, a: TaskAction, today: LocalDate = day) =
        TaskRules.apply(s, a, today, 123L)
    private fun create(fields: TaskFields = TaskFields(title = "Task")) =
        apply(TaskSnapshot(), TaskAction.Create("a", fields, RepeatRule(start = day)))

    @Test fun inboxCaptureMoveAndDeleteListPreserveHistory() {
        var s = create()
        assertNull(s.occurrences.single().fields.listId)
        s = apply(s, TaskAction.SaveList(TaskList("l", "Work")))
        s = apply(s, TaskAction.Move("a:1", "l"))
        s = apply(s, TaskAction.DeleteList("l"))
        assertTrue(s.activeOccurrences.isEmpty())
        assertTrue(s.history.any { it.kind == AuditKind.CREATED })
        assertFalse(s.history.any { it.kind == AuditKind.NOT_DONE })
    }

    @Test fun checklistNeverCompletesParentAndUndoIsAttributable() {
        var s = create(TaskFields(title = "Task", checklist = listOf("One", "Two")))
        s = apply(s, TaskAction.Check("a:1", 0, true))
        s = apply(s, TaskAction.Check("a:1", 1, true))
        assertEquals(TaskStatus.OPEN, s.occurrences.single().status)
        s = apply(s, TaskAction.Resolve("a:1", true, day))
        val outcome = s.occurrences.single().activeOutcome
        s = apply(s, TaskAction.Undo("a:1"))
        assertEquals(outcome, s.history.last().relatedOutcome)
        assertEquals(TaskStatus.OPEN, s.occurrences.single().status)
        assertThrows(TaskRuleException::class.java) { apply(s, TaskAction.Undo("a:1")) }
    }

    @Test fun actualCompletionCannotBeBeforeCreationOrFuture() {
        val s = create()
        listOf(day.minusDays(1), day.plusDays(1)).forEach {
            assertThrows(TaskRuleException::class.java) { apply(s, TaskAction.Resolve("a:1", true, it)) }
        }
        val resolved = apply(s, TaskAction.Resolve("a:1", true, day), day.plusDays(1))
        assertEquals(day, resolved.occurrences.single().completedDate)
        assertEquals(123L, resolved.history.last().recordedAt)
    }

    @Test fun manualImportanceAndDefaultSuggestionHaveDifferentPrecedence() {
        assertEquals(2, TaskRules.suggestDifficulty(TaskFields(), 2).importance)
        assertEquals(0, TaskRules.suggestDifficulty(TaskFields(importance = 0, importanceManual = true), 2).importance)
    }

    @Test fun weeklyAndTimeBoundariesNeverAutomaticallyFail() {
        val friday = create(TaskFields(title = "Week", deadline = DeadlineKind.WORK_WEEK, date = day))
        val o = friday.occurrences.single()
        assertTrue(o.inDay(day))
        assertFalse(o.needsReview(day))
        assertTrue(o.needsReview(day.plusDays(1)))
        assertFalse(o.inDay(day.plusDays(3)))
        val timed = create(TaskFields(title = "Day", deadline = DeadlineKind.DAY, date = day, minute = 1))
        assertFalse(timed.occurrences.single().needsReview(day))
        assertFalse(create().occurrences.single().inDay(day))
        assertFalse(create().occurrences.single().needsReview(day.plusYears(1)))
    }

    @Test fun reschedulingRequiresReviewAndRetainsOriginalDeadline() {
        val fields = TaskFields(title = "Task", deadline = DeadlineKind.DAY, date = day)
        val s = create(fields)
        val action = TaskAction.Edit("a:1", fields.copy(date = day.plusDays(2)), RepeatRule(start = day),
            ChangeScope.ONLY_THIS)
        assertThrows(TaskRuleException::class.java) { apply(s, action, day.plusDays(1)) }
        val result = apply(s, action.copy(reviewedDeadline = true), day.plusDays(1))
        assertEquals(TaskStatus.OPEN, result.occurrences.single().status)
        assertEquals(day, result.history.first { it.kind == AuditKind.MISSED_DEADLINE }.originalDeadline)
    }
}
