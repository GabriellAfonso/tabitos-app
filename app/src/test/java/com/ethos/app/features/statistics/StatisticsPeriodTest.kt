package com.ethos.app.features.statistics

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsPeriodTest {
    private val today = LocalDate.of(2026, 11, 18) // Wednesday
    private val firstDay = LocalDate.of(2025, 3, 5)

    @Test fun weekRunsMondayToSunday() {
        val range = StatisticsPeriod.current(PeriodKind.WEEK, today).range(firstDay, today)
        assertEquals(LocalDate.of(2026, 11, 16), range.start)
        assertEquals(LocalDate.of(2026, 11, 22), range.endInclusive)
    }

    @Test fun monthAndYearAreCalendarPeriods() {
        val month = StatisticsPeriod.current(PeriodKind.MONTH, today).range(firstDay, today)
        assertEquals(LocalDate.of(2026, 11, 1)..LocalDate.of(2026, 11, 30), month)
        val year = StatisticsPeriod.current(PeriodKind.YEAR, today).range(firstDay, today)
        assertEquals(LocalDate.of(2026, 1, 1)..LocalDate.of(2026, 12, 31), year)
    }

    @Test fun allStartsAtTheFirstDataDayAndHasNoArrows() {
        val all = StatisticsPeriod.current(PeriodKind.ALL, today)
        assertEquals(firstDay..today, all.range(firstDay, today))
        assertFalse(all.hasPrevious(firstDay, today))
        assertFalse(all.hasNext(firstDay, today))
        assertEquals(PeriodLabel.All(firstDay), all.label(firstDay, today))
    }

    @Test fun nextStopsAtTheCurrentPeriodAndPreviousAtTheFirstData() {
        val week = StatisticsPeriod.current(PeriodKind.WEEK, today)
        assertFalse(week.hasNext(firstDay, today))
        assertTrue(week.previous().hasNext(firstDay, today))
        val firstWeek = StatisticsPeriod(PeriodKind.WEEK, firstDay)
        assertFalse(firstWeek.hasPrevious(firstDay, today))
        assertTrue(firstWeek.next().hasPrevious(firstDay, today))
    }

    @Test fun weekLabelsNameThisAndLastWeekThenTheDates() {
        val week = StatisticsPeriod.current(PeriodKind.WEEK, today)
        assertEquals(PeriodLabel.ThisWeek, week.label(firstDay, today))
        assertEquals(PeriodLabel.LastWeek, week.previous().label(firstDay, today))
        assertEquals(PeriodLabel.WeekRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8)),
            week.previous().previous().label(firstDay, today))
    }

    @Test fun weekAcrossTheYearKeepsBothDates() {
        val week = StatisticsPeriod(PeriodKind.WEEK, LocalDate.of(2026, 1, 1))
        assertEquals(PeriodLabel.WeekRange(LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 4)),
            week.label(firstDay, today))
        assertEquals(PeriodLabel.Month(YearMonth.of(2026, 10)),
            StatisticsPeriod.current(PeriodKind.MONTH, today).previous().label(firstDay, today))
    }

    @Test fun chartUsesDaysMonthsAndYearsForLongHistories() {
        assertEquals(BucketUnit.DAY, StatisticsPeriod.current(PeriodKind.MONTH, today).bucketUnit(firstDay, today))
        assertEquals(BucketUnit.MONTH, StatisticsPeriod.current(PeriodKind.YEAR, today).bucketUnit(firstDay, today))
        assertEquals(BucketUnit.MONTH, StatisticsPeriod.current(PeriodKind.ALL, today).bucketUnit(firstDay, today))
        assertEquals(BucketUnit.YEAR, StatisticsPeriod.current(PeriodKind.ALL, today)
            .bucketUnit(LocalDate.of(2024, 12, 31), today))
    }

    @Test fun storeStartsEmptyAndKeepsTheChoice() {
        val store = StatisticsPeriodStore()
        assertEquals(null, store.period.value)
        store.choose(StatisticsPeriod.current(PeriodKind.YEAR, today))
        assertEquals(PeriodKind.YEAR, store.period.value?.kind)
    }
}
