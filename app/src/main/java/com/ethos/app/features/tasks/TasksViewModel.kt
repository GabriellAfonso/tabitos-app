package com.ethos.app.features.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.accounting.domain.AccountingRepository
import com.ethos.app.core.accounting.domain.FinancialPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TaskDraft(
    val id: String, val editing: Boolean = false, val fields: TaskFields = TaskFields(),
    val rule: RepeatRule = RepeatRule(), val dateText: String = "", val timeText: String = "",
    val startText: String = "", val untilText: String = "", val countText: String = "",
    val intervalText: String = "1", val monthDayText: String = "1",
    val scope: ChangeScope = ChangeScope.ONLY_THIS, val reviewConfirmed: Boolean = false,
    val ending: Int = 0,
    val recurringOriginal: Boolean = false,
) : java.io.Serializable

data class TasksUiState(
    val snapshot: TaskSnapshot = TaskSnapshot(), val today: LocalDate,
    val loading: Boolean = true, val busy: Boolean = false, val error: TaskProblem? = null,
    val storageError: Boolean = false, val draft: TaskDraft? = null, val detailId: String? = null,
    val completionText: String = "", val uncheckedConfirmation: Int? = null,
    val listDraft: TaskList? = null, val deleteListId: String? = null, val deleteTaskId: String? = null,
    val manageLists: Boolean = false, val selectedListId: String? = null,
    val financialPreview: FinancialPreview? = null,
    val discardConfirmation: Boolean = false,
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository, private val clock: TaskClock, private val saved: SavedStateHandle,
    private val accounting: AccountingRepository? = null,
) : ViewModel() {
    private val mutable = MutableStateFlow(TasksUiState(today = clock.today(), draft = saved["taskDraft"]))
    val state: StateFlow<TasksUiState> = mutable.asStateFlow()
    private var observer: Job? = null
    private var readFailed = false
    private var retryAction: TaskAction? = null
    private var retryClose: Boolean = false
    private var retryConfirmed: Boolean = false
    private var completionAction: TaskAction.Resolve? = null
    private var financialAction: TaskAction? = null

    fun confirmFinancial(confirm: Boolean) {
        val action = financialAction
        financialAction = null
        mutable.update { it.copy(financialPreview = null) }
        if (confirm && action != null) execute(action, confirmed = true)
    }

    init { observe(); refresh() }
    private fun observe() {
        observer?.cancel()
        observer = viewModelScope.launch {
            try {
                repository.observe().collect { snapshot ->
                    readFailed = false
                    mutable.update { it.copy(snapshot = snapshot, loading = false,
                        selectedListId = it.selectedListId?.takeIf { id -> snapshot.activeLists.any { l -> l.id == id } },
                        storageError = if (retryAction == null) false else it.storageError) }
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                readFailed = true
                mutable.update { it.copy(loading = false, storageError = true) }
            }
        }
    }
    fun refresh() {
        mutable.update { it.copy(today = clock.today()) }
        if (!mutable.value.busy && !mutable.value.storageError) execute(TaskAction.Refresh)
    }
    fun retry() {
        mutable.update { it.copy(error = null, storageError = false) }
        observe()
        val action = retryAction
        if (action != null) execute(action, retryClose, retryConfirmed) else refresh()
    }
    fun dismissError() { mutable.update { it.copy(error = null, storageError = false) } }
    fun selectList(id: String?) { mutable.update { it.copy(selectedListId = id) } }
    fun newTask() {
        val today = clock.today()
        updateDraft(TaskDraft(UUID.randomUUID().toString(), fields = TaskFields(listId = state.value.selectedListId),
            rule = RepeatRule(start = today, monthDay = today.dayOfMonth), dateText = today.toString(),
            startText = today.toString(), monthDayText = today.dayOfMonth.toString()))
    }
    fun edit(o: TaskOccurrence) {
        val rule = state.value.snapshot.definitions.first { it.id == o.taskId }.rule
        updateDraft(TaskDraft(o.id, true, o.fields, rule, o.fields.date?.toString().orEmpty(),
            o.fields.minute?.let { LocalTime.of(it / 60, it % 60).toString() }.orEmpty(),
            rule.start.toString(), rule.until?.toString().orEmpty(), rule.count?.toString().orEmpty(),
            rule.interval.toString(), rule.monthDay.toString(),
            ending = if (rule.count != null) 2 else if (rule.until != null) 1 else 0,
            recurringOriginal = rule.kind != RepeatKind.NONE))
    }
    fun updateDraft(draft: TaskDraft?) {
        saved["taskDraft"] = draft
        if (draft == null || state.value.draft?.id != draft.id) saved["taskDraftOriginal"] = draft
        mutable.update { it.copy(draft = draft, discardConfirmation = false) }
    }
    /** Closes the editor, asking first when the draft differs from what was opened (spec 013 FR-011). */
    fun closeDraft() {
        val draft = state.value.draft ?: return
        if (draft == saved.get<TaskDraft>("taskDraftOriginal")) updateDraft(null)
        else mutable.update { it.copy(discardConfirmation = true) }
    }
    fun confirmDiscard(discard: Boolean) {
        if (discard) updateDraft(null) else mutable.update { it.copy(discardConfirmation = false) }
    }
    fun difficulty(value: Int) {
        val d = state.value.draft ?: return
        updateDraft(d.copy(fields = TaskRules.suggestDifficulty(d.fields, value)))
    }
    fun repeat(kind: RepeatKind) {
        val d = state.value.draft ?: return
        repeatRule(d.rule.copy(kind = kind))
    }
    /** Applies a full repeat rule; repeating work always has a deadline, as before. */
    fun repeatRule(rule: RepeatRule) {
        val d = state.value.draft ?: return
        updateDraft(d.copy(rule = rule, fields = d.fields.copy(
            deadline = if (rule.kind != RepeatKind.NONE && d.fields.deadline == DeadlineKind.NONE)
                DeadlineKind.DAY else d.fields.deadline)))
    }
    fun saveDraft() {
        val d = state.value.draft ?: return
        try {
            val date = when {
                d.fields.deadline == DeadlineKind.NONE -> null
                d.rule.kind != RepeatKind.NONE && !d.editing -> LocalDate.parse(d.startText)
                else -> LocalDate.parse(d.dateText)
            }
            val time = if (d.timeText.isBlank() || d.fields.deadline != DeadlineKind.DAY) null
                else LocalTime.parse(d.timeText).let { it.hour * 60 + it.minute }
            val rule = if (d.rule.kind == RepeatKind.NONE) RepeatRule(start = date ?: clock.today()) else d.rule.copy(
                start = if (d.editing && d.scope == ChangeScope.THIS_AND_FUTURE) date!!
                    else LocalDate.parse(d.startText),
                interval = if (d.rule.kind in listOf(RepeatKind.FIXED_INTERVAL, RepeatKind.AFTER_COMPLETION))
                    d.intervalText.toInt() else 1,
                monthDay = if (d.rule.kind == RepeatKind.MONTHLY) d.monthDayText.toInt() else d.rule.monthDay,
                until = if (d.ending == 1) LocalDate.parse(d.untilText) else null,
                count = if (d.ending == 2) d.countText.toInt() else null,
            )
            val fields = d.fields.copy(date = date, minute = time)
            val action = if (d.editing) TaskAction.Edit(d.id, fields, rule, d.scope, d.reviewConfirmed)
                else TaskAction.Create(d.id, fields, rule)
            execute(action, closeDraft = true)
        } catch (_: IllegalArgumentException) { mutable.update { it.copy(error = TaskProblem.INVALID_INPUT) }
        } catch (_: java.time.DateTimeException) { mutable.update { it.copy(error = TaskProblem.INVALID_DATE) } }
    }
    fun detail(id: String?) {
        mutable.update { it.copy(detailId = id, completionText = clock.today().toString(),
            uncheckedConfirmation = null) }
    }
    fun notification(id: String, complete: Boolean) {
        viewModelScope.launch {
            try {
                val snapshot = repository.observe().first()
                val occurrence = snapshot.occurrences.firstOrNull { it.id == id }
                if (occurrence == null || occurrence.status != TaskStatus.OPEN) {
                    mutable.update { it.copy(error = TaskProblem.NOT_OPEN) }
                    return@launch
                }
                mutable.update { it.copy(snapshot = snapshot) }
                detail(id)
                if (complete) this@TasksViewModel.complete(occurrence, true)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { mutable.update { it.copy(storageError = true) } }
        }
    }
    fun completionDate(text: String) { mutable.update { it.copy(completionText = text) } }
    fun quickCheck(id: String) {
        val s = state.value
        if (s.busy || s.loading || s.storageError) return
        val o = s.snapshot.occurrences.firstOrNull { it.id == id } ?: return
        if (o.status != TaskStatus.OPEN && o.status != TaskStatus.DONE) return
        execute(TaskAction.SetCompleted(id, o.status == TaskStatus.OPEN, clock.today()), confirmed = true)
    }
    fun complete(o: TaskOccurrence, done: Boolean, confirmed: Boolean = false) {
        val date = try {
            if (done) LocalDate.parse(state.value.completionText) else clock.today()
        } catch (_: Exception) {
            mutable.update { it.copy(error = TaskProblem.INVALID_DATE) }; return
        }
        val action = TaskAction.Resolve(o.id, done, date)
        val remaining = o.checks.count { !it.checked }
        if (done && remaining > 0 && !confirmed) {
            completionAction = action
            mutable.update { it.copy(uncheckedConfirmation = remaining) }
        } else execute(action)
    }
    fun confirmCompletion(confirm: Boolean) {
        mutable.update { it.copy(uncheckedConfirmation = null) }
        if (confirm) completionAction?.let { execute(it) }
        completionAction = null
    }
    fun manageLists(show: Boolean) { mutable.update { it.copy(manageLists = show) } }
    fun listDraft(list: TaskList?) { mutable.update { it.copy(listDraft = list) } }
    fun newList() = listDraft(TaskList(UUID.randomUUID().toString(), ""))
    fun saveList() { state.value.listDraft?.let { execute(TaskAction.SaveList(it)) } }
    fun requestDeleteList(id: String?) { mutable.update { it.copy(deleteListId = id) } }
    fun requestDeleteTask(id: String?) { mutable.update { it.copy(deleteTaskId = id) } }
    fun action(action: TaskAction) = execute(action)

    private fun execute(action: TaskAction, closeDraft: Boolean = false, confirmed: Boolean = false) {
        if (mutable.value.busy) return
        if (!confirmed && accounting != null && (action is TaskAction.Resolve || action is TaskAction.Undo)) {
            mutable.update { it.copy(busy = true, error = null) }
            viewModelScope.launch {
                try {
                    val preview = accounting.previewTask(action)
                    financialAction = action
                    mutable.update { it.copy(busy = false, financialPreview = preview) }
                } catch (e: CancellationException) { throw e
                } catch (e: TaskRuleException) {
                    mutable.update { it.copy(busy = false, error = e.problem) }
                } catch (_: Exception) {
                    retryAction = action
                    retryConfirmed = false
                    mutable.update { it.copy(busy = false, storageError = true) }
                }
            }
            return
        }
        retryAction = null
        mutable.update { it.copy(busy = true, error = null, storageError = readFailed) }
        viewModelScope.launch {
            try {
                repository.execute(action)
                val committed = repository.observe().first()
                mutable.update { it.copy(snapshot = committed) }
                retryAction = null
                if (closeDraft) updateDraft(null)
                mutable.update { it.copy(busy = false, storageError = readFailed,
                    listDraft = if (action is TaskAction.SaveList) null else it.listDraft,
                    deleteListId = null, deleteTaskId = null,
                    selectedListId = if (action is TaskAction.DeleteList && action.id == it.selectedListId)
                        null else it.selectedListId,
                    detailId = if (action is TaskAction.Delete) null else it.detailId) }
            } catch (e: CancellationException) { throw e
            } catch (e: TaskRuleException) {
                mutable.update { it.copy(busy = false, error = e.problem) }
            } catch (_: Exception) {
                retryAction = action; retryClose = closeDraft; retryConfirmed = confirmed
                mutable.update { it.copy(busy = false, storageError = true) }
            }
        }
    }
}
