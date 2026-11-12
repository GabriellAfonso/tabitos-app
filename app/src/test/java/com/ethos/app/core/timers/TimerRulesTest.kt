package com.ethos.app.core.timers

import com.ethos.app.core.timers.domain.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerRulesTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()
    private val minute = 60_000L

    private fun closed(id: String, session: String, start: String, minutes: Long) =
        TimerSegment(id, session, at(start), at(start) + minutes * minute, created = 0)

    @Test fun pausedIntervalContributesNothingAndRunningUsesMonotonicClock() {
        val first = TimerSegment("a", "s", at("2026-11-02T10:00"), at("2026-11-02T10:05"), created = 0)
        // Resumed 10 minutes later; wall clock then jumps one hour forward, monotonic clock does not.
        val running = TimerSegment("b", "s", at("2026-11-02T10:15"), startElapsed = 1_000_000, bootCount = 3,
            checkpointElapsed = 1_000_000, created = 0)
        val snapshot = TimerSnapshot(segments = listOf(first, running))
        val now = TimerNow(at("2026-11-02T11:18"), 1_000_000 + 3 * minute, 3)
        assertEquals(8 * minute, TimerRules.sessionTotal(snapshot, "s", now))
    }

    @Test fun rebootKeepsLastCheckpointAndExcludesPoweredOffTime() {
        val running = TimerSegment("b", "s", 0, startElapsed = 5_000, bootCount = 1,
            checkpointElapsed = 5_000 + 7 * minute, created = 0)
        val afterBoot = TimerNow(10 * 60 * minute, 2_000, 2)
        assertFalse(TimerRules.sameBoot(running, afterBoot))
        assertEquals(7 * minute, TimerRules.duration(running, afterBoot))
        // Without a boot counter, a monotonic value below the checkpoint also reveals a reboot.
        val unknown = running.copy(bootCount = null)
        assertEquals(7 * minute, TimerRules.duration(unknown, TimerNow(0, 1_000, null)))
    }

    @Test fun capStopsExactlyAtObjective() {
        val running = TimerSegment("b", "s", 0, startElapsed = 0, bootCount = 1, checkpointElapsed = 0,
            limitMillis = 30 * minute, created = 0)
        val late = TimerNow(45 * minute, 45 * minute, 1)
        assertTrue(TimerRules.capped(running, late))
        assertEquals(30 * minute, TimerRules.duration(running, late))
        assertEquals(30 * minute, TimerRules.endWall(running, late))
    }

    @Test fun sessionsSumToWeeklyThreeHoursAndRemainingIsZero() {
        val sessions = listOf(TimerSession("s", TimerKind.HABIT, "h", "Corrida", TimerState.PAUSED,
            TimerPeriod.WEEK, "habit:h", created = 0))
        val segments = listOf(closed("1", "s", "2026-11-02T08:00", 40), closed("2", "s", "2026-11-04T08:00", 50),
            closed("3", "s", "2026-11-07T08:00", 90))
        val snapshot = TimerSnapshot(sessions, segments, goals = listOf(TimerGoal("habit:h", 180)))
        val now = TimerNow(at("2026-11-08T12:00"), 0, 1)
        assertEquals(180 * minute, TimerRules.progress(snapshot, TimerKind.HABIT, "h", TimerPeriod.WEEK,
            LocalDate.parse("2026-11-08"), now, zone))
        assertEquals(0L, TimerRules.remaining(snapshot, sessions[0], now, zone))
        // Next week starts empty.
        assertEquals(0L, TimerRules.progress(snapshot, TimerKind.HABIT, "h", TimerPeriod.WEEK,
            LocalDate.parse("2026-11-09"), now, zone))
    }

    @Test fun sundayToMondaySplitsTenAndTwentyMinutes() {
        val parts = TimerRules.allocate(at("2026-11-08T23:50"), at("2026-11-09T00:20"), zone)
        assertEquals(mapOf(LocalDate.parse("2026-11-08") to 10 * minute, LocalDate.parse("2026-11-09") to 20 * minute),
            parts)
        val weeks = TimerRules.byPeriod(listOf(TimerSegment("x", "s", at("2026-11-08T23:50"), at("2026-11-09T00:20"),
            created = 0)), TimerPeriod.WEEK, TimerNow(0, 0, null), zone)
        assertEquals(10 * minute, weeks[LocalDate.parse("2026-11-02")])
        assertEquals(20 * minute, weeks[LocalDate.parse("2026-11-09")])
    }

    @Test fun daylightSavingDayAllocatesRealDuration() {
        val ny = ZoneId.of("America/New_York")
        val start = LocalDateTime.parse("2026-11-01T00:30").atZone(ny).toInstant().toEpochMilli()
        // 2026-11-01 has 25 hours in New York; four real hours stay four hours on that date.
        val parts = TimerRules.allocate(start, start + 240 * minute, ny)
        assertEquals(240 * minute, parts[LocalDate.parse("2026-11-01")])
    }

    @Test fun manualValidationRejectsInvalidFutureLongEarlyInactiveAndOverlap() {
        val now = at("2026-11-07T12:00")
        val current = TimerNow(now, 0, 1)
        val bounds = TimerBounds(at("2026-11-01T00:00")) { it != LocalDate.parse("2026-11-03") }
        val existing = listOf(closed("e", "s", "2026-11-05T20:00", 30))
        fun check(start: String, end: String) = TimerRules.validateManual(at(start), at(end), now, bounds, existing,
            current, zone)
        fun problem(start: String, end: String) =
            assertThrows(TimerRuleException::class.java) { check(start, end) }.problem
        assertEquals(TimerProblem.INVALID_INTERVAL, problem("2026-11-05T10:00", "2026-11-05T09:00"))
        assertEquals(TimerProblem.FUTURE, problem("2026-11-07T11:00", "2026-11-07T13:00"))
        assertEquals(TimerProblem.TOO_LONG, problem("2026-11-04T00:00", "2026-11-05T01:00"))
        assertEquals(TimerProblem.BEFORE_SOURCE, problem("2026-10-31T10:00", "2026-10-31T11:00"))
        assertEquals(TimerProblem.INACTIVE, problem("2026-11-03T10:00", "2026-11-03T11:00"))
        assertEquals(TimerProblem.OVERLAP, problem("2026-11-05T20:20", "2026-11-05T21:00"))
        check("2026-11-05T20:30", "2026-11-05T21:00")
    }

    @Test fun discardTargetsOnlyTheLatestCountedSegment() {
        val session = TimerSession("s", TimerKind.HABIT, "h", "Ler", TimerState.RUNNING, created = 0)
        val yesterday = closed("a", "s", "2026-11-10T10:00", 20)
        val manual = closed("m", "s", "2026-11-11T07:00", 10).copy(manual = true)
        val accidental = TimerSegment("b", "s", at("2026-11-11T09:00"), startElapsed = 0, created = 0)
        val snapshot = TimerSnapshot(listOf(session), listOf(yesterday, accidental, manual))
        assertEquals("b", TimerRules.discardable(snapshot, session)?.id)
    }

    @Test fun rewardsReachedAndFinishedSessionsCannotBeDiscarded() {
        val segment = closed("a", "s", "2026-11-11T09:00", 5)
        val base = TimerSession("s", TimerKind.TASK, "t", "Ler", TimerState.PAUSED, created = 0)
        val snapshot = TimerSnapshot(listOf(base), listOf(segment))
        assertEquals("a", TimerRules.discardable(snapshot, base)?.id)
        listOf(base.copy(kind = TimerKind.REWARD), base.copy(reached = true), base.copy(state = TimerState.FINISHED))
            .forEach { assertEquals(null, TimerRules.discardable(snapshot.copy(sessions = listOf(it)), it)) }
        val onlyManual = snapshot.copy(segments = listOf(segment.copy(manual = true)))
        assertEquals(null, TimerRules.discardable(onlyManual, base))
    }
}
