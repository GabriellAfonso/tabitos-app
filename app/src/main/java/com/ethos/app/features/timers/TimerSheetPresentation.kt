package com.ethos.app.features.timers

import com.ethos.app.core.timers.domain.TimerKind
import com.ethos.app.core.timers.domain.TimerPeriod
import com.ethos.app.core.timers.domain.TimerRules
import com.ethos.app.core.timers.domain.TimerSegment
import com.ethos.app.core.timers.domain.TimerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Segments shown on the sheet before "Ver tudo". */
const val RECENT_SEGMENT_LIMIT = 3

enum class TimerSheetStatus { RUNNING, PAUSED, REACHED, IDLE }

enum class TimerPrimaryAction { PAUSE, RESUME, START, NONE }

/** Progress toward the objective; [period] is null for purchased reward time. */
data class TimerObjective(val period: TimerPeriod?, val doneMillis: Long, val objectiveMillis: Long) {
    val fraction: Float get() =
        if (objectiveMillis <= 0) 1f else (doneMillis.toFloat() / objectiveMillis).coerceIn(0f, 1f)
}

/** One counted interval (from ▶ to ⏸, or typed by hand); [open] while it is still counting. */
data class TimerSegmentRow(
    val segment: TimerSegment, val startWall: Long, val endWall: Long, val open: Boolean,
) {
    val millis get() = endWall - startWall
}

data class TimerSegmentDay(val date: LocalDate, val rows: List<TimerSegmentRow>)

data class TimerSheetModel(
    val title: String, val kind: TimerKind, val sessionId: String?, val status: TimerSheetStatus,
    val primary: TimerPrimaryAction,
    val clockMillis: Long, val showsRemaining: Boolean, val objective: TimerObjective?,
    val discardSessionId: String?, val finishSessionId: String?, val canAddManual: Boolean,
    val sourceClosed: Boolean, val belowGoal: Boolean, val today: LocalDate,
    val recent: List<TimerSegmentDay>, val all: List<TimerSegmentDay>, val hasMore: Boolean,
)

/** Everything the session sheet decides, computed from timer state so the Composable only draws it. */
fun timerSheetModel(state: TimerUiState, target: TimerTarget, zone: ZoneId): TimerSheetModel {
    val request = target.start
    val snapshot = state.snapshot
    val session = snapshot.open(request.kind, request.sourceId)
    val today = Instant.ofEpochMilli(state.now.wall).atZone(zone).toLocalDate()
    val goal = snapshot.goal(request.goalKey)
    val progress = TimerRules.progress(snapshot, request.kind, request.sourceId, request.period, today,
        state.now, zone)
    val total = session?.let { TimerRules.sessionTotal(snapshot, it.id, state.now) } ?: 0L
    val remaining = session?.let { TimerRules.remaining(snapshot, it, state.now, zone) }
        ?: goal?.let { (it.minutes * 60_000L - progress).coerceAtLeast(0) }
    val done = if (goal != null) progress else total
    val running = session?.state == TimerState.RUNNING
    val reached = remaining == 0L
    val rows = TimerRules.sourceSegments(snapshot, request.kind, request.sourceId)
        .sortedByDescending { it.startWall }
        .map { TimerSegmentRow(it, it.startWall, TimerRules.endWall(it, state.now), open = it.endWall == null) }
    return TimerSheetModel(
        title = request.title, kind = request.kind, sessionId = session?.id,
        status = when {
            running -> TimerSheetStatus.RUNNING
            reached -> TimerSheetStatus.REACHED
            session != null -> TimerSheetStatus.PAUSED
            else -> TimerSheetStatus.IDLE
        },
        primary = when {
            running -> TimerPrimaryAction.PAUSE
            reached || !target.canStart -> TimerPrimaryAction.NONE
            session != null -> TimerPrimaryAction.RESUME
            else -> TimerPrimaryAction.START
        },
        clockMillis = remaining ?: done, showsRemaining = remaining != null,
        objective = remaining?.let {
            TimerObjective(request.period.takeIf { goal != null }, done, done + it)
        },
        discardSessionId = session?.takeIf { TimerRules.discardable(snapshot, it) != null }?.id,
        finishSessionId = session?.takeIf { it.kind != TimerKind.REWARD && it.state == TimerState.PAUSED }?.id,
        canAddManual = request.kind != TimerKind.REWARD && target.canStart,
        sourceClosed = !running && !reached && !target.canStart,
        belowGoal = target.sourceDone && goal != null && progress < goal.minutes * 60_000L,
        today = today,
        recent = groupByDay(rows.take(RECENT_SEGMENT_LIMIT), zone),
        all = groupByDay(rows, zone),
        hasMore = rows.size > RECENT_SEGMENT_LIMIT,
    )
}

private fun groupByDay(rows: List<TimerSegmentRow>, zone: ZoneId): List<TimerSegmentDay> =
    rows.groupBy { Instant.ofEpochMilli(it.startWall).atZone(zone).toLocalDate() }
        .map { (date, dayRows) -> TimerSegmentDay(date, dayRows) }
