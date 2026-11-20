package com.tabitos.app.core.habits.domain

import com.tabitos.app.core.colors.ItemColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

object HabitRules {
    /** Days after a period's last day in which it still accepts records (spec 014). */
    const val LATE_DAYS = 2L
    /** A closed period is final: no records, corrections or outcome changes. */
    fun closed(end: LocalDate, today: LocalDate): Boolean = today > end.plusDays(LATE_DAYS)
    fun start(date: LocalDate, unit: HabitUnit): LocalDate = when (unit) {
        HabitUnit.DAY -> date
        HabitUnit.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        HabitUnit.MONTH -> date.withDayOfMonth(1)
    }
    fun end(date: LocalDate, unit: HabitUnit): LocalDate = when (unit) {
        HabitUnit.DAY -> date
        HabitUnit.WEEK -> start(date, unit).plusDays(6)
        HabitUnit.MONTH -> date.withDayOfMonth(date.lengthOfMonth())
    }
    fun active(s: HabitSnapshot, id: String, date: LocalDate) = s.intervals.any {
        it.habitId == id && date >= it.start && (it.end == null || date < it.end)
    }
    fun currentRule(s: HabitSnapshot, id: String, date: LocalDate) = s.rules
        .filter { it.habitId == id && it.effective <= date }.maxByOrNull { it.effective }
    fun key(rule: HabitRule, date: LocalDate) =
        "${rule.habitId}:${rule.type}:${rule.unit}:${start(date, rule.unit)}"
    private fun dates(from: LocalDate, to: LocalDate): Sequence<LocalDate> =
        generateSequence(from) { it.plusDays(1) }.takeWhile { it <= to }
    /** Daily practice only counts on its chosen weekdays; other rules accept every day. */
    fun scheduled(rule: HabitRule, date: LocalDate) = rule.type != HabitType.PRACTICE ||
        rule.unit != HabitUnit.DAY || rule.weekdays and (1 shl (date.dayOfWeek.value - 1)) != 0

    fun periods(s: HabitSnapshot, id: String, today: LocalDate): List<HabitPeriod> {
        val habit = s.habits.firstOrNull { it.id == id } ?: return emptyList()
        val revisions = s.rules.filter { it.habitId == id }.sortedBy { it.effective }
        // Indexed once per call: scanning every record and interval for each day made years of use slow (spec 031).
        val habitRecords = s.records.filter { it.habitId == id }
        val recordedDates = habitRecords.mapTo(HashSet()) { it.date }
        val liveByDate = habitRecords.filter { !it.deleted }.groupBy { it.date }
        val intervals = s.intervals.filter { it.habitId == id }
        fun activeOn(date: LocalDate) = intervals.any { date >= it.start && (it.end == null || date < it.end) }
        // The first override stored for a key wins, as before.
        val overrides = s.overrides.asReversed().associate { it.key to it.target }
        return revisions.flatMapIndexed { index, rule ->
            val last = minOf(today, revisions.getOrNull(index + 1)?.effective?.minusDays(1) ?: today)
            if (rule.effective > last) return@flatMapIndexed emptyList()
            generateSequence(start(rule.effective, rule.unit)) { end(it, rule.unit).plusDays(1) }
                .takeWhile { it <= last }.mapNotNull { from ->
                    val to = minOf(end(from, rule.unit),
                        revisions.getOrNull(index + 1)?.effective?.minusDays(1) ?: LocalDate.MAX)
                    val eligible = dates(maxOf(from, rule.effective, habit.created), to)
                        .filter { date -> (activeOn(date) || date in recordedDates) && scheduled(rule, date) }
                        .toList()
                    if (eligible.isEmpty()) return@mapNotNull null
                    val records = eligible.flatMap { liveByDate[it].orEmpty() }
                        .sortedWith(compareBy<HabitRecord> { it.date }.thenBy { it.entered }.thenBy { it.id })
                    val periodKey = key(rule, from)
                    val target = overrides[periodKey] ?: rule.target
                    val count = if (rule.type == HabitType.PRACTICE) records.map { it.date }.distinct().size
                        else records.size
                    val raw = if (rule.type == HabitType.PRACTICE) count >= target else count <= target
                    // Habits never owe anything: a period closes by itself after the late window (spec 014).
                    val outcome = when {
                        rule.type == HabitType.PRACTICE && raw -> PeriodOutcome.SUCCESS
                        !closed(to, today) -> PeriodOutcome.OPEN
                        raw -> PeriodOutcome.SUCCESS
                        else -> PeriodOutcome.FAILURE
                    }
                    HabitPeriod(periodKey, id, rule, from, to, target, count, eligible.size, outcome,
                        if (rule.type == HabitType.PRACTICE) emptyList() else records.drop(target).map { it.id })
                }.toList()
        }
    }
    fun pending(s: HabitSnapshot, today: LocalDate) = s.habits.flatMap { periods(s, it.id, today) }
        .filter { it.outcome == PeriodOutcome.PENDING }

    /**
     * Result shown to the person (streak, rate, calendar). An avoid/limit period that already ended within
     * its limit counts as success during the late window, and one already over its limit counts as failure;
     * a late record recounts it. Only [PeriodOutcome] from [periods] reaches the ledger, after closing.
     */
    fun standing(p: HabitPeriod, today: LocalDate): PeriodOutcome = when {
        p.rule.type == HabitType.PRACTICE || p.outcome != PeriodOutcome.OPEN -> p.outcome
        p.count > p.target -> PeriodOutcome.FAILURE
        p.end < today -> PeriodOutcome.SUCCESS
        else -> PeriodOutcome.OPEN
    }
    private fun decided(outcome: PeriodOutcome) = outcome == PeriodOutcome.SUCCESS || outcome == PeriodOutcome.FAILURE
    /** A practice period may still gain days in its late window, so its rate waits for closing. */
    private fun countsForRate(p: HabitPeriod, today: LocalDate) =
        decided(standing(p, today)) && (p.rule.type != HabitType.PRACTICE || closed(p.end, today))

    /** Percentage of successful periods among the last 30 days, 8 weeks or 6 months; null if none. */
    fun successRate(s: HabitSnapshot, id: String, today: LocalDate): Int? {
        val counted = periods(s, id, today).filter { countsForRate(it, today) }
        val unit = counted.lastOrNull()?.rule?.unit ?: return null
        val recent = counted.filter { it.rule.unit == unit }.takeLast(when (unit) {
            HabitUnit.DAY -> 30; HabitUnit.WEEK -> 8; HabitUnit.MONTH -> 6
        })
        return percentSucceeded(recent, today)
    }

    /**
     * Same rule as the card's [successRate], applied to the periods overlapping [range] (statistics, spec 022),
     * so both numbers come from one definition of a counted period.
     */
    fun successRate(s: HabitSnapshot, id: String, today: LocalDate, range: ClosedRange<LocalDate>): Int? =
        successRate(periods(s, id, today), today, range)

    /** [successRate] over periods already built, so statistics evaluate many ranges from one build (spec 031). */
    fun successRate(periods: List<HabitPeriod>, today: LocalDate, range: ClosedRange<LocalDate>): Int? {
        val counted = periods.filter {
            countsForRate(it, today) && it.start <= range.endInclusive && it.end >= range.start
        }
        return if (counted.isEmpty()) null else percentSucceeded(counted, today)
    }

    private fun percentSucceeded(counted: List<HabitPeriod>, today: LocalDate) =
        counted.count { standing(it, today) == PeriodOutcome.SUCCESS } * 100 / counted.size

    fun streak(s: HabitSnapshot, id: String, today: LocalDate): HabitStreak = streak(periods(s, id, today), today)

    /** [streak] over one habit's periods already built (spec 031). */
    fun streak(periods: List<HabitPeriod>, today: LocalDate): HabitStreak {
        var current = 0
        var best = 0
        var uncertain = false
        val bestByUnit = mutableMapOf<HabitUnit, Int>()
        var previous: HabitRule? = null
        periods.forEach { p ->
            if (previous != null && (previous.unit != p.rule.unit || previous.type != p.rule.type)) {
                current = 0
            }
            when (standing(p, today)) {
                PeriodOutcome.SUCCESS -> {
                    current++; best = maxOf(best, current)
                    bestByUnit[p.rule.unit] = maxOf(bestByUnit[p.rule.unit] ?: 0, current)
                }
                PeriodOutcome.FAILURE -> current = 0
                PeriodOutcome.PENDING -> { uncertain = true; current = 0 }
                else -> Unit
            }
            previous = p.rule
        }
        val unit = periods.lastOrNull()?.rule?.unit ?: HabitUnit.DAY
        return HabitStreak(current, bestByUnit[unit] ?: 0, uncertain, unit, bestByUnit.toMap())
    }

    fun apply(
        before: HabitSnapshot, action: HabitAction, today: LocalDate, now: Long, listIds: Set<String>,
    ): HabitSnapshot {
        var s = before
        fun fail(reason: HabitProblem): Nothing = throw HabitRuleException(reason)
        fun check(valid: Boolean, reason: HabitProblem = HabitProblem.INPUT) { if (!valid) fail(reason) }
        fun habit(id: String) = s.habits.firstOrNull { it.id == id } ?: fail(HabitProblem.INPUT)
        fun audit(id: String, reference: String, kind: String, old: String = "", new: String = "") {
            s = s.copy(audits = s.audits + HabitAudit(UUID.randomUUID().toString(), id, reference, now, kind, old, new))
        }
        fun override(rule: HabitRule, date: LocalDate, target: Int?) {
            if (target == null) return
            check(target >= if (rule.type == HabitType.PRACTICE) 1 else 0)
            val k = key(rule, date)
            s = s.copy(overrides = s.overrides.filterNot { it.key == k } + HabitOverride(k, target))
            val p = periods(s, rule.habitId, maxOf(today, date)).firstOrNull { it.key == k }
            if (rule.type == HabitType.PRACTICE && p != null) check(target <= p.activeDays, HabitProblem.CAPACITY)
        }
        fun open(habitId: String, date: LocalDate) {
            val unit = currentRule(s, habitId, date)?.unit ?: HabitUnit.DAY
            check(!closed(end(date, unit), today), HabitProblem.DATE)
        }
        fun dateAllowed(h: Habit, date: LocalDate) {
            check(date >= h.created && date <= today, HabitProblem.DATE)
            check(active(s, h.id, date), HabitProblem.INACTIVE)
            open(h.id, date)
        }
        /** Without an explicit partial target, a practice period never asks more than its available days. */
        fun defaultPartial(rule: HabitRule, date: LocalDate) {
            if (rule.type != HabitType.PRACTICE) return
            val p = periods(s, rule.habitId, maxOf(today, date)).firstOrNull { date in it.start..it.end } ?: return
            if (p.target > p.activeDays) override(rule, date, maxOf(1, p.activeDays))
        }
        when (action) {
            is HabitAction.SetPractice -> {
                check(currentRule(s, action.habitId, action.date)?.type == HabitType.PRACTICE)
                val records = s.records.filter {
                    it.habitId == action.habitId && it.date == action.date && !it.deleted
                }
                // Explicit desired state makes retries idempotent, including legacy duplicate records.
                return if (action.done) {
                    if (records.isNotEmpty()) s else apply(s,
                        HabitAction.Record(action.id, action.habitId, action.date), today, now, listIds)
                } else records.fold(s) { snapshot, record ->
                    apply(snapshot, HabitAction.Correct(record.id, delete = true), today, now, listIds)
                }
            }
            is HabitAction.Save -> {
                val h = action.habit
                check(h.name.isNotBlank() && ItemColors.isValid(h.color) &&
                    h.icon in 0 until Habit.ICON_COUNT && h.difficulty in 0..2 && h.importance in 0..2)
                check(h.listId == null || h.listId in listIds, HabitProblem.DESTINATION)
                val old = before.habits.firstOrNull { it.id == h.id }
                val oldRule = currentRule(s, h.id, today)
                val changedKind = oldRule != null &&
                    (oldRule.type != action.rule.type || oldRule.unit != action.rule.unit)
                val effective = if (oldRule != null && (action.nextPeriod || changedKind))
                    end(today, oldRule.unit).plusDays(1) else today
                val rule = action.rule.copy(habitId = h.id, effective = effective,
                    unit = if (action.rule.type == HabitType.AVOID) HabitUnit.DAY else action.rule.unit,
                    target = if (action.rule.type == HabitType.AVOID) 0 else action.rule.target)
                check(rule.target >= if (rule.type == HabitType.PRACTICE) 1 else 0)
                check(rule.weekdays in 1..127)
                check(rule.type != HabitType.PRACTICE || rule.unit != HabitUnit.DAY || rule.target == 1)
                check(rule.type != HabitType.PRACTICE || rule.target <= when (rule.unit) {
                    HabitUnit.DAY -> 1; HabitUnit.WEEK -> 7; HabitUnit.MONTH -> 31
                }, HabitProblem.CAPACITY)
                val saved = h.copy(name = h.name.trim(), description = h.description.trim(),
                    created = old?.created ?: today, status = old?.status ?: HabitStatus.ACTIVE)
                s = s.copy(habits = s.habits.filterNot { it.id == h.id } + saved)
                if (old == null) s = s.copy(intervals = s.intervals +
                    ActiveInterval(UUID.randomUUID().toString(), h.id, today))
                val ruleChanged = oldRule == null || oldRule.copy(id = rule.id, effective = rule.effective) != rule
                if (ruleChanged) {
                    // A current target edit applies to the entire current period, leaving earlier periods intact.
                    val from = if (oldRule != null && effective == today && !changedKind)
                        maxOf(start(today, rule.unit), oldRule.effective) else effective
                    s = s.copy(rules = s.rules.filterNot { it.habitId == h.id && it.effective >= from } +
                        rule.copy(effective = from))
                    val partial = effective != start(effective, rule.unit) &&
                        (old == null || changedKind || action.nextPeriod)
                    override(rule, effective, action.partialTarget ?: if (old != null && effective == today &&
                        !changedKind) rule.target else null)
                    if (partial && action.partialTarget == null) defaultPartial(rule, effective)
                    val p = periods(s, h.id, maxOf(today, effective)).lastOrNull()
                    if (rule.type == HabitType.PRACTICE && p != null)
                        check(p.target <= p.activeDays, HabitProblem.CAPACITY)
                }
                audit(h.id, rule.id, "SAVE")
            }
            is HabitAction.Record -> if (s.records.none { it.id == action.id }) {
                dateAllowed(habit(action.habitId), action.date)
                val period = periods(s, action.habitId, today).firstOrNull { action.date in it.start..it.end }
                if (period?.rule?.type == HabitType.PRACTICE) {
                    check(period.count < period.target, HabitProblem.CAPACITY)
                    check(period.rule.unit != HabitUnit.DAY ||
                        period.rule.weekdays and (1 shl (action.date.dayOfWeek.value - 1)) != 0)
                }
                s = s.copy(records = s.records + HabitRecord(action.id, action.habitId, action.date, now))
                audit(action.habitId, action.id, "RECORD", new = action.date.toString())
            }
            is HabitAction.Correct -> {
                val record = s.records.firstOrNull { it.id == action.id } ?: fail(HabitProblem.INPUT)
                open(record.habitId, record.date)
                if (!action.delete && action.date != null && action.date != record.date)
                    dateAllowed(habit(record.habitId), action.date)
                val replacement = record.copy(date = action.date ?: record.date, deleted = action.delete)
                s = s.copy(records = s.records.map { if (it.id == record.id) replacement else it })
                audit(record.habitId, record.id, if (action.delete) "DELETE_RECORD" else "CORRECT",
                    record.date.toString(), if (action.delete) "DELETED" else replacement.date.toString())
            }
            HabitAction.Settle -> return s
            is HabitAction.Review -> {
                val p = s.habits.asSequence().flatMap { periods(s, it.id, today).asSequence() }
                    .firstOrNull { it.key == action.key } ?: fail(HabitProblem.INPUT)
                check(p.end < today && p.outcome == PeriodOutcome.PENDING)
                val success = if (p.rule.type == HabitType.PRACTICE) p.count >= p.target else p.count <= p.target
                val result = if (success) PeriodOutcome.SUCCESS else PeriodOutcome.FAILURE
                s = s.copy(reviews = s.reviews.filterNot { it.key == p.key } + HabitReview(p.key, result))
                audit(p.habitId, p.key, "REVIEW", new = result.name)
            }
            is HabitAction.Status -> {
                val h = habit(action.id)
                if (h.status == action.status) return s
                val rule = currentRule(s, h.id, today) ?: fail(HabitProblem.INPUT)
                val destination = if (action.status == HabitStatus.ACTIVE) action.listId else h.listId
                if (action.status == HabitStatus.ACTIVE)
                    check(destination == null || destination in listIds, HabitProblem.DESTINATION)
                if (h.status == HabitStatus.ACTIVE) s = s.copy(intervals = s.intervals.map {
                    if (it.habitId == h.id && it.end == null) it.copy(end = today) else it
                })
                if (action.status == HabitStatus.ACTIVE) s = s.copy(intervals = s.intervals +
                    ActiveInterval(UUID.randomUUID().toString(), h.id, today))
                s = s.copy(habits = s.habits.map { if (it.id == h.id)
                    it.copy(status = action.status, listId = destination) else it })
                if (rule.unit != HabitUnit.DAY && today != start(today, rule.unit)) {
                    if (action.partialTarget != null) override(rule, today, action.partialTarget)
                    else if (action.status == HabitStatus.ACTIVE) defaultPartial(rule, today)
                }
                audit(h.id, h.id, action.status.name)
            }
        }
        // Compare real results, not counts: a correction within the same outcome preserves confirmation.
        val evaluated = s.habits.flatMap { periods(s, it.id, today) }.associateBy { it.key }
        s = s.copy(reviews = s.reviews.map { review ->
            val p = evaluated[review.key] ?: return@map review
            val success = if (p.rule.type == HabitType.PRACTICE) p.count >= p.target else p.count <= p.target
            val raw = if (success) PeriodOutcome.SUCCESS else PeriodOutcome.FAILURE
            if (raw != review.outcome && action !is HabitAction.Review) review.copy(invalidated = true) else review
        })
        return s
    }
}
