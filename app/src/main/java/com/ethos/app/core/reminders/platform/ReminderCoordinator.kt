package com.ethos.app.core.reminders.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ethos.app.core.reminders.domain.*
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.habits.domain.HabitRepository
import com.ethos.app.core.trackers.domain.TrackerRepository
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
    /** Reset and stale notification cleanup cannot race with reconciliation/delivery. */
    suspend fun replaceLocalData(replace: suspend () -> Unit) {
        lock.withLock {
            val old = repository.read().deliveries
            replace()
            old.forEach { platform.cancel(it.key) }
            platform.cancel(ReminderPlatform.DAILY_TAG)
            platform.cancel(ReminderPlatform.RECOVERY_TAG)
            platform.schedule(null)
        }
        request()
    }
    suspend fun sources() =
        ReminderSources(tasks.observe().first(), habits.observe().first(), trackers.observe().first())
    suspend fun reconcileSafely(alarm: Boolean) {
        try { reconcile(alarm); failure.value = false
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { failure.value = true }
    }
    suspend fun reconcile(alarm: Boolean = false) = lock.withLock {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        tasks.execute(TaskAction.Refresh)
        val snapshot = repository.read()
        val source = ReminderRules.project(sources(), snapshot.definitions, today)
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
        val minute = snapshot.settings.summaryMinute
        if (minute == null) platform.cancel(ReminderPlatform.DAILY_TAG)
        if (minute != null && snapshot.settings.lastSummaryDate != today &&
            ReminderRules.instant(today, minute, zone) <= now && capability.summaries) {
            if (platform.summary(ReminderRules.summary(source, today))) repository.summaryDelivered(today)
        }
        val midnight = ReminderRules.instant(today.plusDays(1), 0, zone)
        val nextDelivery = if (capability.reminders) rows.filter {
            it.state == DeliveryState.PENDING && it.dueAt > now }.minOfOrNull { it.dueAt } else null
        val nextSummary = minute?.let {
            ReminderRules.instant(if (ReminderRules.instant(today, it, zone) > now) today else today.plusDays(1),
                it, zone)
        }
        val enabled = snapshot.definitions.any { it.enabled } || minute != null
        platform.schedule(if (enabled) listOfNotNull(midnight, nextDelivery, nextSummary).min() else null)
    }
    suspend fun validate(key: String): ReminderDelivery? = lock.withLock {
        val s = repository.read()
        val row = s.deliveries.firstOrNull { it.key == key && it.state != DeliveryState.CANCELLED }
            ?: return@withLock null
        val today = LocalDate.now()
        val source = ReminderRules.project(sources(), s.definitions, today)
        row.takeIf { ReminderRules.relevant(it, source, today) }
    }
    suspend fun materialize(row: ReminderDelivery) {
        if (row.kind == ReminderKind.TASK) row.anchorDate?.let { tasks.execute(TaskAction.Materialize(it)) }
    }
    suspend fun snooze(key: String, at: Long) = lock.withLock {
        val snapshot = repository.read()
        val row = snapshot.deliveries.firstOrNull { it.key == key } ?: error("Missing reminder")
        val today = LocalDate.now()
        val source = ReminderRules.project(sources(), snapshot.definitions, today)
        require(ReminderRules.relevant(row, source, today))
        if (row.kind != ReminderKind.TASK) require(ReminderRules.relevant(row, source,
            Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()))
        repository.snooze(key, at, System.currentTimeMillis())
        platform.cancel(key)
    }
}

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var coordinator: ReminderCoordinator
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.ethos.app.REMINDER_WAKE") return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch { try { coordinator.reconcileSafely(true) } finally { result.finish() } }
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
