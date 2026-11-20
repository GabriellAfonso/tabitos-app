package com.tabitos.app.features.timers

import com.tabitos.app.core.habits.domain.*
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import com.tabitos.app.core.tasks.domain.TaskStatus
import com.tabitos.app.core.timers.domain.*
import java.time.LocalDate
import java.time.ZoneId

/**
 * Builds the timer sheet target from shared core snapshots. Habit duration goals only apply to practice habits
 * whose current rule is one practice per day or week, so a confirmed goal maps to exactly one practice record.
 */
fun timerTarget(
    kind: TimerKind, id: String, tasks: TaskSnapshot, habits: HabitSnapshot, timers: TimerSnapshot,
    today: LocalDate, zone: ZoneId,
): TimerTarget? = when (kind) {
    TimerKind.TASK -> tasks.occurrences.firstOrNull { it.id == id && it.status != TaskStatus.DELETED }?.let { o ->
        val created = tasks.definitions.firstOrNull { it.id == o.taskId }?.created ?: today
        TimerTarget(TimerStart(TimerKind.TASK, o.id, o.fields.title, "task:${o.taskId}"),
            TimerBounds(created.atStartOfDay(zone).toInstant().toEpochMilli()),
            canStart = o.status == TaskStatus.OPEN, goalAllowed = true, sourceDone = o.status == TaskStatus.DONE)
    }
    TimerKind.HABIT -> habits.habits.firstOrNull { it.id == id }?.let { h ->
        val rule = HabitRules.currentRule(habits, h.id, today)
        val period = when {
            rule == null || rule.type != HabitType.PRACTICE || rule.target != 1 -> TimerPeriod.TOTAL
            rule.unit == HabitUnit.DAY -> TimerPeriod.DAY
            rule.unit == HabitUnit.WEEK -> TimerPeriod.WEEK
            else -> TimerPeriod.TOTAL
        }
        val range = TimerRules.range(period, today)
        val goalKey = if (period != TimerPeriod.TOTAL) "habit:${h.id}" else null
        val earliest = h.created.atStartOfDay(zone).toInstant().toEpochMilli()
        TimerTarget(TimerStart(TimerKind.HABIT, h.id, h.name, goalKey, period),
            TimerBounds(earliest) { HabitRules.active(habits, h.id, it) },
            canStart = h.status == HabitStatus.ACTIVE && HabitRules.active(habits, h.id, today),
            goalAllowed = period != TimerPeriod.TOTAL,
            sourceDone = range != null && habits.records.any { it.habitId == h.id && !it.deleted && it.date in range })
    }
    TimerKind.REWARD -> timers.sessions.firstOrNull { it.kind == TimerKind.REWARD && it.sourceId == id }?.let {
        TimerTarget(TimerStart(TimerKind.REWARD, id, it.title), TimerBounds(0), canStart = true, goalAllowed = false)
    }
}

/** Sessions worth surfacing: not finished and whose source can still use time. */
fun visibleSessions(tasks: TaskSnapshot, habits: HabitSnapshot, state: TimerUiState, zone: ZoneId) =
    state.snapshot.sessions.filter { s ->
        s.state != TimerState.FINISHED && when (s.kind) {
            TimerKind.TASK -> tasks.occurrences.any { it.id == s.sourceId && it.status == TaskStatus.OPEN }
            TimerKind.HABIT -> habits.habits.any { it.id == s.sourceId && it.status == HabitStatus.ACTIVE }
            TimerKind.REWARD -> s.state == TimerState.RUNNING ||
                TimerRules.remaining(state.snapshot, s, state.now, zone) != 0L
        }
    }
