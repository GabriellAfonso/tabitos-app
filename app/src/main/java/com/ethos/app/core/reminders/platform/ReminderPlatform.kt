package com.ethos.app.core.reminders.platform

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.ethos.app.MainActivity
import com.ethos.app.R
import com.ethos.app.core.reminders.domain.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationCapability(val reminders: Boolean, val summaries: Boolean, val exact: Boolean)

@Singleton
class ReminderPlatform @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)
    private val manager get() = context.getSystemService(NotificationManager::class.java)

    fun channels() {
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannels(listOf(
            NotificationChannel(REMINDERS, context.getString(R.string.reminders),
                NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(SUMMARIES, context.getString(R.string.reminder_summaries),
                NotificationManager.IMPORTANCE_DEFAULT)))
    }
    fun capability(): NotificationCapability {
        channels()
        val permission = Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val enabled = permission && manager.areNotificationsEnabled()
        fun allowed(channel: String) = enabled && (Build.VERSION.SDK_INT < 26 ||
            manager.getNotificationChannel(channel)?.importance != NotificationManager.IMPORTANCE_NONE)
        return NotificationCapability(allowed(REMINDERS), allowed(SUMMARIES),
            Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms())
    }
    private fun wake() = PendingIntent.getBroadcast(context, 0,
        Intent(context, ReminderAlarmReceiver::class.java).setAction("com.ethos.app.REMINDER_WAKE"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun schedule(at: Long?) {
        val intent = wake()
        alarms.cancel(intent)
        if (at == null) return
        try {
            if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms())
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } catch (_: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }
    private fun activity(key: String, action: String): PendingIntent {
        val uri = Uri.Builder().scheme("ethos-reminder").authority("local")
            .appendPath(action).appendPath(key).build()
        return PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
            .setAction("com.ethos.app.REMINDER_ACTION").setData(uri)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    @Suppress("DEPRECATION")
    private fun builder(channel: String): Notification.Builder {
        val public = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, channel)
            else Notification.Builder(context)
        public.setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.reminder_private))
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, channel)
            else Notification.Builder(context)
        return b.setSmallIcon(R.drawable.ic_notification_reminder).setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(public.build())
            .setOnlyAlertOnce(true).setShowWhen(true)
    }
    fun show(row: ReminderDelivery, title: String): Boolean {
        if (!capability().reminders) return false
        if (row.kind == ReminderKind.TRACKER) return post(row.key, tracker(row))
        val b = builder(REMINDERS).setContentTitle(title)
            .setContentText(context.getString(R.string.reminder_notice))
            .setWhen(row.dueAt).setContentIntent(activity(row.key, "OPEN"))
        if (row.kind == ReminderKind.TASK) b.addAction(Notification.Action.Builder(null,
            context.getString(R.string.reminder_complete), activity(row.key, "COMPLETE")).build())
        b.addAction(Notification.Action.Builder(null, context.getString(R.string.reminder_snooze),
            activity(row.key, "SNOOZE")).build())
        b.addAction(Notification.Action.Builder(null, context.getString(R.string.reminder_open),
            activity(row.key, "OPEN")).build())
        return post(row.key, b.build())
    }
    /** Neutral notice: no habit name, question or answer; ongoing is best-effort (dismissible on Android 14+). */
    fun tracker(row: ReminderDelivery): Notification = builder(REMINDERS)
        .setContentTitle(context.getString(R.string.app_name)).setContentText(context.getString(R.string.tracker_notice)).setWhen(row.dueAt)
        .setOngoing(true).setAutoCancel(false).setContentIntent(activity(row.key, "FILL"))
        .addAction(Notification.Action.Builder(null, context.getString(R.string.tracker_fill),
            activity(row.key, "FILL")).build())
        .addAction(Notification.Action.Builder(null, context.getString(R.string.reminder_snooze),
            activity(row.key, "SNOOZE")).build())
        .build()
    fun summary(summary: DaySummary): Boolean {
        if (!capability().summaries) return false
        return post(DAILY_TAG, builder(SUMMARIES).setContentTitle(context.getString(R.string.reminder_daily))
            .setContentText(context.getString(R.string.reminder_counts, summary.tasks, summary.habits, summary.reviews))
            .setContentIntent(activity("daily", "HOME")).build())
    }
    fun recovery(count: Int): Boolean {
        if (!capability().summaries) return false
        return post(RECOVERY_TAG, builder(SUMMARIES).setContentTitle(context.getString(R.string.reminder_recovery))
            .setContentText(context.resources.getQuantityString(R.plurals.reminder_recovery_count, count, count))
            .setContentIntent(activity("recovery", "HOME")).build())
    }
    private fun post(tag: String, notification: Notification): Boolean = try {
        manager.notify(tag, 0, notification)
        true
    } catch (_: SecurityException) { false }
    fun cancel(key: String) { manager.cancel(key, 0) }
    fun updateRecovery(count: Int) {
        if (count == 0) cancel(RECOVERY_TAG)
        else if (manager.activeNotifications.any { it.tag == RECOVERY_TAG }) recovery(count)
    }

    companion object {
        const val REMINDERS = "ethos_reminders"
        const val SUMMARIES = "ethos_summaries"
        const val DAILY_TAG = "ethos:daily"
        const val RECOVERY_TAG = "ethos:recovery"
    }
}
