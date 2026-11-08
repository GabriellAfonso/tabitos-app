package com.ethos.app.core.timers.platform

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import com.ethos.app.MainActivity
import com.ethos.app.R
import com.ethos.app.core.habits.domain.HabitRepository
import com.ethos.app.core.habits.domain.HabitStatus
import com.ethos.app.core.tasks.domain.TaskRepository
import com.ethos.app.core.tasks.domain.TaskStatus
import com.ethos.app.core.timers.domain.*
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AndroidTimerClock(private val context: Context? = null) : TimerClock {
    override fun wallMillis() = System.currentTimeMillis()
    override fun elapsedMillis() = SystemClock.elapsedRealtime()
    override fun bootCount(): Int? = context?.let {
        Settings.Global.getInt(it.contentResolver, Settings.Global.BOOT_COUNT, -1).takeIf { count -> count >= 0 }
    }
    override fun zone(): ZoneId = ZoneId.systemDefault()
}

@Singleton
class TimerNotifications @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val manager get() = context.getSystemService(NotificationManager::class.java)

    fun channels() {
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannels(listOf(
            NotificationChannel(SESSIONS, context.getString(R.string.timer_channel),
                NotificationManager.IMPORTANCE_LOW),
            NotificationChannel(ALERTS, context.getString(R.string.timer_alert_channel),
                NotificationManager.IMPORTANCE_DEFAULT)))
    }

    /** Whether session notifications can actually be shown; counting itself never depends on it. */
    fun visible(): Boolean {
        channels()
        val permission = Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permission && manager.areNotificationsEnabled() && (Build.VERSION.SDK_INT < 26 ||
            manager.getNotificationChannel(SESSIONS)?.importance != NotificationManager.IMPORTANCE_NONE)
    }

    private fun open(sessionId: String) = PendingIntent.getActivity(context, sessionId.hashCode(),
        Intent(context, MainActivity::class.java).setAction("com.ethos.app.TIMER_OPEN")
            .setData(Uri.Builder().scheme("ethos-timer").authority("local").appendPath("OPEN")
                .appendPath(sessionId).build())
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    @Suppress("DEPRECATION")
    private fun builder(channel: String): Notification.Builder {
        fun base() = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, channel)
            else Notification.Builder(context)
        val public = base().setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.timer_private))
        return base().setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(public.build())
    }

    fun placeholder(): Notification = builder(SESSIONS).setContentTitle(context.getString(R.string.timer_running))
        .setOngoing(true).build()

    fun ongoing(session: TimerSession, total: Long, remaining: Long?, now: Long): Notification {
        val b = builder(SESSIONS).setContentTitle(session.title).setOngoing(true).setOnlyAlertOnce(true)
            .setContentText(context.getString(if (remaining != null) R.string.timer_notice_remaining
                else R.string.timer_notice_elapsed))
            .setUsesChronometer(true).setShowWhen(true).setContentIntent(open(session.id))
        if (remaining != null) b.setChronometerCountDown(true).setWhen(now + remaining)
        else b.setWhen(now - total)
        val pause = PendingIntent.getBroadcast(context, session.id.hashCode(),
            Intent(context, TimerActionReceiver::class.java).setAction(ACTION_PAUSE)
                .putExtra(EXTRA_SESSION, session.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        b.addAction(Notification.Action.Builder(null, context.getString(R.string.timer_pause), pause).build())
        b.addAction(Notification.Action.Builder(null, context.getString(R.string.timer_open), open(session.id)).build())
        return b.build()
    }

    fun alert(session: TimerSession, kind: TimerAuditKind): Boolean = try {
        manager.notify("ethos:timer:${session.id}", 0, builder(ALERTS).setAutoCancel(true)
            .setContentTitle(session.title)
            .setContentText(context.getString(if (kind == TimerAuditKind.EXHAUSTED) R.string.timer_exhausted
                else R.string.timer_reached))
            .setContentIntent(open(session.id)).build())
        true
    } catch (_: SecurityException) { false }

    companion object {
        const val SESSIONS = "ethos_timers"
        const val ALERTS = "ethos_timer_alerts"
        const val ONGOING_ID = 8001
        const val ACTION_PAUSE = "com.ethos.app.TIMER_PAUSE"
        const val EXTRA_SESSION = "session"
    }
}

/** Serializes timer syncs, mirrors the running session in the foreground service and posts alerts once. */
@Singleton
class TimerCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context, val repository: TimerRepository,
    private val tasks: TaskRepository, private val habits: HabitRepository, val notifications: TimerNotifications,
) {
    private val lock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false
    @Volatile private var closed: Set<Pair<TimerKind, String>> = emptySet()
    val serviceBlocked = MutableStateFlow(false)

    fun start() {
        if (started) return
        started = true
        notifications.channels()
        scope.launch {
            combine(tasks.observe(), habits.observe()) { t, h ->
                t.occurrences.filter { it.status != TaskStatus.OPEN }.map { TimerKind.TASK to it.id }.toSet() +
                    h.habits.filter { it.status != HabitStatus.ACTIVE }.map { TimerKind.HABIT to it.id }
            }.distinctUntilChanged().retryWhen { _, _ -> delay(30_000); true }.collect {
                closed = it
                syncSafely()
            }
        }
        scope.launch {
            // Starts from any feature (including reward purchases) reach the service through committed state.
            repository.observe().map { it.running?.id }.distinctUntilChanged().drop(1)
                .retryWhen { _, _ -> delay(30_000); true }.collect { syncSafely() }
        }
        request()
    }

    fun request() { scope.launch { syncSafely() } }

    suspend fun syncSafely(manageService: Boolean = true): TimerSync? = try { sync(manageService)
    } catch (e: CancellationException) { throw e
    } catch (_: Exception) { null }

    suspend fun sync(manageService: Boolean = true): TimerSync = lock.withLock {
        val result = repository.sync(closed)
        if (result.alerts.isNotEmpty()) {
            val sessions = repository.observe().first().sessions
            result.alerts.forEach { alert ->
                sessions.firstOrNull { it.id == alert.sessionId }?.let { notifications.alert(it, alert.kind) }
            }
        }
        if (!manageService) return@withLock result
        if (result.running != null) ensureService()
        else context.stopService(Intent(context, TimerService::class.java))
        result
    }

    private fun ensureService() {
        try {
            val intent = Intent(context, TimerService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
            serviceBlocked.value = false
        } catch (_: IllegalStateException) {
            // Android may refuse a background start; time stays correct and the app retries when visible.
            serviceBlocked.value = true
        } catch (_: SecurityException) { serviceBlocked.value = true }
    }

    suspend fun pause(sessionId: String) {
        repository.pause(sessionId)
        syncSafely()
    }
}

@AndroidEntryPoint
class TimerService : Service() {
    @Inject lateinit var coordinator: TimerCoordinator
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null
    private val shutdown = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_SHUTDOWN) return
            val result = goAsync()
            scope.launch { try { coordinator.repository.checkpoint() } finally { result.finish() } }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(Intent.ACTION_SHUTDOWN)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(shutdown, filter, RECEIVER_NOT_EXPORTED)
        else @Suppress("UnspecifiedRegisterReceiverFlag") registerReceiver(shutdown, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        coordinator.notifications.channels()
        val placeholder = coordinator.notifications.placeholder()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(TimerNotifications.ONGOING_ID, placeholder, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else startForeground(TimerNotifications.ONGOING_ID, placeholder)
        if (loop?.isActive != true) loop = scope.launch { run() }
        return START_STICKY
    }

    private suspend fun run() {
        val manager = getSystemService(NotificationManager::class.java)
        while (true) {
            val result = coordinator.syncSafely(manageService = false)
            val running = result?.running
            if (result != null && running == null) break
            if (running != null) {
                val snapshot = coordinator.repository.observe().first()
                val clock = AndroidTimerClock(this)
                val now = clock.now()
                val total = TimerRules.sessionTotal(snapshot, running.id, now)
                val remaining = TimerRules.remaining(snapshot, running, now, clock.zone())
                try {
                    manager.notify(TimerNotifications.ONGOING_ID,
                        coordinator.notifications.ongoing(running, total, remaining, now.wall))
                } catch (_: SecurityException) { }
            }
            val wait = result?.nextStopMillis?.coerceIn(50, TimerRules.CHECKPOINT_MILLIS)
                ?: TimerRules.CHECKPOINT_MILLIS
            delay(wait)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        unregisterReceiver(shutdown)
        scope.cancel()
        super.onDestroy()
    }
}

@AndroidEntryPoint
class TimerActionReceiver : BroadcastReceiver() {
    @Inject lateinit var coordinator: TimerCoordinator
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TimerNotifications.ACTION_PAUSE) return
        val id = intent.getStringExtra(TimerNotifications.EXTRA_SESSION) ?: return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch { try { coordinator.pause(id) } finally { result.finish() } }
    }
}
