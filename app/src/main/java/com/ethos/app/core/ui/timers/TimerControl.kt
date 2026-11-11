package com.ethos.app.core.ui.timers

import com.ethos.app.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.staticCompositionLocalOf
import com.ethos.app.core.timers.domain.TimerKind

/** Opens the timer sheet for a task occurrence or habit from its owning feature detail. */
val LocalOpenTimer = staticCompositionLocalOf<(TimerKind, String) -> Unit> { { _, _ -> } }

/** Countdown control shown on a timed task/habit card (spec 016); built by the app from timer state. */
data class TimerChip(val remaining: String, val running: Boolean, val onToggle: () -> Unit)

@Composable
fun TimerChipButton(chip: TimerChip, title: String, enabled: Boolean) {
    val description = stringResource(if (chip.running) R.string.timer_chip_pause else R.string.timer_chip_start,
        title, chip.remaining)
    FilledTonalButton(chip.onToggle, enabled = enabled, contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.sizeIn(minHeight = 48.dp).semantics { contentDescription = description }) {
        Text((if (chip.running) "❚❚ " else "▶ ") + chip.remaining)
    }
}
