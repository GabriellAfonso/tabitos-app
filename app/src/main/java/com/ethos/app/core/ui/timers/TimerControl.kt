package com.ethos.app.core.ui.timers

import androidx.compose.runtime.staticCompositionLocalOf
import com.ethos.app.core.timers.domain.TimerKind

/** Opens the timer sheet for a task occurrence or habit from its owning feature detail. */
val LocalOpenTimer = staticCompositionLocalOf<(TimerKind, String) -> Unit> { { _, _ -> } }
