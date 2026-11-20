package com.tabitos.app.features.tasks

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.tabitos.app.core.colors.PaletteColor
import com.tabitos.app.R
import com.tabitos.app.core.tasks.domain.TaskList
import com.tabitos.app.core.ui.components.DrawerRow
import com.tabitos.app.core.ui.components.HoldGesture
import com.tabitos.app.core.ui.components.ReorderableColumn
import com.tabitos.app.core.ui.components.itemColor
import com.tabitos.app.core.ui.theme.TabitosTheme

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
    val inboxSelected = highlighted && state.selectedListId == null
    DrawerRow(stringResource(R.string.inbox), R.drawable.ic_inbox, inboxSelected,
        Modifier.selectable(inboxSelected, role = Role.Tab) { if (idle) callbacks.onSelect(null) })
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
            DrawerListRow(list.name, list.iconDrawable, itemColor(list.color),
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
    DrawerRow(stringResource(R.string.new_list), R.drawable.ic_add, selected = false,
        modifier = Modifier.clickable(enabled = idle, role = Role.Button, onClick = callbacks.onNew))
}

@Composable
internal fun CreateListButton(enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f),
        ),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.new_list), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** A user list in the drawer; the hold-and-drag gestures come from [modifier]. */
@Composable
private fun DrawerListRow(
    name: String, @DrawableRes icon: Int, tint: Color, selected: Boolean,
    onOpen: () -> Unit, onActions: () -> Unit, modifier: Modifier = Modifier,
) {
    val actionsLabel = stringResource(R.string.list_actions)
    DrawerRow(name, icon, selected, tint = tint, modifier = Modifier
        // ReorderableColumn merges this row with its move actions into one TalkBack item.
        .semantics {
            role = Role.Button
            this.selected = selected
            onClick { onOpen(); true }
            onLongClick(actionsLabel) { onActions(); true }
        }
        .then(modifier))
}

@Preview(showBackground = true)
@Composable
private fun DrawerListRowPreview() {
    TabitosTheme {
        DrawerListRow("Casa", R.drawable.ic_home, itemColor(PaletteColor.PURPLE.position), selected = true,
            onOpen = {}, onActions = {})
    }
}
