package com.ethos.app.core.habits.domain

import java.time.LocalDate

enum class HabitType { PRACTICE, AVOID, LIMIT }
enum class HabitUnit { DAY, WEEK, MONTH }
enum class HabitStatus { ACTIVE, PAUSED, EXCLUDED }
enum class PeriodOutcome { OPEN, SUCCESS, FAILURE, PENDING, NEUTRAL }

data class Habit(
    val id: String, val created: LocalDate, val name: String, val description: String = "",
    val listId: String? = null, val color: Int = 0, val icon: Int = 0,
    val difficulty: Int = 1, val importance: Int = 1, val importanceManual: Boolean = false,
    val status: HabitStatus = HabitStatus.ACTIVE,
) : java.io.Serializable

data class HabitRule(
    val id: String, val habitId: String, val effective: LocalDate,
    val type: HabitType = HabitType.PRACTICE, val unit: HabitUnit = HabitUnit.DAY,
    val target: Int = 1, val weekdays: Int = 127,
) : java.io.Serializable

data class ActiveInterval(val id: String, val habitId: String, val start: LocalDate, val end: LocalDate? = null)
data class HabitOverride(val key: String, val target: Int)
data class HabitRecord(
    val id: String, val habitId: String, val date: LocalDate, val entered: Long, val deleted: Boolean = false,
)
data class HabitReview(val key: String, val outcome: PeriodOutcome, val invalidated: Boolean = false)
data class HabitAudit(
    val id: String, val habitId: String, val reference: String, val time: Long, val kind: String,
    val before: String = "", val after: String = "",
)
data class HabitSnapshot(
    val habits: List<Habit> = emptyList(), val rules: List<HabitRule> = emptyList(),
    val intervals: List<ActiveInterval> = emptyList(), val overrides: List<HabitOverride> = emptyList(),
    val records: List<HabitRecord> = emptyList(), val reviews: List<HabitReview> = emptyList(),
    val audits: List<HabitAudit> = emptyList(),
)
data class HabitPeriod(
    val key: String, val habitId: String, val rule: HabitRule, val start: LocalDate, val end: LocalDate,
    val target: Int, val count: Int, val activeDays: Int, val outcome: PeriodOutcome,
    val excessIds: List<String> = emptyList(),
)
data class HabitStreak(
    val current: Int, val best: Int, val uncertain: Boolean, val unit: HabitUnit,
    val historicalBest: Map<HabitUnit, Int> = emptyMap(),
)

sealed interface HabitAction {
    data class Save(
        val habit: Habit, val rule: HabitRule, val nextPeriod: Boolean = false,
        val partialTarget: Int? = null,
    ) : HabitAction
    data class Record(val id: String, val habitId: String, val date: LocalDate) : HabitAction
    data class SetPractice(val id: String, val habitId: String, val date: LocalDate, val done: Boolean) : HabitAction
    data class Correct(val id: String, val date: LocalDate? = null, val delete: Boolean = false) : HabitAction
    /** Legacy confirmation of a pending period; spec 014 no longer creates pending periods. */
    data class Review(val key: String) : HabitAction
    /** Settles automatically closed periods in the ledger; never changes habit data. */
    data object Settle : HabitAction
    data class Status(
        val id: String, val status: HabitStatus, val partialTarget: Int? = null,
        val listId: String? = null,
    ) : HabitAction
}

class HabitRuleException(val reason: HabitProblem) : IllegalArgumentException(reason.name)
enum class HabitProblem { INPUT, DATE, INACTIVE, CAPACITY, PARTIAL_REQUIRED, DESTINATION }
