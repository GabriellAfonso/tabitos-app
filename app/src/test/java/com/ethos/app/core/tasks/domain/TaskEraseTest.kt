package com.ethos.app.core.tasks.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Erasing the history of deleted tasks (spec 022). */
class TaskEraseTest {
    private val today = LocalDate.of(2026, 11, 17)
    private val dayTask = TaskFields(title = "Pagar conta", deadline = DeadlineKind.DAY, date = today)
    private fun apply(s: TaskSnapshot, a: TaskAction, on: LocalDate = today) = TaskRules.apply(s, a, on, 1L)
    private fun single() = apply(TaskSnapshot(), TaskAction.Create("a", dayTask, RepeatRule(start = today)))
    private fun daily(start: LocalDate = today.minusDays(2)) = apply(TaskSnapshot(), TaskAction.Create("d",
        dayTask.copy(date = start), RepeatRule(RepeatKind.DAILY, start = start)), on = start).let {
        apply(it, TaskAction.Refresh)
    }

    @Test fun deleteWithoutEraseKeepsEverything() {
        val done = apply(single(), TaskAction.Resolve("a:1", true, today))
        val s = apply(done, TaskAction.Delete("a:1", ChangeScope.ONLY_THIS))
        assertEquals(TaskStatus.DELETED, s.occurrences.single().status)
        assertTrue(s.history.any { it.kind == AuditKind.COMPLETED })
    }

    @Test fun eraseOfASingleTaskRemovesItsOccurrenceRecordsAndDefinition() {
        val done = apply(single(), TaskAction.Resolve("a:1", true, today))
        val s = apply(done, TaskAction.Delete("a:1", ChangeScope.ONLY_THIS, eraseHistory = true))
        assertTrue(s.occurrences.isEmpty())
        assertTrue(s.history.isEmpty())
        assertTrue(s.definitions.isEmpty())
    }

    @Test fun onlyThisErasesOneOccurrenceAndTheSeriesGoesOnWithoutRegeneratingIt() {
        val s = daily()
        val erased = apply(s, TaskAction.Delete("d:2", ChangeScope.ONLY_THIS, eraseHistory = true))
        assertFalse(erased.occurrences.any { it.id == "d:2" })
        assertFalse(erased.history.any { it.occurrenceId == "d:2" })
        assertTrue(erased.occurrences.any { it.id == "d:1" })
        assertEquals(1, erased.definitions.size)
        val later = apply(erased, TaskAction.Refresh, on = today.plusDays(3))
        assertFalse(later.occurrences.any { it.id == "d:2" })
        assertTrue(later.occurrences.any { it.ordinal > 3 })
    }

    @Test fun allErasesEveryOccurrenceIncludingPastCompletedOnes() {
        val s = apply(daily(), TaskAction.Resolve("d:1", true, today.minusDays(2)))
        val erased = apply(s, TaskAction.Delete("d:3", ChangeScope.THIS_AND_FUTURE, eraseHistory = true))
        assertTrue(erased.occurrences.none { it.taskId == "d" })
        assertTrue(erased.history.isEmpty())
        assertTrue(erased.definitions.isEmpty())
    }

    @Test fun eraseFromHistoryTakesTheDeletedOccurrencesOfTheSeriesOnly() {
        val s = apply(apply(daily(), TaskAction.Delete("d:1", ChangeScope.ONLY_THIS)),
            TaskAction.Delete("d:2", ChangeScope.ONLY_THIS))
        val erased = apply(s, TaskAction.EraseHistory("d:1"))
        assertFalse(erased.occurrences.any { it.id == "d:1" || it.id == "d:2" })
        assertTrue(erased.occurrences.any { it.id == "d:3" && it.status == TaskStatus.OPEN })
        assertEquals(1, erased.definitions.size)
    }

    @Test fun eraseFromHistoryRequiresADeletedOccurrence() {
        val problem = assertThrows(TaskRuleException::class.java) {
            apply(single(), TaskAction.EraseHistory("a:1"))
        }.problem
        assertEquals(TaskProblem.INVALID_INPUT, problem)
    }
}
