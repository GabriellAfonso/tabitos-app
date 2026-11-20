package com.tabitos.app.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tabitos.app.R
import com.tabitos.app.core.colors.ItemColors
import com.tabitos.app.core.colors.ThemeBackground
import com.tabitos.app.core.ui.theme.TabitosTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * "Cor personalizada": wheel, brightness and Hex | RGB fields kept in sync. The color only reaches the item
 * through [onUse]; dismissing keeps the previous color.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomColorSheet(stored: Int, @DrawableRes icon: Int, onDismiss: () -> Unit, onUse: (Int) -> Unit) {
    var draft by rememberSaveable { mutableStateOf(CustomColorDraft.fromStored(stored)) }
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        CustomColorContent(draft, icon, { draft = it }, onDismiss, onUse)
    }
}

@Composable
private fun CustomColorContent(
    draft: CustomColorDraft, @DrawableRes icon: Int, change: (CustomColorDraft) -> Unit, onDismiss: () -> Unit,
    onUse: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.color_custom_title), Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge)
        ThemePreviews(draft.rgb, icon)
        ColorWheel(draft, change, Modifier.align(Alignment.CenterHorizontally))
        BrightnessSlider(draft, change)
        ModeSelector(draft, change)
        ColorFields(draft, change)
        StartColors(draft, change)
        Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onDismiss) { Text(stringResource(R.string.picker_cancel)) }
            Button({ draft.result?.let(onUse) }, enabled = draft.canUse) {
                Text(stringResource(R.string.color_custom_use))
            }
        }
    }
}

/** The same color on both theme backgrounds, since it does not change with the theme. */
@Composable
private fun ThemePreviews(rgb: Int, @DrawableRes icon: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeBackground.entries.forEach { background ->
            Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(rgbColor(background.rgb))
                .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(rgbColor(rgb)),
                    contentAlignment = Alignment.Center) {
                    Icon(painterResource(icon), contentDescription = null, Modifier.size(20.dp),
                        tint = rgbColor(ItemColors.onColor(rgb)))
                }
                Text(stringResource(previewLabel(background)), Modifier.weight(1f),
                    color = rgbColor(previewText(background)), style = MaterialTheme.typography.labelMedium)
                Icon(painterResource(icon), contentDescription = null, tint = rgbColor(rgb))
            }
        }
    }
}

private fun previewLabel(background: ThemeBackground): Int = when (background) {
    ThemeBackground.LIGHT -> R.string.color_preview_light
    ThemeBackground.DARK -> R.string.color_preview_dark
}

private fun previewText(background: ThemeBackground): Int = when (background) {
    ThemeBackground.LIGHT -> ItemColors.DARK_ON_COLOR
    ThemeBackground.DARK -> ItemColors.LIGHT_ON_COLOR
}

private val hueStops = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)

/** Angle picks the hue and distance from the center the saturation; darkened by the current brightness. */
@Composable
private fun ColorWheel(draft: CustomColorDraft, change: (CustomColorDraft) -> Unit, modifier: Modifier) {
    val current by rememberUpdatedState(draft)
    val description = stringResource(R.string.color_wheel)
    val hint = stringResource(R.string.color_wheel_hint)
    Canvas(modifier.size(WHEEL_SIZE).semantics { contentDescription = "$description. $hint" }
        .pointerInput(Unit) {
            val radius = size.width / 2f
            fun select(position: Offset) {
                val (hue, saturation) = wheelSelection(position.x - radius, position.y - radius, radius)
                change(current.withWheel(hue, saturation))
            }
            awaitEachGesture {
                select(awaitFirstDown().also { it.consume() }.position)
                do {
                    val event = awaitPointerEvent()
                    event.changes.forEach { select(it.position); it.consume() }
                } while (event.changes.any { it.pressed })
            }
        }) {
        val radius = min(size.width, size.height) / 2f
        drawCircle(Brush.sweepGradient(hueStops, center), radius)
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.Transparent), center, radius), radius)
        drawCircle(Color.Black.copy(alpha = 1f - draft.hsv.value), radius)
        val angle = Math.toRadians(draft.hsv.hue.toDouble())
        val marker = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * (draft.hsv.saturation * radius)
        drawCircle(rgbColor(draft.rgb), MARKER_RADIUS.toPx(), marker)
        drawCircle(Color.White, MARKER_RADIUS.toPx(), marker, style = Stroke(MARKER_STROKE.toPx()))
        drawCircle(Color.Black.copy(alpha = 0.4f), MARKER_RADIUS.toPx() + MARKER_STROKE.toPx() / 2, marker,
            style = Stroke(1.dp.toPx()))
    }
}

@Composable
private fun BrightnessSlider(draft: CustomColorDraft, change: (CustomColorDraft) -> Unit) {
    val label = stringResource(R.string.color_brightness)
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Slider(draft.hsv.value, { change(draft.withValue(it)) }, Modifier.semantics { contentDescription = label })
    }
}

@Composable
private fun ModeSelector(draft: CustomColorDraft, change: (CustomColorDraft) -> Unit) {
    val modes = ColorInputMode.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(selected = draft.mode == mode, onClick = { change(draft.withMode(mode)) },
                shape = SegmentedButtonDefaults.itemShape(index, modes.size)) {
                Text(stringResource(modeLabel(mode)))
            }
        }
    }
}

private fun modeLabel(mode: ColorInputMode): Int = when (mode) {
    ColorInputMode.HEX -> R.string.color_mode_hex
    ColorInputMode.RGB -> R.string.color_mode_rgb
}

@Composable
private fun ColumnScope.ColorFields(draft: CustomColorDraft, change: (CustomColorDraft) -> Unit) {
    when (draft.mode) {
        ColorInputMode.HEX -> OutlinedTextField(draft.hexText, { change(draft.withHexText(it)) },
            Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.color_hex)) },
            prefix = { Text("#") }, isError = draft.error == ColorInputError.HEX,
            supportingText = errorText(draft.error == ColorInputError.HEX, R.string.color_hex_error),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii))
        ColorInputMode.RGB -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rgbChannels.forEachIndexed { channel, (short, spoken) ->
                    val name = stringResource(spoken)
                    OutlinedTextField(draft.rgbTexts[channel], { change(draft.withRgbText(channel, it)) },
                        Modifier.weight(1f).semantics { contentDescription = name }, singleLine = true,
                        label = { Text(stringResource(short)) }, isError = draft.error == ColorInputError.RGB,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
            if (draft.error == ColorInputError.RGB) {
                Text(stringResource(R.string.color_rgb_error), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private val rgbChannels = listOf(
    R.string.color_red_channel to R.string.color_red_channel_name,
    R.string.color_green_channel to R.string.color_green_channel_name,
    R.string.color_blue_channel to R.string.color_blue_channel_name,
)

private fun errorText(show: Boolean, message: Int): (@Composable () -> Unit)? =
    if (show) ({ Text(stringResource(message)) }) else null

@Composable
private fun StartColors(draft: CustomColorDraft, change: (CustomColorDraft) -> Unit) {
    Column {
        Text(stringResource(R.string.color_start_from), style = MaterialTheme.typography.labelLarge)
        Row {
            ItemColors.rowColors.forEach { entry ->
                val name = stringResource(R.string.color_start_from_named, stringResource(paletteColorName(entry)))
                Box(Modifier.size(48.dp).clip(CircleShape)
                    .clickable(role = Role.Button) { change(draft.withStart(entry)) }
                    .semantics { contentDescription = name }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(rgbColor(entry.rgb))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                }
            }
        }
    }
}

private val WHEEL_SIZE = 220.dp
private val MARKER_RADIUS = 10.dp
private val MARKER_STROKE = 3.dp

@Preview(showBackground = true)
@Composable
private fun CustomColorContentPreview() {
    TabitosTheme {
        CustomColorContent(CustomColorDraft.fromRgb(0xF6D743), R.drawable.ic_home, {}, {}, {})
    }
}
