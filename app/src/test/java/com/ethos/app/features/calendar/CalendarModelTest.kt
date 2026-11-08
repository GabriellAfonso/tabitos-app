package com.ethos.app.features.calendar

import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.trackers.domain.*
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CalendarModelTest {
    private val today = LocalDate.of(2026, 11, 7)
    private fun task(id: String, date: LocalDate, list: String? = null, deadline: DeadlineKind = DeadlineKind.DAY,
                     status: TaskStatus = TaskStatus.OPEN) = TaskOccurrence("$id:1", id, 1,
        TaskFields(title = id, listId = list, deadline = deadline, date = date), date,
        if (deadline == DeadlineKind.WEEK) date.minusDays(3) else date, date, status)
    private val sources = CalendarSources(
        TaskSnapshot(occurrences = listOf(task("inbox", today), task("work", today, "l1"),
            task("week", today.plusDays(2), deadline = DeadlineKind.WEEK), task("done", today, status = TaskStatus.DONE),
            task("undated", today, deadline = DeadlineKind.NONE))),
        HabitSnapshot(habits = listOf(Habit("h1", today.minusDays(10), "Ler", listId = "l1"),
            Habit("h2", today.minusDays(10), "Caminhar")),
            rules = listOf(HabitRule("r1", "h1", today.minusDays(10)), HabitRule("r2", "h2", today.minusDays(10))),
            intervals = listOf(ActiveInterval("i1", "h1", today.minusDays(10)),
                ActiveInterval("i2", "h2", today.minusDays(10))),
            records = listOf(HabitRecord("a", "h1", today, 1), HabitRecord("b", "h1", today, 2),
                HabitRecord("c", "h2", today, 3), HabitRecord("d", "h2", today, 4, deleted = true))),
        TrackerSnapshot(listOf(Tracker("t1", "h1", today.minusDays(2))),
            entries = listOf(TrackerEntry("e", "t1", 1, today.minusDays(1), 5, 5))))

    @Test fun monthAndWeekRangesStartOnMonday() {
        val month = CalendarModel.range(CalendarView.MONTH, today)
        assertEquals(DayOfWeek.MONDAY, month.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, month.last().dayOfWeek)
        assertTrue(month.containsAll((1..30).map { today.withDayOfMonth(it) }))
        assertEquals(0, month.size % 7)
        val week = CalendarModel.range(CalendarView.WEEK, today)
        assertEquals(LocalDate.of(2026, 11, 2), week.first())
        assertEquals(7, week.size)
        assertEquals(listOf(today), CalendarModel.range(CalendarView.DAY, today))
        assertEquals(today.plusMonths(1), CalendarModel.shift(CalendarView.MONTH, today, 1))
        assertEquals(today.minusWeeks(1), CalendarModel.shift(CalendarView.WEEK, today, -1))
    }

    @Test fun tasksMatchTheFormerTypedDateLookup() {
        (-5L..5L).map { today.plusDays(it) }.forEach { date ->
            val expected = sources.tasks.activeOccurrences.filter { it.inDay(date) }.map { it.id }.toSet()
            assertEquals(expected, CalendarModel.day(sources, CalendarFilters(), date, today).tasks.map { it.id }.toSet())
        }
    }

    @Test fun filtersCombineTypeListAndHabit() {
        val all = CalendarModel.day(sources, CalendarFilters(), today, today)
        assertEquals(setOf("inbox:1", "work:1", "week:1"), all.tasks.map { it.id }.toSet())
        assertEquals(listOf("Caminhar" to 1, "Ler" to 2), all.habits.map { it.habit.name to it.count })
        val inbox = CalendarModel.day(sources, CalendarFilters(list = CalendarFilters.INBOX), today, today)
        assertEquals(setOf("inbox:1", "week:1"), inbox.tasks.map { it.id }.toSet())
        assertEquals(listOf("h2"), inbox.habits.map { it.habit.id })
        val list = CalendarModel.day(sources, CalendarFilters(list = "l1"), today, today)
        assertEquals(listOf("work:1"), list.tasks.map { it.id })
        assertEquals(listOf("h1"), list.habits.map { it.habit.id })
        val habit = CalendarModel.day(sources, CalendarFilters(habitId = "h2"), today, today)
        assertTrue(habit.tasks.isEmpty())
        assertEquals(listOf("h2"), habit.habits.map { it.habit.id })
        assertTrue(habit.fills.isEmpty())
        val onlyTasks = CalendarModel.day(sources, CalendarFilters(types = setOf(CalendarType.TASKS)), today, today)
        assertTrue(onlyTasks.habits.isEmpty() && onlyTasks.entries.isEmpty() && onlyTasks.fills.isEmpty())
    }

    @Test fun trackerEntriesFillsAndMissingDays() {
        val yesterday = CalendarModel.day(sources, CalendarFilters(), today.minusDays(1), today)
        assertEquals(listOf("e"), yesterday.entries.map { it.entry.id })
        assertEquals("e", yesterday.fills.single().existing?.id)
        val day = CalendarModel.day(sources, CalendarFilters(), today, today)
        assertTrue(day.entries.isEmpty())
        assertNull(day.fills.single().existing)
        assertTrue(CalendarModel.day(sources, CalendarFilters(), today.plusDays(1), today).fills.isEmpty())
        assertTrue(CalendarModel.day(sources, CalendarFilters(), today.minusDays(3), today).fills.isEmpty())
        val cells = CalendarModel.cells(sources, CalendarFilters(), CalendarView.MONTH, today, today)
        assertEquals(1, cells.count { it.day.entries.isNotEmpty() })
        assertFalse(cells.first().inPeriod)
    }
}
