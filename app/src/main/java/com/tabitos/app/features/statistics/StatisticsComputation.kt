package com.tabitos.app.features.statistics

import com.tabitos.app.core.habits.domain.HabitPeriod
import com.tabitos.app.core.habits.domain.HabitRules
import com.tabitos.app.core.habits.domain.HabitSnapshot
import com.tabitos.app.core.rewards.domain.RewardSnapshot
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import com.tabitos.app.core.timers.domain.TimerSnapshot
import java.time.LocalDate
import java.time.ZoneId

/** The stored data the page reads. */
data class StatisticsData(
    val tasks: TaskSnapshot, val habits: HabitSnapshot, val timers: TimerSnapshot, val rewards: RewardSnapshot,
)

/**
 * The timer data statistics can show: a running stretch's periodic checkpoint never changes any number, so it is
 * cleared and repeated snapshots compare equal (spec 031). Goals and audits are not shown either.
 */
fun TimerSnapshot.forStatistics(): TimerSnapshot = TimerSnapshot(sessions,
    segments.map { if (it.checkpointElapsed == null) it else it.copy(checkpointElapsed = null) })

/** What the person chose on the page; [item] wins over [filter] when both are set (spec 024). */
data class StatisticsControls(
    val period: StatisticsPeriod? = null, val filter: HistoryFilter = HistoryFilter.ALL,
    val item: StatisticsItem? = null, val limit: Int = HISTORY_PAGE_SIZE,
)

/**
 * Everything that depends only on the stored data, the day and game mode, built once per change of those (spec
 * 031). [content] then derives the page for the chosen period, filter, item and page size without rebuilding it.
 */
class PreparedStatistics(
    private val data: StatisticsData, private val today: LocalDate, private val zone: ZoneId,
    val gameMode: Boolean,
) {
    private val wallet = data.rewards.wallet
    private val firstDay = firstDataDay(data.tasks, data.habits, data.timers, wallet, zone, today)
    private val credited = creditedTime(data.timers, zone)
    /** Each habit's periods are the costly part of habits; built once and shared by every card and range. */
    private val habitPeriods = data.habits.habits.associate { habit ->
        habit.id to HabitRules.periods(data.habits, habit.id, today)
    }
    private val periodsOf: (String) -> List<HabitPeriod> = { habitPeriods[it].orEmpty() }
    private val history = PreparedHistory.of(HistorySources(data.tasks, data.habits, credited, wallet.entries,
        data.rewards.purchases), today, zone, gameMode, periodsOf)

    fun content(controls: StatisticsControls): StatisticsContentState {
        val period = controls.period ?: StatisticsPeriod.current(PeriodKind.WEEK, today)
        val range = period.range(firstDay, today)
        val wanted = controls.item?.let { filterFor(it, data.habits, today) } ?: controls.filter
        val filter = wanted.takeIf { gameMode || it != HistoryFilter.COINS } ?: HistoryFilter.ALL
        // An item whose history was erased meanwhile no longer exists; the page falls back to its type.
        val focused = controls.item?.let { focusedItem(it, data.tasks, data.habits) }
        val item = focused?.item
        val cards = summaryCards(filter, gameMode)
        val items = if (item == null) history.items(range, filter)
            else history.items(range, HistoryFilter.ALL).filter { it.isAbout(item) }
        val longPeriod = period.kind == PeriodKind.YEAR || period.kind == PeriodKind.ALL
        return StatisticsContentState(
            period = period, label = period.label(firstDay, today),
            hasPrevious = period.hasPrevious(firstDay, today), hasNext = period.hasNext(firstDay, today),
            tasks = if (cards.tasks) taskSummary(scopedTasks(data.tasks, item), range,
                period.bucketUnit(firstDay, today), today, zone) else null,
            habits = if (cards.habits) habitSummary(data.habits, range, today,
                habitsInScope(data.habits, filter, item, today), periodsOf) else null,
            time = if (cards.time) timeSummary(scopedTime(credited, filter, item, data.tasks, data.habits, today),
                range) else null,
            coins = if (cards.coins) coinSummary(wallet, range, zone) else null,
            filters = historyFilters(gameMode),
            history = historyRows(items.take(controls.limit), longPeriod),
            hasMore = items.size > controls.limit,
            filter = filter, item = item, focused = focused,
        )
    }
}
