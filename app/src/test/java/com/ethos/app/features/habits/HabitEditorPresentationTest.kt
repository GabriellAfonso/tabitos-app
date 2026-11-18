package com.ethos.app.features.habits

import com.ethos.app.core.habits.domain.Habit
import com.ethos.app.core.habits.domain.HabitRule
import com.ethos.app.core.habits.domain.HabitType
import com.ethos.app.core.habits.domain.HabitUnit
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec 029: the editor sheet writes exactly the values the old editor wrote (spec 014/016). */
class HabitEditorPresentationTest {
    private val day = LocalDate.of(2026, 11, 17)
    private val draft = HabitDraft(Habit("h", day, "Ler"),
        HabitRule("r", "h", day, weekdays = ALL_WEEKDAYS))

    @Test fun frequencyMapsOntoUnitWeekdaysAndTarget() {
        assertEquals(PracticeFrequency.DAILY, practiceFrequencyOf(draft.rule))
        val chosen = withPracticeFrequency(draft, PracticeFrequency.CHOSEN_DAYS)
        assertEquals(HabitUnit.DAY, chosen.rule.unit)
        assertEquals(MONDAY_TO_FRIDAY, chosen.rule.weekdays)
        assertEquals("1", chosen.target)
        assertEquals(PracticeFrequency.CHOSEN_DAYS, practiceFrequencyOf(chosen.rule))
        val weekly = withPracticeFrequency(chosen, PracticeFrequency.WEEKLY)
        assertEquals(HabitUnit.WEEK to ALL_WEEKDAYS, weekly.rule.unit to weekly.rule.weekdays)
        assertEquals("3", weekly.target)
        val monthly = withPracticeFrequency(weekly, PracticeFrequency.MONTHLY)
        assertEquals(HabitUnit.MONTH, monthly.rule.unit)
        assertEquals("10", monthly.target)
        val daily = withPracticeFrequency(monthly, PracticeFrequency.DAILY)
        assertEquals(HabitUnit.DAY to ALL_WEEKDAYS, daily.rule.unit to daily.rule.weekdays)
        assertEquals("1", daily.target)
    }

    @Test fun choosingTheCurrentFrequencyKeepsTheGoal() {
        val weekly = withPracticeFrequency(draft, PracticeFrequency.WEEKLY).copy(target = "5")
        assertSame(weekly, withPracticeFrequency(weekly, PracticeFrequency.WEEKLY))
    }

    @Test fun timedTurnsMonthlyIntoDailyAndKeepsMinutes() {
        val monthly = withPracticeFrequency(draft, PracticeFrequency.MONTHLY)
        val timed = withTimed(monthly, true)
        assertTrue(timed.timed)
        assertEquals(HabitUnit.DAY, timed.rule.unit)
        assertEquals("1", timed.target)
        assertEquals("20", timed.durationText)
        assertEquals("45", withTimed(draft.copy(durationText = "45"), true).durationText)
        assertFalse(withTimed(timed, false).timed)
        val weekly = withTimedWeekly(timed, true)
        assertEquals(HabitUnit.WEEK to ALL_WEEKDAYS, weekly.rule.unit to weekly.rule.weekdays)
        val perDay = withTimedWeekly(timed.copy(rule = timed.rule.copy(weekdays = MONDAY_TO_FRIDAY)), false)
        assertEquals(MONDAY_TO_FRIDAY, perDay.rule.weekdays)
    }

    @Test fun breakingTypesSetTheirTargets() {
        val limit = withBreakingType(draft.copy(rule = draft.rule.copy(type = HabitType.AVOID)), HabitType.LIMIT)
        assertEquals(HabitType.LIMIT, limit.rule.type)
        assertEquals("1", limit.target)
        assertTrue(limit.showsLimitChip)
        val avoid = withBreakingType(limit.copy(rule = limit.rule.copy(unit = HabitUnit.WEEK)), HabitType.AVOID)
        assertEquals(HabitType.AVOID to HabitUnit.DAY, avoid.rule.type to avoid.rule.unit)
        assertEquals("0", avoid.target)
        assertFalse(avoid.showsLimitChip)
    }

    @Test fun weekdaysNeverBecomeEmpty() {
        val one = draft.copy(rule = draft.rule.copy(weekdays = 0b1))
        assertSame(one, toggleHabitWeekday(one, 0))
        assertEquals(0b101, toggleHabitWeekday(one, 2).rule.weekdays)
        assertTrue(toggleHabitWeekday(one, 2).rule.hasWeekday(2))
    }

    @Test fun chipsFollowTheRule() {
        assertFalse(draft.showsDaysChip)
        assertTrue(withPracticeFrequency(draft, PracticeFrequency.CHOSEN_DAYS).showsDaysChip)
        assertTrue(withTimed(draft, true).showsDaysChip)
        assertFalse(withTimedWeekly(withTimed(draft, true), true).showsDaysChip)
        assertFalse(draft.showsTimesChip)
        assertTrue(withPracticeFrequency(draft, PracticeFrequency.WEEKLY).showsTimesChip)
        assertFalse(withTimed(withPracticeFrequency(draft, PracticeFrequency.WEEKLY), true).showsTimesChip)
        val bad = draft.copy(rule = draft.rule.copy(type = HabitType.AVOID))
        assertFalse(bad.showsDaysChip || bad.showsTimesChip)
    }
}
