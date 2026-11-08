package com.ethos.app.features.timers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethos.app.core.timers.domain.*
import com.ethos.app.core.timers.platform.TimerCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Everything the timer sheet needs about its source, computed by the caller from the owning feature's state. */
data class TimerTarget(
    val start: TimerStart, val bounds: TimerBounds, val canStart: Boolean, val goalAllowed: Boolean,
    val sourceDone: Boolean = false,
)

data class ManualDraft(val segmentId: String?, val date: String, val start: String, val end: String)

/** A user-confirmed request to run an existing completion flow; consumed explicitly by the host. */
data class TimerCompletion(val kind: TimerKind, val sourceId: String, val date: LocalDate, val token: Long)

data class TimerUiState(
    val snapshot: TimerSnapshot = TimerSnapshot(), val now: TimerNow = TimerNow(0, 0, null),
    val loading: Boolean = true, val error: Boolean = false, val busy: Boolean = false,
    val problem: TimerProblem? = null, val target: TimerTarget? = null, val conflict: TimerStart? = null,
    val countdown: Boolean = true, val goalText: String = "", val manual: ManualDraft? = null,
    val completion: TimerCompletion? = null, val notificationsVisible: Boolean = true,
    val serviceBlocked: Boolean = false, val reachedPrompt: String? = null, val unavailable: Boolean = false,
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val repository: TimerRepository, private val clock: TimerClock, private val coordinator: TimerCoordinator,
) : ViewModel() {
    private val mutable = MutableStateFlow(TimerUiState(now = clock.now()))
    val state = mutable.asStateFlow()
    private var observer: Job? = null
    private var failed: (() -> Unit)? = null
    private val prompted = mutableSetOf<String>()
    val zone get() = clock.zone()

    init {
        observe()
        viewModelScope.launch {
            coordinator.serviceBlocked.collect { b -> mutable.update { it.copy(serviceBlocked = b) } }
        }
    }

    private fun observe() {
        observer?.cancel()
        observer = viewModelScope.launch {
            try {
                repository.observe().collect { snapshot ->
                    // Offer confirmation once per newly reached objective audit, never for reward exhaustion.
                    val reached = snapshot.audits.lastOrNull { it.kind == TimerAuditKind.REACHED && it.id !in prompted }
                    snapshot.audits.forEach { prompted += it.id }
                    mutable.update { it.copy(snapshot = snapshot, loading = false, error = failed != null,
                        now = clock.now(), reachedPrompt = if (it.loading) it.reachedPrompt
                            else reached?.sessionId ?: it.reachedPrompt) }
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { mutable.update { it.copy(loading = false, error = true) } }
        }
    }

    /** Display-only tick; persisted segments remain the only source of credited time. */
    fun tick() {
        val now = clock.now()
        mutable.update { it.copy(now = now, notificationsVisible = coordinator.notifications.visible()) }
        val s = state.value.snapshot
        val running = s.running ?: return
        val open = s.segments.firstOrNull { it.sessionId == running.id && it.endWall == null } ?: return
        if (TimerRules.capped(open, now) || !TimerRules.sameBoot(open, now)) coordinator.request()
    }

    fun open(target: TimerTarget?) {
        val goal = target?.start?.goalKey?.let { state.value.snapshot.goal(it) }
        mutable.update { it.copy(target = target, problem = null, manual = null, unavailable = false,
            goalText = goal?.minutes?.toString().orEmpty()) }
    }

    fun countdown(value: Boolean) { mutable.update { it.copy(countdown = value) } }

    fun start(request: TimerStart, pauseOther: Boolean = false): Unit = write({ start(request, pauseOther) }) {
        when (repository.start(request, pauseOther)) {
            StartResult.CONFLICT -> mutable.update { it.copy(conflict = request) }
            StartResult.UNAVAILABLE -> mutable.update { it.copy(conflict = null, unavailable = true) }
            StartResult.STARTED -> mutable.update { it.copy(conflict = null, unavailable = false) }
        }
        coordinator.request()
    }

    fun cancelConflict() { mutable.update { it.copy(conflict = null) } }
    fun pause(id: String): Unit = write({ pause(id) }) { coordinator.pause(id) }
    fun finish(id: String): Unit = write({ finish(id) }) { repository.finish(id); coordinator.request() }

    fun goal(text: String) { mutable.update { it.copy(goalText = text.filter(Char::isDigit).take(4)) } }
    fun saveGoal() {
        val key = state.value.target?.start?.goalKey ?: return
        val minutes = state.value.goalText.takeIf { it.isNotEmpty() }?.toIntOrNull()
        if (minutes != null && minutes !in 1..1440) {
            mutable.update { it.copy(problem = TimerProblem.TOO_LONG) }; return
        }
        write(::saveGoal) { repository.saveGoal(key, minutes) }
    }

    /** Owner confirmed reaching an objective: audit, then hand the existing completion flow to the host. */
    fun confirm(kind: TimerKind, sourceId: String, sessionId: String?) {
        write({ confirm(kind, sourceId, sessionId) }) {
            sessionId?.let { repository.confirmed(it) }
            val date = Instant.ofEpochMilli(clock.wallMillis()).atZone(clock.zone()).toLocalDate()
            mutable.update { it.copy(completion = TimerCompletion(kind, sourceId, date, System.nanoTime()),
                reachedPrompt = null, target = null) }
        }
    }
    fun consumeCompletion(token: Long) {
        mutable.update { if (it.completion?.token == token) it.copy(completion = null) else it }
    }
    fun dismissReached() { mutable.update { it.copy(reachedPrompt = null) } }

    fun manual(draft: ManualDraft?) { mutable.update { it.copy(manual = draft, problem = null) } }
    fun newManual() {
        val today = Instant.ofEpochMilli(clock.wallMillis()).atZone(clock.zone()).toLocalDate()
        manual(ManualDraft(null, today.toString(), "", ""))
    }
    fun editSegment(segment: TimerSegment) {
        val start = Instant.ofEpochMilli(segment.startWall).atZone(clock.zone())
        val end = Instant.ofEpochMilli(segment.endWall ?: return).atZone(clock.zone())
        manual(ManualDraft(segment.id, start.toLocalDate().toString(), start.toLocalTime().withNano(0).toString(),
            end.toLocalTime().withNano(0).toString()))
    }

    /** End at or before start means the activity crossed midnight into the next day. */
    fun interval(draft: ManualDraft): Pair<Long, Long> {
        val date = LocalDate.parse(draft.date)
        val start = date.atTime(LocalTime.parse(draft.start)).atZone(clock.zone())
        var end = date.atTime(LocalTime.parse(draft.end)).atZone(clock.zone())
        if (!end.isAfter(start)) end = date.plusDays(1).atTime(LocalTime.parse(draft.end)).atZone(clock.zone())
        return start.toInstant().toEpochMilli() to end.toInstant().toEpochMilli()
    }

    fun saveManual() {
        val draft = state.value.manual ?: return
        val target = state.value.target ?: return
        val (start, end) = try { interval(draft) } catch (_: Exception) {
            mutable.update { it.copy(problem = TimerProblem.INVALID_INTERVAL) }; return
        }
        write(::saveManual) {
            if (draft.segmentId == null) repository.addManual(target.start, start, end, target.bounds)
            else repository.correct(draft.segmentId, start, end, target.bounds)
            mutable.update { it.copy(manual = null) }
        }
    }
    fun deleteSegment(id: String): Unit = write({ deleteSegment(id) }) { repository.delete(id) }

    private fun write(retry: () -> Unit, operation: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, error = false, problem = null) }
        viewModelScope.launch {
            try {
                operation()
                failed = null
                mutable.update { it.copy(busy = false) }
            } catch (e: CancellationException) { throw e
            } catch (e: TimerRuleException) { mutable.update { it.copy(busy = false, problem = e.problem) }
            } catch (_: Exception) {
                failed = retry
                mutable.update { it.copy(busy = false, error = true) }
            }
        }
    }
    fun retry() { observe(); failed?.invoke() }
}
