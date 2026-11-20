package com.tabitos.app.features.reminders

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.tabitos.app.R
import com.tabitos.app.core.preferences.TaskReminderDefaults
import com.tabitos.app.core.reminders.domain.ReminderSettings
import com.tabitos.app.core.reminders.domain.ReminderSnapshot
import com.tabitos.app.core.reminders.platform.NotificationCapability
import com.tabitos.app.core.reminders.platform.ReminderPlatform
import com.tabitos.app.core.ui.components.SettingsLinkRow
import com.tabitos.app.core.ui.components.SettingsRow
import com.tabitos.app.core.ui.components.TimePickerDialog
import com.tabitos.app.core.ui.theme.TabitosTheme
import java.time.LocalTime

/** What the notification rows of Settings can do; Android screens are opened by the caller's Context. */
class NotificationSettingsActions(
    val allowNotifications: () -> Unit,
    val openExactAlarms: () -> Unit,
    val openChannel: (String) -> Unit,
    val saveSummary: (String?) -> Unit,
    val retry: () -> Unit,
    val changeDefaults: (TaskReminderDefaults?) -> Unit,
    val saveDefaults: () -> Unit,
)

/**
 * "Permitir" asks for the runtime permission while it is missing; once granted, a block can only come from
 * Android's own switches, so it opens the app's notification settings instead.
 */
@Composable
fun notificationSettingsActions(vm: ReminderViewModel, requestPermission: () -> Unit): NotificationSettingsActions {
    val context = LocalContext.current
    return NotificationSettingsActions(
        allowNotifications = {
            if (context.needsNotificationPermission()) requestPermission()
            else context.openAndroidSettings(context.appNotificationSettings())
        },
        openExactAlarms = {
            if (Build.VERSION.SDK_INT >= 31) {
                context.openAndroidSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, context.packageUri()))
            }
        },
        openChannel = { channel -> context.openAndroidSettings(context.channelSettings(channel)) },
        saveSummary = { time -> vm.summary(time, requestPermission) },
        retry = vm::retry,
        changeDefaults = vm::changeDefaultsDraft,
        saveDefaults = { vm.saveDefaults(requestPermission) },
    )
}

private fun Context.packageUri() = "package:$packageName".toUri()

private fun Context.appDetails() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri())

private fun Context.appNotificationSettings(): Intent = if (Build.VERSION.SDK_INT >= 26) {
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
} else appDetails()

private fun Context.channelSettings(channel: String): Intent = if (Build.VERSION.SDK_INT >= 26) {
    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .putExtra(Settings.EXTRA_CHANNEL_ID, channel)
} else appDetails()

/** Some manufacturers remove specific settings screens; the app details page always exists. */
private fun Context.openAndroidSettings(intent: Intent) {
    try { startActivity(intent) } catch (_: ActivityNotFoundException) { startActivity(appDetails()) }
}

/** Blocked warning (only when something is blocked), storage errors and the daily summary. */
@Composable
fun NotificationSettings(state: ReminderUiState, actions: NotificationSettingsActions) {
    state.capability.blockedNotifications()?.let { BlockedNotificationsRow(it, actions.allowNotifications) }
    Column(Modifier.padding(horizontal = 16.dp)) { ReminderErrors(state, actions.retry) }
    DefaultReminderSettings(state, actions)
    DailySummarySetting(state, actions.saveSummary)
}

@StringRes
private fun BlockedNotifications.explanation(): Int = when (this) {
    BlockedNotifications.ALL -> R.string.notifications_blocked_all
    BlockedNotifications.REMINDERS -> R.string.notifications_blocked_reminders
    BlockedNotifications.SUMMARIES -> R.string.notifications_blocked_summaries
}

@Composable
private fun BlockedNotificationsRow(blocked: BlockedNotifications, allow: () -> Unit) {
    SettingsRow(stringResource(R.string.notifications_blocked), R.drawable.ic_notifications_off,
        supporting = stringResource(blocked.explanation()), warning = true) {
        FilledTonalButton(onClick = allow) { Text(stringResource(R.string.notifications_allow)) }
    }
}

/** Tapping the row (or turning the switch on) picks the time; saving it is what turns the summary on. */
@Composable
private fun DailySummarySetting(state: ReminderUiState, save: (String?) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val time = state.snapshot.settings.summaryMinute?.let { LocalTime.of(it / 60, it % 60) }
    val enabled = !state.busy && !state.loading
    val title = stringResource(R.string.reminder_daily)
    val description = stringResource(R.string.review_setting_description)
    val schedule = time?.let { stringResource(R.string.summary_on, it.toString()) }
    SettingsRow(title, R.drawable.ic_checklist,
        Modifier.clickable(enabled = enabled, role = Role.Button) { picking = true },
        supporting = listOfNotNull(description, schedule).joinToString("\n")) {
        Switch(time != null, onCheckedChange = { on -> if (on) picking = true else save(null) }, enabled = enabled,
            modifier = Modifier.semantics { contentDescription = title })
    }
    if (picking) TimePickerDialog(stringResource(R.string.summary_time), time, enabled,
        onConfirm = { picking = false; save(it.toString()) }, onDismiss = { picking = false })
}

/** Precise alarm access (only while denied) and the Android channel screens, after the app's own switches. */
@Composable
fun NotificationSystemSettings(state: ReminderUiState, actions: NotificationSettingsActions) {
    if (state.capability.asksPreciseAlarms) {
        SettingsLinkRow(stringResource(R.string.exact_setting), R.drawable.ic_schedule, actions.openExactAlarms,
            supporting = stringResource(R.string.exact_off), warning = true, trailingIcon = R.drawable.ic_open_in_new)
    }
    var choosing by rememberSaveable { mutableStateOf(false) }
    SettingsLinkRow(stringResource(R.string.channels_setting), R.drawable.ic_volume_up, { choosing = true },
        supporting = stringResource(R.string.channels_setting_hint), trailingIcon = R.drawable.ic_open_in_new)
    if (choosing) {
        ChannelDialog(onChoose = { choosing = false; actions.openChannel(it) }, onDismiss = { choosing = false })
    }
}

@Composable
private fun ChannelDialog(onChoose: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.channels_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.channels_dialog_hint), style = MaterialTheme.typography.bodyMedium)
                SettingsLinkRow(stringResource(R.string.reminders), R.drawable.ic_notification_reminder,
                    { onChoose(ReminderPlatform.REMINDERS) }, trailingIcon = R.drawable.ic_open_in_new)
                SettingsLinkRow(stringResource(R.string.reminder_summaries), R.drawable.ic_wb_sunny,
                    { onChoose(ReminderPlatform.SUMMARIES) }, trailingIcon = R.drawable.ic_open_in_new)
            }
        },
        confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

private val previewActions = NotificationSettingsActions({}, {}, {}, {}, {}, {}, {})

@Preview
@Composable
private fun NotificationSettingsPreview() {
    TabitosTheme {
        Column {
            val state = ReminderUiState(loading = false, capability = NotificationCapability(true, true, false),
                snapshot = ReminderSnapshot(settings = ReminderSettings(summaryMinute = 8 * 60)))
            NotificationSettings(state, previewActions)
            NotificationSystemSettings(state, previewActions)
        }
    }
}

@Preview
@Composable
private fun NotificationSettingsBlockedPreview() {
    TabitosTheme(darkTheme = true) {
        Column {
            val state = ReminderUiState(loading = false, capability = NotificationCapability(false, false, true))
            NotificationSettings(state, previewActions)
            NotificationSystemSettings(state, previewActions)
        }
    }
}
