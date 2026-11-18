package com.ethos.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ethos.app.R
import com.ethos.app.core.ui.theme.EthosTheme

// Index is the persisted color of lists and habits; append new colors, never reorder.
val itemColors = listOf(Color(0xFF54745C), Color(0xFF426B96), Color(0xFF79568E),
    Color(0xFFA45B25), Color(0xFFA04D68), Color(0xFF657078), Color(0xFFB3261E))
/** Spoken names of [itemColors], same order. */
val itemColorNames = listOf(R.string.color_sage, R.string.color_blue, R.string.color_purple,
    R.string.color_orange, R.string.color_rose, R.string.color_gray, R.string.color_red)

/** Position of red in [itemColors]; bad habits start with it so the card reads as "stop". */
const val RED_ITEM_COLOR = 6

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(selected: Int, enabled: Boolean = true, onSelect: (Int) -> Unit) {
    Text(stringResource(R.string.color_label), style = MaterialTheme.typography.labelLarge)
    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        itemColors.forEachIndexed { index, color ->
            val name = stringResource(itemColorNames[index])
            Box(Modifier.size(48.dp).clip(CircleShape)
                .selectable(selected == index, enabled = enabled, role = Role.RadioButton,
                    onClick = { onSelect(index) })
                .semantics { contentDescription = name }
                .border(if (selected == index) 2.dp else 0.dp,
                    if (selected == index) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                    if (selected == index) {
                        Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ColorPickerPreview() {
    EthosTheme { Column { ColorPicker(selected = 2, onSelect = {}) } }
}
