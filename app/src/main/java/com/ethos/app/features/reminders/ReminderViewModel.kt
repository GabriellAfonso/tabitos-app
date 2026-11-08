package com.ethos.app.features.reminders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethos.app.core.reminders.domain.*
import com.ethos.app.core.reminders.platform.*
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.habits.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ReminderDraft(
    val definition: ReminderDefinition, val date: String = "", val time: String = "09:00",
    val offset: String = "30", val reference: String = "", val future: Boolean = false,
) : java.io.Serializable

data class ReminderUiState(
    val snapshot: ReminderSnapshot = ReminderSnapshot(), val loading: Boolean = true,
    val busy: Boolean = false, val error: Boolean = false, val inputError: Boolean = false,
    val kind: ReminderKind? = null, val targetId: String? = null, val title: String = "",
    val definitions: List<ReminderDefinition> = emptyList(), val draft: ReminderDraft? = null,
    val scopeFuture: Boolean = false, val snoozeKey: String? = null, val customDate: String = "",
    val customTime: String = "", val fallback: Boolean = false,
    val capability: NotificationCapability = NotificationCapability(false, false, false),
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: ReminderRepository, private val coordinator: ReminderCoordinator,
    private val tasks: TaskRepository, private val habits: HabitRepository, private val saved: SavedStateHandle,
) : ViewModel() {
    private val mutable = MutableStateFlow(ReminderUiState(draft = saved["reminderDraft"],
        kind = saved.get<String>("reminderKind")?.let(ReminderKind::valueOf), targetId = saved["reminderTarget"],
        snoozeKey = saved["snoozeKey"]))
    val state = mutable.asStateFlow()
    private var sources = ReminderSources(TaskSnapshot(), HabitSnapshot())
    private var observer: Job? = null
    init {
        observe()
        viewModelScope.launch { coordinator.failure.collect { failed ->
            if (failed) mutable.update { it.copy(error = true) }
        } }
    }
    private fun observe() {
        observer?.cancel()
        observer = viewModelScope.launch {
            combine(repository.observe(), tasks.observe(), habits.observe()) { r, t, h -> Triple(r, t, h) }
                .catch { mutable.update { it.copy(loading = false, error = true) } }
                .collect { (r, t, h) ->
                    sources = ReminderSources(t, h)
                    mutable.update { derive(it.copy(snapshot = r, loading = false)) }
                }
        }
    }
    private fun derive(s: ReminderUiState): ReminderUiState {
        val o = sources.tasks.occurrences.firstOrNull { it.id == s.targetId }
        val h = sources.habits.habits.firstOrNull { it.id == s.targetId }
        return s.copy(title = if (s.kind == ReminderKind.TASK) o?.fields?.title.orEmpty() else h?.name.orEmpty(),
            definitions = if (s.kind == ReminderKind.TASK && o != null)
                ReminderRules.effective(s.snapshot.definitions, o).map { d ->
                    if (d.mode == ReminderMode.CHOSEN && d.occurrenceId == null && o.plannedDate != null)
                        d.copy(chosenDate = o.plannedDate.plusDays(d.anchorDay)) else d
                } else s.snapshot.definitions.filter {
                it.kind == s.kind && it.sourceId == s.targetId && it.enabled })
    }
    fun open(kind: ReminderKind, id: String) {
        saved["reminderKind"] = kind.name
        saved["reminderTarget"] = id
        mutable.update { derive(it.copy(kind = kind, targetId = id, scopeFuture = false, inputError = false)) }
    }
    fun close() {
        if (state.value.busy) return
        saved.remove<String>("reminderKind"); saved.remove<String>("reminderTarget")
        draft(null)
        mutable.update { it.copy(kind = null, targetId = null) }
    }
    fun scope(future: Boolean) { mutable.update { it.copy(scopeFuture = future) } }
    fun draft(d: ReminderDraft?) {
        saved["reminderDraft"] = d
        mutable.update { it.copy(draft = d, inputError = false) }
    }
    fun edit(d: ReminderDefinition? = null) {
        val s = state.value
        val kind = s.kind ?: return
        val o = sources.tasks.occurrences.firstOrNull { it.id == s.targetId }
        val id = UUID.randomUUID().toString()
        val def = d ?: ReminderDefinition(id, kind = kind,
            sourceId = if (kind == ReminderKind.TASK) o?.taskId ?: return else s.targetId ?: return,
            occurrenceId = o?.id, mode = if (kind == ReminderKind.HABIT) ReminderMode.WEEKDAYS else ReminderMode.CHOSEN,
            chosenDate = LocalDate.now(), createdAt = System.currentTimeMillis())
        draft(ReminderDraft(def, (def.chosenDate ?: LocalDate.now()).toString(),
            LocalTime.of(def.minute / 60, def.minute % 60).toString(), def.offsetMinutes.toString(),
            def.referenceMinute?.let { LocalTime.of(it / 60, it % 60).toString() }.orEmpty(), s.scopeFuture))
    }
    private fun scoped(d: ReminderDefinition, future: Boolean): ReminderDefinition {
        if (d.kind == ReminderKind.HABIT) return d
        val o = sources.tasks.occurrences.first { it.id == state.value.targetId }
        return d.copy(id = "${d.groupId}:${if (future) "from:${o.ordinal}" else o.id}",
            sourceId = o.taskId, occurrenceId = if (future) null else o.id, fromOrdinal = o.ordinal)
    }
    fun save(onEnabled: () -> Unit = {}) {
        val draft = state.value.draft ?: return
        val d = try {
            val time = LocalTime.parse(draft.time)
            val o = sources.tasks.occurrences.firstOrNull { it.id == state.value.targetId }
            val date = if (draft.definition.mode == ReminderMode.CHOSEN) LocalDate.parse(draft.date) else null
            val ref = draft.reference.takeIf { it.isNotBlank() }?.let(LocalTime::parse)
            require(draft.definition.mode != ReminderMode.RELATIVE ||
                (o?.dueDate != null && (o.fields.minute != null || ref != null)))
            if (date != null) require(ReminderRules.instant(date, time.hour * 60 + time.minute,
                ZoneId.systemDefault()) > System.currentTimeMillis())
            scoped(draft.definition.copy(chosenDate = date, minute = time.hour * 60 + time.minute,
                createdAt = System.currentTimeMillis(),
                offsetMinutes = if (draft.definition.mode == ReminderMode.RELATIVE) draft.offset.toLong() else 0,
                referenceMinute = ref?.let { it.hour * 60 + it.minute },
                anchorDay = if (date != null && o?.plannedDate != null)
                    ChronoUnit.DAYS.between(o.plannedDate, date) else 0), draft.future).also(ReminderRules::validate)
        } catch (_: Exception) { mutable.update { it.copy(inputError = true) }; return }
        write { repository.save(d); this@ReminderViewModel.draft(null); onEnabled() }
    }
    fun remove(d: ReminderDefinition) { write { repository.remove(scoped(d, state.value.scopeFuture)) } }
    fun summary(text: String?, onEnabled: () -> Unit = {}) {
        val minute = try { text?.let { LocalTime.parse(it).let { t -> t.hour * 60 + t.minute } }
        } catch (_: Exception) { mutable.update { it.copy(inputError = true) }; return }
        write { repository.summary(minute); if (minute != null) onEnabled() }
    }
    fun refresh() {
        mutable.update { it.copy(capability = coordinator.platform.capability()) }
        coordinator.request()
    }
    fun retry() { mutable.update { it.copy(error = false) }; observe(); refresh() }
    private fun write(action: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, error = false, inputError = false) }
        viewModelScope.launch {
            try { action(); coordinator.reconcileSafely(false)
            } catch (e: CancellationException) { throw e
            } catch (_: IllegalArgumentException) { mutable.update { it.copy(inputError = true) }
            } catch (_: Exception) { mutable.update { it.copy(error = true) }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun fallback(show: Boolean) { mutable.update { it.copy(fallback = show) } }
    fun dismissSnooze() {
        saved.remove<String>("snoozeKey")
        mutable.update { it.copy(snoozeKey = null, inputError = false) }
    }
    fun custom(date: String, time: String) { mutable.update { it.copy(customDate = date, customTime = time) } }
    fun snooze(minutes: Int?) {
        val key = state.value.snoozeKey ?: return
        val at = try {
            if (minutes != null) System.currentTimeMillis() + minutes * 60000L else {
                val date = LocalDate.parse(state.value.customDate)
                val t = LocalTime.parse(state.value.customTime)
                ReminderRules.instant(date, t.hour * 60 + t.minute, ZoneId.systemDefault())
            }
        } catch (_: Exception) { mutable.update { it.copy(inputError = true) }; return }
        if (at <= System.currentTimeMillis()) { mutable.update { it.copy(inputError = true) }; return }
        write { coordinator.snooze(key, at); dismissSnooze() }
    }

    suspend fun request(key: String, action: String, token: String): ReminderDelivery? {
        if (saved.get<String>("handledReminder") == token) return null
        if (action !in setOf("OPEN", "COMPLETE", "SNOOZE", "FILL")) return null
        val row = coordinator.validate(key)
        if (row == null || (action == "COMPLETE" && row.kind != ReminderKind.TASK)) {
            saved["handledReminder"] = token
            fallback(true)
            return null
        }
        coordinator.materialize(row)
        // Wait for repository emission before delegating to the current UI completion flow.
        if (row.kind == ReminderKind.TASK && tasks.observe().first().occurrences.none {
                it.id == row.sourceId && it.status == TaskStatus.OPEN }) {
            saved["handledReminder"] = token
            fallback(true)
            return null
        }
        saved["handledReminder"] = token
        if (action == "SNOOZE") {
            saved["snoozeKey"] = key
            mutable.update { it.copy(snoozeKey = key, customDate = LocalDate.now().toString(),
                customTime = LocalTime.now().plusHours(1).withSecond(0).withNano(0).toString()) }
            return null
        }
        return row
    }
}
