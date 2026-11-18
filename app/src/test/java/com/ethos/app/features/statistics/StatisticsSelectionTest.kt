package com.ethos.app.features.statistics

import com.ethos.app.core.habits.domain.Habit
import com.ethos.app.core.habits.domain.HabitAction
import com.ethos.app.core.habits.domain.HabitRecord
import com.ethos.app.core.habits.domain.HabitRule
import com.ethos.app.core.habits.domain.HabitRules
import com.ethos.app.core.habits.domain.HabitSnapshot
import com.ethos.app.core.habits.domain.HabitStatus
import com.ethos.app.core.habits.domain.HabitType
import com.ethos.app.core.tasks.domain.AuditKind
import com.ethos.app.core.tasks.domain.RepeatRule
import com.ethos.app.core.tasks.domain.TaskAudit
import com.ethos.app.core.tasks.domain.TaskDefinition
import com.ethos.app.core.tasks.domain.TaskFields
import com.ethos.app.core.tasks.domain.TaskOccurrence
import com.ethos.app.core.tasks.domain.TaskSnapshot
import com.ethos.app.core.tasks.domain.TaskStatus
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec 024: type filters with "Maus hábitos" and one task series or habit chosen by id. */
class StatisticsSelectionTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 18)
    private val week = StatisticsPeriod.current(PeriodKind.WEEK, today).range(today, today)
    private val moment = today.atTime(9, 0).toInstant(zone).toEpochMilli()

    private val habits: HabitSnapshot = listOf(
        Triple("read", "Ler", HabitType.PRACTICE), Triple("smoke", "Não fumar", HabitType.AVOID),
        Triple("old", "Correr", HabitType.PRACTICE),
    ).fold(HabitSnapshot()) { s, (id, name, type) ->
        HabitRules.apply(s, HabitAction.Save(Habit(id, today.minusDays(5), name),
            HabitRule("r:$id", id, today.minusDays(5), type, target = if (type == HabitType.PRACTICE) 1 else 0)),
            today.minusDays(5), 1, emptySet())
    }.let { s ->
        val excluded = HabitRules.apply(s, HabitAction.Status("old", HabitStatus.EXCLUDED), today, 2, emptySet())
        excluded.copy(records = listOf(HabitRecord("p", "read", today, moment),
            HabitRecord("e", "smoke", today, moment)))
    }

    /** Two series with the same title: only the id tells them apart. */
    private val tasks = TaskSnapshot(
        definitions = listOf(definition("water", "Regar as plantas"), definition("water2", "Regar as plantas"),
            definition("gone", "Ligar para o dentista")),
        occurrences = listOf(occurrence("w:1", "water"), occurrence("w:2", "water"), occurrence("x:1", "water2"),
            occurrence("g:1", "gone", TaskStatus.DELETED)),
        history = listOf(completed("1", "w:1"), completed("2", "w:2"), completed("3", "x:1")),
    )

    private fun definition(id: String, title: String) =
        TaskDefinition(id, today.minusDays(10), TaskFields(title = title), RepeatRule(start = today.minusDays(10)))

    private fun occurrence(id: String, series: String, status: TaskStatus = TaskStatus.DONE) =
        TaskOccurrence(id, series, 1, TaskFields(title = "Regar as plantas"), today, today, today, status,
            completedDate = today)

    private fun completed(id: String, occurrence: String) =
        TaskAudit(id, occurrence, AuditKind.COMPLETED, moment, today, null)

    private fun history(filter: HistoryFilter) =
        PreparedHistory.of(HistorySources(tasks, habits, emptyList(), emptyList(), emptyList()), today, zone,
            gameMode = true).items(week, filter)

    @Test fun badHabitsHaveTheirOwnFilter() {
        assertEquals(setOf(HistoryKind.HABIT_DONE), history(HistoryFilter.HABITS).map { it.kind }.toSet())
        assertTrue(history(HistoryFilter.BAD_HABITS).all { it.kind.isBadHabit })
        assertEquals(listOf("Não fumar"), history(HistoryFilter.BAD_HABITS).map { it.title }.distinct())
        assertEquals(HistoryFilter.BAD_HABITS, filterFor(StatisticsItem.Habit("smoke"), habits, today))
        assertEquals(HistoryFilter.HABITS, filterFor(StatisticsItem.Habit("read"), habits, today))
    }

    @Test fun focusedItemKeepsItsNameAndMarksDeletedOnes() {
        assertEquals(FocusedItem(StatisticsItem.TaskSeries("gone"), "Ligar para o dentista", deleted = true),
            focusedItem(StatisticsItem.TaskSeries("gone"), tasks, habits))
        assertEquals(FocusedItem(StatisticsItem.Habit("old"), "Correr", deleted = true),
            focusedItem(StatisticsItem.Habit("old"), tasks, habits))
        // Erased history leaves nothing to focus: the page falls back to the type.
        assertNull(focusedItem(StatisticsItem.Habit("erased"), tasks, habits))
    }

    @Test fun aSeriesNeverMixesWithAnotherOfTheSameName() {
        val water = StatisticsItem.TaskSeries("water")
        val scoped = scopedTasks(tasks, water)
        assertEquals(setOf("w:1", "w:2"), scoped.occurrences.map { it.id }.toSet())
        assertEquals(2, taskSummary(scoped, week, BucketUnit.DAY, today, zone).completed)
        assertEquals(2, history(HistoryFilter.ALL).count { it.isAbout(water) })
    }

    @Test fun summaryListsOnlyTheChosenTypeOrHabit() {
        val bad = habitSummary(habits, week, today, habitsInScope(habits, HistoryFilter.BAD_HABITS, null, today))
        assertEquals(listOf("smoke"), bad.items.map { it.id })
        val good = habitSummary(habits, week, today, habitsInScope(habits, HistoryFilter.HABITS, null, today))
        assertEquals(listOf("read"), good.items.map { it.id })
        // A chosen excluded habit is still shown, so its own history can be read.
        val old = habitSummary(habits, week, today,
            habitsInScope(habits, HistoryFilter.HABITS, StatisticsItem.Habit("old"), today))
        assertEquals(listOf("old"), old.items.map { it.id })
    }

    @Test fun cardsFollowTheTypeAndCoinsNeedGameMode() {
        assertEquals(SummaryCards(tasks = true, habits = false, time = true, coins = false),
            summaryCards(HistoryFilter.TASKS, gameMode = true))
        assertFalse(summaryCards(HistoryFilter.BAD_HABITS, gameMode = true).time)
        assertFalse(summaryCards(HistoryFilter.ALL, gameMode = false).coins)
        assertTrue(summaryCards(HistoryFilter.ALL, gameMode = true).coins)
    }
}
