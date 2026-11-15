package com.ethos.app.features.shell

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.ethos.app.R
import com.ethos.app.core.ui.theme.EthosColors
import com.ethos.app.core.ui.theme.EthosTheme

/** The "+" on Hoje: the user chooses explicitly between task, habit and habit to break (spec 014). */
data class CreateActions(
    val taskEnabled: Boolean,
    val habitEnabled: Boolean,
    val onNewTask: () -> Unit,
    val onNewHabit: () -> Unit,
    val onBreakHabit: () -> Unit,
)

@Composable
fun TodayCreateButton(actions: CreateActions) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.create_item)
    Box {
        FloatingActionButton(onClick = { expanded = !expanded },
            modifier = Modifier.semantics { contentDescription = description }) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CreateOption(R.string.create_task, R.drawable.ic_task_alt, MaterialTheme.colorScheme.primary,
                actions.taskEnabled) { expanded = false; actions.onNewTask() }
            CreateOption(R.string.create_good_habit, R.drawable.ic_sprout, EthosColors.growth,
                actions.habitEnabled) { expanded = false; actions.onNewHabit() }
            CreateOption(R.string.break_bad_habit, R.drawable.ic_block, MaterialTheme.colorScheme.error,
                actions.habitEnabled) { expanded = false; actions.onBreakHabit() }
        }
    }
}

/** Each option carries its own symbol and color: a check for tasks, a seedling to grow, a "no" sign to break. */
@Composable
private fun CreateOption(
    @StringRes label: Int, @DrawableRes icon: Int, tint: Color, enabled: Boolean, onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        leadingIcon = { Icon(painterResource(icon), contentDescription = null, tint = tint) },
        enabled = enabled,
        onClick = onClick,
    )
}

@Preview
@Composable
private fun TodayCreateButtonPreview() {
    EthosTheme { TodayCreateButton(CreateActions(taskEnabled = true, habitEnabled = true, {}, {}, {})) }
}
