package com.ethos.app.core.ui.reminders

import androidx.compose.runtime.staticCompositionLocalOf
import com.ethos.app.core.reminders.domain.ReminderKind

val LocalOpenReminders = staticCompositionLocalOf<(ReminderKind, String) -> Unit> { { _, _ -> } }
