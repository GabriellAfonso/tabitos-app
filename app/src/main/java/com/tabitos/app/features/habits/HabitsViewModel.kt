package com.tabitos.app.features.habits

import com.tabitos.app.core.ui.accounting.FinancialConfirmationKind
import com.tabitos.app.core.ui.accounting.financialConfirmationKind
import com.tabitos.app.core.ui.components.RED_ITEM_COLOR
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabitos.app.core.startup.StartupSequence
import com.tabitos.app.core.habits.domain.*
import com.tabitos.app.core.tasks.domain.TaskClock
import com.tabitos.app.core.accounting.domain.AccountingRepository
import com.tabitos.app.core.accounting.domain.FinancialPreview
import com.tabitos.app.core.preferences.GameModeSource
import com.tabitos.app.core.timers.domain.TimerRepository
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
    val destinationChosen: Boolean = false,
    /** Only for excluding: delete the habit completely with its history instead (spec 022). */
    val eraseHistory: Boolean = false) : java.io.Serializable
/** Manual practice of a timed habit on [date], waiting for confirmation (spec 016). */
data class TimedPractice(val habitId: String, val date: LocalDate)
data class HabitsUiState(
    val snapshot: HabitSnapshot = HabitSnapshot(), val today: LocalDate,
    val periods: List<HabitPeriod> = emptyList(), val streaks: Map<String, HabitStreak> = emptyMap(),
    val rates: Map<String, Int?> = emptyMap(),
    /** Daily result per habit and date as shown in its calendar ([HabitRules.standing]). */
    val dayStandings: Map<String, Map<LocalDate, PeriodOutcome>> = emptyMap(),
    val loading: Boolean = true, val busy: Boolean = false, val storageError: Boolean = false,
    val problem: HabitProblem? = null, val draft: HabitDraft? = null, val detail: String? = null,
    /** Day of a bad habit whose record count is being corrected in the popup's day sheet (spec 023). */
    val dayEditor: LocalDate? = null,
    val lifecycle: HabitLifecycleDraft? = null, val preview: HabitPeriod? = null,
    val previewReady: Boolean = false,
    val financialPreview: FinancialPreview? = null,
    /** Names the coin confirmation dialog; only meaningful while [financialPreview] is set. */
    val financialKind: FinancialConfirmationKind = FinancialConfirmationKind.OTHER,
    /** Duration goal in minutes per habit; a habit with a goal is the timed variant. */
    val durations: Map<String, Int> = emptyMap(),
    /** Timed habit day waiting for "Concluir sem completar o tempo?". */
    val timedConfirmation: TimedPractice? = null,
    /** Game mode (spec 017): off hides difficulty/estimates and skips financial confirmations. */
    val gameMode: Boolean = GameModeSource.DEFAULT_ENABLED,
    /** Excluded habit waiting for "Apagar por completo" to be confirmed (spec 022). */
    val eraseConfirmation: String? = null,
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repository: HabitRepository, private val clock: TaskClock, private val saved: SavedStateHandle,
    private val accounting: AccountingRepository? = null,
    private val timers: TimerRepository? = null,
    private val gameMode: GameModeSource? = null,
    private val startup: StartupSequence = StartupSequence.released(),
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
        gameMode?.let { source ->
            viewModelScope.launch {
                try { source.enabled.collect { on -> mutable.update { it.copy(gameMode = on) } }
                } catch (e: CancellationException) { throw e
                } catch (_: Exception) { }
            }
        }
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
    private fun derive(s: HabitsUiState): HabitsUiState {
        val periods = s.snapshot.habits.flatMap { HabitRules.periods(s.snapshot, it.id, s.today) }
        return s.copy(periods = periods, dayStandings = dayStandings(periods, s.today),
            streaks = s.snapshot.habits.associate { it.id to HabitRules.streak(s.snapshot, it.id, s.today) },
            rates = s.snapshot.habits.associate { it.id to HabitRules.successRate(s.snapshot, it.id, s.today) })
    }
    private fun dayStandings(periods: List<HabitPeriod>, today: LocalDate) = periods
        .filter { it.rule.unit == HabitUnit.DAY }.groupBy { it.habitId }
        .mapValues { (_, days) -> days.associate { it.start to HabitRules.standing(it, today) } }
    private var settled = false
    /**
     * Lets periods that closed by themselves reach the ledger (avoid/limit successes, spec 014). Waits for Hoje's
     * first content so its write never delays Hoje's reads on a cold open (spec 027).
     */
    private fun settle() {
        viewModelScope.launch {
            try {
                startup.awaitTodayShown()
                repository.execute(HabitAction.Settle)
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
        val habit = Habit(id, clock.today(), "", listId = listId)
        draft(HabitDraft(if (breaking) habit.copy(color = RED_ITEM_COLOR, icon = BLOCK_HABIT_ICON) else habit,
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
    fun detail(id: String?) { mutable.update { it.copy(detail = id, dayEditor = null, problem = null) } }
    /** Marks or unmarks a practice day from the popup's week strip; timed habits confirm first (spec 023). */
    fun setPractice(habitId: String, date: LocalDate, done: Boolean) {
        if (done && habitId in state.value.durations) {
            mutable.update { it.copy(timedConfirmation = TimedPractice(habitId, date)) }; return
        }
        execute(HabitAction.SetPractice(UUID.randomUUID().toString(), habitId, date, done))
    }
    fun editDay(date: LocalDate?) { mutable.update { it.copy(dayEditor = date, problem = null) } }
    fun addOccurrence(habitId: String, date: LocalDate) {
        execute(HabitAction.Record(UUID.randomUUID().toString(), habitId, date))
    }
    fun removeOccurrence(habitId: String, date: LocalDate) {
        val record = latestRecord(state.value.snapshot.records, habitId, date) ?: return
        execute(HabitAction.Correct(record.id, delete = true))
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
            mutable.update { it.copy(timedConfirmation = TimedPractice(habitId, today)) }; return
        }
        val action = if (practice) HabitAction.SetPractice(id, habitId, today, existing == null)
            else HabitAction.Record(id, habitId, today)
        execute(action)
    }
    /** Manual practice of a timed habit, after the user accepted not finishing the time. */
    fun confirmTimed(confirm: Boolean) {
        val pending = state.value.timedConfirmation ?: return
        mutable.update { it.copy(timedConfirmation = null) }
        if (confirm) execute(HabitAction.SetPractice(UUID.randomUUID().toString(), pending.habitId, pending.date, true))
    }
    fun lifecycle(value: HabitLifecycleDraft?) { mutable.update { it.copy(lifecycle = value, problem = null) } }
    fun confirmLifecycle() {
        try {
            val d = state.value.lifecycle ?: return
            if (d.requiresDestination && !d.destinationChosen) {
                mutable.update { it.copy(problem = HabitProblem.DESTINATION) }; return
            }
            if (d.status == HabitStatus.EXCLUDED && d.eraseHistory) { erase(d.id); return }
            execute(HabitAction.Status(d.id, d.status, d.partial.takeIf { it.isNotBlank() }?.toInt(), d.listId), true)
        } catch (_: Exception) { mutable.update { it.copy(problem = HabitProblem.INPUT) } }
    }
    fun action(action: HabitAction) { execute(action) }
    fun requestErase(id: String?) { mutable.update { it.copy(eraseConfirmation = id) } }

    /** Deletes the habit with its history in one transaction; coins and XP stay (spec 022). */
    fun erase(id: String) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, problem = null) }
        viewModelScope.launch {
            try {
                repository.erase(id)
                mutable.update { it.copy(busy = false, lifecycle = null, eraseConfirmation = null,
                    detail = if (it.detail == id) null else it.detail) }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                mutable.update { it.copy(busy = false, storageError = true) }
            }
        }
    }
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
        val hasFinancialEffect = action !is HabitAction.Status ||
            (action.status != HabitStatus.EXCLUDED && action.partialTarget != null)
        val asksFinancialConfirmation = !practiceRecord && accounting != null && state.value.gameMode &&
            hasFinancialEffect
        if (!confirmed && asksFinancialConfirmation) {
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
                        mutable.update { it.copy(busy = false, financialPreview = preview,
                            financialKind = financialConfirmationKind(action)) }
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
                    lifecycle = if (close) null else it.lifecycle) }
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
