package com.ethos.app.features.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethos.app.core.habits.domain.HabitRepository
import com.ethos.app.core.habits.domain.HabitSnapshot
import com.ethos.app.core.preferences.GameModeSource
import com.ethos.app.core.rewards.domain.RewardRepository
import com.ethos.app.core.rewards.domain.RewardSnapshot
import com.ethos.app.core.tasks.domain.TaskAction
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.tasks.domain.TaskRepository
import com.ethos.app.core.tasks.domain.TaskSnapshot
import com.ethos.app.core.timers.domain.TimerClock
import com.ethos.app.core.timers.domain.TimerRepository
import com.ethos.app.core.timers.domain.TimerSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the page shows for one period; computed off the main thread from the shared snapshots. */
data class StatisticsContentState(
    val period: StatisticsPeriod, val label: PeriodLabel, val hasPrevious: Boolean, val hasNext: Boolean,
    val tasks: TaskSummary, val habits: HabitSummary, val time: TimeSummary, val coins: CoinSummary?,
    val filters: List<HistoryFilter>, val history: List<HistoryRow>, val hasMore: Boolean,
)

data class StatisticsUiState(
    val loading: Boolean = true, val error: Boolean = false, val content: StatisticsContentState? = null,
    val filter: HistoryFilter = HistoryFilter.ALL, val gameMode: Boolean = true,
    /** Deleted task/habit whose dialog is open; [confirmingErase] is the irreversible confirmation step. */
    val deleted: HistorySubject? = null, val confirmingErase: Boolean = false,
    val erasing: Boolean = false, val eraseFailed: Boolean = false,
)

private data class Sources(
    val tasks: TaskSnapshot, val habits: HabitSnapshot, val timers: TimerSnapshot, val rewards: RewardSnapshot,
)

private data class Controls(
    val period: StatisticsPeriod?, val filter: HistoryFilter, val limit: Int, val gameMode: Boolean,
    val today: LocalDate,
)

/** Dialog state of erasing a deleted item's history; kept apart from the computed content. */
private data class EraseState(
    val deleted: HistorySubject? = null, val confirming: Boolean = false, val erasing: Boolean = false,
    val failed: Boolean = false,
)

private sealed interface Computation {
    data object Loading : Computation
    data object Failed : Computation
    data class Done(val controls: Controls, val content: StatisticsContentState) : Computation
}

/**
 * Activity-scoped like the other app ViewModels, but statistics are computed only while the page collects
 * [state] ([SharingStarted.WhileSubscribed]), so other pages never pay for them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val tasks: TaskRepository, private val habits: HabitRepository, private val timers: TimerRepository,
    private val rewards: RewardRepository, private val gameMode: GameModeSource, private val clock: TaskClock,
    private val timerClock: TimerClock, private val store: StatisticsPeriodStore,
) : ViewModel() {
    private val filter = MutableStateFlow(HistoryFilter.ALL)
    private val limit = MutableStateFlow(HISTORY_PAGE_SIZE)
    private val retries = MutableStateFlow(0)
    private val erase = MutableStateFlow(EraseState())

    /** The day can change while the page is open; the current period follows the injected clock. */
    private fun days() = flow { while (true) { emit(clock.today()); delay(DAY_CHECK_MILLIS) } }.distinctUntilChanged()

    private val computation: Flow<Computation> = retries.flatMapLatest {
        val sources = combine(tasks.observe(), habits.observe(), timers.observe(), rewards.observe(), ::Sources)
        val controls = combine(store.period, filter, limit, gameMode.enabled, days(), ::Controls)
        combine<Sources, Controls, Computation>(sources, controls) { s, c ->
            Computation.Done(c, compute(s, c, timerClock.zone()))
        }
            .conflate().onStart { emit(Computation.Loading) }.catch { emit(Computation.Failed) }
    }.flowOn(Dispatchers.Default)

    val state: StateFlow<StatisticsUiState> = combine(computation, erase) { result, dialog ->
        val base = StatisticsUiState(deleted = dialog.deleted, confirmingErase = dialog.confirming,
            erasing = dialog.erasing, eraseFailed = dialog.failed)
        when (result) {
            Computation.Loading -> base
            Computation.Failed -> base.copy(loading = false, error = true)
            is Computation.Done ->
                base.copy(loading = false, content = result.content, gameMode = result.controls.gameMode,
                    filter = result.controls.filter.takeIf { it in result.content.filters } ?: HistoryFilter.ALL)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StatisticsUiState())

    fun retry() { retries.update { it + 1 } }

    fun selectKind(kind: PeriodKind) { choose(StatisticsPeriod.current(kind, clock.today())) }
    fun previous() { state.value.content?.takeIf { it.hasPrevious }?.let { choose(it.period.previous()) } }
    fun next() { state.value.content?.takeIf { it.hasNext }?.let { choose(it.period.next()) } }

    private fun choose(period: StatisticsPeriod) {
        limit.value = HISTORY_PAGE_SIZE
        store.choose(period)
    }

    fun selectFilter(value: HistoryFilter) {
        limit.value = HISTORY_PAGE_SIZE
        filter.value = value
    }

    fun showMore() { limit.update { it + HISTORY_PAGE_SIZE } }

    fun openDeleted(subject: HistorySubject?) { erase.value = EraseState(deleted = subject) }

    fun askEraseConfirmation() { erase.update { it.copy(confirming = true) } }

    /** Erases the whole history of the deleted task/habit in one transaction; coins and XP stay. */
    fun confirmErase() {
        val current = erase.value
        val subject = current.deleted ?: return
        if (current.erasing) return
        erase.value = current.copy(erasing = true, failed = false)
        viewModelScope.launch {
            try {
                when (subject) {
                    is HistorySubject.Task -> tasks.execute(TaskAction.EraseHistory(subject.occurrenceId))
                    is HistorySubject.Habit -> habits.erase(subject.habitId)
                    else -> Unit
                }
                erase.value = EraseState()
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                erase.update { it.copy(erasing = false, failed = true) }
            }
        }
    }

    private companion object {
        const val DAY_CHECK_MILLIS = 60_000L
        const val STOP_TIMEOUT_MILLIS = 5_000L

        fun compute(s: Sources, c: Controls, zone: ZoneId): StatisticsContentState {
            val today = c.today
            val wallet = s.rewards.wallet
            val period = c.period ?: StatisticsPeriod.current(PeriodKind.WEEK, today)
            val firstDay = firstDataDay(s.tasks, s.habits, s.timers, wallet, zone, today)
            val range = period.range(firstDay, today)
            val credited = creditedTime(s.timers, zone)
            val filter = c.filter.takeIf { c.gameMode || it != HistoryFilter.COINS } ?: HistoryFilter.ALL
            val items = historyItems(HistorySources(s.tasks, s.habits, credited, wallet.entries, s.rewards.purchases),
                range, today, zone, c.gameMode, filter)
            val longPeriod = period.kind == PeriodKind.YEAR || period.kind == PeriodKind.ALL
            return StatisticsContentState(
                period = period, label = period.label(firstDay, today),
                hasPrevious = period.hasPrevious(firstDay, today), hasNext = period.hasNext(firstDay, today),
                tasks = taskSummary(s.tasks, range, period.bucketUnit(firstDay, today), today, zone),
                habits = habitSummary(s.habits, range, today),
                time = timeSummary(credited, range),
                coins = if (c.gameMode) coinSummary(wallet, range, zone) else null,
                filters = historyFilters(c.gameMode),
                history = historyRows(items.take(c.limit), longPeriod),
                hasMore = items.size > c.limit,
            )
        }
    }
}
