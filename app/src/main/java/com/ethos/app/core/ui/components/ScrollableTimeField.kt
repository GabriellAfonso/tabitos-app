package com.ethos.app.core.ui.components

import android.widget.NumberPicker
import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ethos.app.R
import com.ethos.app.core.ui.theme.EthosTheme
import java.time.LocalTime

/** Keeps the existing ISO draft contract while choosing hours and minutes without a keyboard. */
@Composable
fun ScrollableTimeField(
    label: Int,
    value: String,
    enabled: Boolean,
    optional: Boolean = false,
    onChange: (String) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val time = runCatching { LocalTime.parse(value) }.getOrNull()
    val title = stringResource(label)
    OutlinedCard(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(time?.withSecond(0)?.withNano(0)?.toString() ?: stringResource(R.string.not_set))
        }
    }
    if (!open) return
    var hour by rememberSaveable { mutableIntStateOf(time?.hour ?: 9) }
    var minute by rememberSaveable { mutableIntStateOf(time?.minute ?: 0) }
    AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(title) },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TimeWheel(hour, 23, stringResource(R.string.time_wheel_hours)) { hour = it }
                TimeWheel(minute, 59, stringResource(R.string.time_wheel_minutes)) { minute = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onChange(LocalTime.of(hour, minute).toString())
                open = false
            }, enabled = enabled) { Text(stringResource(R.string.picker_ok)) }
        },
        dismissButton = {
            if (optional && value.isNotBlank()) {
                TextButton(onClick = { onChange(""); open = false }, enabled = enabled) {
                    Text(stringResource(R.string.remove_time))
                }
            }
            TextButton(onClick = { open = false }) { Text(stringResource(R.string.picker_cancel)) }
        },
    )
}

@Composable
private fun TimeWheel(value: Int, maximum: Int, label: String, onChange: (Int) -> Unit) {
    val theme = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        android.R.style.Theme_Material_Light_Dialog
    } else {
        android.R.style.Theme_Material_Dialog
    }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        AndroidView(
            modifier = Modifier.width(96.dp),
            factory = { context ->
                NumberPicker(ContextThemeWrapper(context, theme)).apply {
                    minValue = 0
                    maxValue = maximum
                    wrapSelectorWheel = true
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    setFormatter { it.toString().padStart(2, '0') }
                }
            },
            update = { picker ->
                picker.contentDescription = label
                picker.value = value
                picker.setOnValueChangedListener { _, _, selected -> onChange(selected) }
            },
        )
    }
}

@Preview
@Composable
private fun ScrollableTimeFieldPreview() {
    EthosTheme {
        ScrollableTimeField(label = R.string.reminder_time, value = "09:30", enabled = true, onChange = {})
    }
}
