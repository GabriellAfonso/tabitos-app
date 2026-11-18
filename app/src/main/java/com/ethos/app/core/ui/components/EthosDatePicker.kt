package com.ethos.app.core.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ethos.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** The Material date picker models a day as UTC midnight; converting through UTC never shifts the day. */
fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
fun pickerMillisToDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/** Calendar picker without typing (spec 013); [excluded] is a day that cannot be chosen, like the current one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EthosDatePickerDialog(
    initial: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit,
    min: LocalDate? = null, max: LocalDate? = null, excluded: LocalDate? = null,
    confirmLabel: Int = R.string.picker_ok,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.coerceIn(min ?: LocalDate.MIN, max ?: LocalDate.MAX)
            .takeIf { it != excluded }?.toPickerMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val day = pickerMillisToDate(utcTimeMillis)
                return (min == null || day >= min) && (max == null || day <= max) && day != excluded
            }
            override fun isSelectableYear(year: Int): Boolean =
                (min == null || year >= min.year) && (max == null || year <= max.year)
        },
    )
    DatePickerDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton({ state.selectedDateMillis?.let { onConfirm(pickerMillisToDate(it)) } },
            enabled = state.selectedDateMillis != null) { Text(stringResource(confirmLabel)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.picker_cancel)) } },
    ) { DatePicker(state) }
}
