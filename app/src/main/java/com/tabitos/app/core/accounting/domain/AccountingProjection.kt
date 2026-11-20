package com.tabitos.app.core.accounting.domain

import com.tabitos.app.core.tasks.domain.*
import com.tabitos.app.core.habits.domain.*
import java.time.LocalDate

object AccountingProjection {
    fun practiceOrigins(
        before: HabitSnapshot, after: HabitSnapshot, action: HabitAction, today: LocalDate,
    ): Map<String, String> {
        if (action !is HabitAction.Correct || action.delete || action.date == null) return emptyMap()
        val old = before.records.firstOrNull { it.id == action.id } ?: return emptyMap()
        if (old.date == action.date) return emptyMap()
        val previousKey = "practice:${old.habitId}:${old.date}"
        val nextKey = "practice:${old.habitId}:${action.date}"
        val previous = habits(before, today).filter { it.active }.map { it.key }.toSet()
        val next = habits(after, today).filter { it.active }.map { it.key }.toSet()
        return if (previousKey in previous && previousKey !in next && nextKey !in previous && nextKey in next)
            mapOf(nextKey to previousKey) else emptyMap()
    }

    fun tasks(s: TaskSnapshot): List<FinancialOutcome> = s.occurrences
        .filter { it.status != TaskStatus.DELETED }.flatMap { o ->
            listOf(true, false).map { reward -> FinancialOutcome(
                "task:${o.id}:${if (reward) "done" else "failed"}", "task:${o.id}", o.fields.title,
                o.completedDate ?: o.dueDate ?: s.definitions.first { it.id == o.taskId }.created,
                reward, if (reward) o.fields.difficulty else o.fields.importance,
                o.status == if (reward) TaskStatus.DONE else TaskStatus.NOT_DONE,
            ) }
        }

    fun habits(s: HabitSnapshot, today: LocalDate): List<FinancialOutcome> = s.habits.flatMap { h ->
        HabitRules.periods(s, h.id, today).flatMap { p ->
            val records = s.records.filter { it.habitId == h.id && !it.deleted && it.date in p.start..p.end &&
                (HabitRules.active(s, h.id, it.date) || s.records.any { r -> r.id == it.id }) }
            val result = mutableListOf<FinancialOutcome>()
            if (p.rule.type == HabitType.PRACTICE) {
                records.filter { p.rule.unit != HabitUnit.DAY ||
                    p.rule.weekdays and (1 shl (it.date.dayOfWeek.value - 1)) != 0 }
                    .map { it.date }.distinct().sorted().take(p.target).forEach { date ->
                        result += FinancialOutcome("practice:${h.id}:$date", "habit:${h.id}", h.name,
                            date, true, h.difficulty, true)
                    }
                // Missed habits cost nothing (spec 014); only penalties confirmed before it stay in history.
                result += FinancialOutcome("period:${p.key}:failed", "habit:${h.id}", h.name, p.end,
                    false, h.importance, p.outcome == PeriodOutcome.FAILURE && s.reviews.any {
                        it.key == p.key && !it.invalidated && it.outcome == PeriodOutcome.FAILURE })
            } else {
                records.forEach { r -> result += FinancialOutcome("event:${r.id}", "habit:${h.id}",
                    h.name, r.date, false, h.importance, r.id in p.excessIds) }
                result += FinancialOutcome("period:${p.key}:success", "habit:${h.id}", h.name, p.end,
                    true, h.difficulty, p.outcome == PeriodOutcome.SUCCESS && p.end < today)
            }
            result
        }
    }
}
