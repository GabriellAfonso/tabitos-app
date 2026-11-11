package com.ethos.app.features.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.ethos.app.R
import com.ethos.app.core.ui.theme.EthosTheme

@Composable
fun TodayCreateButton(
    taskEnabled: Boolean, habitEnabled: Boolean, onNewTask: () -> Unit, onNewHabit: () -> Unit,
    onBreakHabit: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.create_item)
    Box {
        FloatingActionButton(onClick = { expanded = !expanded },
            modifier = Modifier.semantics { contentDescription = description }) {
            Text(stringResource(R.string.create_plus), style = MaterialTheme.typography.headlineMedium)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.new_task)) }, enabled = taskEnabled,
                onClick = { expanded = false; onNewTask() })
            DropdownMenuItem(text = { Text(stringResource(R.string.new_habit)) }, enabled = habitEnabled,
                onClick = { expanded = false; onNewHabit() })
            // Explicit choice of what is being created (spec 014): task, habit or habit to break.
            DropdownMenuItem(text = { Text(stringResource(R.string.break_habit)) }, enabled = habitEnabled,
                onClick = { expanded = false; onBreakHabit() })
        }
    }
}

@Preview
@Composable
private fun TodayCreateButtonPreview() {
    EthosTheme { TodayCreateButton(taskEnabled = true, habitEnabled = true, onNewTask = {}, onNewHabit = {}) }
}
