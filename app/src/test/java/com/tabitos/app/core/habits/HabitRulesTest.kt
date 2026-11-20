package com.tabitos.app.core.habits

import com.tabitos.app.core.habits.domain.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class HabitRulesTest {
    private val monday = LocalDate.of(2026, 11, 2)
    private fun initial(type: HabitType = HabitType.PRACTICE, unit: HabitUnit = HabitUnit.WEEK,
        target: Int = 3): HabitSnapshot = HabitRules.apply(HabitSnapshot(),
        HabitAction.Save(Habit("h", monday, "Leitura"), HabitRule("r", "h", monday, type, unit, target)),
        monday, 1, emptySet())
    private fun record(s: HabitSnapshot, id: String, date: LocalDate) =
        HabitRules.apply(s, HabitAction.Record(id, "h", date), date, 2, emptySet())

    @Test fun practiceCountsDistinctDaysAndRecognizesSuccessImmediately() {
        var s = initial()
        s = record(record(s, "a", monday), "b", monday)
        assertEquals(1, HabitRules.periods(s, "h", monday).single().count)
        s = record(s, "c", monday.plusDays(1))
        s = record(s, "d", monday.plusDays(2))
        assertEquals(PeriodOutcome.SUCCESS, HabitRules.periods(s, "h", monday.plusDays(2)).single().outcome)
    }
    @Test fun incompleteWeekStaysOpenInLateWindowThenClosesAsMissed() {
        val s = record(initial(), "a", monday)
        // Sunday + 2 days: still accepts records, nothing pending anywhere.
        assertEquals(PeriodOutcome.OPEN, HabitRules.periods(s, "h", monday.plusDays(8)).first().outcome)
        assertTrue(HabitRules.pending(s, monday.plusDays(8)).isEmpty())
        assertEquals(PeriodOutcome.FAILURE, HabitRules.periods(s, "h", monday.plusDays(9)).first().outcome)
        assertTrue(HabitRules.pending(s, monday.plusDays(9)).isEmpty())
    }
    @Test fun limitCountsEventsAndIdentifiesEachExcess() {
        val s = record(record(initial(HabitType.LIMIT, target = 1), "a", monday), "b", monday)
        val p = HabitRules.periods(s, "h", monday).single()
        assertEquals(2, p.count)
        assertEquals(listOf("b"), p.excessIds)
    }
    @Test fun avoidWithoutEventsSucceedsByItselfAfterWindow() {
        val s = initial(HabitType.AVOID, HabitUnit.DAY, 0)
        assertEquals(PeriodOutcome.OPEN, HabitRules.periods(s, "h", monday.plusDays(2)).first().outcome)
        assertEquals(PeriodOutcome.SUCCESS, HabitRules.periods(s, "h", monday.plusDays(3)).first().outcome)
        assertTrue(HabitRules.pending(s, monday.plusDays(3)).isEmpty())
    }
    @Test fun lateRecordChangesOutcomeOnlyInsideWindow() {
        var s = initial(HabitType.AVOID, HabitUnit.DAY, 0)
        s = HabitRules.apply(s, HabitAction.Record("late", "h", monday), monday.plusDays(2), 4, emptySet())
        assertEquals(PeriodOutcome.FAILURE, HabitRules.periods(s, "h", monday.plusDays(3)).first().outcome)
        try {
            HabitRules.apply(s, HabitAction.Record("closed", "h", monday), monday.plusDays(3), 5, emptySet())
            fail("closed period accepted a record")
        } catch (e: HabitRuleException) { assertEquals(HabitProblem.DATE, e.reason) }
        try {
            HabitRules.apply(s, HabitAction.Correct("late", delete = true), monday.plusDays(3), 6, emptySet())
            fail("closed period accepted a correction")
        } catch (e: HabitRuleException) { assertEquals(HabitProblem.DATE, e.reason) }
    }
    @Test fun pauseBlocksNewRecordsButAllowsOldDateCorrection() {
        var s = record(initial(), "a", monday)
        s = HabitRules.apply(s, HabitAction.Status("h", HabitStatus.PAUSED, 1), monday.plusDays(1), 4, emptySet())
        try {
            record(s, "bad", monday.plusDays(1)); fail("inactive date accepted")
        } catch (e: HabitRuleException) { assertEquals(HabitProblem.INACTIVE, e.reason) }
        s = HabitRules.apply(s, HabitAction.Correct("a", delete = true), monday.plusDays(2), 5, emptySet())
        assertTrue(s.records.single().deleted)
    }
    @Test fun impossiblePartialPracticeRejectedButLargeOccurrenceLimitAllowed() {
        val date = LocalDate.of(2026, 11, 28)
        val h = Habit("h", date, "Teste")
        val r = HabitRule("r", "h", date, unit = HabitUnit.MONTH, target = 10)
        try {
            HabitRules.apply(HabitSnapshot(), HabitAction.Save(h, r, partialTarget = 5), date, 1, emptySet())
            fail("impossible target accepted")
        } catch (e: HabitRuleException) { assertEquals(HabitProblem.CAPACITY, e.reason) }
        val s = HabitRules.apply(HabitSnapshot(), HabitAction.Save(h,
            r.copy(type = HabitType.LIMIT), partialTarget = 50), date, 1, emptySet())
        assertEquals(50, HabitRules.periods(s, "h", date).single().target)
    }
    @Test fun futureTypeChangePreservesHistoricalRule() {
        var s = initial()
        s = HabitRules.apply(s, HabitAction.Save(s.habits.single(),
            HabitRule("next", "h", monday, HabitType.AVOID, HabitUnit.DAY, 0)),
            monday.plusDays(2), 5, emptySet())
        assertEquals(monday.plusDays(7), s.rules.last().effective)
        assertEquals(HabitType.PRACTICE, HabitRules.periods(s, "h", monday.plusDays(2)).single().rule.type)
    }
    @Test fun scheduledOffDayDoesNotReplaceMissedDay() {
        val h = Habit("h", monday, "Teste")
        val s = HabitRules.apply(HabitSnapshot(), HabitAction.Save(h,
            HabitRule("r", "h", monday, weekdays = 1)), monday, 1, emptySet())
        val periods = HabitRules.periods(s, "h", monday.plusDays(3))
        assertEquals(listOf(monday), periods.map { it.start })
        assertEquals(PeriodOutcome.FAILURE, periods.single().outcome)
    }
    @Test fun leapMonthAndCalendarWeekBoundaries() {
        val feb = LocalDate.of(2028, 2, 28)
        assertEquals(LocalDate.of(2028, 2, 29), HabitRules.end(feb, HabitUnit.MONTH))
        assertEquals(monday, HabitRules.start(monday.plusDays(6), HabitUnit.WEEK))
    }
    @Test fun openWindowNeitherBreaksNorCountsStreak() {
        var s = initial(HabitType.PRACTICE, HabitUnit.DAY, 1)
        s = record(s, "a", monday)
        s = record(s, "c", monday.plusDays(2))
        val streak = HabitRules.streak(s, "h", monday.plusDays(2))
        assertFalse(streak.uncertain)
        assertEquals(2, streak.current)
        assertEquals(1, HabitRules.streak(s, "h", monday.plusDays(4)).current)
    }
    @Test fun pauseSameDayRetainsAlreadyEnteredHistoryAndCorrections() {
        var s = record(initial(HabitType.PRACTICE, HabitUnit.DAY, 1), "a", monday)
        s = HabitRules.apply(s, HabitAction.Status("h", HabitStatus.PAUSED), monday, 3, emptySet())
        assertEquals(PeriodOutcome.SUCCESS, HabitRules.periods(s, "h", monday).single().outcome)
        s = HabitRules.apply(s, HabitAction.Correct("a", monday), monday, 4, emptySet())
        assertEquals(1, s.records.size)
    }
    @Test fun restoringDoesNotCreateDemandsForExcludedInterval() {
        var s = initial(HabitType.PRACTICE, HabitUnit.DAY, 1)
        s = HabitRules.apply(s, HabitAction.Status("h", HabitStatus.EXCLUDED), monday.plusDays(1), 3, emptySet())
        s = HabitRules.apply(s, HabitAction.Status("h", HabitStatus.ACTIVE), monday.plusDays(5), 4, emptySet())
        assertEquals(listOf(monday, monday.plusDays(5)),
            HabitRules.periods(s, "h", monday.plusDays(5)).map { it.start })
    }
    @Test fun nextGoalPreservesOlderGoalsAndCurrentGoalEditKeepsPeriodIdentity() {
        var s = initial()
        val key = HabitRules.periods(s, "h", monday).single().key
        s = HabitRules.apply(s, HabitAction.Save(s.habits.single(),
            s.rules.single().copy(id = "edited", target = 2)), monday.plusDays(1), 3, emptySet())
        assertEquals(key, HabitRules.periods(s, "h", monday.plusDays(1)).single().key)
        s = HabitRules.apply(s, HabitAction.Save(s.habits.single(),
            s.rules.single().copy(id = "next", target = 4), nextPeriod = true), monday.plusDays(2), 4, emptySet())
        val periods = HabitRules.periods(s, "h", monday.plusDays(7))
        assertEquals(listOf(2, 4), periods.map { it.target })
    }
    @Test fun partialPracticeTargetDefaultsToAvailableDays() {
        val saturday = LocalDate.of(2026, 11, 7)
        val s = HabitRules.apply(HabitSnapshot(), HabitAction.Save(Habit("h", saturday, "Correr"),
            HabitRule("r", "h", saturday, unit = HabitUnit.WEEK, target = 5)), saturday, 1, emptySet())
        assertEquals(2, HabitRules.periods(s, "h", saturday).single().target)
        assertEquals(5, HabitRules.periods(s, "h", saturday.plusDays(2)).last().target)
    }
    @Test fun successRateUsesClosedPeriodsOnly() {
        var s = initial(HabitType.PRACTICE, HabitUnit.DAY, 1)
        s = record(s, "a", monday)
        assertNull(HabitRules.successRate(s, "h", monday.plusDays(2)))
        assertEquals(50, HabitRules.successRate(s, "h", monday.plusDays(4)))
    }
    @Test fun retroactiveBoundsAndRepeatedRecordId() {
        var s = record(initial(), "a", monday)
        s = record(s, "a", monday)
        assertEquals(1, s.records.size)
        listOf(monday.minusDays(1), monday.plusDays(1)).forEach { date ->
            try {
                HabitRules.apply(s, HabitAction.Record("bad", "h", date), monday, 4, emptySet())
                fail("invalid date accepted")
            } catch (e: HabitRuleException) { assertEquals(HabitProblem.DATE, e.reason) }
        }
    }
    @Test fun avoidCountsEndedDaysInLateWindowWithoutPayingEarly() {
        val s = initial(HabitType.AVOID, HabitUnit.DAY, 0)
        val wednesday = monday.plusDays(2)
        // Monday and Tuesday ended without relapse; today does not count until it ends.
        assertEquals(2, HabitRules.streak(s, "h", wednesday).current)
        assertEquals(100, HabitRules.successRate(s, "h", wednesday))
        assertEquals(PeriodOutcome.OPEN, HabitRules.periods(s, "h", wednesday).first().outcome)
    }
    @Test fun lateRelapseRecountsStreakInsideWindow() {
        var s = initial(HabitType.AVOID, HabitUnit.DAY, 0)
        val wednesday = monday.plusDays(2)
        s = HabitRules.apply(s, HabitAction.Record("late", "h", monday.plusDays(1)), wednesday, 4, emptySet())
        val tuesday = HabitRules.periods(s, "h", wednesday)[1]
        assertEquals(PeriodOutcome.FAILURE, HabitRules.standing(tuesday, wednesday))
        assertEquals(0, HabitRules.streak(s, "h", wednesday).current)
        assertEquals(1, HabitRules.streak(s, "h", wednesday).best)
    }
    @Test fun relapseTodayBreaksStreakImmediately() {
        val s = record(initial(HabitType.AVOID, HabitUnit.DAY, 0), "a", monday.plusDays(1))
        assertEquals(0, HabitRules.streak(s, "h", monday.plusDays(1)).current)
    }
    @Test fun missedDayBreaksStreakButRetainsBest() {
        var s = initial(HabitType.PRACTICE, HabitUnit.DAY, 1)
        s = record(record(s, "a", monday), "b", monday.plusDays(1))
        val streak = HabitRules.streak(s, "h", monday.plusDays(5))
        assertEquals(0, streak.current)
        assertEquals(2, streak.best)
    }
}
