package com.tabitos.app.features.calendar

import com.tabitos.app.core.habits.domain.Habit
import com.tabitos.app.core.habits.domain.HabitStatus
import com.tabitos.app.core.tasks.domain.TaskList
import com.tabitos.app.core.tasks.domain.TaskOccurrence
import com.tabitos.app.core.ui.format.TabitosDates
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/** A filter shown as a removable chip under the period row (spec 030). */
sealed interface ActiveFilter {
    /** Only some types are shown; [shown] keeps the sheet's order and may be empty. */
    data class Types(val shown: List<CalendarType>) : ActiveFilter
    /** [name] is null for the inbox and for a list that no longer exists ([inbox] tells them apart). */
    data class TaskListFilter(val name: String?, val inbox: Boolean) : ActiveFilter
    data class HabitFilter(val name: String) : ActiveFilter
}

/** What a task row says under its title; [listName] null with [inbox] false means a removed list. */
data class TaskMeta(val listName: String?, val inbox: Boolean, val time: String?, val overdue: Boolean)

/** Pure decisions for the calendar page, so the Composables only draw (spec 030). */
object CalendarPresentation {
    private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

    /** "Novembro de 2026", "16–22 nov" / "30 nov – 6 dez", "ter, 17 nov"; years only outside [today]'s. */
    fun periodLabel(view: CalendarView, anchor: LocalDate, today: LocalDate): String = when (view) {
        CalendarView.MONTH -> TabitosDates.monthTitle(YearMonth.from(anchor)).replaceFirstChar { it.titlecase(ptBr) }
        CalendarView.WEEK -> weekLabel(CalendarModel.weekStart(anchor), today)
        CalendarView.DAY -> TabitosDates.shortWithWeekday(anchor, today)
    }

    private fun weekLabel(start: LocalDate, today: LocalDate): String {
        val end = start.plusDays(6)
        return if (start.month == end.month) "${start.dayOfMonth}–${TabitosDates.shortPlain(end, today)}"
            else "${TabitosDates.shortPlain(start, today)} – ${TabitosDates.shortPlain(end, today)}"
    }

    /** "Ir para hoje" only appears when the visible period does not already contain today. */
    fun showsTodayButton(view: CalendarView, anchor: LocalDate, today: LocalDate): Boolean = when (view) {
        CalendarView.MONTH -> YearMonth.from(anchor) != YearMonth.from(today)
        CalendarView.WEEK -> CalendarModel.weekStart(anchor) != CalendarModel.weekStart(today)
        CalendarView.DAY -> anchor != today
    }

    fun isActive(filters: CalendarFilters): Boolean = activeFilters(filters, emptyList(), emptyList()).isNotEmpty()

    fun activeFilters(filters: CalendarFilters, lists: List<TaskList>, habits: List<Habit>): List<ActiveFilter> =
        buildList {
            if (filters.types != CalendarType.entries.toSet()) {
                add(ActiveFilter.Types(CalendarType.entries.filter { it in filters.types }))
            }
            if (filters.list != CalendarFilters.ALL_LISTS) {
                val inbox = filters.list == CalendarFilters.INBOX
                add(ActiveFilter.TaskListFilter(lists.firstOrNull { it.id == filters.list }?.name, inbox))
            }
            filters.habitId?.let { id ->
                add(ActiveFilter.HabitFilter(habits.firstOrNull { it.id == id }?.name.orEmpty()))
            }
        }

    /** Removing a chip resets only its own part of the filters. */
    fun without(filters: CalendarFilters, filter: ActiveFilter): CalendarFilters = when (filter) {
        is ActiveFilter.Types -> filters.copy(types = CalendarType.entries.toSet())
        is ActiveFilter.TaskListFilter -> filters.copy(list = CalendarFilters.ALL_LISTS)
        is ActiveFilter.HabitFilter -> filters.copy(habitId = null)
    }

    fun toggleType(filters: CalendarFilters, type: CalendarType): CalendarFilters =
        filters.copy(types = if (type in filters.types) filters.types - type else filters.types + type)

    /** Habits offered in the filter sheet: excluded ones only while they are the chosen filter. */
    fun sheetHabits(habits: List<Habit>, filters: CalendarFilters): List<Habit> =
        habits.filter { it.status != HabitStatus.EXCLUDED || it.id == filters.habitId }.sortedBy { it.name }

    fun taskMeta(task: TaskOccurrence, lists: List<TaskList>, today: LocalDate): TaskMeta {
        val listId = task.fields.listId
        return TaskMeta(
            listName = listId?.let { id -> lists.firstOrNull { it.id == id }?.name },
            inbox = listId == null,
            time = task.fields.minute?.let(TabitosDates::time),
            overdue = task.needsReview(today),
        )
    }
}

/** Separate marks for a grid cell, so tasks, habit records and tracker entries never add up into one number. */
val CalendarDay.hasTasks: Boolean get() = tasks.isNotEmpty()
val CalendarDay.hasHabitRecords: Boolean get() = habits.isNotEmpty()
val CalendarDay.hasTrackerEntry: Boolean get() = entries.isNotEmpty()

/** TalkBack says "sem dados" for a cell with nothing while trackers are shown, never a zero count. */
fun CalendarDay.announcesNoData(filters: CalendarFilters): Boolean = empty && CalendarType.TRACKERS in filters.types

/** Fillable trackers without an entry to edit; trackers with one are edited from their entry row. */
val CalendarDay.newFills: List<TrackerFill> get() = fills.filter { it.existing == null }
