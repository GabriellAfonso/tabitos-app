package com.tabitos.app.features.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.tabitos.app.R
import com.tabitos.app.core.ui.components.TabitosDatePickerDialog
import com.tabitos.app.core.ui.format.TabitosDates
import com.tabitos.app.core.ui.format.label
import java.time.LocalDate
import java.time.LocalTime

private fun parseDate(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()
private fun parseTime(text: String): LocalTime? = runCatching { LocalTime.parse(text) }.getOrNull()

/** How a picker shows its value: an outlined field, or a compact "label … value" row inside a grouped card. */
enum class PickerStyle { FIELD, ROW }

@Composable
private fun PickerTrigger(
    style: PickerStyle, label: Int, value: String?, empty: Int, enabled: Boolean, tag: String, onClick: () -> Unit,
) {
    val labelText = stringResource(label)
    val shown = value ?: stringResource(empty)
    val description = stringResource(R.string.field_value, labelText, shown)
    val semantics = Modifier.testTag(tag).clearAndSetSemantics {
        contentDescription = description
        role = Role.Button
        onClick { if (enabled) onClick(); enabled }
    }
    when (style) {
        PickerStyle.FIELD -> OutlinedCard(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp).then(semantics)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(labelText, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(shown, style = MaterialTheme.typography.bodyLarge)
            }
        }
        PickerStyle.ROW -> Row(Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp).clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick).then(semantics).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(labelText, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(shown, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
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
    style: PickerStyle = PickerStyle.FIELD,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val date = parseDate(value)
    // A row sits beside other values in a narrow card, so it uses the short form ("28 nov").
    val shown = date?.let {
        (if (style == PickerStyle.ROW) TabitosDates.short(it, today) else TabitosDates.long(it, today)).label()
    }
    PickerTrigger(style, label, shown, R.string.not_set, enabled, tag) { open = true }
    if (!open) return
    TabitosDatePickerDialog(initial = date ?: today, min = min, max = max,
        onConfirm = { onChange(it.toString()); open = false }, onDismiss = { open = false })
}

/** Optional 24-hour time; [value] is `HH:mm` text or empty when the task has no time. */
@Composable
fun TimeField(value: String, enabled: Boolean, style: PickerStyle = PickerStyle.FIELD, onChange: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val time = parseTime(value)
    val empty = if (style == PickerStyle.ROW) R.string.no_time else R.string.not_set
    PickerTrigger(style, R.string.time_field, time?.let { TabitosDates.time(it.hour * 60 + it.minute) }, empty,
        enabled, "time-field") { open = true }
    if (open) TimeDialog(time, onChange) { open = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(time: LocalTime?, onChange: (String) -> Unit, close: () -> Unit) {
    val state = rememberTimePickerState(time?.hour ?: 9, time?.minute ?: 0, is24Hour = true)
    AlertDialog(onDismissRequest = close, text = { TimePicker(state) },
        confirmButton = { TextButton({
            onChange(LocalTime.of(state.hour, state.minute).toString())
            close()
        }) { Text(stringResource(R.string.picker_ok)) } },
        dismissButton = {
            if (time != null) TextButton({ onChange(""); close() }) {
                Text(stringResource(R.string.remove_time)) }
            TextButton(close) { Text(stringResource(R.string.picker_cancel)) }
        })
}
