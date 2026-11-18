package com.ethos.app.features.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethos.app.core.habits.domain.HabitRepository
import com.ethos.app.core.preferences.GameModeSource
import com.ethos.app.core.rewards.domain.RewardRepository
import com.ethos.app.core.tasks.domain.TaskAction
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.tasks.domain.TaskRepository
import com.ethos.app.core.timers.domain.TimerClock
import com.ethos.app.core.timers.domain.TimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the page shows for one period; computed off the main thread from the shared snapshots. */
data class StatisticsContentState(
    val period: StatisticsPeriod, val label: PeriodLabel, val hasPrevious: Boolean, val hasNext: Boolean,
    /** Resumo cards; null when the chosen filter hides the card (spec 024). */
    val tasks: TaskSummary?, val habits: HabitSummary?, val time: TimeSummary?, val coins: CoinSummary?,
    val filters: List<HistoryFilter>, val history: List<HistoryRow>, val hasMore: Boolean,
    /** Filter and item in force after checking them against the data; both tabs use them. */
    val filter: HistoryFilter = HistoryFilter.ALL, val item: StatisticsItem? = null,
    /** Item the page is focused on, shown as a removable chip; null shows the whole type. */
    val focused: FocusedItem? = null,
)

data class StatisticsUiState(
    val loading: Boolean = true, val error: Boolean = false, val content: StatisticsContentState? = null,
    val filter: HistoryFilter = HistoryFilter.ALL, val item: StatisticsItem? = null,
    val tab: StatisticsTab = StatisticsTab.SUMMARY, val gameMode: Boolean = true,
    /** Deleted task/habit whose dialog is open; [confirmingErase] is the irreversible confirmation step. */
    val deleted: HistorySubject? = null, val confirmingErase: Boolean = false,
    val erasing: Boolean = false, val eraseFailed: Boolean = false,
)

/** What the person chose; [item] wins over [filter] when both are set (spec 024). */
private data class Selection(val filter: HistoryFilter = HistoryFilter.ALL, val item: StatisticsItem? = null)

/** Dialog state of erasing a deleted item's history; kept apart from the computed content. */
private data class EraseState(
    val deleted: HistorySubject? = null, val confirming: Boolean = false, val erasing: Boolean = false,
    val failed: Boolean = false,
)

private sealed interface Computation {
    data object Loading : Computation
    data object Failed : Computation
    data class Done(val gameMode: Boolean, val content: StatisticsContentState) : Computation
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
    private val selection = MutableStateFlow(Selection())
    private val tab = MutableStateFlow(StatisticsTab.SUMMARY)
    private val limit = MutableStateFlow(HISTORY_PAGE_SIZE)
    private val retries = MutableStateFlow(0)
    private val erase = MutableStateFlow(EraseState())

    /** The day can change while the page is open; the current period follows the injected clock. */
    private fun days() = flow { while (true) { emit(clock.today()); delay(DAY_CHECK_MILLIS) } }.distinctUntilChanged()

    private val computation: Flow<Computation> = retries.flatMapLatest {
        // Timer checkpoints change no number, so they are dropped before any work (spec 031).
        val statisticsTimers = timers.observe().map { it.forStatistics() }.distinctUntilChanged()
        val data = combine(tasks.observe(), habits.observe(), statisticsTimers, rewards.observe(), ::StatisticsData)
        // The heavy part follows data, day and game mode only; period, filter and page size reuse it.
        val prepared = combine(data, gameMode.enabled, days()) { d, game, today ->
            PreparedStatistics(d, today, timerClock.zone(), game)
        }.conflate()
        val controls = combine(store.period, selection, limit) { period, chosen, size ->
            StatisticsControls(period, chosen.filter, chosen.item, size)
        }
        combine<PreparedStatistics, StatisticsControls, Computation>(prepared, controls) { p, c ->
            Computation.Done(p.gameMode, p.content(c))
        }
            .conflate().onStart { emit(Computation.Loading) }.catch { emit(Computation.Failed) }
    }.flowOn(Dispatchers.Default)

    val state: StateFlow<StatisticsUiState> = combine(computation, erase, tab) { result, dialog, shownTab ->
        val base = StatisticsUiState(deleted = dialog.deleted, confirmingErase = dialog.confirming,
            erasing = dialog.erasing, eraseFailed = dialog.failed, tab = shownTab)
        when (result) {
            Computation.Loading -> base
            Computation.Failed -> base.copy(loading = false, error = true)
            is Computation.Done ->
                base.copy(loading = false, content = result.content, gameMode = result.gameMode,
                    filter = result.content.filter, item = result.content.item)
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

    /** Changing the type always goes back to "Todos" of that type. */
    fun selectFilter(value: HistoryFilter) {
        limit.value = HISTORY_PAGE_SIZE
        selection.value = Selection(filter = value)
    }

    fun selectItem(item: StatisticsItem?) {
        limit.value = HISTORY_PAGE_SIZE
        selection.update { it.copy(item = item) }
    }

    fun selectTab(value: StatisticsTab) { tab.value = value }

    /** "Ver histórico" of a deleted item's entry: its whole history instead of the erase dialog. */
    fun viewDeletedHistory() {
        val item = when (val subject = erase.value.deleted) {
            is HistorySubject.Task -> StatisticsItem.TaskSeries(subject.seriesId)
            is HistorySubject.Habit -> StatisticsItem.Habit(subject.habitId)
            else -> return
        }
        erase.value = EraseState()
        focus(item)
    }

    /** "Ver histórico" from a task or habit popup: Histórico of that item (spec 024). */
    fun focus(item: StatisticsItem) {
        limit.value = HISTORY_PAGE_SIZE
        selection.value = Selection(item = item)
        tab.value = StatisticsTab.HISTORY
    }

    /** Filter, item and tab are only kept while the page is open (spec 024). */
    fun leave() {
        limit.value = HISTORY_PAGE_SIZE
        selection.value = Selection()
        tab.value = StatisticsTab.SUMMARY
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
    }
}
