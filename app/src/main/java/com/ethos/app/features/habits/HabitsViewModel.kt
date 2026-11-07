package com.ethos.app.features.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.tasks.domain.TaskClock
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HabitDraft(
    val habit: Habit, val rule: HabitRule, val editing: Boolean = false,
    val target: String = "1", val partial: String = "", val nextPeriod: Boolean = false,
) : java.io.Serializable
data class HabitLifecycleDraft(val id: String, val status: HabitStatus, val partial: String = "",
    val listId: String? = null, val requiresDestination: Boolean = false,
    val destinationChosen: Boolean = false) : java.io.Serializable
data class HabitsUiState(
    val snapshot: HabitSnapshot = HabitSnapshot(), val today: LocalDate = LocalDate.now(),
    val periods: List<HabitPeriod> = emptyList(), val streaks: Map<String, HabitStreak> = emptyMap(),
    val loading: Boolean = true, val busy: Boolean = false, val storageError: Boolean = false,
    val problem: HabitProblem? = null, val draft: HabitDraft? = null, val detail: String? = null,
    val recordDate: String = "", val correctionId: String? = null,
    val lifecycle: HabitLifecycleDraft? = null, val preview: HabitPeriod? = null,
    val previewReady: Boolean = false,
) {
    val pendingCount: Int get() = periods.count { it.outcome == PeriodOutcome.PENDING }
}

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repository: HabitRepository, private val clock: TaskClock, private val saved: SavedStateHandle,
) : ViewModel() {
    private val mutable = MutableStateFlow(HabitsUiState(today = clock.today(), draft = saved["habitDraft"]))
    val state: StateFlow<HabitsUiState> = mutable.asStateFlow()
    private var observer: Job? = null
    private var retryAction: HabitAction? = null
    private var readFailed = false
    private var closeAfterRetry = false
    private var lists: Set<String> = emptySet()
    init { observe() }
    private fun derive(s: HabitsUiState): HabitsUiState = s.copy(
        periods = s.snapshot.habits.flatMap { HabitRules.periods(s.snapshot, it.id, s.today) },
        streaks = s.snapshot.habits.associate { it.id to HabitRules.streak(s.snapshot, it.id, s.today) },
    )
    private fun observe() {
        observer?.cancel()
        observer = viewModelScope.launch {
            try {
                repository.observe().collect { snapshot ->
                    readFailed = false
                    mutable.update { derive(it.copy(snapshot = snapshot, loading = false,
                        storageError = retryAction != null)) }
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                readFailed = true
                mutable.update { it.copy(loading = false, storageError = true) }
            }
        }
    }
    fun refresh() { mutable.update { derive(it.copy(today = clock.today())) } }
    fun listIds(ids: Set<String>) { lists = ids }
    fun draft(value: HabitDraft?) {
        saved["habitDraft"] = value
        mutable.update { it.copy(draft = value, preview = null, previewReady = false, problem = null) }
    }
    fun newHabit(listId: String?) {
        val id = UUID.randomUUID().toString()
        draft(HabitDraft(Habit(id, clock.today(), "", listId = listId),
            HabitRule(UUID.randomUUID().toString(), id, clock.today())))
    }
    fun edit(h: Habit) {
        val r = HabitRules.currentRule(state.value.snapshot, h.id, clock.today()) ?: return
        draft(HabitDraft(h, r.copy(id = UUID.randomUUID().toString()), editing = true, target = r.target.toString()))
    }
    fun difficulty(value: Int) {
        val d = state.value.draft ?: return
        draft(d.copy(habit = d.habit.copy(difficulty = value,
            importance = if (value == 2 && !d.habit.importanceManual) 2 else d.habit.importance)))
    }
    private fun saveAction(): HabitAction.Save {
        val d = state.value.draft ?: error("Missing draft")
        return HabitAction.Save(d.habit, d.rule.copy(target = d.target.toInt()), d.nextPeriod,
            d.partial.takeIf { it.isNotBlank() }?.toInt())
    }
    fun preview() {
        try {
            val action = saveAction()
            val result = HabitRules.apply(state.value.snapshot, action, clock.today(), clock.nowMillis(), lists)
            val date = result.rules.filter { it.habitId == action.habit.id }.maxOf { it.effective }
            val p = HabitRules.periods(result, action.habit.id, maxOf(date, clock.today())).lastOrNull()
            mutable.update { it.copy(preview = p, previewReady = true, problem = null) }
        } catch (e: HabitRuleException) { mutable.update { it.copy(problem = e.reason) }
        } catch (_: Exception) { mutable.update { it.copy(problem = HabitProblem.INPUT) } }
    }
    fun save() {
        try {
            if (!state.value.previewReady) { preview(); return }
            execute(saveAction(), close = true)
        } catch (_: Exception) { mutable.update { it.copy(problem = HabitProblem.INPUT) } }
    }
    fun detail(id: String?) { mutable.update { it.copy(detail = id, recordDate = clock.today().toString(),
        correctionId = null, problem = null) } }
    fun date(value: String) { mutable.update { it.copy(recordDate = value) } }
    fun correct(record: HabitRecord) { mutable.update { it.copy(correctionId = record.id,
        recordDate = record.date.toString()) } }
    fun record() {
        try {
            val s = state.value
            val date = LocalDate.parse(s.recordDate)
            val action = s.correctionId?.let { HabitAction.Correct(it, date) }
                ?: HabitAction.Record(UUID.randomUUID().toString(), s.detail ?: return, date)
            execute(action)
        } catch (_: Exception) { mutable.update { it.copy(problem = HabitProblem.DATE) } }
    }
    fun lifecycle(value: HabitLifecycleDraft?) { mutable.update { it.copy(lifecycle = value, problem = null) } }
    fun confirmLifecycle() {
        try {
            val d = state.value.lifecycle ?: return
            if (d.requiresDestination && !d.destinationChosen) {
                mutable.update { it.copy(problem = HabitProblem.DESTINATION) }; return
            }
            execute(HabitAction.Status(d.id, d.status, d.partial.takeIf { it.isNotBlank() }?.toInt(), d.listId), true)
        } catch (_: Exception) { mutable.update { it.copy(problem = HabitProblem.INPUT) } }
    }
    fun action(action: HabitAction) { execute(action) }
    fun retry() {
        observe()
        val action = retryAction
        if (action != null) execute(action, closeAfterRetry)
    }
    private fun execute(action: HabitAction, close: Boolean = false) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, problem = null) }
        viewModelScope.launch {
            try {
                repository.execute(action)
                retryAction = null
                if (close) draft(null)
                mutable.update { it.copy(busy = false, storageError = readFailed,
                    lifecycle = if (close) null else it.lifecycle, correctionId = null) }
            } catch (e: CancellationException) { throw e
            } catch (e: HabitRuleException) {
                mutable.update { it.copy(busy = false, problem = e.reason) }
            } catch (_: Exception) {
                retryAction = action; closeAfterRetry = close
                mutable.update { it.copy(busy = false, storageError = true) }
            }
        }
    }
}
