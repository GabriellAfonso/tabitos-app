package com.ethos.app.features.trackers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethos.app.core.habits.domain.HabitRepository
import com.ethos.app.core.habits.domain.HabitSnapshot
import com.ethos.app.core.reminders.domain.*
import com.ethos.app.core.reminders.platform.ReminderCoordinator
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.trackers.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TrackerFormDraft(
    val trackerId: String, val habitId: String, val multiple: Boolean = false,
    val questions: List<TrackerQuestion> = emptyList(), val reminder: String = "", val editing: Boolean = false,
) : java.io.Serializable

data class TrackerEntryDraft(
    val entryId: String, val trackerId: String, val date: LocalDate, val version: Int,
    val answers: List<TrackerAnswer> = emptyList(), val editing: Boolean = false,
) : java.io.Serializable {
    fun answer(id: String) = answers.firstOrNull { it.questionId == id }
}

data class TrackerUiState(
    val snapshot: TrackerSnapshot = TrackerSnapshot(), val habits: HabitSnapshot = HabitSnapshot(),
    val reminderMinutes: Map<String, Int> = emptyMap(), val today: LocalDate,
    val loading: Boolean = true, val busy: Boolean = false, val storageError: Boolean = false,
    val problem: TrackerProblem? = null, val problemQuestion: String? = null, val hubHabitId: String? = null,
    val form: TrackerFormDraft? = null, val entry: TrackerEntryDraft? = null, val deleteEntryId: String? = null,
    val period: Int = 7, val dateText: String = "", val pending: List<Tracker> = emptyList(),
    val analytics: TrackerAnalytics? = null,
) {
    val hubTracker: Tracker? get() = snapshot.forHabit(hubHabitId)
    val hubHabitName: String get() = habits.habits.firstOrNull { it.id == hubHabitId }?.name.orEmpty()
    fun editable(t: Tracker) = TrackerRules.editable(t, habits)
    fun fillable(t: Tracker, date: LocalDate) = TrackerRules.fillable(t, habits, date, today)
}

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val repository: TrackerRepository, private val habits: HabitRepository,
    private val reminders: ReminderRepository, private val coordinator: ReminderCoordinator,
    private val clock: TaskClock, private val saved: SavedStateHandle,
) : ViewModel() {
    private val mutable = MutableStateFlow(TrackerUiState(today = clock.today(), hubHabitId = saved["trackerHub"],
        form = saved["trackerForm"], entry = saved["trackerEntry"], dateText = clock.today().toString()))
    val state: StateFlow<TrackerUiState> = mutable.asStateFlow()
    private var observer: Job? = null
    private var definitions: List<ReminderDefinition> = emptyList()
    private var retryAction: (suspend () -> Unit)? = null

    init { observe() }

    private fun minuteNow(): Int = Instant.ofEpochMilli(clock.nowMillis()).atZone(ZoneId.systemDefault()).toLocalTime()
        .let { it.hour * 60 + it.minute }

    private fun derive(s: TrackerUiState): TrackerUiState {
        val minutes = definitions.filter { it.kind == ReminderKind.TRACKER && it.enabled }
            .associate { it.sourceId to it.minute }
        val hub = s.snapshot.forHabit(s.hubHabitId)
        return s.copy(reminderMinutes = minutes,
            pending = TrackerRules.pending(s.snapshot, s.habits, minutes, s.today, minuteNow()),
            analytics = hub?.let { TrackerRules.analytics(s.snapshot, it.id, s.today, s.period) })
    }

    private fun observe() {
        observer?.cancel()
        observer = viewModelScope.launch {
            combine(repository.observe(), habits.observe(), reminders.observe()) { t, h, r -> Triple(t, h, r) }
                .catch { mutable.update { it.copy(loading = false, storageError = true) } }
                .collect { (t, h, r) ->
                    definitions = r.definitions
                    mutable.update { derive(it.copy(snapshot = t, habits = h, loading = false,
                        storageError = retryAction != null)) }
                }
        }
    }

    fun refresh() { mutable.update { derive(it.copy(today = clock.today())) } }
    private fun problem(p: TrackerProblem?, q: String? = null) =
        mutable.update { it.copy(problem = p, problemQuestion = q) }

    fun open(habitId: String?) {
        saved["trackerHub"] = habitId
        mutable.update { derive(it.copy(hubHabitId = habitId, problem = null, period = 7,
            dateText = clock.today().toString())) }
    }
    fun period(days: Int) { mutable.update { derive(it.copy(period = days)) } }
    fun dateText(value: String) { mutable.update { it.copy(dateText = value) } }

    fun form(value: TrackerFormDraft?) {
        saved["trackerForm"] = value
        mutable.update { it.copy(form = value, problem = null, problemQuestion = null) }
    }
    fun editForm(habitId: String) {
        val s = state.value
        val t = s.snapshot.forHabit(habitId)
        val minute = t?.let { s.reminderMinutes[it.id] }
        form(if (t == null) TrackerFormDraft(UUID.randomUUID().toString(), habitId)
            else TrackerFormDraft(t.id, habitId, t.multiple, s.snapshot.current(t.id)?.questions.orEmpty(),
                minute?.let { LocalTime.of(it / 60, it % 60).toString() }.orEmpty(), editing = true))
    }
    fun addQuestion(type: QuestionType, multiple: Boolean = false) {
        val d = state.value.form ?: return
        val options = if (type == QuestionType.CHOICE) listOf(newOption()) else emptyList()
        form(d.copy(questions = d.questions + TrackerQuestion(UUID.randomUUID().toString(), type, "",
            multiple = multiple, options = options)))
    }
    fun newOption() = TrackerOption(UUID.randomUUID().toString(), "")

    fun saveForm(onReminder: () -> Unit = {}) {
        val d = state.value.form ?: return
        val minute = try {
            d.reminder.takeIf { it.isNotBlank() }?.let { LocalTime.parse(it).let { t -> t.hour * 60 + t.minute } }
        } catch (_: Exception) { problem(TrackerProblem.QUESTION, "reminder"); return }
        write {
            repository.execute(TrackerAction.SaveForm(d.trackerId, d.habitId, d.multiple, d.questions))
            val id = "tracker:${d.trackerId}"
            val old = definitions.firstOrNull { it.id == id }
            if (minute != null) {
                if (old?.enabled != true || old.minute != minute) {
                    reminders.save(ReminderDefinition(id, kind = ReminderKind.TRACKER, sourceId = d.trackerId,
                        mode = ReminderMode.WEEKDAYS, minute = minute, createdAt = clock.nowMillis()))
                }
                onReminder()
            } else if (old?.enabled == true) reminders.remove(old)
            coordinator.reconcileSafely(false)
            form(null)
        }
    }
    fun setActive(trackerId: String, active: Boolean) = write {
        repository.execute(TrackerAction.SetActive(trackerId, active)); coordinator.reconcileSafely(false)
    }

    fun fill(trackerId: String, date: LocalDate) {
        val s = state.value
        val t = s.snapshot.tracker(trackerId) ?: return
        if (!s.fillable(t, date)) { problem(TrackerProblem.DATE); return }
        val existing = if (t.multiple) null else TrackerRules.entryFor(s.snapshot, t.id, date)
        if (existing != null) { editEntry(existing.id); return }
        entry(TrackerEntryDraft(UUID.randomUUID().toString(), t.id, date, t.version))
    }
    fun fillTyped(trackerId: String) {
        val date = try { LocalDate.parse(state.value.dateText.trim()) } catch (_: Exception) {
            problem(TrackerProblem.DATE); return }
        fill(trackerId, date)
    }
    fun editEntry(entryId: String) {
        val e = state.value.snapshot.entries.firstOrNull { it.id == entryId } ?: return
        entry(TrackerEntryDraft(e.id, e.trackerId, e.date, e.version, e.answers, editing = true))
    }
    fun entry(value: TrackerEntryDraft?) {
        saved["trackerEntry"] = value
        mutable.update { it.copy(entry = value, problem = null, problemQuestion = null) }
    }
    fun answer(value: TrackerAnswer) {
        val d = state.value.entry ?: return
        entry(d.copy(answers = d.answers.filterNot { it.questionId == value.questionId } + value))
    }
    fun saveEntry() {
        val d = state.value.entry ?: return
        write {
            repository.execute(TrackerAction.SaveEntry(d.entryId, d.trackerId, d.date, d.answers))
            coordinator.reconcileSafely(false)
            entry(null)
        }
    }
    fun requestDelete(entryId: String?) { mutable.update { it.copy(deleteEntryId = entryId, problem = null) } }
    fun confirmDelete() {
        val id = state.value.deleteEntryId ?: return
        write {
            repository.execute(TrackerAction.DeleteEntry(id)); coordinator.reconcileSafely(false)
            if (state.value.entry?.entryId == id) entry(null)
            requestDelete(null)
        }
    }
    fun retry() {
        val action = retryAction
        mutable.update { it.copy(storageError = false) }
        observe()
        if (action != null) write(action)
    }

    private fun write(action: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, problem = null, problemQuestion = null) }
        viewModelScope.launch {
            try {
                action(); retryAction = null
                mutable.update { it.copy(busy = false, storageError = false) }
            } catch (e: CancellationException) { throw e
            } catch (e: TrackerRuleException) {
                mutable.update { it.copy(busy = false, problem = e.problem, problemQuestion = e.questionId) }
            } catch (_: Exception) {
                retryAction = action
                mutable.update { it.copy(busy = false, storageError = true) }
            }
        }
    }
}
