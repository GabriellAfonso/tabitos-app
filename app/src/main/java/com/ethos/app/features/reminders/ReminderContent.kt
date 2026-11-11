package com.ethos.app.features.reminders

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.ethos.app.core.ui.components.ScrollableTimeField
import com.ethos.app.R
import com.ethos.app.core.reminders.domain.*
import com.ethos.app.core.reminders.platform.ReminderPlatform
import com.ethos.app.core.ui.theme.EthosTheme
import java.time.LocalTime

/**
 * Returns the in-context notification permission request and refreshes reminder capability on resume.
 * Asking only happens when a feature that needs it is configured (AGENTS: permissions).
 */
@Composable
fun rememberNotificationPermissionRequest(refresh: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { refresh() }
    }
    return {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED)
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
fun ReminderSettingsContent(state: ReminderUiState, vm: ReminderViewModel, requestPermission: () -> Unit) {
    ReminderSettingsPanel(state, { vm.summary(it, requestPermission) }, vm::retry)
    val context = LocalContext.current
    fun open(intent: Intent) {
        try { context.startActivity(intent) } catch (_: android.content.ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${context.packageName}".toUri()))
        }
    }
    TextButton({ requestPermission() }) { Text(stringResource(R.string.reminder_allow)) }
    listOf(ReminderPlatform.REMINDERS to R.string.reminder_channel_settings,
        ReminderPlatform.SUMMARIES to R.string.reminder_summary_channel).forEach { (channel, label) ->
        TextButton({
            if (Build.VERSION.SDK_INT >= 26) open(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, channel))
            else open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${context.packageName}".toUri()))
        }) { Text(stringResource(label)) }
    }
    if (Build.VERSION.SDK_INT >= 31 && !state.capability.exact) {
        TextButton({ open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            "package:${context.packageName}".toUri())) }) {
            Text(stringResource(R.string.reminder_precision_access)) }
    }
}

@Composable
fun ReminderSettingsPanel(state: ReminderUiState, summary: (String?) -> Unit, retry: () -> Unit) {
    var time by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(state.snapshot.settings.summaryMinute) {
        state.snapshot.settings.summaryMinute?.let { time = LocalTime.of(it / 60, it % 60).toString() }
    }
    Text(stringResource(R.string.reminders), style = MaterialTheme.typography.headlineSmall)
    Text(stringResource(if (state.capability.reminders) R.string.reminder_allowed else R.string.reminder_blocked))
    Text(stringResource(if (state.capability.summaries) R.string.reminder_summary_allowed
        else R.string.reminder_summary_blocked))
    Text(stringResource(if (state.capability.exact) R.string.reminder_exact else R.string.reminder_approximate))
    ReminderErrors(state, retry)
    Text(stringResource(R.string.reminder_daily), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(if (state.snapshot.settings.summaryMinute == null) R.string.reminder_daily_off
        else R.string.reminder_daily_on))
    ScrollableTimeField(R.string.reminder_time, time, !state.busy) { time = it }
    Button({ summary(time) }, enabled = !state.busy && !state.loading) {
        Text(stringResource(R.string.reminder_save_summary)) }
    if (state.snapshot.settings.summaryMinute != null) TextButton({ summary(null) }, enabled = !state.busy) {
        Text(stringResource(R.string.reminder_disable_summary)) }
}

@Composable
fun ReminderDialogs(state: ReminderUiState, vm: ReminderViewModel, requestPermission: () -> Unit) {
    if (state.fallback) AlertDialog(onDismissRequest = { vm.fallback(false) },
        text = { Text(stringResource(R.string.reminder_gone)) },
        confirmButton = { TextButton({ vm.fallback(false) }) { Text(stringResource(R.string.close)) } })
    if (state.snoozeKey != null) AlertDialog(onDismissRequest = vm::dismissSnooze,
        title = { Text(stringResource(R.string.reminder_snooze)) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderErrors(state, vm::retry)
                listOf(10, 30, 60).forEach { minute -> Button({ vm.snooze(minute) }, enabled = !state.busy) {
                    Text(pluralStringResource(R.plurals.reminder_minutes, minute, minute)) } }
                ReminderInput(R.string.reminder_date, state.customDate, !state.busy) {
                    vm.custom(it, state.customTime) }
                ScrollableTimeField(R.string.reminder_time, state.customTime, !state.busy) {
                    vm.custom(state.customDate, it) }
                Button({ vm.snooze(null) }, enabled = !state.busy) { Text(stringResource(R.string.reminder_custom)) }
            }
        }, confirmButton = { TextButton(vm::dismissSnooze, enabled = !state.busy) {
            Text(stringResource(R.string.cancel)) } })
    val d = state.draft
    if (d != null) {
        ReminderEditor(state, vm::draft, { vm.save(requestPermission) }, vm::retry)
    } else if (state.targetId != null) {
        AlertDialog(onDismissRequest = vm::close, title = { Text(stringResource(R.string.reminders)) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.title)
                ReminderErrors(state, vm::retry)
                if (state.kind == ReminderKind.TASK) ReminderScope(state.scopeFuture, vm::scope)
                if (state.definitions.isEmpty()) Text(stringResource(R.string.reminder_empty))
                state.definitions.forEach { definition ->
                    Text(stringResource(modeLabel(definition.mode)))
                    Text(LocalTime.of(definition.minute / 60, definition.minute % 60).toString())
                    if (definition.mode == ReminderMode.CHOSEN) Text(definition.chosenDate.toString())
                    if (definition.mode == ReminderMode.RELATIVE)
                        Text(pluralStringResource(R.plurals.reminder_minutes_before,
                            definition.offsetMinutes.toInt(), definition.offsetMinutes))
                    if (definition.mode == ReminderMode.WEEKDAYS) {
                        Text((0..6).filter { definition.weekdays and (1 shl it) != 0 }
                            .map { stringWeekday(it) }.joinToString(", "))
                    }
                    TextButton({ vm.edit(definition) }, enabled = !state.busy) { Text(stringResource(R.string.edit)) }
                    TextButton({ vm.remove(definition) }, enabled = !state.busy) {
                        Text(stringResource(R.string.delete)) }
                    HorizontalDivider()
                }
                Button({ vm.edit() }, enabled = !state.busy && !state.loading) {
                    Text(stringResource(R.string.reminder_add)) }
            }
        }, confirmButton = { TextButton(vm::close, enabled = !state.busy) { Text(stringResource(R.string.close)) } })
    }
}

@Composable
private fun stringWeekday(index: Int): String = stringResource(listOf(R.string.reminder_mon, R.string.reminder_tue,
    R.string.reminder_wed, R.string.reminder_thu, R.string.reminder_fri, R.string.reminder_sat,
    R.string.reminder_sun)[index])

private fun modeLabel(mode: ReminderMode) = when (mode) {
    ReminderMode.CHOSEN -> R.string.reminder_chosen
    ReminderMode.RELATIVE -> R.string.reminder_relative
    ReminderMode.WEEKDAYS -> R.string.reminder_weekdays
}

@Composable
fun ReminderEditor(state: ReminderUiState, change: (ReminderDraft?) -> Unit, save: () -> Unit, retry: () -> Unit) {
    val draft = state.draft ?: return
    val d = draft.definition
    AlertDialog(onDismissRequest = { if (!state.busy) change(null) },
        title = { Text(stringResource(R.string.reminder_add)) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderErrors(state, retry)
                Text(stringResource(R.string.reminder_deadline_hint))
                if (d.kind == ReminderKind.TASK) {
                    ReminderScope(draft.future) { change(draft.copy(future = it)) }
                    ReminderMode.entries.forEach { mode -> TextButton({ change(draft.copy(
                        definition = d.copy(mode = mode))) }, enabled = !state.busy) {
                        Text(stringResource(modeLabel(mode))) } }
                }
                Text(stringResource(modeLabel(d.mode)), style = MaterialTheme.typography.titleSmall)
                if (d.mode == ReminderMode.CHOSEN) ReminderInput(R.string.reminder_date, draft.date, !state.busy) {
                    change(draft.copy(date = it)) }
                if (d.mode == ReminderMode.RELATIVE) {
                    ReminderInput(R.string.reminder_offset, draft.offset, !state.busy) {
                        change(draft.copy(offset = it)) }
                    ScrollableTimeField(R.string.reminder_reference, draft.reference, !state.busy, optional = true) {
                        change(draft.copy(reference = it)) }
                } else ScrollableTimeField(R.string.reminder_time, draft.time, !state.busy) {
                    change(draft.copy(time = it)) }
                if (d.mode == ReminderMode.WEEKDAYS) (0..6).forEach { index ->
                    val checked = d.weekdays and (1 shl index) != 0
                    val label = stringWeekday(index)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked, { change(draft.copy(definition = d.copy(
                            weekdays = d.weekdays xor (1 shl index)))) }, enabled = !state.busy,
                            modifier = Modifier.semantics { contentDescription = label })
                        Text(label)
                    }
                }
            }
        }, confirmButton = { TextButton(save, enabled = !state.busy) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton({ change(null) }, enabled = !state.busy) {
            Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun ReminderScope(future: Boolean, change: (Boolean) -> Unit) {
    TextButton({ change(!future) }) {
        Text(stringResource(if (future) R.string.reminder_future else R.string.reminder_only_this)) }
}

@Composable
private fun ReminderErrors(state: ReminderUiState, retry: () -> Unit) {
    if (state.loading) Text(stringResource(R.string.reminder_loading))
    if (state.error) {
        Text(stringResource(R.string.reminder_storage_error), color = MaterialTheme.colorScheme.error)
        TextButton(retry) { Text(stringResource(R.string.retry)) }
    }
    if (state.inputError) Text(stringResource(R.string.reminder_input_error), color = MaterialTheme.colorScheme.error)
}

@Composable
private fun ReminderInput(label: Int, value: String, enabled: Boolean, change: (String) -> Unit) {
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), enabled = enabled,
        label = { Text(stringResource(label)) }, singleLine = true)
}

@Preview
@Composable
private fun ReminderSettingsPreview() {
    EthosTheme { Column { ReminderSettingsPanel(ReminderUiState(loading = false), {}, {}) } }
}
