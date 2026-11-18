package com.ethos.app.features.statistics

import com.ethos.app.core.timers.domain.TimerSegment
import com.ethos.app.core.timers.domain.TimerSnapshot
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Estatísticas with years of history (spec 031): timings on the JVM and content reused across controls. */
class StatisticsLargeHistoryTest {
    private val today = LocalDate.of(2026, 11, 18)
    private val zone = ZoneOffset.UTC
    private val dataset by lazy { StatisticsDataset(today, zone) }

    private val controls: List<StatisticsControls> by lazy {
        val periods = PeriodKind.entries.map { StatisticsPeriod.current(it, today) } +
            StatisticsPeriod.current(PeriodKind.MONTH, today).previous() +
            StatisticsPeriod.current(PeriodKind.YEAR, today).previous().previous()
        val items = listOf(StatisticsItem.Habit("habit0"), StatisticsItem.Habit("habit5"),
            StatisticsItem.TaskSeries("task3"))
        periods.flatMap { period ->
            HistoryFilter.entries.map { StatisticsControls(period, filter = it) } +
                items.map { StatisticsControls(period, item = it) } +
                StatisticsControls(period, limit = HISTORY_PAGE_SIZE * 3)
        }
    }

    private fun prepare(gameMode: Boolean = true) = PreparedStatistics(dataset.data, today, zone, gameMode)

    private fun millisOf(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / NANOS_PER_MILLI
    }

    /** Median of a few runs after a warm-up, so one slow run (JIT, GC) does not decide. */
    private fun medianMillis(block: () -> Unit): Long {
        block()
        return List(RUNS) { millisOf(block) }.sorted()[RUNS / 2]
    }

    @Test fun preparedContentIsTheSameAsComputingFromScratch() {
        listOf(true, false).forEach { gameMode ->
            val prepared = prepare(gameMode)
            controls.forEach { c -> assertEquals(c.toString(), prepare(gameMode).content(c), prepared.content(c)) }
        }
    }

    @Test fun openingTheBusiestPeriodStaysUnderOneSecond() {
        val all = StatisticsControls(StatisticsPeriod.current(PeriodKind.ALL, today))
        val open = medianMillis { prepare().content(all) }
        println("spec031 open (prepare + Tudo): $open ms")
        assertTrue("open took $open ms", open < OPEN_LIMIT_MILLIS)
    }

    @Test fun changingPeriodOrFilterStaysUnderOneHundredMilliseconds() {
        val prepared = prepare()
        val change = controls.maxOf { c -> medianMillis { prepared.content(c) } }
        println("spec031 slowest control change: $change ms")
        assertTrue("change took $change ms", change < CHANGE_LIMIT_MILLIS)
    }

    @Test fun timerCheckpointsDoNotChangeWhatStatisticsRead() {
        val closed = TimerSegment("1", "s", 0, 1_000, created = 0)
        val open = TimerSegment("2", "s", 2_000, startElapsed = 10, checkpointElapsed = 100, created = 0)
        val snapshot = TimerSnapshot(segments = listOf(closed, open))
        assertEquals(snapshot.forStatistics(),
            snapshot.copy(segments = listOf(closed, open.copy(checkpointElapsed = 900))).forStatistics())
        assertNotEquals(snapshot.forStatistics(),
            snapshot.copy(segments = listOf(closed, open.copy(endWall = 3_000))).forStatistics())
    }

    private companion object {
        const val RUNS = 5
        const val NANOS_PER_MILLI = 1_000_000L
        const val OPEN_LIMIT_MILLIS = 1_000L
        const val CHANGE_LIMIT_MILLIS = 100L
    }
}
