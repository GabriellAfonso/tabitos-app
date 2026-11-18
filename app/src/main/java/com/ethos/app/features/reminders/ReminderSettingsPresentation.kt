package com.ethos.app.features.reminders

import com.ethos.app.core.reminders.platform.NotificationCapability

/** Which notifications Android is blocking, so Settings explains only what does not arrive (spec 025). */
enum class BlockedNotifications { ALL, REMINDERS, SUMMARIES }

/** Null when reminders and summaries are both allowed: Settings then shows no warning. */
fun NotificationCapability.blockedNotifications(): BlockedNotifications? = when {
    !reminders && !summaries -> BlockedNotifications.ALL
    !reminders -> BlockedNotifications.REMINDERS
    !summaries -> BlockedNotifications.SUMMARIES
    else -> null
}

/** "Horário preciso" appears only when Android denies precise alarms, as the one thing to fix (2026-11-17). */
val NotificationCapability.asksPreciseAlarms: Boolean get() = !exact
