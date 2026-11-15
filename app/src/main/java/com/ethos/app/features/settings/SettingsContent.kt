package com.ethos.app.features.settings

import androidx.compose.material3.Switch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.preferences.AppearanceMode
import com.ethos.app.core.ui.theme.EthosTheme

@Composable
fun SettingsContent(state: AppearanceUiState, onSelect: (AppearanceMode) -> Unit, onRetry: () -> Unit) {
    Text(stringResource(R.string.appearance), style = MaterialTheme.typography.headlineSmall)
    Text(stringResource(R.string.appearance_hint), style = MaterialTheme.typography.bodyMedium)
    Column(Modifier.selectableGroup()) {
        AppearanceMode.entries.forEach { mode ->
            val title = when (mode) {
                AppearanceMode.SYSTEM -> R.string.theme_system
                AppearanceMode.LIGHT -> R.string.theme_light
                AppearanceMode.DARK -> R.string.theme_dark
            }
            Row(
                Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp).selectable(
                    selected = state.mode == mode,
                    enabled = !state.loading && !state.saving && state.error != AppearanceError.READ,
                    role = Role.RadioButton,
                    onClick = { onSelect(mode) },
                ).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RadioButton(selected = state.mode == mode, onClick = null)
                Text(stringResource(title))
            }
        }
    }
    if (state.loading || state.saving) {
        Text(stringResource(if (state.loading) R.string.loading_preference else R.string.saving_preference))
    }
    state.error?.let { error ->
        Card(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(if (error == AppearanceError.READ) R.string.read_error else R.string.write_error))
                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}

@Preview
@Composable
private fun SettingsPreview() {
    EthosTheme { Column { SettingsContent(AppearanceUiState(loading = false), {}, {}) } }
}

@Preview
@Composable
private fun SettingsFailurePreview() {
    EthosTheme(darkTheme = true) {
        Column { SettingsContent(AppearanceUiState(loading = false, error = AppearanceError.WRITE), {}, {}) }
    }
}

/** Global switch for the timer alarm (spec 016): rings when a timed task or habit finishes. */
@Composable
fun TimerAlarmSetting(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp)
        .toggleable(enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.timer_alarm_setting), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.timer_alarm_setting_hint), style = MaterialTheme.typography.bodySmall)
        }
        Switch(enabled, onCheckedChange = null)
    }
}

/** Row that opens a settings sub-page (coin history, excluded habits); the whole row is the touch target. */
@Composable
fun SettingsLink(@StringRes title: Int, @DrawableRes icon: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview
@Composable
private fun SettingsLinkPreview() {
    EthosTheme { SettingsLink(R.string.history, R.drawable.ic_history) {} }
}
