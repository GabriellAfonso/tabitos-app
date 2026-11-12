package com.ethos.app.core.timers.domain

import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

enum class TimerKind { TASK, HABIT, REWARD }
enum class TimerState { RUNNING, PAUSED, FINISHED }

/** TOTAL accumulates every credited segment; DAY/WEEK only what was performed inside the current civil period. */
enum class TimerPeriod { TOTAL, DAY, WEEK }
enum class TimerAuditKind {
    START, PAUSE, RESUME, RECOVERED, REACHED, EXHAUSTED, MANUAL, CORRECTED, DELETED, FINISHED, CONFIRMED,
    SOURCE_CLOSED, DISCARDED,
}
enum class TimerProblem { INVALID_INTERVAL, FUTURE, TOO_LONG, BEFORE_SOURCE, INACTIVE, OVERLAP, NO_REMAINING }

class TimerRuleException(val problem: TimerProblem) : IllegalArgumentException(problem.name)

/** Monotonic elapsed time measures duration; wall time only places that duration on civil dates. */
interface TimerClock {
    fun wallMillis(): Long
    fun elapsedMillis(): Long
    fun bootCount(): Int?
    fun zone(): ZoneId
}

data class TimerNow(val wall: Long, val elapsed: Long, val boot: Int?)

fun TimerClock.now() = TimerNow(wallMillis(), elapsedMillis(), bootCount())

data class TimerSession(
    val id: String, val kind: TimerKind, val sourceId: String, val title: String,
    val state: TimerState, val period: TimerPeriod = TimerPeriod.TOTAL, val goalKey: String? = null,
    val limitMillis: Long? = null, val created: Long, val reached: Boolean = false,
)

data class TimerSegment(
    val id: String, val sessionId: String, val startWall: Long, val endWall: Long? = null,
    val startElapsed: Long? = null, val bootCount: Int? = null, val checkpointElapsed: Long? = null,
    val limitMillis: Long? = null, val manual: Boolean = false, val active: Boolean = true, val created: Long,
)

data class TimerAudit(
    val id: String, val sessionId: String, val kind: TimerAuditKind, val at: Long, val segmentId: String? = null,
    val beforeStart: Long? = null, val beforeEnd: Long? = null, val afterStart: Long? = null,
    val afterEnd: Long? = null,
)

data class TimerGoal(val sourceKey: String, val minutes: Int)

data class TimerSnapshot(
    val sessions: List<TimerSession> = emptyList(), val segments: List<TimerSegment> = emptyList(),
    val audits: List<TimerAudit> = emptyList(), val goals: List<TimerGoal> = emptyList(),
) {
    val running get() = sessions.firstOrNull { it.state == TimerState.RUNNING }
    fun open(kind: TimerKind, sourceId: String) =
        sessions.firstOrNull { it.kind == kind && it.sourceId == sourceId && it.state != TimerState.FINISHED }
    fun goal(key: String?) = goals.firstOrNull { it.sourceKey == key }
}

/** What a caller knows about a source when starting; repository re-derives remaining time inside a transaction. */
data class TimerStart(
    val kind: TimerKind, val sourceId: String, val title: String, val goalKey: String? = null,
    val period: TimerPeriod = TimerPeriod.TOTAL,
)

/** Validity of manual time for a source: earliest creation instant and civil dates on which it was active. */
data class TimerBounds(val earliest: Long, val activeDate: (java.time.LocalDate) -> Boolean = { true })

enum class StartResult { STARTED, CONFLICT, UNAVAILABLE }

data class TimerSync(val running: TimerSession?, val nextStopMillis: Long?, val alerts: List<TimerAudit>)

interface TimerRepository {
    fun observe(): Flow<TimerSnapshot>
    suspend fun start(request: TimerStart, pauseOther: Boolean): StartResult
    suspend fun pause(sessionId: String)
    suspend fun finish(sessionId: String)
    suspend fun confirmed(sessionId: String)
    /** Recovers reboots, enforces caps and pauses closed sources; returns newly raised alerts once. */
    suspend fun sync(closedSources: Set<Pair<TimerKind, String>> = emptySet()): TimerSync
    suspend fun checkpoint()
    suspend fun saveGoal(sourceKey: String, minutes: Int?)
    suspend fun addManual(request: TimerStart, start: Long, end: Long, bounds: TimerBounds)
    suspend fun correct(segmentId: String, start: Long, end: Long, bounds: TimerBounds)
    suspend fun delete(segmentId: String)
    /** Drops the time counted since the last start/resume of an open session (spec 018). */
    suspend fun discardLatest(sessionId: String)
}
