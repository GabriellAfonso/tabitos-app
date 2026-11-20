package com.tabitos.app.core.ui.reminders

import androidx.compose.runtime.staticCompositionLocalOf
import com.tabitos.app.core.reminders.domain.ReminderKind

val LocalOpenReminders = staticCompositionLocalOf<(ReminderKind, String) -> Unit> { { _, _ -> } }
