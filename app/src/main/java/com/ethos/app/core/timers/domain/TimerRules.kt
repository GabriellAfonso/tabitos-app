package com.ethos.app.core.timers.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object TimerRules {
    const val MAX_MANUAL_MILLIS = 24L * 60 * 60 * 1000
    const val CHECKPOINT_MILLIS = 10_000L

    /** True while the segment's monotonic origin is still meaningful (same boot, counter not reset). */
    fun sameBoot(segment: TimerSegment, now: TimerNow): Boolean {
        val reference = segment.checkpointElapsed ?: segment.startElapsed ?: return false
        if (segment.bootCount != null && now.boot != null && segment.bootCount != now.boot) return false
        return now.elapsed >= reference
    }

    /** Uncapped counted duration; after a reboot only the last durable checkpoint is known. */
    fun rawDuration(segment: TimerSegment, now: TimerNow): Long {
        segment.endWall?.let { return (it - segment.startWall).coerceAtLeast(0) }
        val start = segment.startElapsed ?: return 0
        val end = if (sameBoot(segment, now)) now.elapsed else segment.checkpointElapsed ?: start
        return (end - start).coerceAtLeast(0)
    }

    fun duration(segment: TimerSegment, now: TimerNow): Long {
        val raw = rawDuration(segment, now)
        return segment.limitMillis?.let { minOf(raw, it.coerceAtLeast(0)) } ?: raw
    }

    fun capped(segment: TimerSegment, now: TimerNow) =
        segment.endWall == null && segment.limitMillis != null && rawDuration(segment, now) >= segment.limitMillis

    fun endWall(segment: TimerSegment, now: TimerNow) = segment.endWall ?: (segment.startWall + duration(segment, now))

    fun sessionTotal(snapshot: TimerSnapshot, sessionId: String, now: TimerNow) =
        snapshot.segments.filter { it.sessionId == sessionId && it.active }.sumOf { duration(it, now) }

    /** Splits [start, end) at local midnights; DST is handled by converting real instants in the zone. */
    fun allocate(start: Long, end: Long, zone: ZoneId): Map<LocalDate, Long> {
        val result = linkedMapOf<LocalDate, Long>()
        var cursor = start
        while (cursor < end) {
            val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val next = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val stop = minOf(next, end)
            result[date] = (result[date] ?: 0) + (stop - cursor)
            cursor = stop
        }
        return result
    }

    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun range(period: TimerPeriod, date: LocalDate): ClosedRange<LocalDate>? = when (period) {
        TimerPeriod.TOTAL -> null
        TimerPeriod.DAY -> date..date
        TimerPeriod.WEEK -> weekStart(date)..weekStart(date).plusDays(6)
    }

    fun sourceSegments(snapshot: TimerSnapshot, kind: TimerKind, sourceId: String): List<TimerSegment> {
        val ids = snapshot.sessions.filter { it.kind == kind && it.sourceId == sourceId }.map { it.id }.toSet()
        return snapshot.segments.filter { it.active && it.sessionId in ids }
    }

    /** Credited source time, restricted to the civil period containing [date] when the period is DAY/WEEK. */
    fun progress(
        snapshot: TimerSnapshot, kind: TimerKind, sourceId: String, period: TimerPeriod, date: LocalDate,
        now: TimerNow, zone: ZoneId,
    ): Long {
        val segments = sourceSegments(snapshot, kind, sourceId)
        val range = range(period, date) ?: return segments.sumOf { duration(it, now) }
        return segments.sumOf { segment ->
            allocate(segment.startWall, endWall(segment, now), zone).filterKeys { it in range }.values.sum()
        }
    }

    fun byPeriod(segments: List<TimerSegment>, period: TimerPeriod, now: TimerNow, zone: ZoneId):
        Map<LocalDate, Long> {
        val result = sortedMapOf<LocalDate, Long>(compareByDescending { it })
        segments.filter { it.active }.forEach { segment ->
            allocate(segment.startWall, endWall(segment, now), zone).forEach { (date, millis) ->
                val key = if (period == TimerPeriod.WEEK) weekStart(date) else date
                result[key] = (result[key] ?: 0) + millis
            }
        }
        return result
    }

    /** Remaining objective time for a session, or null when it has no objective. */
    fun remaining(snapshot: TimerSnapshot, session: TimerSession, now: TimerNow, zone: ZoneId): Long? {
        session.limitMillis?.let { return (it - sessionTotal(snapshot, session.id, now)).coerceAtLeast(0) }
        val goal = snapshot.goal(session.goalKey) ?: return null
        val today = Instant.ofEpochMilli(now.wall).atZone(zone).toLocalDate()
        val done = progress(snapshot, session.kind, session.sourceId, session.period, today, now, zone)
        return (goal.minutes * 60_000L - done).coerceAtLeast(0)
    }

    fun validateManual(
        start: Long, end: Long, now: Long, bounds: TimerBounds, others: List<TimerSegment>, current: TimerNow,
        zone: ZoneId,
    ) {
        if (start >= end) throw TimerRuleException(TimerProblem.INVALID_INTERVAL)
        if (end > now) throw TimerRuleException(TimerProblem.FUTURE)
        if (end - start > MAX_MANUAL_MILLIS) throw TimerRuleException(TimerProblem.TOO_LONG)
        if (start < bounds.earliest) throw TimerRuleException(TimerProblem.BEFORE_SOURCE)
        if (allocate(start, end, zone).keys.any { !bounds.activeDate(it) }) {
            throw TimerRuleException(TimerProblem.INACTIVE)
        }
        if (others.any { it.active && it.startWall < end && start < endWall(it, current) }) {
            throw TimerRuleException(TimerProblem.OVERLAP)
        }
    }

    fun minutesText(millis: Long): Pair<Long, Long> = (millis / 3_600_000) to ((millis / 60_000) % 60)
}
