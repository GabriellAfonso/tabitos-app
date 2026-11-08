package com.ethos.app.core.reminders.domain

import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.trackers.domain.TrackerRules
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object ReminderRules {
    fun instant(date: LocalDate, minute: Int, zone: ZoneId): Long = date
        .atTime(LocalTime.of(minute / 60, minute % 60)).atZone(zone).toInstant().toEpochMilli()

    fun validate(d: ReminderDefinition) {
        require(d.id.isNotBlank() && d.sourceId.isNotBlank() && d.groupId.isNotBlank())
        require(d.minute in 0..1439 && d.weekdays in 1..127 && d.fromOrdinal > 0)
        require(d.offsetMinutes in 0..525600 && (d.referenceMinute == null || d.referenceMinute in 0..1439))
        require(d.mode != ReminderMode.CHOSEN || d.chosenDate != null)
        require(d.kind == ReminderKind.TASK || d.mode == ReminderMode.WEEKDAYS)
    }

    fun effective(defs: List<ReminderDefinition>, o: TaskOccurrence): List<ReminderDefinition> = defs
        .filter { it.kind == ReminderKind.TASK && it.sourceId == o.taskId &&
            (it.occurrenceId == o.id || (it.occurrenceId == null && it.fromOrdinal <= o.ordinal)) }
        .groupBy { it.groupId }.values.mapNotNull { group -> group
            .sortedWith(compareByDescending<ReminderDefinition> { it.occurrenceId == o.id }
                .thenByDescending { it.fromOrdinal }.thenByDescending { it.createdAt })
            .firstOrNull()?.takeIf { it.enabled } }

    fun project(sources: ReminderSources, defs: List<ReminderDefinition>, today: LocalDate): ReminderSources {
        val days = (defs.maxOfOrNull { it.offsetMinutes / 1440 } ?: 0) + 32
        val horizon = today.plusDays(days.coerceAtMost(398))
        return sources.copy(tasks = TaskRules.apply(sources.tasks, TaskAction.Refresh, horizon, 0))
    }

    fun relevant(row: ReminderDelivery, s: ReminderSources, date: LocalDate): Boolean = when (row.kind) {
        ReminderKind.TASK -> s.tasks.occurrences.any { it.id == row.sourceId && it.status == TaskStatus.OPEN }
        ReminderKind.HABIT -> {
            val h = s.habits.habits.firstOrNull { it.id == row.sourceId }
            val period = HabitRules.periods(s.habits, row.sourceId, date).firstOrNull { it.key == row.periodKey }
            h?.status == HabitStatus.ACTIVE && HabitRules.active(s.habits, row.sourceId, date) &&
                period != null && date in period.start..period.end &&
                (period.rule.type != HabitType.PRACTICE || period.count < period.target)
        }
        // A tracker slot belongs to its own civil date: past slots never stay pending (no burst).
        ReminderKind.TRACKER -> s.trackers.tracker(row.sourceId)?.let { t ->
            date == row.slotDate && TrackerRules.remindable(t, s.habits, date) &&
                !TrackerRules.hasEntry(s.trackers, t.id, row.slotDate)
        } == true
    }

    fun schedule(
        s: ReminderSources, defs: List<ReminderDefinition>, today: LocalDate, zone: ZoneId,
    ): List<ReminderDelivery> {
        val rows = mutableListOf<ReminderDelivery>()
        fun add(d: ReminderDefinition, target: String, period: String, date: LocalDate,
                anchor: LocalDate?, at: Long) {
            if (at < d.createdAt) return
            rows += ReminderDelivery("${d.groupId}|$target|$date", d.groupId, d.kind, target,
                period, date, anchor, at)
        }
        s.tasks.occurrences.filter { it.status == TaskStatus.OPEN }.forEach { o ->
            effective(defs, o).forEach { d ->
                when (d.mode) {
                    ReminderMode.CHOSEN -> {
                        val date = if (d.occurrenceId != null || o.plannedDate == null) d.chosenDate!!
                            else o.plannedDate.plusDays(d.anchorDay)
                        add(d, o.id, o.id, date, o.plannedDate, instant(date, d.minute, zone))
                    }
                    ReminderMode.RELATIVE -> {
                        val date = o.dueDate ?: return@forEach
                        val minute = o.fields.minute ?: d.referenceMinute ?: return@forEach
                        val at = instant(date, minute, zone) - d.offsetMinutes * 60000
                        add(d, o.id, o.id, Instant.ofEpochMilli(at).atZone(zone).toLocalDate(), o.plannedDate, at)
                    }
                    ReminderMode.WEEKDAYS -> {
                        val start = o.periodStart ?: o.dueDate ?: return@forEach
                        val end = o.dueDate ?: return@forEach
                        generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end }.forEach { date ->
                            if (d.weekdays and (1 shl (date.dayOfWeek.value - 1)) != 0)
                                add(d, o.id, o.id, date, o.plannedDate, instant(date, d.minute, zone))
                        }
                    }
                }
            }
        }
        defs.filter { it.enabled && it.kind == ReminderKind.HABIT }.forEach { d ->
            val h = s.habits.habits.firstOrNull { it.id == d.sourceId } ?: return@forEach
            if (h.status != HabitStatus.ACTIVE) return@forEach
            // Include this month's missed slots after long downtime; relevance discards closed periods.
            (-31L..32L).forEach { offset ->
                val date = today.plusDays(offset)
                if (d.weekdays and (1 shl (date.dayOfWeek.value - 1)) == 0 ||
                    !HabitRules.active(s.habits, h.id, date)) return@forEach
                val p = HabitRules.periods(s.habits, h.id, date).firstOrNull { date in it.start..it.end }
                    ?: return@forEach
                if (p.rule.type == HabitType.PRACTICE && p.count >= p.target) return@forEach
                add(d, h.id, p.key, date, null, instant(date, d.minute, zone))
            }
        }
        defs.filter { it.enabled && it.kind == ReminderKind.TRACKER }.forEach { d ->
            val t = s.trackers.tracker(d.sourceId) ?: return@forEach
            (0L..32L).forEach { offset ->
                val date = today.plusDays(offset)
                if (d.weekdays and (1 shl (date.dayOfWeek.value - 1)) == 0 ||
                    !TrackerRules.remindable(t, s.habits, date) || TrackerRules.hasEntry(s.trackers, t.id, date))
                    return@forEach
                add(d, t.id, date.toString(), date, null, instant(date, d.minute, zone))
            }
        }
        return rows.distinctBy { it.key }
    }

    fun summary(s: ReminderSources, date: LocalDate): DaySummary {
        val periods = s.habits.habits.flatMap { HabitRules.periods(s.habits, it.id, date) }
        return DaySummary(s.tasks.occurrences.count { it.inDay(date) },
            s.habits.habits.count { h -> h.status == HabitStatus.ACTIVE &&
                periods.any { it.habitId == h.id && date in it.start..it.end } },
            s.tasks.occurrences.count { it.needsReview(date) } + periods.count { it.outcome == PeriodOutcome.PENDING })
    }

    fun snooze(row: ReminderDelivery, at: Long, now: Long): ReminderDelivery {
        require(at > now)
        require(row.state != DeliveryState.CANCELLED)
        return row.copy(dueAt = at, snoozed = true, state = DeliveryState.PENDING)
    }

    fun reconcileRows(
        scheduled: List<ReminderDelivery>, previous: List<ReminderDelivery>, definitions: List<ReminderDefinition>,
        source: ReminderSources, now: Long, zone: ZoneId,
    ): List<ReminderDelivery> {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val old = previous.associateBy { it.key }
        val merged = scheduled.associate { row -> row.key to (old[row.key]?.let {
            when {
                it.state == DeliveryState.CANCELLED && row.dueAt > now -> row
                it.snoozed || it.state != DeliveryState.PENDING -> it
                else -> row
            }
        } ?: row) }.toMutableMap()
        previous.forEach { row -> if (row.key !in merged) {
            val exists = definitions.any { it.groupId == row.groupId && it.enabled }
            merged[row.key] = if (row.kind == ReminderKind.HABIT && exists &&
                relevant(row, source, today)) row else row.copy(state = DeliveryState.CANCELLED)
        } }
        return merged.values.map { row ->
            val checkDate = if (row.dueAt > now && !row.snoozed) row.slotDate else today
            if (!relevant(row, source, checkDate)) row.copy(state = DeliveryState.CANCELLED) else row
        }
    }
}
