package com.ethos.app.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.ui.theme.EthosTheme

/** A pickable symbol; [name] is read by TalkBack since the symbol alone carries no meaning. */
data class IconOption(val symbol: String, @StringRes val name: Int)

/** Shows the symbols themselves so the user picks by sight, mirroring [ColorPicker]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(
    @StringRes label: Int,
    options: List<IconOption>,
    selected: Int,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onSelect: (Int) -> Unit,
) {
    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { index, option ->
            IconOptionButton(option, isSelected = selected == index, enabled = enabled, tint = tint,
                onClick = { onSelect(index) })
        }
    }
}

@Composable
private fun IconOptionButton(
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
        Text(option.symbol, color = tint, style = MaterialTheme.typography.titleLarge)
    }
}

@Preview
@Composable
private fun IconPickerPreview() {
    EthosTheme {
        Column {
            IconPicker(
                label = R.string.icon_label,
                options = listOf(IconOption("☰", R.string.icon_list), IconOption("⌂", R.string.icon_home),
                    IconOption("♡", R.string.icon_health), IconOption("☆", R.string.icon_star)),
                selected = 1,
                tint = itemColors[1],
                onSelect = {},
            )
        }
    }
}
