package com.ethos.app.features.calendar

import com.ethos.app.core.habits.domain.Habit
import com.ethos.app.core.habits.domain.HabitRules
import com.ethos.app.core.habits.domain.HabitType
import com.ethos.app.core.habits.domain.HabitSnapshot
import com.ethos.app.core.tasks.domain.TaskOccurrence
import com.ethos.app.core.tasks.domain.TaskSnapshot
import com.ethos.app.core.trackers.domain.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class CalendarView { MONTH, WEEK, DAY }
enum class CalendarType { TASKS, HABITS, TRACKERS }

/** [list]: [ALL_LISTS], [INBOX] or a list id. [habitId] narrows habits/trackers and hides tasks. */
data class CalendarFilters(
    val types: Set<CalendarType> = CalendarType.entries.toSet(), val list: String = ALL_LISTS,
    val habitId: String? = null,
) : java.io.Serializable {
    companion object { const val ALL_LISTS = "*"; const val INBOX = "" }
}

/** [breaking]: the habit's rule that day avoids or limits it, so records are slips, not practice. */
data class HabitDayRecords(val habit: Habit, val count: Int, val breaking: Boolean = false)
data class TrackerDayEntry(val tracker: Tracker, val habit: Habit, val entry: TrackerEntry)
data class TrackerFill(val tracker: Tracker, val habit: Habit, val existing: TrackerEntry?)

data class CalendarDay(
    val date: LocalDate, val tasks: List<TaskOccurrence> = emptyList(), val habits: List<HabitDayRecords> = emptyList(),
    val entries: List<TrackerDayEntry> = emptyList(), val fills: List<TrackerFill> = emptyList(),
    /** Some tracker matches the filters, so a day without entries says "Sem dados". */
    val tracked: Boolean = false,
) {
    val habitRecordCount: Int get() = habits.sumOf { it.count }
    val empty: Boolean get() = tasks.isEmpty() && habits.isEmpty() && entries.isEmpty()
}

data class CalendarCell(val day: CalendarDay, val inPeriod: Boolean)

data class CalendarSources(
    val tasks: TaskSnapshot = TaskSnapshot(), val habits: HabitSnapshot = HabitSnapshot(),
    val trackers: TrackerSnapshot = TrackerSnapshot(),
)

object CalendarModel {
    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Visible dates: full Monday–Sunday weeks covering the month, one week, or one day. */
    fun range(view: CalendarView, anchor: LocalDate): List<LocalDate> = when (view) {
        CalendarView.DAY -> listOf(anchor)
        CalendarView.WEEK -> (0L..6L).map { weekStart(anchor).plusDays(it) }
        CalendarView.MONTH -> {
            val first = weekStart(anchor.withDayOfMonth(1))
            val last = weekStart(anchor.withDayOfMonth(anchor.lengthOfMonth())).plusDays(6)
            generateSequence(first) { it.plusDays(1) }.takeWhile { it <= last }.toList()
        }
    }

    fun shift(view: CalendarView, anchor: LocalDate, delta: Long): LocalDate = when (view) {
        CalendarView.MONTH -> anchor.plusMonths(delta)
        CalendarView.WEEK -> anchor.plusWeeks(delta)
        CalendarView.DAY -> anchor.plusDays(delta)
    }

    private fun listMatches(filter: String, listId: String?) =
        filter == CalendarFilters.ALL_LISTS || (filter == CalendarFilters.INBOX && listId == null) || filter == listId

    private fun isBreaking(habits: HabitSnapshot, habitId: String, date: LocalDate) =
        HabitRules.currentRule(habits, habitId, date)?.type.let { it != null && it != HabitType.PRACTICE }

    fun day(s: CalendarSources, f: CalendarFilters, date: LocalDate, today: LocalDate): CalendarDay {
        val habits = s.habits.habits
            .filter { listMatches(f.list, it.listId) && (f.habitId == null || it.id == f.habitId) }
        val habitIds = habits.associateBy { it.id }
        // Same rule as the former typed-date lookup, so no task visible there disappears here.
        val tasks = if (CalendarType.TASKS !in f.types || f.habitId != null) emptyList()
            else s.tasks.activeOccurrences.filter { it.inDay(date) && listMatches(f.list, it.fields.listId) }
                .sortedWith(compareBy<TaskOccurrence> { it.position }.thenBy { it.id })
        val records = if (CalendarType.HABITS !in f.types) emptyList() else s.habits.records
            .filter { !it.deleted && it.date == date && it.habitId in habitIds }.groupBy { it.habitId }
            .map { (id, rows) -> HabitDayRecords(habitIds.getValue(id), rows.size, isBreaking(s.habits, id, date)) }
            .sortedBy { it.habit.name }
        val trackers = if (CalendarType.TRACKERS !in f.types) emptyList()
            else s.trackers.trackers.filter { it.habitId in habitIds }
        val entries = trackers.flatMap { t -> s.trackers.entries.filter { it.trackerId == t.id && it.date == date }
            .sortedBy { it.filledAt }.map { TrackerDayEntry(t, habitIds.getValue(t.habitId), it) } }
        val fills = trackers.filter { TrackerRules.fillable(it, s.habits, date, today) }.map { t ->
            TrackerFill(t, habitIds.getValue(t.habitId),
                if (t.multiple) null else TrackerRules.entryFor(s.trackers, t.id, date))
        }
        return CalendarDay(date, tasks, records, entries, fills, tracked = trackers.isNotEmpty())
    }

    fun cells(
        s: CalendarSources, f: CalendarFilters, view: CalendarView, anchor: LocalDate, today: LocalDate,
    ): List<CalendarCell> = range(view, anchor).map {
        CalendarCell(day(s, f, it, today), view != CalendarView.MONTH || it.month == anchor.month)
    }
}
