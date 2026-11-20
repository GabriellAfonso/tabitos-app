package com.tabitos.app.features.habits

import com.tabitos.app.core.habits.domain.HabitRule
import com.tabitos.app.core.habits.domain.HabitType
import com.tabitos.app.core.habits.domain.HabitUnit

/** Choice sheet opened from a chip or row of the habit editor sheet (spec 029). */
enum class HabitEditorSheet {
    FREQUENCY, DAYS, TIMES, MINUTES, BREAKING_TYPE, LIMIT, LOOK, LIST, DESCRIPTION, IMPORTANCE, DIFFICULTY,
}

/** Practice frequency in plain words, mapped onto the existing day/weekday/week/month rules (spec 014). */
enum class PracticeFrequency { DAILY, CHOSEN_DAYS, WEEKLY, MONTHLY }

/** Weekday bit masks, Monday = bit 0. */
const val ALL_WEEKDAYS = 0b1111111
const val MONDAY_TO_FRIDAY = 0b0011111

private const val DEFAULT_WEEKLY_TARGET = "3"
private const val DEFAULT_MONTHLY_TARGET = "10"
private const val DEFAULT_TIMED_MINUTES = "20"

val HabitDraft.isPractice: Boolean get() = rule.type == HabitType.PRACTICE

fun practiceFrequencyOf(rule: HabitRule): PracticeFrequency = when (rule.unit) {
    HabitUnit.DAY -> if (rule.weekdays == ALL_WEEKDAYS) PracticeFrequency.DAILY else PracticeFrequency.CHOSEN_DAYS
    HabitUnit.WEEK -> PracticeFrequency.WEEKLY
    HabitUnit.MONTH -> PracticeFrequency.MONTHLY
}

/** Each frequency starts from its usual goal; choosing the current one keeps what the user already set. */
fun withPracticeFrequency(draft: HabitDraft, frequency: PracticeFrequency): HabitDraft {
    if (practiceFrequencyOf(draft.rule) == frequency) return draft
    val rule = draft.rule
    return when (frequency) {
        PracticeFrequency.DAILY -> draft.copy(rule = rule.copy(unit = HabitUnit.DAY, weekdays = ALL_WEEKDAYS),
            target = "1")
        PracticeFrequency.CHOSEN_DAYS -> draft.copy(rule = rule.copy(unit = HabitUnit.DAY,
            weekdays = MONDAY_TO_FRIDAY), target = "1")
        PracticeFrequency.WEEKLY -> draft.copy(rule = rule.copy(unit = HabitUnit.WEEK, weekdays = ALL_WEEKDAYS),
            target = DEFAULT_WEEKLY_TARGET)
        PracticeFrequency.MONTHLY -> draft.copy(rule = rule.copy(unit = HabitUnit.MONTH, weekdays = ALL_WEEKDAYS),
            target = DEFAULT_MONTHLY_TARGET)
    }
}

/** A timed habit is practiced once per day or week, so a monthly goal becomes daily (spec 016). */
fun withTimed(draft: HabitDraft, timed: Boolean): HabitDraft =
    if (!timed) draft.copy(timed = false)
    else draft.copy(timed = true, target = "1", durationText = draft.durationText.ifBlank { DEFAULT_TIMED_MINUTES },
        rule = draft.rule.copy(unit = if (draft.rule.unit == HabitUnit.MONTH) HabitUnit.DAY else draft.rule.unit))

val HabitDraft.isTimedWeekly: Boolean get() = rule.unit == HabitUnit.WEEK

/** Timed habit counted per week (every day allowed) or per day on the chosen weekdays. */
fun withTimedWeekly(draft: HabitDraft, weekly: Boolean): HabitDraft = draft.copy(target = "1",
    rule = draft.rule.copy(unit = if (weekly) HabitUnit.WEEK else HabitUnit.DAY,
        weekdays = if (weekly) ALL_WEEKDAYS else draft.rule.weekdays))

/** "Quebrar hábito": avoiding allows none per day; limiting starts at one. */
fun withBreakingType(draft: HabitDraft, type: HabitType): HabitDraft =
    if (type == HabitType.AVOID) draft.copy(rule = draft.rule.copy(type = HabitType.AVOID, unit = HabitUnit.DAY),
        target = "0")
    else draft.copy(rule = draft.rule.copy(type = HabitType.LIMIT), target = "1")

/** Toggles weekday [index] (0 = Monday) and never leaves the selection empty. */
fun toggleHabitWeekday(draft: HabitDraft, index: Int): HabitDraft {
    val bits = draft.rule.weekdays xor (1 shl index)
    return if (bits == 0) draft else draft.copy(rule = draft.rule.copy(weekdays = bits))
}

fun HabitRule.hasWeekday(index: Int): Boolean = weekdays and (1 shl index) != 0

/** Weekdays matter for daily practice on chosen days and for timed habits counted per day. */
val HabitDraft.showsDaysChip: Boolean get() = isPractice && rule.unit == HabitUnit.DAY &&
    (timed || practiceFrequencyOf(rule) == PracticeFrequency.CHOSEN_DAYS)

/** How many times per week or month, only for untimed weekly/monthly practice. */
val HabitDraft.showsTimesChip: Boolean get() = isPractice && !timed && rule.unit != HabitUnit.DAY

val HabitDraft.showsLimitChip: Boolean get() = rule.type == HabitType.LIMIT

