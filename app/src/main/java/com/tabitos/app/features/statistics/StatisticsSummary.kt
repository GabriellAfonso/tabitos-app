package com.tabitos.app.features.statistics

import com.tabitos.app.core.accounting.domain.AccountingSnapshot
import com.tabitos.app.core.accounting.domain.Progress
import com.tabitos.app.core.habits.domain.Habit
import com.tabitos.app.core.habits.domain.HabitPeriod
import com.tabitos.app.core.habits.domain.HabitRules
import com.tabitos.app.core.habits.domain.HabitSnapshot
import com.tabitos.app.core.habits.domain.HabitStatus
import com.tabitos.app.core.habits.domain.HabitType
import com.tabitos.app.core.habits.domain.HabitUnit
import com.tabitos.app.core.tasks.domain.AuditKind
import com.tabitos.app.core.tasks.domain.DeadlineKind
import com.tabitos.app.core.tasks.domain.TaskOccurrence
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import com.tabitos.app.core.tasks.domain.TaskStatus
import com.tabitos.app.core.timers.domain.TimerKind
import com.tabitos.app.core.timers.domain.TimerRules
import com.tabitos.app.core.timers.domain.TimerSession
import com.tabitos.app.core.timers.domain.TimerSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Bucket(val start: LocalDate, val unit: BucketUnit, val count: Int, val future: Boolean)

data class TaskSummary(
    val completed: Int = 0, val onTimePercent: Int? = null, val postponed: Int = 0,
    val buckets: List<Bucket> = emptyList(),
)

data class HabitStat(
    val id: String, val name: String, val color: Int, val icon: Int, val bad: Boolean,
    val ratePercent: Int?, val streak: Int, val unit: HabitUnit,
)

data class HabitSummary(val items: List<HabitStat> = emptyList(), val overallPercent: Int? = null)

data class TimeStat(val kind: TimerKind, val sourceId: String, val title: String, val millis: Long)

data class TimeSummary(val totalMillis: Long = 0, val items: List<TimeStat> = emptyList())

data class CoinSummary(val earned: Long, val spent: Long, val progress: Progress)

/** Timer time of one session on one local day; [lastMoment] is when its latest counted stretch there ended. */
data class CreditedTime(val session: TimerSession, val day: LocalDate, val millis: Long, val lastMoment: Long)

internal fun localDay(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

private const val PERCENT = 100

/** First day with any recorded data; "Tudo" starts here and ‹ stops here. */
fun firstDataDay(
    tasks: TaskSnapshot, habits: HabitSnapshot, timers: TimerSnapshot, wallet: AccountingSnapshot,
    zone: ZoneId, today: LocalDate,
): LocalDate = listOfNotNull(
    tasks.definitions.minOfOrNull { it.created },
    tasks.history.minOfOrNull { it.recordedAt }?.let { localDay(it, zone) },
    habits.habits.minOfOrNull { it.created },
    habits.records.minOfOrNull { it.date },
    timers.segments.minOfOrNull { it.startWall }?.let { localDay(it, zone) },
    wallet.entries.minOfOrNull { it.time }?.let { localDay(it, zone) },
).minOrNull()?.let { minOf(it, today) } ?: today

private val TaskOccurrence.hasDeadline get() = fields.deadline != DeadlineKind.NONE && dueDate != null

/** A week/work-week deadline is met on any day up to the end of its window ([TaskOccurrence.dueDate]). */
private val TaskOccurrence.completedOnTime get() = completedDate != null && dueDate != null &&
    completedDate <= dueDate

fun taskSummary(
    tasks: TaskSnapshot, range: ClosedRange<LocalDate>, unit: BucketUnit, today: LocalDate, zone: ZoneId,
): TaskSummary {
    val completed = tasks.occurrences.filter {
        it.status == TaskStatus.DONE && it.completedDate != null && it.completedDate in range
    }
    val withDeadline = completed.filter { it.hasDeadline }
    val onTime = if (withDeadline.isEmpty()) null else withDeadline.count { it.completedOnTime } * PERCENT /
        withDeadline.size
    val postponed = tasks.history.count { it.kind == AuditKind.POSTPONED && localDay(it.recordedAt, zone) in range }
    val counts = completed.groupingBy { bucketStart(it.completedDate!!, unit) }.eachCount()
    val buckets = bucketStarts(range, unit).map { start ->
        Bucket(start, unit, counts[start] ?: 0, future = start > today)
    }
    return TaskSummary(completed.size, onTime, postponed, buckets)
}

fun bucketStart(date: LocalDate, unit: BucketUnit): LocalDate = when (unit) {
    BucketUnit.DAY -> date
    BucketUnit.MONTH -> date.withDayOfMonth(1)
    BucketUnit.YEAR -> date.withDayOfYear(1)
}

private fun bucketStarts(range: ClosedRange<LocalDate>, unit: BucketUnit): List<LocalDate> =
    generateSequence(bucketStart(range.start, unit)) { start ->
        when (unit) {
            BucketUnit.DAY -> start.plusDays(1)
            BucketUnit.MONTH -> start.plusMonths(1)
            BucketUnit.YEAR -> start.plusYears(1)
        }
    }.takeWhile { it <= range.endInclusive }.toList()

/**
 * Constancy and streak use the habit card's own rules, so the two screens never disagree (spec 014). [included]
 * narrows the list to the filtered type or habit (spec 024); by default excluded habits stay out.
 */
fun habitSummary(
    habits: HabitSnapshot, range: ClosedRange<LocalDate>, today: LocalDate,
    included: (Habit) -> Boolean = { it.status != HabitStatus.EXCLUDED },
    periodsOf: (String) -> List<HabitPeriod> = { HabitRules.periods(habits, it, today) },
): HabitSummary {
    val items = habits.habits.filter(included).sortedBy { it.name.lowercase() }.map { h ->
        val periods = periodsOf(h.id)
        val streak = HabitRules.streak(periods, today)
        val type = HabitRules.currentRule(habits, h.id, today)?.type ?: HabitType.PRACTICE
        HabitStat(h.id, h.name, h.color, h.icon, bad = type != HabitType.PRACTICE,
            ratePercent = HabitRules.successRate(periods, today, range), streak = streak.current,
            unit = streak.unit)
    }
    val rates = items.mapNotNull { it.ratePercent }
    return HabitSummary(items, if (rates.isEmpty()) null else rates.sum() / rates.size)
}

/**
 * Counted timer time of tasks and habits, split at local midnights. A stretch still counting is not counted yet;
 * timed rewards are not activities and stay out (spec 022).
 */
fun creditedTime(timers: TimerSnapshot, zone: ZoneId): List<CreditedTime> {
    val sessions = timers.sessions.filter { it.kind != TimerKind.REWARD }.associateBy { it.id }
    val result = linkedMapOf<Pair<String, LocalDate>, CreditedTime>()
    timers.segments.filter { it.active && it.endWall != null && it.sessionId in sessions }.forEach { segment ->
        val session = sessions.getValue(segment.sessionId)
        val counted = segment.limitMillis?.let { minOf(segment.endWall!! - segment.startWall, it) }
            ?: (segment.endWall!! - segment.startWall)
        val end = segment.startWall + counted.coerceAtLeast(0)
        TimerRules.allocate(segment.startWall, end, zone).forEach { (day, millis) ->
            val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            val key = session.id to day
            val previous = result[key]
            result[key] = CreditedTime(session, day, (previous?.millis ?: 0) + millis,
                maxOf(previous?.lastMoment ?: 0, minOf(end, dayEnd)))
        }
    }
    return result.values.toList()
}

fun timeSummary(credited: List<CreditedTime>, range: ClosedRange<LocalDate>): TimeSummary {
    val inRange = credited.filter { it.day in range }
    val items = inRange.groupBy { it.session.kind to it.session.sourceId }.map { (source, parts) ->
        val title = parts.maxBy { it.session.created }.session.title
        TimeStat(source.first, source.second, title, parts.sumOf { it.millis })
    }.sortedByDescending { it.millis }
    return TimeSummary(items.sumOf { it.millis }, items)
}

/** Coins earned are new gains of the period; spent are reward purchases. Level is the current one. */
fun coinSummary(wallet: AccountingSnapshot, range: ClosedRange<LocalDate>, zone: ZoneId): CoinSummary {
    val entries = wallet.entries.filter { localDay(it.time, zone) in range }
    val earned = entries.filter { it.kind == LEDGER_GAIN || it.kind == LEDGER_REAPPLY }.sumOf { maxOf(it.coins, 0) }
    val spent = -entries.filter { it.kind == LEDGER_PURCHASE }.sumOf { it.coins }
    return CoinSummary(earned, spent, wallet.progress)
}

/** Ledger kinds written by the accounting and reward repositories. */
internal const val LEDGER_GAIN = "GAIN"
internal const val LEDGER_REAPPLY = "REAPPLY"
internal const val LEDGER_PURCHASE = "PURCHASE"
internal const val LEDGER_PENALTY = "PENALTY"
internal const val LEDGER_REVERSAL = "REVERSAL"
internal const val LEDGER_CORRECTION = "CORRECTION"
