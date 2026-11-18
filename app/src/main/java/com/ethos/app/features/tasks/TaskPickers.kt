package com.ethos.app.features.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.ui.components.EthosDatePickerDialog
import com.ethos.app.core.ui.format.EthosDates
import com.ethos.app.core.ui.format.label
import java.time.LocalDate
import java.time.LocalTime

private fun parseDate(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()
private fun parseTime(text: String): LocalTime? = runCatching { LocalTime.parse(text) }.getOrNull()

@Composable
private fun PickerField(label: Int, value: String?, enabled: Boolean, tag: String, onClick: () -> Unit) {
    val labelText = stringResource(label)
    val shown = value ?: stringResource(R.string.not_set)
    val description = stringResource(R.string.field_value, labelText, shown)
    OutlinedCard(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp).testTag(tag).clearAndSetSemantics {
            contentDescription = description
            role = Role.Button
            onClick { if (enabled) onClick(); enabled }
        }) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(labelText, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(shown, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Date chosen through a calendar picker; [value] and [onChange] use ISO text so the draft and its
 * parsing stay unchanged (spec 013 research R3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: Int, value: String, today: LocalDate, enabled: Boolean, onChange: (String) -> Unit,
    min: LocalDate? = null, max: LocalDate? = null, tag: String = "date:$label",
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val date = parseDate(value)
    PickerField(label, date?.let { EthosDates.long(it, today).label() }, enabled, tag) { open = true }
    if (!open) return
    EthosDatePickerDialog(initial = date ?: today, min = min, max = max,
        onConfirm = { onChange(it.toString()); open = false }, onDismiss = { open = false })
}

/** Optional 24-hour time; [value] is `HH:mm` text or empty when the task has no time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(value: String, enabled: Boolean, onChange: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val time = parseTime(value)
    PickerField(R.string.time_field, time?.let { EthosDates.time(it.hour * 60 + it.minute) }, enabled,
        "time-field") { open = true }
    if (!open) return
    val state = rememberTimePickerState(time?.hour ?: 9, time?.minute ?: 0, is24Hour = true)
    AlertDialog(onDismissRequest = { open = false }, text = { TimePicker(state) },
        confirmButton = { TextButton({
            onChange(LocalTime.of(state.hour, state.minute).toString())
            open = false
        }) { Text(stringResource(R.string.picker_ok)) } },
        dismissButton = {
            if (time != null) TextButton({ onChange(""); open = false }) {
                Text(stringResource(R.string.remove_time)) }
            TextButton({ open = false }) { Text(stringResource(R.string.picker_cancel)) }
        })
}
