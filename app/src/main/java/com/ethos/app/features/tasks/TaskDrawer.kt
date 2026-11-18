package com.ethos.app.features.tasks

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.tasks.domain.TaskList
import com.ethos.app.core.ui.components.HoldGesture
import com.ethos.app.core.ui.components.ReorderableColumn
import com.ethos.app.core.ui.components.itemColors
import com.ethos.app.core.ui.theme.EthosTheme

/** What the drawer can do with the user's lists; Caixa de entrada is fixed and has no actions. */
data class DrawerListCallbacks(
    val onSelect: (String?) -> Unit,
    val onNew: () -> Unit,
    val onEdit: (TaskList) -> Unit,
    val onDelete: (TaskList) -> Unit,
    val onReorder: (List<TaskList>) -> Unit,
)

/**
 * Lists section of the drawer. Tap opens a list; hold and drag reorders; hold and release opens Editar/Excluir
 * (TalkBack: long-press action and Mover para cima/baixo). [highlighted] marks the open list on the tasks page.
 */
@Composable
fun TaskDrawer(state: TasksUiState, highlighted: Boolean, callbacks: DrawerListCallbacks) {
    val idle = !state.busy
    NavigationDrawerItem(label = { Text(stringResource(R.string.inbox)) },
        selected = highlighted && state.selectedListId == null,
        onClick = { if (idle) callbacks.onSelect(null) })
    var menuFor by rememberSaveable { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    ReorderableColumn(
        items = state.snapshot.activeLists, itemKey = { it.id }, enabled = idle,
        onReorder = callbacks.onReorder, spacing = 0.dp,
        hold = HoldGesture(
            onTap = { callbacks.onSelect(it.id) },
            onHold = { menuFor = it.id },
            onLift = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
        ),
    ) { list, handle ->
        Box {
            DrawerListRow(list.name, list.iconDrawable, itemColors[list.color],
                selected = highlighted && state.selectedListId == list.id, modifier = handle,
                onOpen = { if (idle) callbacks.onSelect(list.id) }, onActions = { if (idle) menuFor = list.id })
            DropdownMenu(expanded = menuFor == list.id, onDismissRequest = { menuFor = null }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.edit)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_edit), contentDescription = null) },
                    onClick = { menuFor = null; callbacks.onEdit(list) })
                DropdownMenuItem(text = { Text(stringResource(R.string.delete)) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                    },
                    onClick = { menuFor = null; callbacks.onDelete(list) })
            }
        }
    }
    NavigationDrawerItem(label = { Text(stringResource(R.string.new_list)) },
        icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
        selected = false, onClick = { if (idle) callbacks.onNew() })
}

/** Same look as a [NavigationDrawerItem], but the gestures come from [modifier] so the row can be held. */
@Composable
private fun DrawerListRow(
    name: String, @DrawableRes icon: Int, tint: Color, selected: Boolean,
    onOpen: () -> Unit, onActions: () -> Unit, modifier: Modifier = Modifier,
) {
    val actionsLabel = stringResource(R.string.list_actions)
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        modifier = Modifier.fillMaxWidth().height(56.dp)
            // ReorderableColumn merges this row with its move actions into one TalkBack item.
            .semantics {
                role = Role.Button
                this.selected = selected
                onClick { onOpen(); true }
                onLongClick(actionsLabel) { onActions(); true }
            }
            .then(modifier),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(icon), contentDescription = null, tint = tint)
            Text(name, style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DrawerListRowPreview() {
    EthosTheme {
        DrawerListRow("Casa", R.drawable.ic_home, itemColors[2], selected = true, onOpen = {}, onActions = {})
    }
}
