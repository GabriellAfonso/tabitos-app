package com.tabitos.app.core.reminders.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tabitos.app.core.reminders.domain.*
import com.tabitos.app.core.tasks.domain.*
import com.tabitos.app.core.habits.domain.HabitRepository
import com.tabitos.app.core.trackers.domain.TrackerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderCoordinator @Inject constructor(
    private val repository: ReminderRepository, private val tasks: TaskRepository,
    private val habits: HabitRepository, private val trackers: TrackerRepository, val platform: ReminderPlatform,
    private val clock: TaskClock,
) {
    private val lock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false
    val failure = MutableStateFlow(false)

    fun start() {
        if (started) return
        started = true
        platform.channels()
        scope.launch {
            // Delivery writes must not recursively trigger scheduling; observe configuration only.
            combine(tasks.observe(), habits.observe(), trackers.observe(), repository.observe().map {
                it.definitions to it.settings.summaryMinute }.distinctUntilChanged()) { _, _, _, _ -> Unit }
                // The entry point decides first delivery mode (alarm versus foreground/recovery).
                .drop(1).retryWhen { _, _ -> failure.value = true; delay(30000); true }
                .collect { reconcileSafely(false) }
        }
    }
    fun request() { scope.launch { reconcileSafely(false) } }
    suspend fun sources() =
        ReminderSources(tasks.observe().first(), habits.observe().first(), trackers.observe().first())
    suspend fun reconcileSafely(alarm: Boolean, reviewAt: Long? = null) {
        try { reconcile(alarm, reviewAt); failure.value = false
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { failure.value = true }
    }
    suspend fun reconcile(alarm: Boolean = false, reviewAt: Long? = null) = lock.withLock {
        val now = clock.nowMillis()
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        tasks.execute(TaskAction.Refresh)
        val snapshot = repository.read()
        val currentSources = sources()
        val source = ReminderRules.project(currentSources, snapshot.definitions, today)
        val scheduled = ReminderRules.schedule(source, snapshot.definitions, today, zone)
        val previous = snapshot.deliveries.associateBy { it.key }
        val rows = ReminderRules.reconcileRows(scheduled, snapshot.deliveries, snapshot.definitions,
            source, now, zone)
        repository.deliveries(rows.filter { previous[it.key] != it })
        rows.filter { it.state == DeliveryState.CANCELLED }.forEach { platform.cancel(it.key) }
        val capability = platform.capability()
        val due = rows.filter { it.state == DeliveryState.PENDING && it.dueAt <= now }
        // Tracker slots are today-only (at most one per tracker), so they are shown directly instead of summarized.
        val direct = if (capability.reminders) due.filter {
            (alarm && now - it.dueAt <= 120000) || it.kind == ReminderKind.TRACKER } else emptyList()
        val updates = mutableListOf<ReminderDelivery>()
        direct.forEach { row ->
            val title = when (row.kind) {
                ReminderKind.TASK -> source.tasks.occurrences.first { it.id == row.sourceId }.fields.title
                ReminderKind.HABIT -> source.habits.habits.first { it.id == row.sourceId }.name
                ReminderKind.TRACKER -> ""
            }
            if (platform.show(row, title)) updates += row.copy(state = DeliveryState.POSTED)
        }
        val missed = due.filter { it !in direct }
        // A blocked reminder channel must not be bypassed by routing its content to another channel.
        if (missed.isNotEmpty() && capability.reminders && capability.summaries && platform.recovery(missed.size)) {
            updates += missed.map { it.copy(state = DeliveryState.RECOVERED) }
        }
        repository.deliveries(updates)
        if (missed.isEmpty()) platform.updateRecovery(rows.count { it.state == DeliveryState.RECOVERED })
        reconcileReview(snapshot.settings, currentSources, reviewAt, zone)
        val midnight = ReminderRules.instant(today.plusDays(1), 0, zone)
        val nextDelivery = if (capability.reminders) rows.filter {
            it.state == DeliveryState.PENDING && it.dueAt > now }.minOfOrNull { it.dueAt } else null
        val enabled = snapshot.definitions.any { it.enabled } || snapshot.settings.summaryMinute != null
        platform.schedule(if (enabled) listOfNotNull(midnight, nextDelivery).min() else null)
    }

    private suspend fun reconcileReview(
        settings: ReminderSettings, source: ReminderSources, reviewAt: Long?, zone: ZoneId,
    ) {
        val now = clock.nowMillis()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val tomorrow = today.plusDays(1)
        val minute = settings.summaryMinute
        val slot = minute?.let { ReminderRules.instant(today, it, zone) }
        platform.clearExpiredReview(slot?.takeIf { it <= now })
        if (minute == null || slot == null) {
            platform.scheduleReview(today, null)
            platform.scheduleReview(tomorrow, null)
            return
        }
        val nextDay = if (slot > now) today else tomorrow
        platform.scheduleReview(nextDay, ReminderRules.instant(nextDay, minute, zone))
        if (!ReminderRules.isReviewDue(settings, reviewAt, now, zone)) return
        val eligible = ReminderRules.hasItemsToReview(source, today)
        if (repository.claimReview(today, minute) && eligible) {
            val midnight = tomorrow.atStartOfDay(zone).toInstant().toEpochMilli()
            platform.showReview(slot, midnight - clock.nowMillis())
        }
    }
    suspend fun validate(key: String): ReminderDelivery? = lock.withLock {
        val s = repository.read()
        val row = s.deliveries.firstOrNull { it.key == key && it.state != DeliveryState.CANCELLED }
            ?: return@withLock null
        val today = clock.today()
        val source = ReminderRules.project(sources(), s.definitions, today)
        row.takeIf { ReminderRules.relevant(it, source, today) }
    }
    suspend fun materialize(row: ReminderDelivery) {
        if (row.kind == ReminderKind.TASK) row.anchorDate?.let { tasks.execute(TaskAction.Materialize(it)) }
    }
    suspend fun snooze(key: String, at: Long) = lock.withLock {
        val snapshot = repository.read()
        val row = snapshot.deliveries.firstOrNull { it.key == key } ?: error("Missing reminder")
        val today = clock.today()
        val source = ReminderRules.project(sources(), snapshot.definitions, today)
        require(ReminderRules.relevant(row, source, today))
        if (row.kind != ReminderKind.TASK) require(ReminderRules.relevant(row, source,
            Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()))
        repository.snooze(key, at, clock.nowMillis())
        platform.cancel(key)
    }
}

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var coordinator: ReminderCoordinator
    override fun onReceive(context: Context, intent: Intent) {
        val reviewAt = when (intent.action) {
            ReminderPlatform.REVIEW_WAKE -> intent.getLongExtra(ReminderPlatform.REVIEW_INSTANT, -1L)
            "com.tabitos.app.REMINDER_WAKE" -> null
            else -> return
        }
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { coordinator.reconcileSafely(true, reviewAt) } finally { result.finish() }
        }
    }
}

@AndroidEntryPoint
class ReminderRecoveryReceiver : BroadcastReceiver() {
    @Inject lateinit var coordinator: ReminderCoordinator
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED")) return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch { try { coordinator.reconcileSafely(false) } finally { result.finish() } }
    }
}
