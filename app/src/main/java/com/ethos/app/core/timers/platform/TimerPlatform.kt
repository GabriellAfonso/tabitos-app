package com.ethos.app.core.timers.platform

import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import androidx.annotation.StringRes
import com.ethos.app.core.preferences.AppearanceRepository
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
                NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null) },
            NotificationChannel(ALARMS, context.getString(R.string.timer_alarm_channel),
                NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                enableVibration(true)
            }))
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

    /** [publicActions] stay usable on a lock screen that hides the private content. */
    @Suppress("DEPRECATION")
    private fun builder(
        channel: String, @StringRes publicText: Int = R.string.timer_private,
        publicActions: List<Notification.Action> = emptyList(),
    ): Notification.Builder {
        fun base() = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, channel)
            else Notification.Builder(context)
        val public = base().setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(publicText))
        publicActions.forEach(public::addAction)
        return base().setSmallIcon(R.drawable.ic_notification_timer)
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

    /**
     * [alarm] rings on the alarm channel when the user keeps "Alarme ao terminar o cronômetro" on. A ringing alert
     * offers "Parar alarme"; cancelling the notification is what silences its sound (spec 026).
     */
    fun alert(session: TimerSession, kind: TimerAuditKind, alarm: Boolean): Boolean = try {
        val builder = if (ringsAlarm(kind, alarm)) alarmBuilder(session) else builder(ALERTS)
        manager.notify(alertTag(session.id), 0, builder.setAutoCancel(true)
            .setContentTitle(session.title)
            .setContentText(context.getString(if (kind == TimerAuditKind.EXHAUSTED) R.string.timer_exhausted
                else R.string.timer_completed))
            .setContentIntent(open(session.id)).build())
        true
    } catch (_: SecurityException) { false }

    /** Removing the alert stops its sound; a no-op when it was already tapped or dismissed. */
    fun cancelAlert(sessionId: String) = manager.cancel(alertTag(sessionId), 0)

    /**
     * How long the alarm channel sound plays once. Notification sounds never loop, so after this the alert is
     * silent and "Parar alarme" has nothing left to stop. Unknown durations fall back to a conservative guess.
     */
    fun alarmSoundMillis(): Long {
        val sound = if (Build.VERSION.SDK_INT >= 26) manager.getNotificationChannel(ALARMS)?.sound
            else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val duration = sound?.let(::soundDurationMillis) ?: ALARM_FALLBACK_MILLIS
        return duration.coerceIn(ALARM_MIN_MILLIS, ALARM_MAX_MILLIS)
    }

    private fun soundDurationMillis(sound: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, sound)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (_: RuntimeException) { null } finally { retriever.release() }
    }

    /** Re-posts a still visible alarm alert on the silent channel, without the stop action that no longer applies. */
    fun quietAlert(session: TimerSession) {
        val visible = manager.activeNotifications.any { it.tag == alertTag(session.id) }
        if (visible) alert(session, TimerAuditKind.REACHED, alarm = false)
    }

    private fun alarmBuilder(session: TimerSession): Notification.Builder {
        val stopIntent = PendingIntent.getBroadcast(context, session.id.hashCode(),
            Intent(context, TimerActionReceiver::class.java).setAction(ACTION_STOP_ALARM)
                .putExtra(EXTRA_SESSION, session.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = Notification.Action.Builder(null, context.getString(R.string.timer_alarm_stop), stopIntent).build()
        val open = Notification.Action.Builder(null, context.getString(R.string.timer_open), open(session.id)).build()
        // Swiping the alert away silences it too, so the in-app popup must forget it the same way.
        return builder(ALARMS, R.string.timer_alarm_private, listOf(stop)).setCategory(Notification.CATEGORY_ALARM)
            .setDeleteIntent(stopIntent).addAction(stop).addAction(open)
    }

    private fun alertTag(sessionId: String) = "ethos:timer:$sessionId"

    companion object {
        /** Only a reached task or habit goal rings; bought reward time always ends with a silent notice. */
        fun ringsAlarm(kind: TimerAuditKind, alarm: Boolean) = alarm && kind == TimerAuditKind.REACHED

        const val SESSIONS = "ethos_timers"
        const val ALERTS = "ethos_timer_alerts_silent"
        const val ALARMS = "ethos_timer_alarms"
        /** Used when the sound length cannot be read (e.g. a ringtone the app has no access to). */
        private const val ALARM_FALLBACK_MILLIS = 30_000L
        /** Vibration still runs on a silent channel; keeps the popup long enough to be noticed. */
        private const val ALARM_MIN_MILLIS = 5_000L
        private const val ALARM_MAX_MILLIS = 5 * 60_000L
        const val ONGOING_ID = 8001
        const val ACTION_PAUSE = "com.ethos.app.TIMER_PAUSE"
        const val ACTION_STOP_ALARM = "com.ethos.app.TIMER_STOP_ALARM"
        const val EXTRA_SESSION = "session"
    }
}

/** A finished task or habit timer whose alarm notification is still sounding or waiting to be stopped. */
data class TimerAlarm(val sessionId: String, val title: String)

/** Serializes timer syncs, mirrors the running session in the foreground service and posts alerts once. */
@Singleton
class TimerCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context, val repository: TimerRepository,
    private val tasks: TaskRepository, private val habits: HabitRepository, val notifications: TimerNotifications,
    private val preferences: AppearanceRepository,
) {
    private val lock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false
    @Volatile private var closed: Set<Pair<TimerKind, String>> = emptySet()
    val serviceBlocked = MutableStateFlow(false)
    private val ringing = MutableStateFlow<TimerAlarm?>(null)
    /** Kept in memory only: after a process restart the notification itself still offers "Parar alarme". */
    val alarm: StateFlow<TimerAlarm?> = ringing.asStateFlow()

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
            val alarm = preferences.timerAlarm.first()
            result.alerts.forEach { alert ->
                val session = sessions.firstOrNull { it.id == alert.sessionId } ?: return@forEach
                val posted = notifications.alert(session, alert.kind, alarm)
                if (posted && TimerNotifications.ringsAlarm(alert.kind, alarm)) ring(session)
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

    /** Shows the alarm until the user stops it or its sound ends by itself, whichever comes first. */
    private fun ring(session: TimerSession) {
        val alarm = TimerAlarm(session.id, session.title)
        ringing.value = alarm
        scope.launch {
            delay(notifications.alarmSoundMillis())
            // Stopping already cleared it; a newer alarm must not be dropped or silenced by this one.
            if (ringing.compareAndSet(alarm, null)) notifications.quietAlert(session)
        }
    }

    /** Silences and forgets the alarm of [sessionId]; the completion it announced stays as it is. */
    fun stopAlarm(sessionId: String) {
        notifications.cancelAlert(sessionId)
        ringing.update { current -> current?.takeUnless { it.sessionId == sessionId } }
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
        val id = intent.getStringExtra(TimerNotifications.EXTRA_SESSION) ?: return
        when (intent.action) {
            TimerNotifications.ACTION_STOP_ALARM -> coordinator.stopAlarm(id)
            TimerNotifications.ACTION_PAUSE -> {
                val result = goAsync()
                CoroutineScope(Dispatchers.IO).launch { try { coordinator.pause(id) } finally { result.finish() } }
            }
        }
    }
}
