package com.ethos.app.features.statistics

import com.ethos.app.core.accounting.domain.AccountingSnapshot
import com.ethos.app.core.accounting.domain.LedgerEntry
import com.ethos.app.core.habits.domain.ActiveInterval
import com.ethos.app.core.habits.domain.Habit
import com.ethos.app.core.habits.domain.HabitRecord
import com.ethos.app.core.habits.domain.HabitRule
import com.ethos.app.core.habits.domain.HabitSnapshot
import com.ethos.app.core.habits.domain.HabitType
import com.ethos.app.core.rewards.domain.RewardSnapshot
import com.ethos.app.core.tasks.domain.AuditKind
import com.ethos.app.core.tasks.domain.DeadlineKind
import com.ethos.app.core.tasks.domain.RepeatKind
import com.ethos.app.core.tasks.domain.RepeatRule
import com.ethos.app.core.tasks.domain.TaskAudit
import com.ethos.app.core.tasks.domain.TaskDefinition
import com.ethos.app.core.tasks.domain.TaskFields
import com.ethos.app.core.tasks.domain.TaskOccurrence
import com.ethos.app.core.tasks.domain.TaskSnapshot
import com.ethos.app.core.tasks.domain.TaskStatus
import com.ethos.app.core.timers.domain.TimerKind
import com.ethos.app.core.timers.domain.TimerSegment
import com.ethos.app.core.timers.domain.TimerSession
import com.ethos.app.core.timers.domain.TimerSnapshot
import com.ethos.app.core.timers.domain.TimerState
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Synthetic years of daily use for measuring Estatísticas (spec 031): daily tasks done most days, habits recorded
 * most days, one timer stretch a day and a ledger entry per paid result. Seeded, so every run builds the same data.
 */
class StatisticsDataset(val today: LocalDate, val zone: ZoneId, years: Long = 5) {
    private val start = today.minusYears(years)
    private val days = generateSequence(start) { it.plusDays(1) }.takeWhile { it <= today }.toList()
    private val random = Random(SEED)
    private val ledger = mutableListOf<LedgerEntry>()

    private fun millis(day: LocalDate, hour: Int) = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun pay(key: String, title: String, day: LocalDate, at: Long) {
        val balance = (ledger.lastOrNull()?.balance ?: 0) + COINS
        ledger += LedgerEntry(ledger.size + 1L, key, title, day, at, LEDGER_GAIN, COINS, COINS, XP, balance,
            ledger.size * XP + XP)
    }

    private fun tasks(): TaskSnapshot {
        val definitions = (1..TASKS).map { n ->
            TaskDefinition("task$n", start, TaskFields(title = "Tarefa $n", deadline = DeadlineKind.DAY, date = start),
                RepeatRule(RepeatKind.DAILY, start), nextDate = today.plusDays(1), nextOrdinal = days.size + 1)
        }
        val occurrences = mutableListOf<TaskOccurrence>()
        val history = mutableListOf<TaskAudit>()
        definitions.forEach { d ->
            days.forEachIndexed { index, day ->
                val id = "${d.id}:${index + 1}"
                val done = random.nextInt(PERCENT) < DONE_PERCENT
                occurrences += TaskOccurrence(id, d.id, index + 1, d.fields.copy(date = day), day, day, day,
                    if (done) TaskStatus.DONE else TaskStatus.NOT_DONE, completedDate = if (done) day else null)
                val at = millis(day, TASK_HOUR)
                if (random.nextInt(PERCENT) < POSTPONED_PERCENT) {
                    history += TaskAudit("$id:p", id, AuditKind.POSTPONED, at - 1, day, day.minusDays(1))
                }
                history += TaskAudit("$id:r", id, if (done) AuditKind.COMPLETED else AuditKind.NOT_DONE, at,
                    if (done) day else null)
                if (done) pay("task:$id:done", d.fields.title, day, at)
            }
        }
        return TaskSnapshot(definitions = definitions, occurrences = occurrences, history = history)
    }

    private fun habits(): HabitSnapshot {
        val types = List(PRACTICE_HABITS) { HabitType.PRACTICE } + List(AVOID_HABITS) { HabitType.AVOID }
        val habits = types.indices.map { Habit("habit$it", start, "Hábito $it") }
        val rules = types.mapIndexed { i, type -> HabitRule("rule$i", "habit$i", start, type) }
        val intervals = types.indices.map { ActiveInterval("interval$it", "habit$it", start) }
        val records = mutableListOf<HabitRecord>()
        types.forEachIndexed { i, type ->
            val chance = if (type == HabitType.PRACTICE) DONE_PERCENT else SLIP_PERCENT
            days.filter { random.nextInt(PERCENT) < chance }.forEach { day ->
                val at = millis(day, HABIT_HOUR)
                records += HabitRecord("record$i:$day", "habit$i", day, at)
                if (type == HabitType.PRACTICE) pay("practice:habit$i:$day", "Hábito $i", day, at)
            }
        }
        return HabitSnapshot(habits, rules, intervals, records = records)
    }

    private fun timers(): TimerSnapshot {
        val sessions = days.mapIndexed { i, day ->
            TimerSession("session$i", TimerKind.TASK, "task1:${i + 1}", "Tarefa 1", TimerState.FINISHED,
                created = millis(day, TIMER_HOUR))
        }
        val segments = days.mapIndexed { i, day ->
            val begin = millis(day, TIMER_HOUR)
            TimerSegment("segment$i", "session$i", begin, begin + TIMER_MILLIS, created = begin)
        }
        return TimerSnapshot(sessions, segments)
    }

    /** Built in this order so ledger sequences follow the records they pay. */
    val data: StatisticsData = run {
        val tasks = tasks()
        val habits = habits()
        StatisticsData(tasks, habits, timers(), RewardSnapshot(wallet = AccountingSnapshot(entries = ledger.toList())))
    }

    private companion object {
        const val SEED = 31
        const val TASKS = 6
        const val PRACTICE_HABITS = 4
        const val AVOID_HABITS = 2
        const val PERCENT = 100
        const val DONE_PERCENT = 85
        const val POSTPONED_PERCENT = 5
        const val SLIP_PERCENT = 10
        const val TASK_HOUR = 9
        const val HABIT_HOUR = 20
        const val TIMER_HOUR = 14
        const val TIMER_MILLIS = 30 * 60_000L
        const val COINS = 10L
        const val XP = 20L
    }
}
