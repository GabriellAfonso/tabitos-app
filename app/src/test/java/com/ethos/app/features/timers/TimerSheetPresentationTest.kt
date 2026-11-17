package com.ethos.app.features.timers

import com.ethos.app.core.timers.domain.TimerBounds
import com.ethos.app.core.timers.domain.TimerGoal
import com.ethos.app.core.timers.domain.TimerKind
import com.ethos.app.core.timers.domain.TimerNow
import com.ethos.app.core.timers.domain.TimerPeriod
import com.ethos.app.core.timers.domain.TimerSegment
import com.ethos.app.core.timers.domain.TimerSession
import com.ethos.app.core.timers.domain.TimerSnapshot
import com.ethos.app.core.timers.domain.TimerStart
import com.ethos.app.core.timers.domain.TimerState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerSheetPresentationTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val minute = 60_000L
    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()
    private val now = TimerNow(at("2026-11-17T13:10"), 0, null)
    private val start = TimerStart(TimerKind.TASK, "t", "Ler 30 minutos", goalKey = "task:t", period = TimerPeriod.DAY)
    private val target = TimerTarget(start, TimerBounds(0), canStart = true, goalAllowed = true)
    private val goal = TimerGoal("task:t", 30)

    private fun session(state: TimerState) = TimerSession("s", TimerKind.TASK, "t", "Ler 30 minutos", state,
        TimerPeriod.DAY, "task:t", created = 0)

    private fun closed(id: String, from: String, minutes: Long, manual: Boolean = false) =
        TimerSegment(id, "s", at(from), at(from) + minutes * minute, manual = manual, created = 0)

    private fun model(state: TimerState, segments: List<TimerSegment>, canStart: Boolean = true) = timerSheetModel(
        TimerUiState(snapshot = TimerSnapshot(listOf(session(state)), segments, goals = listOf(goal)), now = now,
            loading = false),
        target.copy(canStart = canStart), zone)

    @Test fun pausedSessionShowsRemainingObjectiveAndOffersResumeAndFinish() {
        val model = model(TimerState.PAUSED, listOf(closed("a", "2026-11-17T07:40", 12)))
        assertEquals(TimerSheetStatus.PAUSED, model.status)
        assertEquals(TimerPrimaryAction.RESUME, model.primary)
        assertEquals(18 * minute, model.clockMillis)
        assertEquals(TimerObjective(TimerPeriod.DAY, 12 * minute, 30 * minute), model.objective)
        assertEquals("s", model.finishSessionId)
        assertTrue(model.canAddManual)
    }

    @Test fun reachedObjectiveOffersNoMainActionNorDiscard() {
        val model = model(TimerState.PAUSED, listOf(closed("a", "2026-11-17T07:00", 30)))
        assertEquals(TimerSheetStatus.REACHED, model.status)
        assertEquals(TimerPrimaryAction.NONE, model.primary)
        assertEquals(1f, model.objective!!.fraction)
    }

    @Test fun closedSourceCannotStartOrReceiveManualTime() {
        val model = model(TimerState.PAUSED, listOf(closed("a", "2026-11-17T07:40", 12)), canStart = false)
        assertEquals(TimerPrimaryAction.NONE, model.primary)
        assertTrue(model.sourceClosed)
        assertFalse(model.canAddManual)
    }

    @Test fun recentKeepsNewestSegmentsGroupedByStartDay() {
        val segments = listOf(
            closed("old", "2026-11-15T20:00", 5), closed("y", "2026-11-16T19:00", 10, manual = true),
            closed("m", "2026-11-17T07:00", 3), closed("n", "2026-11-17T09:00", 4))
        val model = model(TimerState.PAUSED, segments)
        assertEquals(listOf(LocalDate.parse("2026-11-17"), LocalDate.parse("2026-11-16")), model.recent.map { it.date })
        assertEquals(listOf("n", "m"), model.recent[0].rows.map { it.segment.id })
        assertTrue(model.hasMore)
        assertEquals(4, model.all.sumOf { it.rows.size })
    }

    @Test fun withoutSessionOrGoalShowsElapsedAndStart() {
        val model = timerSheetModel(TimerUiState(now = now, loading = false), target, zone)
        assertEquals(TimerSheetStatus.IDLE, model.status)
        assertEquals(TimerPrimaryAction.START, model.primary)
        assertFalse(model.showsRemaining)
        assertNull(model.objective)
        assertTrue(model.recent.isEmpty())
    }
}
