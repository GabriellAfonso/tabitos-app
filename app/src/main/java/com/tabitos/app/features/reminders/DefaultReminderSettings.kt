package com.tabitos.app.features.reminders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tabitos.app.R
import com.tabitos.app.core.preferences.TaskReminderDefaults
import com.tabitos.app.core.ui.components.SettingsLinkRow
import com.tabitos.app.core.ui.components.SettingsSectionHeader
import com.tabitos.app.core.ui.components.SettingsSwitchRow
import com.tabitos.app.core.ui.components.TimePickerDialog
import java.time.LocalTime

@Composable
internal fun DefaultReminderSettings(state: ReminderUiState, actions: NotificationSettingsActions) {
    SettingsLinkRow(
        title = stringResource(R.string.task_reminder_defaults),
        icon = R.drawable.ic_notification_reminder,
        onClick = {
            if (state.defaultsReadError) actions.retry() else actions.changeDefaults(state.defaults)
        },
        supporting = stringResource(
            if (state.defaultsReadError) R.string.task_reminder_defaults_read_error else R.string.task_reminder_new_only
        ),
        warning = state.defaultsReadError,
        enabled = !state.busy && (state.defaults != null || state.defaultsReadError),
    )
    state.defaultsDraft?.let { draft -> DefaultsDialog(draft, state, actions) }
}

@Composable
private fun DefaultsDialog(draft: TaskReminderDefaults, state: ReminderUiState, actions: NotificationSettingsActions) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val close = { if (!state.busy) actions.changeDefaults(null) }
    val time = draft.dateOnlyMinute?.let { LocalTime.ofSecondOfDay(it * 60L) }
    val dayLabel = stringResource(R.string.task_reminder_day_time)
    AlertDialog(
        onDismissRequest = close,
        title = { Text(stringResource(R.string.task_reminder_defaults)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (state.error) Text(stringResource(R.string.reminder_storage_error))
                SettingsSectionHeader(stringResource(R.string.task_reminder_with_time))
                OffsetSwitch(TaskReminderDefaults.HALF_HOUR, stringResource(R.string.task_reminder_half_hour),
                    draft, !state.busy, actions.changeDefaults)
                OffsetSwitch(TaskReminderDefaults.ONE_DAY, stringResource(R.string.task_reminder_one_day),
                    draft, !state.busy, actions.changeDefaults)
                SettingsSectionHeader(stringResource(R.string.task_reminder_without_time))
                SettingsLinkRow(title = dayLabel, icon = R.drawable.ic_schedule, onClick = { picking = true },
                    supporting = time?.toString() ?: stringResource(R.string.summary_off), enabled = !state.busy)
            }
        },
        confirmButton = {
            TextButton(onClick = actions.saveDefaults, enabled = !state.busy) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = close, enabled = !state.busy) { Text(stringResource(R.string.cancel)) }
        },
    )
    if (picking) {
        TimePickerDialog(title = dayLabel, initial = time, enabled = !state.busy, onConfirm = {
            actions.changeDefaults(draft.copy(dateOnlyMinute = it.toSecondOfDay() / 60))
            picking = false
        }, onDismiss = { picking = false }, onRemove = time?.let {
            {
                actions.changeDefaults(draft.copy(dateOnlyMinute = null))
                picking = false
            }
        })
    }
}

@Composable
private fun OffsetSwitch(
    minutes: Int, label: String, draft: TaskReminderDefaults, enabled: Boolean, change: (TaskReminderDefaults) -> Unit,
) {
    SettingsSwitchRow(title = label, icon = R.drawable.ic_schedule, checked = minutes in draft.offsetMinutes,
        enabled = enabled, onCheckedChange = { checked ->
            val offsets = if (checked) draft.offsetMinutes + minutes else draft.offsetMinutes - minutes
            change(draft.copy(offsetMinutes = offsets))
        })
}
