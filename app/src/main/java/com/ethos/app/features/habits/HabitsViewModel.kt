package com.ethos.app.features.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.accounting.domain.AccountingRepository
import com.ethos.app.core.accounting.domain.FinancialPreview
import com.ethos.app.core.timers.domain.TimerRepository
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
    /** Timed practice habit (spec 016): minutes per day or per week, depending on the rule unit. */
    val timed: Boolean = false, val durationText: String = "",
) : java.io.Serializable
data class HabitLifecycleDraft(val id: String, val status: HabitStatus, val partial: String = "",
    val listId: String? = null, val requiresDestination: Boolean = false,
    val destinationChosen: Boolean = false) : java.io.Serializable
data class HabitsUiState(
    val snapshot: HabitSnapshot = HabitSnapshot(), val today: LocalDate,
    val periods: List<HabitPeriod> = emptyList(), val streaks: Map<String, HabitStreak> = emptyMap(),
    val rates: Map<String, Int?> = emptyMap(),
    val loading: Boolean = true, val busy: Boolean = false, val storageError: Boolean = false,
    val problem: HabitProblem? = null, val draft: HabitDraft? = null, val detail: String? = null,
    val recordDate: String = "", val correctionId: String? = null,
    val lifecycle: HabitLifecycleDraft? = null, val preview: HabitPeriod? = null,
    val previewReady: Boolean = false,
    val financialPreview: FinancialPreview? = null,
    val lastPractice: HabitAction.SetPractice? = null,
    /** Duration goal in minutes per habit; a habit with a goal is the timed variant. */
    val durations: Map<String, Int> = emptyMap(),
    /** Timed habit waiting for "Concluir sem completar o tempo?". */
    val timedConfirmation: String? = null,
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repository: HabitRepository, private val clock: TaskClock, private val saved: SavedStateHandle,
    private val accounting: AccountingRepository? = null,
    private val timers: TimerRepository? = null,
) : ViewModel() {
    /** Goal to store once the habit itself is saved; null minutes removes it (normal variant). */
    private var pendingGoal: Pair<String, Int?>? = null
    private val mutable = MutableStateFlow(HabitsUiState(today = clock.today(), draft = saved["habitDraft"]))
    val state: StateFlow<HabitsUiState> = mutable.asStateFlow()
    private var observer: Job? = null
    private var retryAction: HabitAction? = null
    private var readFailed = false
    private var closeAfterRetry = false
    private var lists: Set<String> = emptySet()
    private var financialAction: HabitAction? = null
    private var financialClose = false
    fun confirmFinancial(confirm: Boolean) {
        val action = financialAction
        financialAction = null
        mutable.update { it.copy(financialPreview = null) }
        if (confirm && action != null) execute(action, financialClose, confirmed = true)
    }
    init {
        observe()
        timers?.let { repository ->
            viewModelScope.launch {
                try {
                    repository.observe().collect { snapshot ->
                        mutable.update { it.copy(durations = snapshot.goals.filter { g -> g.sourceKey.startsWith(GOAL) }
                            .associate { g -> g.sourceKey.removePrefix(GOAL) to g.minutes }) }
                    }
                } catch (e: CancellationException) { throw e
                } catch (_: Exception) { }
            }
        }
    }
    private fun derive(s: HabitsUiState): HabitsUiState = s.copy(
        periods = s.snapshot.habits.flatMap { HabitRules.periods(s.snapshot, it.id, s.today) },
        streaks = s.snapshot.habits.associate { it.id to HabitRules.streak(s.snapshot, it.id, s.today) },
        rates = s.snapshot.habits.associate { it.id to HabitRules.successRate(s.snapshot, it.id, s.today) },
    )
    private var settled = false
    /** Lets periods that closed by themselves reach the ledger (avoid/limit successes, spec 014). */
    private fun settle() {
        viewModelScope.launch {
            try { repository.execute(HabitAction.Settle)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { settled = false }
        }
    }
    private fun observe() {
        observer?.cancel()
        observer = viewModelScope.launch {
            try {
                repository.observe().collect { snapshot ->
                    readFailed = false
                    mutable.update { derive(it.copy(snapshot = snapshot, loading = false,
                        storageError = retryAction != null)) }
                    if (!settled) { settled = true; settle() }
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                readFailed = true
                mutable.update { it.copy(loading = false, storageError = true) }
            }
        }
    }
    fun refresh() {
        val day = clock.today()
        val changed = day != state.value.today
        mutable.update { derive(it.copy(today = day)) }
        if (changed && !state.value.loading) settle()
    }
    fun listIds(ids: Set<String>) { lists = ids }
    fun draft(value: HabitDraft?) {
        saved["habitDraft"] = value
        mutable.update { it.copy(draft = value, preview = null, previewReady = false, problem = null) }
    }
    /** [breaking] opens the "Quebrar hábito" group (avoid/limit) instead of practice (spec 014). */
    fun newHabit(listId: String?, breaking: Boolean = false) {
        val id = UUID.randomUUID().toString()
        draft(HabitDraft(Habit(id, clock.today(), "", listId = listId),
            HabitRule(UUID.randomUUID().toString(), id, clock.today(),
                type = if (breaking) HabitType.AVOID else HabitType.PRACTICE),
            target = if (breaking) "0" else "1"))
    }
    fun edit(h: Habit) {
        val r = HabitRules.currentRule(state.value.snapshot, h.id, clock.today()) ?: return
        val minutes = state.value.durations[h.id]
        draft(HabitDraft(h, r.copy(id = UUID.randomUUID().toString()), editing = true, target = r.target.toString(),
            timed = minutes != null, durationText = minutes?.toString().orEmpty()))
    }
    fun difficulty(value: Int) {
        val d = state.value.draft ?: return
        draft(d.copy(habit = d.habit.copy(difficulty = value,
            importance = if (value == 2 && !d.habit.importanceManual) 2 else d.habit.importance)))
    }
    private fun saveAction(): HabitAction.Save {
        val d = state.value.draft ?: error("Missing draft")
        val timed = d.timed && d.rule.type == HabitType.PRACTICE
        val minutes = if (timed) d.durationText.toInt().also { require(it in 1..MAX_MINUTES) } else null
        pendingGoal = "$GOAL${d.habit.id}" to minutes
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
    /** The habit's duration was reached: one practice on [date], undoable from the feedback (spec 016). */
    fun timerCompleted(habitId: String, date: LocalDate) {
        val exists = state.value.snapshot.records.any { it.habitId == habitId && it.date == date && !it.deleted }
        if (!exists) execute(HabitAction.SetPractice(UUID.randomUUID().toString(), habitId, date, true))
    }
    /** Practice toggles today directly; unwanted occurrences keep financial confirmation. */
    fun quickRecord(habitId: String) {
        if (state.value.busy || state.value.loading || state.value.storageError) return
        val today = clock.today()
        val snapshot = state.value.snapshot
        val practice = HabitRules.currentRule(snapshot, habitId, today)?.type == HabitType.PRACTICE
        val existing = snapshot.records.firstOrNull { it.habitId == habitId && it.date == today && !it.deleted }
        val id = UUID.randomUUID().toString()
        if (practice && existing == null && habitId in state.value.durations) {
            mutable.update { it.copy(timedConfirmation = habitId) }; return
        }
        val action = if (practice) HabitAction.SetPractice(id, habitId, today, existing == null)
            else HabitAction.Record(id, habitId, today)
        execute(action)
    }
    /** Manual practice of a timed habit, after the user accepted not finishing the time. */
    fun confirmTimed(confirm: Boolean) {
        val id = state.value.timedConfirmation ?: return
        mutable.update { it.copy(timedConfirmation = null) }
        if (confirm) execute(HabitAction.SetPractice(UUID.randomUUID().toString(), id, clock.today(), true))
    }
    fun lifecycle(value: HabitLifecycleDraft?) { mutable.update { it.copy(lifecycle = value, problem = null) } }
    fun dismissPracticeFeedback() { mutable.update { it.copy(lastPractice = null) } }
    fun undoPractice() {
        val last = state.value.lastPractice ?: return
        if (!state.value.busy) execute(last.copy(id = UUID.randomUUID().toString(), done = !last.done))
    }
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
    private fun execute(action: HabitAction, close: Boolean = false, confirmed: Boolean = false) {
        if (state.value.busy) return
        val practiceRecord = when (action) {
            is HabitAction.SetPractice -> true
            is HabitAction.Record -> HabitRules.currentRule(state.value.snapshot, action.habitId, action.date)
                ?.type == HabitType.PRACTICE
            is HabitAction.Correct -> action.delete && state.value.snapshot.records
                .firstOrNull { it.id == action.id }?.let {
                    HabitRules.currentRule(state.value.snapshot, it.habitId, it.date)?.type == HabitType.PRACTICE
                } == true
            else -> false
        }
        if (!confirmed && !practiceRecord && accounting != null && (action !is HabitAction.Status ||
                (action.status != HabitStatus.EXCLUDED && action.partialTarget != null))) {
            mutable.update { it.copy(busy = true, problem = null) }
            viewModelScope.launch {
                try {
                    val preview = accounting.previewHabit(action)
                    if (action is HabitAction.Save && preview.entries.isEmpty()) {
                        mutable.update { it.copy(busy = false) }
                        execute(action, close, confirmed = true)
                    } else {
                        financialAction = action
                        financialClose = close
                        mutable.update { it.copy(busy = false, financialPreview = preview) }
                    }
                } catch (e: CancellationException) { throw e
                } catch (e: HabitRuleException) {
                    mutable.update { it.copy(busy = false, problem = e.reason) }
                } catch (_: Exception) {
                    retryAction = action; closeAfterRetry = close
                    mutable.update { it.copy(busy = false, storageError = true) }
                }
            }
            return
        }
        mutable.update { it.copy(busy = true, problem = null) }
        viewModelScope.launch {
            try {
                repository.execute(action)
                if (action is HabitAction.Save) {
                    pendingGoal?.let { (key, minutes) -> timers?.saveGoal(key, minutes) }
                    pendingGoal = null
                }
                // Publish committed records before enabling another tap, even if observer delivery lags.
                val committed = repository.observe().first()
                mutable.update { derive(it.copy(snapshot = committed)) }
                retryAction = null
                if (close) draft(null)
                mutable.update { it.copy(busy = false, storageError = readFailed,
                    lastPractice = if (action is HabitAction.SetPractice) action else it.lastPractice,
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

/** Duration goals of habits are stored under this prefix (spec 008 source key). */
private const val GOAL = "habit:"
private const val MAX_MINUTES = 1440
