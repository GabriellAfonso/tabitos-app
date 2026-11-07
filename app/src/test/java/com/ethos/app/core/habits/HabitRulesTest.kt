package com.ethos.app.core.habits

import com.ethos.app.core.habits.domain.*
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
    @Test fun incompleteWeekCreatesOnePendingPeriod() {
        val s = record(initial(), "a", monday)
        assertEquals(1, HabitRules.pending(s, monday.plusDays(7)).size)
        assertEquals(PeriodOutcome.PENDING, HabitRules.pending(s, monday.plusDays(7)).single().outcome)
    }
    @Test fun limitCountsEventsAndIdentifiesEachExcess() {
        val s = record(record(initial(HabitType.LIMIT, target = 1), "a", monday), "b", monday)
        val p = HabitRules.periods(s, "h", monday).single()
        assertEquals(2, p.count)
        assertEquals(listOf("b"), p.excessIds)
    }
    @Test fun avoidZeroRecordsStillNeedsDailyConfirmation() {
        val s = initial(HabitType.AVOID, HabitUnit.DAY, 0)
        val p = HabitRules.pending(s, monday.plusDays(1)).single()
        val reviewed = HabitRules.apply(s, HabitAction.Review(p.key), monday.plusDays(1), 3, emptySet())
        assertTrue(HabitRules.pending(reviewed, monday.plusDays(1)).isEmpty())
    }
    @Test fun correctionReopensChangedReviewedOutcomeOnlyOnce() {
        var s = initial(HabitType.AVOID, HabitUnit.DAY, 0)
        val p = HabitRules.pending(s, monday.plusDays(1)).single()
        s = HabitRules.apply(s, HabitAction.Review(p.key), monday.plusDays(1), 3, emptySet())
        s = HabitRules.apply(s, HabitAction.Record("forgotten", "h", monday), monday.plusDays(1), 4, emptySet())
        assertEquals(1, HabitRules.pending(s, monday.plusDays(1)).size)
        assertTrue(s.reviews.single().invalidated)
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
        val changed = record(s, "tuesday", monday.plusDays(1))
        assertEquals(1, HabitRules.pending(changed, monday.plusDays(2)).size)
        assertEquals(monday, HabitRules.pending(changed, monday.plusDays(2)).single().start)
    }
    @Test fun leapMonthAndCalendarWeekBoundaries() {
        val feb = LocalDate.of(2028, 2, 28)
        assertEquals(LocalDate.of(2028, 2, 29), HabitRules.end(feb, HabitUnit.MONTH))
        assertEquals(monday, HabitRules.start(monday.plusDays(6), HabitUnit.WEEK))
    }
    @Test fun pendingGapDoesNotClaimConfirmedContinuity() {
        var s = initial(HabitType.PRACTICE, HabitUnit.DAY, 1)
        s = record(s, "a", monday)
        s = record(s, "c", monday.plusDays(2))
        val streak = HabitRules.streak(s, "h", monday.plusDays(2))
        assertTrue(streak.uncertain)
        assertEquals(1, streak.current)
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
    @Test fun sameOutcomeCorrectionDoesNotReopenReview() {
        var s = initial(HabitType.LIMIT, HabitUnit.DAY, 3)
        s = record(s, "a", monday)
        val p = HabitRules.pending(s, monday.plusDays(1)).single()
        s = HabitRules.apply(s, HabitAction.Review(p.key), monday.plusDays(1), 3, emptySet())
        s = HabitRules.apply(s, HabitAction.Record("b", "h", monday), monday.plusDays(1), 4, emptySet())
        assertFalse(s.reviews.single().invalidated)
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
    @Test fun confirmedFailureBreaksStreakButRetainsBest() {
        var s = initial(HabitType.PRACTICE, HabitUnit.DAY, 1)
        s = record(record(s, "a", monday), "b", monday.plusDays(1))
        val p = HabitRules.pending(s, monday.plusDays(3)).single()
        s = HabitRules.apply(s, HabitAction.Review(p.key), monday.plusDays(3), 4, emptySet())
        val streak = HabitRules.streak(s, "h", monday.plusDays(3))
        assertEquals(0, streak.current)
        assertEquals(2, streak.best)
    }
}
