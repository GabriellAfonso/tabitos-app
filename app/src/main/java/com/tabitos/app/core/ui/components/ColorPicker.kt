package com.tabitos.app.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tabitos.app.R
import com.tabitos.app.core.colors.ItemColor
import com.tabitos.app.core.colors.ItemColors
import com.tabitos.app.core.colors.PaletteColor
import com.tabitos.app.core.ui.theme.TabitosTheme

/**
 * Color row of the list and habit editors: the five app colors and "Personalizada", always on one line (cells
 * share the width). Rose, gray and custom colors show in the last circle; [icon] previews the item in the sheet.
 */
@Composable
fun ColorPicker(selected: Int, @DrawableRes icon: Int, enabled: Boolean = true, onSelect: (Int) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Text(stringResource(R.string.color_label), style = MaterialTheme.typography.labelLarge)
    Row(Modifier.fillMaxWidth().selectableGroup()) {
        ItemColors.rowColors.forEach { entry ->
            val stored = ItemColors.encode(ItemColor.Palette(entry))
            ColorCell(selected == stored, enabled, stringResource(paletteColorName(entry)),
                onClick = { onSelect(stored) }) {
                Swatch(itemColor(stored), check = if (selected == stored) onItemColor(stored) else null)
            }
        }
        CustomCell(selected, enabled) { picking = true }
    }
    if (picking) CustomColorSheet(selected, icon, onDismiss = { picking = false }) {
        picking = false
        onSelect(it)
    }
}

@Composable
private fun RowScope.CustomCell(selected: Int, enabled: Boolean, open: () -> Unit) {
    val hasCustom = !ItemColors.isRowColor(selected)
    val name = if (hasCustom) itemColorName(selected) else stringResource(R.string.color_custom_choose)
    ColorCell(hasCustom, enabled, name, onClick = open) {
        if (hasCustom) {
            Swatch(itemColor(selected), onItemColor(selected))
            Box(Modifier.align(Alignment.TopEnd).size(PENCIL_SIZE).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = null, Modifier.size(10.dp),
                    tint = MaterialTheme.colorScheme.onPrimary)
            }
        } else {
            Box(Modifier.size(SWATCH_SIZE).clip(CircleShape).background(Brush.sweepGradient(rainbow)),
                contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_palette), contentDescription = null, Modifier.size(18.dp),
                    tint = Color.White)
            }
        }
    }
}

@Composable
private fun RowScope.ColorCell(
    selected: Boolean, enabled: Boolean, name: String, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit,
) {
    Box(Modifier.weight(1f).height(CELL_HEIGHT).clip(CircleShape)
        .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
        .semantics { contentDescription = name }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(RING_SIZE).border(if (selected) 2.dp else 0.dp,
            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun Swatch(color: Color, check: Color?) {
    Box(Modifier.size(SWATCH_SIZE).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        check?.let { Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = it) }
    }
}

private val rainbow = listOf(Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF1E88E5),
    Color(0xFF8E24AA), Color(0xFFE53935))
private val CELL_HEIGHT = 48.dp
private val RING_SIZE = 44.dp
private val SWATCH_SIZE = 32.dp
private val PENCIL_SIZE = 15.dp

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ColorPickerPreview() {
    TabitosTheme {
        Column {
            ColorPicker(selected = PaletteColor.PURPLE.position, icon = R.drawable.ic_home, onSelect = {})
            ColorPicker(selected = ItemColors.custom(0x1E7F86), icon = R.drawable.ic_home, onSelect = {})
        }
    }
}
