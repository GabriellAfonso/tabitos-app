package com.ethos.app.features.calendar

import com.ethos.app.core.habits.domain.ActiveInterval
import com.ethos.app.core.habits.domain.Habit
import com.ethos.app.core.habits.domain.HabitRecord
import com.ethos.app.core.habits.domain.HabitRule
import com.ethos.app.core.habits.domain.HabitSnapshot
import com.ethos.app.core.habits.domain.HabitStatus
import com.ethos.app.core.habits.domain.HabitType
import com.ethos.app.core.tasks.domain.DeadlineKind
import com.ethos.app.core.tasks.domain.TaskFields
import com.ethos.app.core.tasks.domain.TaskList
import com.ethos.app.core.tasks.domain.TaskOccurrence
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarPresentationTest {
    private val today = LocalDate.of(2026, 11, 17)
    private val work = TaskList("l1", "Trabalho")
    private val walk = Habit("h1", today.minusDays(10), "Caminhar")
    private val soda = Habit("h2", today.minusDays(10), "Refrigerante")
    private val old = Habit("h3", today.minusDays(10), "Antigo", status = HabitStatus.EXCLUDED)

    @Test fun periodLabelsNameMonthWeekAndDay() {
        assertEquals("Novembro de 2026", CalendarPresentation.periodLabel(CalendarView.MONTH, today, today))
        assertEquals("16–22 nov", CalendarPresentation.periodLabel(CalendarView.WEEK, today, today))
        assertEquals("30 nov – 6 dez",
            CalendarPresentation.periodLabel(CalendarView.WEEK, LocalDate.of(2026, 12, 2), today))
        assertEquals("28 dez – 3 jan 2027",
            CalendarPresentation.periodLabel(CalendarView.WEEK, LocalDate.of(2026, 12, 30), today))
        assertEquals("ter, 17 nov", CalendarPresentation.periodLabel(CalendarView.DAY, today, today))
    }

    @Test fun todayButtonOnlyOutsideThePeriodWithToday() {
        assertFalse(CalendarPresentation.showsTodayButton(CalendarView.MONTH, today.withDayOfMonth(1), today))
        assertTrue(CalendarPresentation.showsTodayButton(CalendarView.MONTH, today.plusMonths(1), today))
        assertFalse(CalendarPresentation.showsTodayButton(CalendarView.WEEK, LocalDate.of(2026, 11, 22), today))
        assertTrue(CalendarPresentation.showsTodayButton(CalendarView.WEEK, LocalDate.of(2026, 11, 23), today))
        assertFalse(CalendarPresentation.showsTodayButton(CalendarView.DAY, today, today))
        assertTrue(CalendarPresentation.showsTodayButton(CalendarView.DAY, today.plusDays(1), today))
    }

    @Test fun activeFiltersBecomeChipsAndRemovingOneResetsOnlyIt() {
        assertTrue(CalendarPresentation.activeFilters(CalendarFilters(), listOf(work), listOf(walk)).isEmpty())
        assertFalse(CalendarPresentation.isActive(CalendarFilters()))
        val filters = CalendarFilters(types = setOf(CalendarType.TRACKERS, CalendarType.TASKS), list = "l1",
            habitId = "h1")
        val chips = CalendarPresentation.activeFilters(filters, listOf(work), listOf(walk))
        assertEquals(listOf(
            ActiveFilter.Types(listOf(CalendarType.TASKS, CalendarType.TRACKERS)),
            ActiveFilter.TaskListFilter("Trabalho", inbox = false),
            ActiveFilter.HabitFilter("Caminhar"),
        ), chips)
        val withoutList = CalendarPresentation.without(filters, chips[1])
        assertEquals(CalendarFilters.ALL_LISTS, withoutList.list)
        assertEquals(filters.types, withoutList.types)
        assertEquals("h1", withoutList.habitId)
        assertEquals(CalendarType.entries.toSet(), CalendarPresentation.without(filters, chips[0]).types)
        assertNull(CalendarPresentation.without(filters, chips[2]).habitId)
    }

    @Test fun inboxAndRemovedListsAreTold() {
        assertEquals(listOf(ActiveFilter.TaskListFilter(null, inbox = true)), CalendarPresentation.activeFilters(
            CalendarFilters(list = CalendarFilters.INBOX), listOf(work), emptyList()))
        assertEquals(listOf(ActiveFilter.TaskListFilter(null, inbox = false)), CalendarPresentation.activeFilters(
            CalendarFilters(list = "gone"), listOf(work), emptyList()))
    }

    @Test fun toggleTypeAddsAndRemoves() {
        val noTasks = CalendarPresentation.toggleType(CalendarFilters(), CalendarType.TASKS)
        assertEquals(setOf(CalendarType.HABITS, CalendarType.TRACKERS), noTasks.types)
        assertEquals(CalendarType.entries.toSet(), CalendarPresentation.toggleType(noTasks, CalendarType.TASKS).types)
    }

    @Test fun sheetListsExcludedHabitsOnlyWhileChosen() {
        val habits = listOf(walk, old, soda)
        assertEquals(listOf("Caminhar", "Refrigerante"),
            CalendarPresentation.sheetHabits(habits, CalendarFilters()).map { it.name })
        assertEquals(listOf("Antigo", "Caminhar", "Refrigerante"),
            CalendarPresentation.sheetHabits(habits, CalendarFilters(habitId = "h3")).map { it.name })
    }

    @Test fun taskMetaNamesListTimeAndOverdue() {
        fun task(list: String?, minute: Int?, due: LocalDate) = TaskOccurrence("o", "t", 1,
            TaskFields(title = "x", listId = list, deadline = DeadlineKind.DAY, date = due, minute = minute),
            due, due, due)
        assertEquals(TaskMeta("Trabalho", inbox = false, time = "09:05", overdue = false),
            CalendarPresentation.taskMeta(task("l1", 9 * 60 + 5, today), listOf(work), today))
        assertEquals(TaskMeta(null, inbox = true, time = null, overdue = true),
            CalendarPresentation.taskMeta(task(null, null, today.minusDays(3)), listOf(work), today))
    }

    @Test fun avoidHabitRecordsAreSlipsAndDayMarksStaySeparate() {
        val habits = HabitSnapshot(habits = listOf(walk, soda),
            rules = listOf(HabitRule("r1", "h1", today.minusDays(10)),
                HabitRule("r2", "h2", today.minusDays(10), type = HabitType.AVOID)),
            intervals = listOf(ActiveInterval("i1", "h1", today.minusDays(10)),
                ActiveInterval("i2", "h2", today.minusDays(10))),
            records = listOf(HabitRecord("a", "h1", today, 1), HabitRecord("b", "h2", today, 2)))
        val day = CalendarModel.day(CalendarSources(habits = habits), CalendarFilters(), today, today)
        assertEquals(listOf("Caminhar" to false, "Refrigerante" to true),
            day.habits.map { it.habit.name to it.breaking })
        assertTrue(day.hasHabitRecords)
        assertFalse(day.hasTasks)
        assertFalse(day.hasTrackerEntry)
        assertFalse(day.tracked)
        assertFalse(day.announcesNoData(CalendarFilters()))
    }
}
