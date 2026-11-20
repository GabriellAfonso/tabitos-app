package com.tabitos.app.core.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tabitos.app.core.colors.PaletteColor
import com.tabitos.app.R
import com.tabitos.app.core.icons.AppIcon
import com.tabitos.app.core.ui.theme.TabitosTheme

/** A pickable icon; [name] is read by TalkBack since the drawing alone carries no meaning. */
data class IconOption(@param:DrawableRes val icon: Int, @param:StringRes val name: Int)

/**
 * One row with the editor's [principal] icons plus "see more", which opens the whole catalog in a sheet, so
 * offering many icons never makes the editor taller.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(
    @StringRes label: Int,
    principal: List<AppIcon>,
    selected: AppIcon,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onSelect: (AppIcon) -> Unit,
) {
    var catalogOpen by rememberSaveable { mutableStateOf(false) }
    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        principalRow(principal, selected).forEach { icon ->
            IconOptionButton(icon.option, isSelected = icon == selected, enabled = enabled, tint = tint,
                onClick = { onSelect(icon) })
        }
        MoreIconsButton(enabled = enabled, onClick = { catalogOpen = true })
    }
    if (catalogOpen) {
        IconCatalogSheet(selected = selected, tint = tint, onDismiss = { catalogOpen = false },
            onChoose = { icon ->
                catalogOpen = false
                onSelect(icon)
            })
    }
}

@Composable
internal fun IconOptionButton(
    option: IconOption, isSelected: Boolean, enabled: Boolean, tint: Color, onClick: () -> Unit,
) {
    val name = stringResource(option.name)
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(48.dp).clip(CircleShape)
        .selectable(isSelected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
        .semantics { contentDescription = name }
        .background(if (isSelected) colors.secondaryContainer else Color.Transparent)
        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) colors.primary else colors.outlineVariant,
            CircleShape),
        contentAlignment = Alignment.Center) {
        Icon(painterResource(option.icon), contentDescription = null, tint = tint)
    }
}

@Composable
private fun MoreIconsButton(enabled: Boolean, onClick: () -> Unit) {
    val name = stringResource(R.string.icon_more)
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(48.dp).clip(CircleShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = name }
        .border(1.dp, colors.outline, CircleShape),
        contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_more_horiz), contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

@Preview
@Composable
private fun IconPickerPreview() {
    TabitosTheme {
        Column {
            IconPicker(
                label = R.string.icon_label,
                principal = listOf(AppIcon.LIST, AppIcon.WORK, AppIcon.HOME, AppIcon.STUDY),
                selected = AppIcon.PETS,
                tint = itemColor(PaletteColor.BLUE.position),
                onSelect = {},
            )
        }
    }
}
