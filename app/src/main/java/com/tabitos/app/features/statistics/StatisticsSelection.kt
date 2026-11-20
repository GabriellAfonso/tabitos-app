package com.tabitos.app.features.statistics

import com.tabitos.app.core.habits.domain.Habit
import com.tabitos.app.core.habits.domain.HabitRules
import com.tabitos.app.core.habits.domain.HabitSnapshot
import com.tabitos.app.core.habits.domain.HabitStatus
import com.tabitos.app.core.habits.domain.HabitType
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import com.tabitos.app.core.tasks.domain.TaskStatus
import com.tabitos.app.core.timers.domain.TimerKind
import java.time.LocalDate

/** One task series or habit the page can focus on (spec 024); always by id, never by name. */
sealed interface StatisticsItem {
    data class Habit(val habitId: String) : StatisticsItem
    /** Every occurrence of a task shares its series id (`TaskOccurrence.taskId`). */
    data class TaskSeries(val taskId: String) : StatisticsItem
}

/** Name of the focused item for its removable chip; [deleted] marks an excluded habit or deleted series. */
data class FocusedItem(val item: StatisticsItem, val title: String, val deleted: Boolean)

/** Avoid/limit habits are "Maus hábitos"; a habit not active today keeps the type of its latest rule. */
fun isBadHabit(habits: HabitSnapshot, habitId: String, today: LocalDate): Boolean {
    val rule = HabitRules.currentRule(habits, habitId, today)
        ?: habits.rules.filter { it.habitId == habitId }.maxByOrNull { it.effective }
    return rule != null && rule.type != HabitType.PRACTICE
}

/** A chosen item always implies its own type, so the chips never contradict it. */
fun filterFor(item: StatisticsItem, habits: HabitSnapshot, today: LocalDate): HistoryFilter = when (item) {
    is StatisticsItem.TaskSeries -> HistoryFilter.TASKS
    is StatisticsItem.Habit -> if (isBadHabit(habits, item.habitId, today)) HistoryFilter.BAD_HABITS
        else HistoryFilter.HABITS
}

/**
 * The focused item with its current name; null once its history was erased, so the page falls back to the type.
 * Items are only focused from their own popup or a history entry, never from a list (spec 024).
 */
fun focusedItem(item: StatisticsItem, tasks: TaskSnapshot, habits: HabitSnapshot): FocusedItem? = when (item) {
    is StatisticsItem.TaskSeries -> tasks.definitions.firstOrNull { it.id == item.taskId }?.let { definition ->
        val occurrences = tasks.occurrences.filter { it.taskId == definition.id }
        if (occurrences.isEmpty()) null
        else FocusedItem(item, definition.fields.title, deleted = occurrences.all { it.status == TaskStatus.DELETED })
    }
    is StatisticsItem.Habit -> habits.habits.firstOrNull { it.id == item.habitId }?.let {
        FocusedItem(item, it.name, deleted = it.status == HabitStatus.EXCLUDED)
    }
}

/** Tasks the Resumo counts: one series when chosen, otherwise all of them. */
fun scopedTasks(tasks: TaskSnapshot, item: StatisticsItem?): TaskSnapshot {
    if (item !is StatisticsItem.TaskSeries) return tasks
    val occurrences = tasks.occurrences.filter { it.taskId == item.taskId }
    val ids = occurrences.map { it.id }.toSet()
    return tasks.copy(occurrences = occurrences, history = tasks.history.filter { it.occurrenceId in ids })
}

/** Habits the Resumo lists for the type filter and item; a chosen habit is shown even when excluded. */
fun habitsInScope(
    habits: HabitSnapshot, filter: HistoryFilter, item: StatisticsItem?, today: LocalDate,
): (Habit) -> Boolean = { habit ->
    when {
        item is StatisticsItem.Habit -> habit.id == item.habitId
        habit.status == HabitStatus.EXCLUDED -> false
        filter == HistoryFilter.HABITS -> !isBadHabit(habits, habit.id, today)
        filter == HistoryFilter.BAD_HABITS -> isBadHabit(habits, habit.id, today)
        else -> true
    }
}

/** Timer time of the chosen type or item; Tudo and Tempo keep everything. */
fun scopedTime(
    credited: List<CreditedTime>, filter: HistoryFilter, item: StatisticsItem?, tasks: TaskSnapshot,
    habits: HabitSnapshot, today: LocalDate,
): List<CreditedTime> {
    val seriesOf = tasks.occurrences.associate { it.id to it.taskId }
    return credited.filter { part ->
        val session = part.session
        when (item) {
            is StatisticsItem.TaskSeries -> session.kind == TimerKind.TASK && seriesOf[session.sourceId] == item.taskId
            is StatisticsItem.Habit -> session.kind == TimerKind.HABIT && session.sourceId == item.habitId
            null -> when (filter) {
                HistoryFilter.TASKS -> session.kind == TimerKind.TASK
                HistoryFilter.HABITS -> session.kind == TimerKind.HABIT && !isBadHabit(habits, session.sourceId, today)
                HistoryFilter.BAD_HABITS ->
                    session.kind == TimerKind.HABIT && isBadHabit(habits, session.sourceId, today)
                else -> true
            }
        }
    }
}

/** Whether a history entry is about [item]; erased or reward entries never are. */
fun HistoryItem.isAbout(item: StatisticsItem): Boolean = when (val s = subject) {
    is HistorySubject.Task -> item is StatisticsItem.TaskSeries && s.seriesId == item.taskId
    is HistorySubject.Habit -> item is StatisticsItem.Habit && s.habitId == item.habitId
    else -> false
}

/** Cards of the Resumo for a type filter (spec 024); Tudo shows every card. */
data class SummaryCards(val tasks: Boolean, val habits: Boolean, val time: Boolean, val coins: Boolean)

fun summaryCards(filter: HistoryFilter, gameMode: Boolean): SummaryCards = when (filter) {
    HistoryFilter.ALL -> SummaryCards(tasks = true, habits = true, time = true, coins = gameMode)
    HistoryFilter.TASKS -> SummaryCards(tasks = true, habits = false, time = true, coins = false)
    HistoryFilter.HABITS -> SummaryCards(tasks = false, habits = true, time = true, coins = false)
    // Only practice habits can be timed (spec 016), so bad habits have no time card.
    HistoryFilter.BAD_HABITS -> SummaryCards(tasks = false, habits = true, time = false, coins = false)
    HistoryFilter.TIME -> SummaryCards(tasks = false, habits = false, time = true, coins = false)
    HistoryFilter.COINS -> SummaryCards(tasks = false, habits = false, time = false, coins = gameMode)
}
