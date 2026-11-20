package com.tabitos.app.features.habits

import com.tabitos.app.core.habits.domain.Habit
import com.tabitos.app.core.habits.domain.HabitRecord
import com.tabitos.app.core.habits.domain.HabitRule
import com.tabitos.app.core.habits.domain.HabitRules
import com.tabitos.app.core.habits.domain.HabitStatus
import com.tabitos.app.core.habits.domain.HabitStreak
import com.tabitos.app.core.habits.domain.HabitType
import com.tabitos.app.core.habits.domain.HabitUnit
import com.tabitos.app.core.habits.domain.PeriodOutcome
import java.time.LocalDate

/** What a day of the week strip shows (spec 023); marks are always a symbol or number, never color alone. */
enum class HabitDayMark { DONE, MISSED, CLEAN, COUNTED, OVER_LIMIT, OPEN }

data class HabitWeekDay(val date: LocalDate, val mark: HabitDayMark, val count: Int, val editable: Boolean)

/** Main area of the popup, chosen from the habit's state today. */
enum class HabitDetailMain { MARK, DONE_TODAY, BREAKING, PAUSED, EXCLUDED, UNAVAILABLE }

/** Count against the goal of the current period, shown only for weekly/monthly practice and limit habits. */
data class HabitDetailProgress(val count: Int, val target: Int, val unit: HabitUnit, val limit: Boolean)

data class HabitDetailModel(
    val habit: Habit,
    /** Rule in force today, or the latest one for habits that are not active today. */
    val rule: HabitRule,
    val streak: HabitStreak?,
    val rate: Int?,
    val week: List<HabitWeekDay>,
    val main: HabitDetailMain,
    val todayCount: Int,
    val progress: HabitDetailProgress?,
    val timerMinutes: Int?,
) {
    val breaking: Boolean get() = rule.type != HabitType.PRACTICE
}

/** Days shown in the strip: today and the six before it. */
private const val WEEK_DAYS = 7L

fun habitDetailModel(state: HabitsUiState, habitId: String): HabitDetailModel? {
    val s = state.snapshot
    val today = state.today
    val habit = s.habits.firstOrNull { it.id == habitId } ?: return null
    val rule = HabitRules.currentRule(s, habitId, today) ?: s.rules.filter { it.habitId == habitId }
        .maxByOrNull { it.effective } ?: return null
    val records = s.records.filter { it.habitId == habitId && !it.deleted }
    val standings = state.dayStandings[habitId].orEmpty()
    val week = (WEEK_DAYS - 1 downTo 0).map { back ->
        val date = today.minusDays(back)
        val count = records.count { it.date == date }
        HabitWeekDay(date, dayMark(HabitRules.currentRule(s, habitId, date) ?: rule, count, standings[date]), count,
            canRecordOn(state, habit, date))
    }
    val todayCount = records.count { it.date == today }
    val period = state.periods.lastOrNull { it.habitId == habitId && today in it.start..it.end }
    val progress = period?.takeIf { it.rule.type == HabitType.LIMIT || it.rule.unit != HabitUnit.DAY }?.let {
        HabitDetailProgress(it.count, it.target, it.rule.unit, limit = it.rule.type != HabitType.PRACTICE)
    }
    return HabitDetailModel(habit, rule, state.streaks[habitId], state.rates[habitId], week,
        mainArea(habit, rule, todayCount, week.last().editable), todayCount, progress, state.durations[habitId])
}

private fun dayMark(rule: HabitRule, count: Int, standing: PeriodOutcome?): HabitDayMark = when {
    rule.type == HabitType.PRACTICE -> when {
        count > 0 -> HabitDayMark.DONE
        standing == PeriodOutcome.FAILURE -> HabitDayMark.MISSED
        else -> HabitDayMark.OPEN
    }
    standing == PeriodOutcome.FAILURE -> HabitDayMark.OVER_LIMIT
    count > 0 -> HabitDayMark.COUNTED
    standing == PeriodOutcome.SUCCESS -> HabitDayMark.CLEAN
    else -> HabitDayMark.OPEN
}

/** Same conditions the rules check before accepting a record ([HabitRules.apply]), so a tap never fails on dates. */
internal fun canRecordOn(state: HabitsUiState, habit: Habit, date: LocalDate): Boolean {
    val s = state.snapshot
    val rule = HabitRules.currentRule(s, habit.id, date) ?: return false
    return habit.status == HabitStatus.ACTIVE && date >= habit.created && date <= state.today &&
        HabitRules.active(s, habit.id, date) && HabitRules.scheduled(rule, date) &&
        !HabitRules.closed(HabitRules.end(date, rule.unit), state.today)
}

private fun mainArea(habit: Habit, rule: HabitRule, todayCount: Int, todayOpen: Boolean): HabitDetailMain = when {
    habit.status == HabitStatus.EXCLUDED -> HabitDetailMain.EXCLUDED
    habit.status == HabitStatus.PAUSED -> HabitDetailMain.PAUSED
    !todayOpen -> HabitDetailMain.UNAVAILABLE
    rule.type != HabitType.PRACTICE -> HabitDetailMain.BREAKING
    todayCount > 0 -> HabitDetailMain.DONE_TODAY
    else -> HabitDetailMain.MARK
}

/** Latest record of [date], the one "−" and "Desfazer 1" remove. */
fun latestRecord(records: List<HabitRecord>, habitId: String, date: LocalDate): HabitRecord? =
    records.filter { it.habitId == habitId && it.date == date && !it.deleted }
        .maxWithOrNull(compareBy<HabitRecord> { it.entered }.thenBy { it.id })
