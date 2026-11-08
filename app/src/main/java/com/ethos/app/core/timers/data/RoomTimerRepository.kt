package com.ethos.app.core.timers.data

import androidx.room.withTransaction
import com.ethos.app.core.tasks.data.TaskDatabase
import com.ethos.app.core.timers.domain.*
import java.util.UUID
import kotlinx.coroutines.flow.*

/** Transaction helpers shared with reward purchases; callers must hold a database transaction. */
class TimerStore(private val database: TaskDatabase, private val clock: TimerClock) {
    private val dao get() = database.timers()

    suspend fun snapshot() = TimerSnapshot(dao.sessions().map { it.domain() }, dao.segments().map { it.domain() },
        dao.audits().map { it.domain() }, dao.goals().map { TimerGoal(it.sourceKey, it.minutes) })

    suspend fun audit(session: String, kind: TimerAuditKind, at: Long, segment: TimerSegment? = null,
        before: TimerSegment? = null): TimerAudit {
        val row = TimerAudit(UUID.randomUUID().toString(), session, kind, at, segment?.id ?: before?.id,
            before?.startWall, before?.endWall, segment?.startWall, segment?.endWall)
        dao.audit(row.entity())
        return row
    }

    /** Closes the open segment at its credited (capped/recovered) duration and pauses the session. */
    suspend fun pause(snapshot: TimerSnapshot, session: TimerSession, kind: TimerAuditKind, now: TimerNow): TimerAudit {
        val open = snapshot.segments.firstOrNull { it.sessionId == session.id && it.endWall == null }
        var closed: TimerSegment? = null
        var reached = session.reached
        if (open != null) {
            if (TimerRules.capped(open, now)) reached = true
            val credited = TimerRules.duration(open, now)
            closed = open.copy(endWall = open.startWall + credited,
                checkpointElapsed = open.startElapsed?.plus(credited))
            dao.segment(closed.entity())
        }
        dao.session(session.copy(state = TimerState.PAUSED, reached = reached).entity())
        return audit(session.id, kind, now.wall, closed)
    }

    suspend fun pauseRunning(snapshot: TimerSnapshot, now: TimerNow) {
        snapshot.running?.let { pause(snapshot, it, TimerAuditKind.PAUSE, now) }
    }

    /** Opens a new segment limited to the remaining objective; zero remaining never starts counting. */
    suspend fun run(snapshot: TimerSnapshot, session: TimerSession, now: TimerNow, resumed: Boolean): Boolean {
        val remaining = TimerRules.remaining(snapshot, session, now, clock.zone())
        if (remaining == 0L) {
            dao.session(session.copy(state = if (session.state == TimerState.RUNNING) TimerState.PAUSED
                else session.state, reached = true).entity())
            return false
        }
        val segment = TimerSegment(UUID.randomUUID().toString(), session.id, now.wall, null, now.elapsed, now.boot,
            now.elapsed, remaining, created = now.wall)
        dao.session(session.copy(state = TimerState.RUNNING, reached = false).entity())
        dao.segment(segment.entity())
        audit(session.id, if (resumed) TimerAuditKind.RESUME else TimerAuditKind.START, now.wall, segment)
        return true
    }

    /** Starts a timed reward purchase as the single running session; repeat calls are idempotent. */
    suspend fun startReward(purchaseId: String, title: String, minutes: Int) {
        val id = "purchase:$purchaseId"
        if (dao.session(id) != null) return
        val now = clock.now()
        val snapshot = snapshot()
        pauseRunning(snapshot, now)
        val session = TimerSession(id, TimerKind.REWARD, purchaseId, title, TimerState.PAUSED,
            limitMillis = minutes * 60_000L, created = now.wall)
        dao.session(session.entity())
        run(snapshot(), session, now, resumed = false)
    }
}

class RoomTimerRepository(private val database: TaskDatabase, private val clock: TimerClock) : TimerRepository {
    private val dao get() = database.timers()
    private val store = TimerStore(database, clock)

    override fun observe(): Flow<TimerSnapshot> = database.invalidationTracker.createFlow("timer_sessions",
        "timer_segments", "timer_audits", "timer_goals", emitInitialState = true)
        .map { database.withTransaction { store.snapshot() } }

    override suspend fun start(request: TimerStart, pauseOther: Boolean): StartResult = database.withTransaction {
        val now = clock.now()
        var snapshot = store.snapshot()
        val existing = snapshot.open(request.kind, request.sourceId)
        val running = snapshot.running
        if (running != null && running.id == existing?.id) return@withTransaction StartResult.STARTED
        if (running != null) {
            if (!pauseOther) return@withTransaction StartResult.CONFLICT
            store.pause(snapshot, running, TimerAuditKind.PAUSE, now)
            snapshot = store.snapshot()
        }
        val session = existing ?: TimerSession(UUID.randomUUID().toString(), request.kind, request.sourceId,
            request.title, TimerState.PAUSED, request.period, request.goalKey, created = now.wall)
        val current = if (existing == null) session
            else session.copy(goalKey = request.goalKey ?: session.goalKey, period = request.period,
                title = request.title)
        if (existing == null) {
            // Without an objective left, do not create an empty session; the owner may confirm instead.
            if (TimerRules.remaining(snapshot.copy(sessions = snapshot.sessions + current), current, now,
                    clock.zone()) == 0L) return@withTransaction StartResult.UNAVAILABLE
            dao.session(current.entity())
        }
        if (store.run(store.snapshot(), current, now, resumed = existing != null)) StartResult.STARTED
        else StartResult.UNAVAILABLE
    }

    override suspend fun pause(sessionId: String) {
        database.withTransaction {
            val snapshot = store.snapshot()
            val session = snapshot.sessions.firstOrNull { it.id == sessionId && it.state == TimerState.RUNNING }
                ?: return@withTransaction
            store.pause(snapshot, session, TimerAuditKind.PAUSE, clock.now())
        }
    }

    override suspend fun finish(sessionId: String) {
        database.withTransaction {
            val now = clock.now()
            val snapshot = store.snapshot()
            val session = snapshot.sessions.firstOrNull { it.id == sessionId } ?: return@withTransaction
            if (session.state == TimerState.FINISHED) return@withTransaction
            if (session.state == TimerState.RUNNING) store.pause(snapshot, session, TimerAuditKind.PAUSE, now)
            dao.session((dao.session(sessionId)!!.domain()).copy(state = TimerState.FINISHED).entity())
            store.audit(sessionId, TimerAuditKind.FINISHED, now.wall)
        }
    }
    /** Records that the owner chose to confirm; the domain outcome itself goes through existing flows. */
    override suspend fun confirmed(sessionId: String) {
        database.withTransaction {
            if (dao.session(sessionId) != null) store.audit(sessionId, TimerAuditKind.CONFIRMED, clock.wallMillis())
        }
    }

    override suspend fun sync(closedSources: Set<Pair<TimerKind, String>>): TimerSync = database.withTransaction {
        val now = clock.now()
        val snapshot = store.snapshot()
        val running = snapshot.running ?: return@withTransaction TimerSync(null, null, emptyList())
        val open = snapshot.segments.firstOrNull { it.sessionId == running.id && it.endWall == null }
        val alert = when {
            open == null -> store.pause(snapshot, running, TimerAuditKind.PAUSE, now)
            !TimerRules.sameBoot(open, now) -> store.pause(snapshot, running, TimerAuditKind.RECOVERED, now)
            TimerRules.capped(open, now) -> store.pause(snapshot, running,
                if (running.kind == TimerKind.REWARD) TimerAuditKind.EXHAUSTED else TimerAuditKind.REACHED, now)
            (running.kind to running.sourceId) in closedSources ->
                store.pause(snapshot, running, TimerAuditKind.SOURCE_CLOSED, now)
            else -> null
        }
        if (alert != null) return@withTransaction TimerSync(null, null,
            listOf(alert).filter { it.kind == TimerAuditKind.REACHED || it.kind == TimerAuditKind.EXHAUSTED })
        dao.segment(open!!.copy(checkpointElapsed = now.elapsed).entity())
        TimerSync(running, open.limitMillis?.let { it - TimerRules.rawDuration(open, now) }, emptyList())
    }

    override suspend fun checkpoint() {
        database.withTransaction {
            val now = clock.now()
            val snapshot = store.snapshot()
            val running = snapshot.running ?: return@withTransaction
            val open = snapshot.segments.firstOrNull { it.sessionId == running.id && it.endWall == null }
                ?: return@withTransaction
            if (TimerRules.sameBoot(open, now)) dao.segment(open.copy(checkpointElapsed = now.elapsed).entity())
        }
    }

    override suspend fun saveGoal(sourceKey: String, minutes: Int?) {
        require(minutes == null || minutes in 1..1440)
        database.withTransaction {
            if (minutes == null) dao.removeGoal(sourceKey)
            else dao.goal(TimerGoalEntity(sourceKey, minutes, clock.wallMillis()))
        }
    }

    override suspend fun addManual(request: TimerStart, start: Long, end: Long, bounds: TimerBounds) {
        require(request.kind != TimerKind.REWARD)
        database.withTransaction {
            val now = clock.now()
            val snapshot = store.snapshot()
            TimerRules.validateManual(start, end, now.wall, bounds, snapshot.segments, now, clock.zone())
            val session = snapshot.open(request.kind, request.sourceId) ?: TimerSession(UUID.randomUUID().toString(),
                request.kind, request.sourceId, request.title, TimerState.FINISHED, request.period, request.goalKey,
                created = now.wall).also { dao.session(it.entity()) }
            val segment = TimerSegment(UUID.randomUUID().toString(), session.id, start, end, manual = true,
                created = now.wall)
            dao.segment(segment.entity())
            store.audit(session.id, TimerAuditKind.MANUAL, now.wall, segment)
        }
    }

    override suspend fun correct(segmentId: String, start: Long, end: Long, bounds: TimerBounds) {
        database.withTransaction {
            val now = clock.now()
            val snapshot = store.snapshot()
            val old = snapshot.segments.first { it.id == segmentId && it.active && it.endWall != null }
            val session = snapshot.sessions.first { it.id == old.sessionId }
            // Correcting purchased time may shorten the record but never create paid time.
            if (session.kind == TimerKind.REWARD) require(end - start <= old.endWall!! - old.startWall)
            TimerRules.validateManual(start, end, now.wall, bounds, snapshot.segments.filter { it.id != segmentId },
                now, clock.zone())
            dao.segment(old.copy(active = false).entity())
            val segment = TimerSegment(UUID.randomUUID().toString(), old.sessionId, start, end, manual = true,
                created = now.wall)
            dao.segment(segment.entity())
            store.audit(old.sessionId, TimerAuditKind.CORRECTED, now.wall, segment, old)
        }
    }

    override suspend fun delete(segmentId: String) {
        database.withTransaction {
            val old = dao.segments().map { it.domain() }.first { it.id == segmentId && it.active && it.endWall != null }
            dao.segment(old.copy(active = false).entity())
            store.audit(old.sessionId, TimerAuditKind.DELETED, clock.wallMillis(), before = old)
        }
    }
}
