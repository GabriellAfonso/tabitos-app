package com.tabitos.app.features.statistics

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Calendar periods of the statistics page (spec 022); rolling windows were rejected. */
enum class PeriodKind { WEEK, MONTH, YEAR, ALL }

/** Unit of one bar of the task chart. */
enum class BucketUnit { DAY, MONTH, YEAR }

/** What the period row says; turned into text with resources by the page. */
sealed interface PeriodLabel {
    data object ThisWeek : PeriodLabel
    data object LastWeek : PeriodLabel
    data class WeekRange(val start: LocalDate, val end: LocalDate) : PeriodLabel
    data class Month(val month: YearMonth) : PeriodLabel
    data class Year(val year: Int) : PeriodLabel
    data class All(val since: LocalDate) : PeriodLabel
}

/** A period of [kind] containing [anchor]; [PeriodKind.ALL] ignores the anchor and starts at the first data day. */
data class StatisticsPeriod(val kind: PeriodKind, val anchor: LocalDate) {
    fun range(firstDay: LocalDate, today: LocalDate): ClosedRange<LocalDate> = when (kind) {
        PeriodKind.WEEK -> weekStart(anchor)..weekStart(anchor).plusDays(DAYS_AFTER_MONDAY)
        PeriodKind.MONTH -> anchor.withDayOfMonth(1)..anchor.with(TemporalAdjusters.lastDayOfMonth())
        PeriodKind.YEAR -> anchor.withDayOfYear(1)..anchor.with(TemporalAdjusters.lastDayOfYear())
        PeriodKind.ALL -> minOf(firstDay, today)..today
    }

    fun previous(): StatisticsPeriod = shift(-1)
    fun next(): StatisticsPeriod = shift(1)

    fun hasPrevious(firstDay: LocalDate, today: LocalDate): Boolean =
        kind != PeriodKind.ALL && range(firstDay, today).start > firstDay

    /** Arrows never go past the period that contains today. */
    fun hasNext(firstDay: LocalDate, today: LocalDate): Boolean =
        kind != PeriodKind.ALL && range(firstDay, today).endInclusive < today

    fun label(firstDay: LocalDate, today: LocalDate): PeriodLabel {
        val range = range(firstDay, today)
        return when (kind) {
            PeriodKind.WEEK -> when (range.start) {
                weekStart(today) -> PeriodLabel.ThisWeek
                weekStart(today).minusWeeks(1) -> PeriodLabel.LastWeek
                else -> PeriodLabel.WeekRange(range.start, range.endInclusive)
            }
            PeriodKind.MONTH -> PeriodLabel.Month(YearMonth.from(anchor))
            PeriodKind.YEAR -> PeriodLabel.Year(anchor.year)
            PeriodKind.ALL -> PeriodLabel.All(range.start)
        }
    }

    /** Days for week/month; months for year and all, or years when all spans more than [MAX_MONTHLY_YEARS]. */
    fun bucketUnit(firstDay: LocalDate, today: LocalDate): BucketUnit = when (kind) {
        PeriodKind.WEEK, PeriodKind.MONTH -> BucketUnit.DAY
        PeriodKind.YEAR -> BucketUnit.MONTH
        PeriodKind.ALL -> {
            val range = range(firstDay, today)
            val years = range.endInclusive.year - range.start.year + 1
            if (years > MAX_MONTHLY_YEARS) BucketUnit.YEAR else BucketUnit.MONTH
        }
    }

    private fun shift(steps: Long) = when (kind) {
        PeriodKind.WEEK -> copy(anchor = anchor.plusWeeks(steps))
        PeriodKind.MONTH -> copy(anchor = anchor.plusMonths(steps))
        PeriodKind.YEAR -> copy(anchor = anchor.plusYears(steps))
        PeriodKind.ALL -> this
    }

    companion object {
        private const val DAYS_AFTER_MONDAY = 6L
        const val MAX_MONTHLY_YEARS = 2

        /** Changing the kind always goes back to the period that contains today. */
        fun current(kind: PeriodKind, today: LocalDate) = StatisticsPeriod(kind, today)

        fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
}

/**
 * The chosen period lives as long as the app process: kept while navigating, back to "Esta semana" when the app
 * is reopened (spec 022 FR-007). Null means the current week.
 */
@Singleton
class StatisticsPeriodStore @Inject constructor() {
    private val chosen = MutableStateFlow<StatisticsPeriod?>(null)
    val period: StateFlow<StatisticsPeriod?> = chosen.asStateFlow()

    fun choose(period: StatisticsPeriod) { chosen.value = period }
}
