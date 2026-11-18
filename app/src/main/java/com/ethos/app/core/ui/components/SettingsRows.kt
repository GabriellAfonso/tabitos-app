package com.ethos.app.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.ui.theme.EthosTheme

/** Named group of a settings list (spec 025); announced as a heading by TalkBack. */
@Composable
fun SettingsSectionHeader(title: String) {
    Text(title, Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
        .semantics { heading() },
        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

/**
 * One settings line: icon, title, current value and an optional trailing control. [warning] paints the icon and
 * value in the error color for something the user still has to fix.
 */
@Composable
fun SettingsRow(
    title: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    warning: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val accent = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    ListItem(
        headlineContent = { Text(title) },
        modifier = modifier,
        supportingContent = supporting?.let { { Text(it, color = accent) } },
        leadingContent = { Icon(painterResource(icon), contentDescription = null, tint = accent) },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

/** Row that opens something; [trailingIcon] is a chevron for app pages or "open outside" for Android settings. */
@Composable
fun SettingsLinkRow(
    title: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    supporting: String? = null,
    warning: Boolean = false,
    enabled: Boolean = true,
    @DrawableRes trailingIcon: Int? = R.drawable.ic_chevron_right,
) {
    SettingsRow(title, icon, Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        supporting, warning, trailingIcon?.let { { Icon(painterResource(it), contentDescription = null) } })
}

/** On/off setting; the whole row toggles, so the switch itself carries no separate touch target. */
@Composable
fun SettingsSwitchRow(
    title: String,
    @DrawableRes icon: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    SettingsRow(title, icon,
        Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        supporting) { Switch(checked, onCheckedChange = null, enabled = enabled) }
}

@Preview
@Composable
private fun SettingsRowsPreview() {
    EthosTheme {
        Column {
            SettingsSectionHeader("Notificações e alarmes")
            SettingsRow("Notificações bloqueadas", R.drawable.ic_notifications_off,
                supporting = "Lembretes e resumos não aparecem.", warning = true) {
                FilledTonalButton(onClick = {}) { Text("Permitir") }
            }
            SettingsSwitchRow("Alarme ao terminar o cronômetro", R.drawable.ic_alarm, checked = true,
                onCheckedChange = {}, supporting = "Toca como despertador.")
            SettingsLinkRow("Tema", R.drawable.ic_contrast, onClick = {}, supporting = "Seguir o sistema",
                trailingIcon = null)
            SettingsLinkRow("Hábitos excluídos", R.drawable.ic_self_improvement, onClick = {})
        }
    }
}

@Preview
@Composable
private fun SettingsRowsDarkPreview() {
    EthosTheme(darkTheme = true) {
        Column {
            SettingsSectionHeader("Aparência")
            SettingsLinkRow("Sons e vibração no Android", R.drawable.ic_volume_up, onClick = {},
                supporting = "Lembretes e resumos", trailingIcon = R.drawable.ic_open_in_new)
        }
    }
}
