package com.tabitos.app.features.timers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabitos.app.core.timers.domain.*
import com.tabitos.app.core.timers.platform.TimerAlarm
import com.tabitos.app.core.timers.platform.TimerCoordinator
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

/** A reached duration to complete through the owning feature; consumed explicitly by the host (spec 016). */
data class TimerCompletion(val kind: TimerKind, val sourceId: String, val date: LocalDate, val token: Long)

/** A discard that loses enough time to ask first (spec 018). */
data class TimerDiscard(val sessionId: String, val millis: Long)

data class TimerUiState(
    val snapshot: TimerSnapshot = TimerSnapshot(), val now: TimerNow = TimerNow(0, 0, null),
    val loading: Boolean = true, val error: Boolean = false, val busy: Boolean = false,
    val problem: TimerProblem? = null, val target: TimerTarget? = null, val conflict: TimerStart? = null,
    val manual: ManualDraft? = null,
    val completion: TimerCompletion? = null, val notificationsVisible: Boolean = true,
    val serviceBlocked: Boolean = false, val unavailable: Boolean = false,
    val discard: TimerDiscard? = null,
    /** Alarm still sounding or not yet stopped; shown as a popup with "Parar alarme" (spec 026). */
    val alarm: TimerAlarm? = null,
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val repository: TimerRepository, private val clock: TimerClock, private val coordinator: TimerCoordinator,
) : ViewModel() {
    private val mutable = MutableStateFlow(TimerUiState(now = clock.now()))
    val state = mutable.asStateFlow()
    private var observer: Job? = null
    private var failed: (() -> Unit)? = null
    /** Reached audits already handed to the host in this process; the CONFIRMED audit covers restarts. */
    private val handled = mutableSetOf<String>()
    val zone get() = clock.zone()

    init {
        observe()
        viewModelScope.launch {
            coordinator.serviceBlocked.collect { b -> mutable.update { it.copy(serviceBlocked = b) } }
        }
        viewModelScope.launch { coordinator.alarm.collect { a -> mutable.update { it.copy(alarm = a) } } }
    }

    /** Silences the alarm from the in-app popup; the timer already completed its task or habit (spec 016). */
    fun stopAlarm() { state.value.alarm?.let { coordinator.stopAlarm(it.sessionId) } }

    private fun observe() {
        observer?.cancel()
        observer = viewModelScope.launch {
            try {
                repository.observe().collect { snapshot ->
                    mutable.update { it.copy(snapshot = snapshot, loading = false, error = failed != null,
                        now = clock.now()) }
                    completeNextReached(snapshot)
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
        mutable.update { it.copy(target = target, problem = null, manual = null, unavailable = false) }
    }


    fun start(request: TimerStart, pauseOther: Boolean = false): Unit = write({ start(request, pauseOther) }) {
        when (repository.start(request, pauseOther)) {
            StartResult.CONFLICT -> mutable.update { it.copy(conflict = request) }
            StartResult.UNAVAILABLE -> mutable.update { it.copy(conflict = null, unavailable = true) }
            StartResult.STARTED -> mutable.update { it.copy(conflict = null, unavailable = false) }
        }
        coordinator.request()
    }

    /** Asks before dropping a minute or more; shorter accidental counts go straight away (spec 018). */
    fun requestDiscard(sessionId: String) {
        val snapshot = state.value.snapshot
        val session = snapshot.sessions.firstOrNull { it.id == sessionId } ?: return
        val latest = TimerRules.discardable(snapshot, session) ?: return
        val millis = TimerRules.duration(latest, clock.now())
        if (millis < TimerRules.DISCARD_CONFIRM_MILLIS) discard(sessionId)
        else mutable.update { it.copy(discard = TimerDiscard(sessionId, millis)) }
    }

    fun confirmDiscard() { state.value.discard?.let { discard(it.sessionId) } }
    fun cancelDiscard() { mutable.update { it.copy(discard = null) } }

    private fun discard(sessionId: String): Unit = write({ discard(sessionId) }) {
        repository.discardLatest(sessionId)
        mutable.update { it.copy(discard = null) }
        coordinator.request()
    }

    fun cancelConflict() { mutable.update { it.copy(conflict = null) } }
    fun pause(id: String): Unit = write({ pause(id) }) { coordinator.pause(id) }
    fun finish(id: String): Unit = write({ finish(id) }) { repository.finish(id); coordinator.request() }

    /** Duration goals are edited with the task/habit itself (spec 016); null removes the goal. */
    fun saveGoal(sourceKey: String, minutes: Int?): Unit = write({ saveGoal(sourceKey, minutes) }) {
        repository.saveGoal(sourceKey, minutes)
    }

    /**
     * Reaching the duration completes the task/habit by itself (spec 016), one at a time, dated when the time
     * was reached. Even if the app was closed then, the next sync still completes it exactly once.
     */
    private fun completeNextReached(snapshot: TimerSnapshot) {
        if (state.value.completion != null) return
        val reached = TimerRules.unconfirmedReached(snapshot).firstOrNull { it.id !in handled } ?: return
        val session = snapshot.sessions.firstOrNull { it.id == reached.sessionId } ?: return
        handled += reached.id
        viewModelScope.launch {
            try {
                repository.confirmed(session.id)
                val date = Instant.ofEpochMilli(reached.at).atZone(clock.zone()).toLocalDate()
                mutable.update { it.copy(completion = TimerCompletion(session.kind, session.sourceId, date,
                    System.nanoTime())) }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { handled -= reached.id }
        }
    }
    fun consumeCompletion(token: Long) {
        mutable.update { if (it.completion?.token == token) it.copy(completion = null) else it }
        completeNextReached(state.value.snapshot)
    }

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
