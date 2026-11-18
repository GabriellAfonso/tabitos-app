package com.ethos.app.features.statistics

import com.ethos.app.core.accounting.domain.LedgerEntry
import com.ethos.app.core.habits.domain.HabitPeriod
import com.ethos.app.core.habits.domain.HabitRules
import com.ethos.app.core.habits.domain.HabitSnapshot
import com.ethos.app.core.habits.domain.HabitStatus
import com.ethos.app.core.habits.domain.HabitType
import com.ethos.app.core.habits.domain.PeriodOutcome
import com.ethos.app.core.rewards.domain.Purchase
import com.ethos.app.core.tasks.domain.AuditKind
import com.ethos.app.core.tasks.domain.TaskAudit
import com.ethos.app.core.tasks.domain.TaskSnapshot
import com.ethos.app.core.tasks.domain.TaskStatus
import com.ethos.app.core.timers.domain.TimerKind
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

enum class HistoryKind {
    TASK_COMPLETED, TASK_NOT_DONE, TASK_MISSED, TASK_POSTPONED, TASK_POSTPONE_UNDONE, TASK_UNDONE,
    TASK_ADVANCED, TASK_ADVANCE_UNDONE,
    HABIT_DONE, HABIT_OCCURRENCE, HABIT_DAY_OK, HABIT_DAY_OVER, TIMER, COINS,
}

private val taskKinds = setOf(HistoryKind.TASK_COMPLETED, HistoryKind.TASK_NOT_DONE, HistoryKind.TASK_MISSED,
    HistoryKind.TASK_POSTPONED, HistoryKind.TASK_POSTPONE_UNDONE, HistoryKind.TASK_UNDONE,
    HistoryKind.TASK_ADVANCED, HistoryKind.TASK_ADVANCE_UNDONE)
/** Results of avoid/limit habits; practice habits only produce [HistoryKind.HABIT_DONE]. */
private val badHabitKinds = setOf(HistoryKind.HABIT_OCCURRENCE, HistoryKind.HABIT_DAY_OK, HistoryKind.HABIT_DAY_OVER)

val HistoryKind.isTask: Boolean get() = this in taskKinds
val HistoryKind.isBadHabit: Boolean get() = this in badHabitKinds

/** Type filters of the page (spec 024): [HABITS] are good habits, [BAD_HABITS] avoid/limit habits. */
enum class HistoryFilter { ALL, TASKS, HABITS, BAD_HABITS, TIME, COINS }

/** What a history item is about; [exists] is false once the task/habit was deleted (spec 022). */
sealed interface HistorySubject {
    /** [seriesId] groups the occurrences of a repeating task (spec 024). */
    data class Task(val occurrenceId: String, val seriesId: String, val exists: Boolean) : HistorySubject
    data class Habit(val habitId: String, val exists: Boolean) : HistorySubject
    data class Reward(val rewardId: String?) : HistorySubject
    /** A coin entry whose task/habit history was erased: its title was cleared on purpose. */
    data class Erased(val task: Boolean) : HistorySubject
    data object None : HistorySubject
}

data class HistoryItem(
    val id: String, val moment: Long, val day: LocalDate, val kind: HistoryKind, val subject: HistorySubject,
    val title: String, val from: LocalDate? = null, val to: LocalDate? = null, val millis: Long? = null,
    val coins: Long? = null, val xp: Long? = null, val adjustment: Boolean = false, val ledgerKind: String? = null,
)

/** Flat rows for the timeline: month headers only in long periods, then day headers, then items. */
sealed interface HistoryRow {
    data class MonthHeader(val month: YearMonth) : HistoryRow
    data class DayHeader(val day: LocalDate) : HistoryRow
    data class Item(val item: HistoryItem) : HistoryRow
}

data class HistorySources(
    val tasks: TaskSnapshot, val habits: HabitSnapshot, val credited: List<CreditedTime>,
    val ledger: List<LedgerEntry>, val purchases: List<Purchase>,
)

const val HISTORY_PAGE_SIZE = 50

/** Ledger entries are written in the same action as the record they pay, a few milliseconds apart. */
private const val LEDGER_MATCH_MILLIS = 5_000L

/** Filters offered on the Histórico tab; coins exist only in game mode (spec 017). */
fun historyFilters(gameMode: Boolean): List<HistoryFilter> =
    HistoryFilter.entries.filter { gameMode || it != HistoryFilter.COINS }

/**
 * Every history item of all time in display order, built once per data change (spec 031); a period, filter or item
 * only picks from it. Ledger entries are matched to their records once, whatever period or filter is shown.
 */
class PreparedHistory private constructor(
    private val all: List<HistoryItem>, private val coins: List<HistoryItem>,
) {
    fun items(range: ClosedRange<LocalDate>, filter: HistoryFilter): List<HistoryItem> =
        (if (filter == HistoryFilter.COINS) coins else all).filter { it.day in range && it.matches(filter) }

    companion object {
        fun of(
            sources: HistorySources, today: LocalDate, zone: ZoneId, gameMode: Boolean,
            periodsOf: (String) -> List<HabitPeriod> = { HabitRules.periods(sources.habits, it, today) },
        ): PreparedHistory {
            val ledger = if (gameMode) sources.ledger else emptyList()
            val coins = ledger.map { coinItem(it, sources, zone) }.sortedWith(newestFirst)
            val matcher = LedgerMatcher(ledger)
            val all = taskItems(sources.tasks, matcher, zone) + habitItems(sources.habits, matcher, today, zone, periodsOf) +
                timerItems(sources) + matcher.unused().map { coinItem(it, sources, zone) }
            return PreparedHistory(all.sortedWith(newestFirst), coins)
        }
    }
}

/** Items sit on their day; inside a day the latest moment comes first. */
private val newestFirst = compareByDescending<HistoryItem> { it.day }.thenByDescending { it.moment }

private fun HistoryItem.matches(filter: HistoryFilter) = when (filter) {
    HistoryFilter.ALL -> true
    HistoryFilter.TASKS -> kind.isTask
    HistoryFilter.HABITS -> kind == HistoryKind.HABIT_DONE
    HistoryFilter.BAD_HABITS -> kind.isBadHabit
    HistoryFilter.TIME -> kind == HistoryKind.TIMER
    HistoryFilter.COINS -> coins != null || kind == HistoryKind.COINS
}


/** Hands out each ledger entry once, to the first item that claims it. */
private class LedgerMatcher(private val entries: List<LedgerEntry>) {
    private val byKey = entries.groupBy { it.key }
    private val used = mutableSetOf<Long>()

    fun claim(keys: List<String>, moment: Long?): List<LedgerEntry> = keys.flatMap { byKey[it].orEmpty() }
        .filter { it.sequence !in used && (moment == null || abs(it.time - moment) <= LEDGER_MATCH_MILLIS) }
        .onEach { used += it.sequence }

    fun unused() = entries.filter { it.sequence !in used }
}

private fun HistoryItem.withCoins(entries: List<LedgerEntry>) =
    if (entries.isEmpty()) this else copy(coins = entries.sumOf { it.coins }, xp = entries.sumOf { it.xp })

private val shownTaskKinds = mapOf(
    AuditKind.COMPLETED to HistoryKind.TASK_COMPLETED,
    AuditKind.NOT_DONE to HistoryKind.TASK_NOT_DONE,
    AuditKind.MISSED_DEADLINE to HistoryKind.TASK_MISSED,
    AuditKind.ADVANCED to HistoryKind.TASK_ADVANCED,
    AuditKind.ADVANCE_UNDONE to HistoryKind.TASK_ADVANCE_UNDONE,
    AuditKind.POSTPONED to HistoryKind.TASK_POSTPONED,
    AuditKind.POSTPONE_UNDONE to HistoryKind.TASK_POSTPONE_UNDONE,
    AuditKind.UNDONE to HistoryKind.TASK_UNDONE,
)

private fun taskItems(tasks: TaskSnapshot, matcher: LedgerMatcher, zone: ZoneId): List<HistoryItem> {
    val occurrences = tasks.occurrences.associateBy { it.id }
    return tasks.history.mapNotNull { audit ->
        val kind = shownTaskKinds[audit.kind] ?: return@mapNotNull null
        val occurrence = occurrences[audit.occurrenceId] ?: return@mapNotNull null
        val subject = HistorySubject.Task(occurrence.id, occurrence.taskId, occurrence.status != TaskStatus.DELETED)
        val item = HistoryItem("audit:${audit.id}", audit.recordedAt, localDay(audit.recordedAt, zone), kind, subject,
            occurrence.fields.title, from = audit.from(), to = audit.to(),
            adjustment = kind == HistoryKind.TASK_POSTPONE_UNDONE || kind == HistoryKind.TASK_ADVANCE_UNDONE,
        )
        when (audit.kind) {
            AuditKind.COMPLETED, AuditKind.NOT_DONE, AuditKind.UNDONE ->
                item.withCoins(matcher.claim(listOf("task:${occurrence.id}:done", "task:${occurrence.id}:failed"),
                    audit.recordedAt))
            else -> item
        }
    }
}

private fun TaskAudit.from(): LocalDate? = when (kind) {
    AuditKind.COMPLETED -> actualDate
    AuditKind.MISSED_DEADLINE, AuditKind.POSTPONED, AuditKind.ADVANCED -> originalDeadline
    else -> null
}

private fun TaskAudit.to(): LocalDate? = when (kind) {
    AuditKind.POSTPONED, AuditKind.POSTPONE_UNDONE, AuditKind.ADVANCED, AuditKind.ADVANCE_UNDONE -> actualDate
    else -> null
}

private fun habitItems(
    habits: HabitSnapshot, matcher: LedgerMatcher, today: LocalDate, zone: ZoneId,
    periodsOf: (String) -> List<HabitPeriod>,
): List<HistoryItem> {
    val byId = habits.habits.associateBy { it.id }
    fun subject(id: String) = HistorySubject.Habit(id, byId[id]?.status?.let { it != HabitStatus.EXCLUDED } == true)
    val records = habits.records.filter { !it.deleted && it.habitId in byId }.map { record ->
        val practice = HabitRules.currentRule(habits, record.habitId, record.date)?.type == HabitType.PRACTICE
        HistoryItem("record:${record.id}", record.entered, record.date,
            if (practice) HistoryKind.HABIT_DONE else HistoryKind.HABIT_OCCURRENCE, subject(record.habitId),
            byId.getValue(record.habitId).name,
        ).withCoins(matcher.claim(listOf("practice:${record.habitId}:${record.date}", "event:${record.id}"),
            record.entered))
    }
    val settled = habits.habits.flatMap { h ->
        periodsOf(h.id).filter { it.rule.type != HabitType.PRACTICE && it.end < today }
            .mapNotNull { p ->
                val kind = when (HabitRules.standing(p, today)) {
                    PeriodOutcome.SUCCESS -> HistoryKind.HABIT_DAY_OK
                    PeriodOutcome.FAILURE -> HistoryKind.HABIT_DAY_OVER
                    else -> return@mapNotNull null
                }
                val moment = p.end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                HistoryItem("period:${p.key}", moment, p.end, kind, subject(h.id), h.name)
                    .withCoins(matcher.claim(listOf("period:${p.key}:success", "period:${p.key}:failed"), null))
            }
    }
    return records + settled
}

private fun timerItems(sources: HistorySources): List<HistoryItem> {
    val occurrences = sources.tasks.occurrences.associateBy { it.id }
    val habits = sources.habits.habits.associateBy { it.id }
    return sources.credited.map { credited ->
        val session = credited.session
        val subject = when (session.kind) {
            TimerKind.TASK -> occurrences[session.sourceId].let { occurrence ->
                HistorySubject.Task(session.sourceId, occurrence?.taskId.orEmpty(),
                    occurrence?.status?.let { it != TaskStatus.DELETED } == true)
            }
            else -> HistorySubject.Habit(session.sourceId,
                habits[session.sourceId]?.status?.let { it != HabitStatus.EXCLUDED } == true)
        }
        HistoryItem("timer:${session.id}:${credited.day}", credited.lastMoment, credited.day, HistoryKind.TIMER,
            subject, session.title, millis = credited.millis)
    }
}

private fun coinItem(entry: LedgerEntry, sources: HistorySources, zone: ZoneId): HistoryItem {
    val subject = ledgerSubject(entry, sources)
    return HistoryItem("ledger:${entry.sequence}", entry.time, localDay(entry.time, zone), HistoryKind.COINS,
        subject, entry.title, coins = entry.coins, xp = entry.xp,
        adjustment = entry.kind == LEDGER_REVERSAL || entry.kind == LEDGER_CORRECTION, ledgerKind = entry.kind)
}

/** Ledger keys: `task:<occurrence>:done|failed`, `practice:<habit>:<date>`, `event:<record>`, `period:<habit>:…`. */
private fun ledgerSubject(entry: LedgerEntry, sources: HistorySources): HistorySubject {
    val key = entry.key
    val habitId = when {
        key.startsWith(PRACTICE_KEY) -> key.removePrefix(PRACTICE_KEY).substringBeforeLast(':')
        key.startsWith(PERIOD_KEY) -> key.removePrefix(PERIOD_KEY).substringBefore(':')
        key.startsWith(EVENT_KEY) -> sources.habits.records
            .firstOrNull { it.id == key.removePrefix(EVENT_KEY) }?.habitId
        else -> null
    }
    return when {
        key.startsWith(PURCHASE_KEY) -> HistorySubject.Reward(
            sources.purchases.firstOrNull { it.ledgerSequence == entry.sequence }?.reward?.id)
        entry.title.isBlank() -> HistorySubject.Erased(task = key.startsWith(TASK_KEY))
        key.startsWith(TASK_KEY) -> {
            val id = key.removePrefix(TASK_KEY).substringBeforeLast(':')
            val occurrence = sources.tasks.occurrences.firstOrNull { it.id == id }
            HistorySubject.Task(id, occurrence?.taskId.orEmpty(),
                occurrence?.status?.let { it != TaskStatus.DELETED } == true)
        }
        habitId != null -> HistorySubject.Habit(habitId,
            sources.habits.habits.any { it.id == habitId && it.status != HabitStatus.EXCLUDED })
        else -> HistorySubject.None
    }
}

private const val TASK_KEY = "task:"
private const val PRACTICE_KEY = "practice:"
private const val PERIOD_KEY = "period:"
private const val EVENT_KEY = "event:"
private const val PURCHASE_KEY = "purchase:"

/** Newest day first; month headers when the period spans months (year and "Tudo"). */
fun historyRows(items: List<HistoryItem>, monthHeaders: Boolean): List<HistoryRow> {
    val rows = mutableListOf<HistoryRow>()
    var month: YearMonth? = null
    var day: LocalDate? = null
    items.sortedWith(newestFirst).forEach { item ->
        val itemMonth = YearMonth.from(item.day)
        if (monthHeaders && itemMonth != month) { rows += HistoryRow.MonthHeader(itemMonth); month = itemMonth }
        if (item.day != day) { rows += HistoryRow.DayHeader(item.day); day = item.day }
        rows += HistoryRow.Item(item)
    }
    return rows
}
