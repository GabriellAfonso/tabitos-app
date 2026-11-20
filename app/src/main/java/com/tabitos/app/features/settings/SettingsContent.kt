package com.tabitos.app.features.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tabitos.app.R
import com.tabitos.app.core.preferences.AppearanceMode
import com.tabitos.app.core.ui.components.SettingsLinkRow
import com.tabitos.app.core.ui.components.SettingsSwitchRow
import com.tabitos.app.core.ui.theme.TabitosTheme

/** Callbacks of the Activity-level appearance ViewModel, which also owns the timer alarm preference. */
class AppearanceActions(
    val select: (AppearanceMode) -> Unit,
    val retry: () -> Unit,
    val setTimerAlarm: (Boolean) -> Unit,
)

/** "Tema" row (spec 025): shows the current choice and opens the three options in a dialog. */
@Composable
fun ThemeSetting(state: AppearanceUiState, onSelect: (AppearanceMode) -> Unit, onRetry: () -> Unit) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    val value = when {
        state.loading -> R.string.loading_preference
        state.saving -> R.string.saving_preference
        else -> themeLabel(state.mode)
    }
    SettingsLinkRow(stringResource(R.string.theme_setting), R.drawable.ic_contrast, onClick = { choosing = true },
        supporting = stringResource(value), enabled = state.canChoose, trailingIcon = null)
    state.error?.let { error -> AppearanceErrorCard(error, onRetry) }
    if (choosing) {
        ThemeDialog(state.mode, onSelect = { choosing = false; onSelect(it) }, onDismiss = { choosing = false })
    }
}

@StringRes
private fun themeLabel(mode: AppearanceMode): Int = when (mode) {
    AppearanceMode.SYSTEM -> R.string.theme_system
    AppearanceMode.LIGHT -> R.string.theme_light
    AppearanceMode.DARK -> R.string.theme_dark
}

@Composable
private fun ThemeDialog(selected: AppearanceMode, onSelect: (AppearanceMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_setting)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.appearance_hint), style = MaterialTheme.typography.bodyMedium)
                Column(Modifier.selectableGroup()) {
                    AppearanceMode.entries.forEach { mode -> ThemeOption(mode, mode == selected) { onSelect(mode) } }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.picker_cancel)) } },
    )
}

@Composable
private fun ThemeOption(mode: AppearanceMode, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(stringResource(themeLabel(mode)))
    }
}

@Composable
private fun AppearanceErrorCard(error: AppearanceError, onRetry: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(if (error == AppearanceError.READ) R.string.read_error else R.string.write_error))
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

/** Global switch for the timer alarm (spec 016): rings when a timed task or habit finishes. */
@Composable
fun TimerAlarmSetting(enabled: Boolean, onChange: (Boolean) -> Unit) {
    SettingsSwitchRow(stringResource(R.string.timer_alarm_setting), R.drawable.ic_alarm, enabled, onChange)
}

@Preview
@Composable
private fun ThemeSettingPreview() {
    TabitosTheme {
        Column {
            ThemeSetting(AppearanceUiState(mode = AppearanceMode.DARK, loading = false), {}, {})
            TimerAlarmSetting(enabled = true) {}
        }
    }
}

@Preview
@Composable
private fun ThemeSettingFailurePreview() {
    TabitosTheme(darkTheme = true) {
        Column { ThemeSetting(AppearanceUiState(loading = false, error = AppearanceError.WRITE), {}, {}) }
    }
}
