package com.tabitos.app.features.reminders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabitos.app.core.startup.DeferredSource
import com.tabitos.app.core.preferences.TaskReminderDefaults
import com.tabitos.app.core.preferences.TaskReminderDefaultsRepository
import com.tabitos.app.core.startup.StartupSequence
import com.tabitos.app.core.reminders.domain.*
import com.tabitos.app.core.reminders.platform.*
import com.tabitos.app.core.tasks.domain.*
import com.tabitos.app.core.habits.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Reminder being created or edited; [editing] is true for a saved one, which the editor can delete. */
data class ReminderDraft(
    val definition: ReminderDefinition, val date: LocalDate, val minute: Int = DEFAULT_REMINDER_MINUTE,
    val offsetMinutes: Long = DEFAULT_REMINDER_OFFSET, val referenceMinute: Int? = null,
    val future: Boolean = false, val editing: Boolean = false,
) : java.io.Serializable {
    /** The definition as it would be saved, used for the live summary and for saving. */
    fun asDefinition(): ReminderDefinition = definition.copy(
        chosenDate = date.takeIf { definition.mode == ReminderMode.CHOSEN }, minute = minute,
        offsetMinutes = if (definition.mode == ReminderMode.RELATIVE) offsetMinutes else 0,
        referenceMinute = referenceMinute.takeIf { definition.mode == ReminderMode.RELATIVE })
}

data class ReminderUiState(
    val snapshot: ReminderSnapshot = ReminderSnapshot(), val loading: Boolean = true,
    val busy: Boolean = false, val error: Boolean = false, val inputError: Boolean = false,
    val kind: ReminderKind? = null, val targetId: String? = null, val title: String = "",
    val definitions: List<ReminderDefinition> = emptyList(), val draft: ReminderDraft? = null,
    val target: ReminderTarget = ReminderTarget(), val repeats: Boolean = false,
    val scopeFuture: Boolean = false, val snoozeKey: String? = null, val customDate: String = "",
    val customTime: String = "", val fallback: Boolean = false,
    val capability: NotificationCapability = NotificationCapability(false, false, false),
    val defaults: TaskReminderDefaults? = null,
    val defaultsDraft: TaskReminderDefaults? = null,
    val defaultsReadError: Boolean = false,
) {
    /** "Só esta | Esta e próximas" only matters for a task that repeats. */
    val showsScope: Boolean get() = kind == ReminderKind.TASK && repeats
    val quickOffsets: List<Long>
        get() = if (kind == ReminderKind.TASK) target.quickOffsets(definitions) else emptyList()
}

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: ReminderRepository, private val coordinator: ReminderCoordinator,
    private val tasks: TaskRepository, private val habits: HabitRepository, private val saved: SavedStateHandle,
    private val defaultRepository: TaskReminderDefaultsRepository, private val clock: TaskClock,
    private val startup: StartupSequence = StartupSequence.released(),
) : ViewModel() {
    private val mutable = MutableStateFlow(ReminderUiState(draft = saved["reminderDraft"],
        kind = saved.get<String>("reminderKind")?.let(ReminderKind::valueOf), targetId = saved["reminderTarget"],
        snoozeKey = saved["snoozeKey"], defaultsDraft = saved["taskReminderDefaultsDraft"]))
    val state = mutable.asStateFlow()
    private var sources = ReminderSources(TaskSnapshot(), HabitSnapshot())
    private var observer: Job? = null
    private var defaultsObserver: Job? = null
    init {
        // Hoje loads first; reminders start in their preload turn, when Configurações opens or when an editor
        // needs them (spec 027).
        if (saved.get<String>("reminderKind") != null) startup.request(setOf(DeferredSource.REMINDERS))
        viewModelScope.launch { startup.awaitTurn(DeferredSource.REMINDERS); observe() }
        viewModelScope.launch { coordinator.failure.collect { failed ->
            if (failed) mutable.update { it.copy(error = true) }
        } }
    }
    private fun observe() {
        defaultsObserver?.cancel()
        defaultsObserver = viewModelScope.launch {
            defaultRepository.defaults.distinctUntilChanged()
                .catch { mutable.update { it.copy(defaults = null, defaultsReadError = true) } }
                .collect { defaults -> mutable.update { it.copy(defaults = defaults, defaultsReadError = false) } }
        }
        observer?.cancel()
        observer = viewModelScope.launch {
            combine(repository.observe(), tasks.observe(), habits.observe()) { r, t, h -> Triple(r, t, h) }
                .catch {
                    mutable.update { it.copy(loading = false, error = true) }
                    startup.markLoaded(DeferredSource.REMINDERS)
                }
                .collect { (r, t, h) ->
                    sources = ReminderSources(t, h)
                    mutable.update { derive(it.copy(snapshot = r, loading = false)) }
                    startup.markLoaded(DeferredSource.REMINDERS)
                }
        }
    }
    private fun derive(s: ReminderUiState): ReminderUiState {
        val o = sources.tasks.occurrences.firstOrNull { it.id == s.targetId }
        val h = sources.habits.habits.firstOrNull { it.id == s.targetId }
        val series = sources.tasks.definitions.firstOrNull { it.id == o?.taskId }
        return s.copy(title = if (s.kind == ReminderKind.TASK) o?.fields?.title.orEmpty() else h?.name.orEmpty(),
            target = ReminderTarget(o?.dueDate, o?.fields?.minute, now()),
            repeats = series != null && series.rule.kind != RepeatKind.NONE,
            definitions = if (s.kind == ReminderKind.TASK && o != null)
                ReminderRules.effective(s.snapshot.definitions, o).map { d ->
                    if (d.mode == ReminderMode.CHOSEN && d.occurrenceId == null && o.plannedDate != null)
                        d.copy(chosenDate = o.plannedDate.plusDays(d.anchorDay)) else d
                } else s.snapshot.definitions.filter {
                it.kind == s.kind && it.sourceId == s.targetId && it.enabled })
    }
    fun open(kind: ReminderKind, id: String) {
        startup.request(setOf(DeferredSource.REMINDERS))
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
        val def = d ?: ReminderDefinition(UUID.randomUUID().toString(), kind = kind,
            sourceId = if (kind == ReminderKind.TASK) o?.taskId ?: return else s.targetId ?: return,
            occurrenceId = o?.id, mode = newReminderMode(kind, s.target), createdAt = clock.nowMillis())
        draft(ReminderDraft(def, date = def.chosenDate ?: clock.today(), minute = def.minute,
            offsetMinutes = if (d == null) DEFAULT_REMINDER_OFFSET else def.offsetMinutes,
            referenceMinute = def.referenceMinute ?: referenceFor(s.target), future = s.scopeFuture,
            editing = d != null))
    }
    private fun newReminderMode(kind: ReminderKind, target: ReminderTarget) = when {
        kind == ReminderKind.HABIT -> ReminderMode.WEEKDAYS
        target.hasDeadline -> ReminderMode.RELATIVE
        else -> ReminderMode.CHOSEN
    }
    /** A task due on a day without a time counts relative reminders from the default date-only time (spec 033). */
    private fun referenceFor(target: ReminderTarget): Int? =
        if (target.dueMinute == null) state.value.defaults?.dateOnlyMinute ?: DEFAULT_REMINDER_MINUTE else null
    private fun scoped(d: ReminderDefinition, future: Boolean): ReminderDefinition {
        if (d.kind == ReminderKind.HABIT) return d
        val o = sources.tasks.occurrences.first { it.id == state.value.targetId }
        return d.copy(id = "${d.groupId}:${if (future) "from:${o.ordinal}" else o.id}",
            sourceId = o.taskId, occurrenceId = if (future) null else o.id, fromOrdinal = o.ordinal)
    }
    fun save(onEnabled: () -> Unit = {}) {
        val draft = state.value.draft ?: return
        val definition = draft.asDefinition()
        val o = sources.tasks.occurrences.firstOrNull { it.id == state.value.targetId }
        val d = try {
            require(state.value.target.copy(now = now()).moment(definition).canBeSaved(definition.mode))
            val anchorDay = definition.chosenDate?.let { date ->
                o?.plannedDate?.let { ChronoUnit.DAYS.between(it, date) } } ?: 0
            scoped(definition.copy(createdAt = clock.nowMillis(), anchorDay = anchorDay), draft.future)
                .also(ReminderRules::validate)
        } catch (_: Exception) { mutable.update { it.copy(inputError = true) }; return }
        persist(d, onEnabled)
    }
    /** Shortcut of the reminders sheet: a relative reminder saved at once, with the sheet's scope. */
    fun quickAdd(offsetMinutes: Long, onEnabled: () -> Unit = {}) {
        val s = state.value
        val o = sources.tasks.occurrences.firstOrNull { it.id == s.targetId } ?: return
        if (s.kind != ReminderKind.TASK || !s.target.hasDeadline) return
        val id = UUID.randomUUID().toString()
        val definition = ReminderDefinition(id, kind = ReminderKind.TASK, sourceId = o.taskId, occurrenceId = o.id,
            mode = ReminderMode.RELATIVE, offsetMinutes = offsetMinutes, referenceMinute = referenceFor(s.target),
            createdAt = clock.nowMillis())
        persist(scoped(definition, s.scopeFuture).also(ReminderRules::validate), onEnabled)
    }
    private fun persist(d: ReminderDefinition, onEnabled: () -> Unit) {
        write { repository.save(d); this@ReminderViewModel.draft(null); onEnabled() }
    }
    /** Deletes the reminder open in the editor, as it was saved, and returns to the list. */
    fun removeDraft() {
        val draft = state.value.draft ?: return
        val saved = state.value.definitions.firstOrNull { it.id == draft.definition.id } ?: return
        write { repository.remove(scoped(saved, state.value.scopeFuture)); this@ReminderViewModel.draft(null) }
    }
    fun remove(d: ReminderDefinition) { write { repository.remove(scoped(d, state.value.scopeFuture)) } }
    fun summary(text: String?, onEnabled: () -> Unit = {}) {
        val minute = try { text?.let { LocalTime.parse(it).let { t -> t.hour * 60 + t.minute } }
        } catch (_: Exception) { mutable.update { it.copy(inputError = true) }; return }
        write { repository.summary(minute); if (minute != null) onEnabled() }
    }
    private fun now(): LocalDateTime =
        Instant.ofEpochMilli(clock.nowMillis()).atZone(ZoneId.systemDefault()).toLocalDateTime()
    fun refresh() {
        mutable.update { it.copy(capability = coordinator.platform.capability()) }
        coordinator.request()
    }
    fun retry() { mutable.update { it.copy(error = false) }; observe(); refresh() }
    fun changeDefaultsDraft(defaults: TaskReminderDefaults?) {
        saved["taskReminderDefaultsDraft"] = defaults
        mutable.update { it.copy(defaultsDraft = defaults) }
    }

    fun saveDefaults(onEnabled: () -> Unit) {
        val defaults = state.value.defaultsDraft ?: return
        write(reconcile = false) {
            defaultRepository.save(defaults)
            changeDefaultsDraft(null)
            if (defaults.enabled) onEnabled()
        }
    }

    private fun write(reconcile: Boolean = true, action: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, error = false, inputError = false) }
        viewModelScope.launch {
            try {
                action()
                if (reconcile) coordinator.reconcileSafely(false)
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
            if (minutes != null) clock.nowMillis() + minutes * 60000L else {
                val date = LocalDate.parse(state.value.customDate)
                val t = LocalTime.parse(state.value.customTime)
                ReminderRules.instant(date, t.hour * 60 + t.minute, ZoneId.systemDefault())
            }
        } catch (_: Exception) { mutable.update { it.copy(inputError = true) }; return }
        if (at <= clock.nowMillis()) { mutable.update { it.copy(inputError = true) }; return }
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
            mutable.update { it.copy(snoozeKey = key, customDate = clock.today().toString(),
                customTime = now().toLocalTime().plusHours(1).withSecond(0).withNano(0).toString()) }
            return null
        }
        return row
    }
}
