package com.tabitos.app.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.tabitos.app.R
import com.tabitos.app.core.colors.ItemColor
import com.tabitos.app.core.colors.ItemColors
import com.tabitos.app.core.colors.PaletteColor

private const val OPAQUE_ALPHA = 0xFF000000.toInt()

/** Opaque Compose color for an RGB value (`0xRRGGBB`). */
fun rgbColor(rgb: Int): Color = Color(OPAQUE_ALPHA or rgb)

/** Tone of a saved list/habit color; the same in light and dark theme. */
fun itemColor(stored: Int): Color = rgbColor(ItemColors.rgb(stored))

/** Icon drawn on top of [itemColor]: light or dark, whichever contrasts more. */
fun onItemColor(stored: Int): Color = rgbColor(ItemColors.onColor(ItemColors.rgb(stored)))

@StringRes
fun paletteColorName(entry: PaletteColor): Int = when (entry) {
    PaletteColor.GREEN -> R.string.color_green
    PaletteColor.BLUE -> R.string.color_blue
    PaletteColor.PURPLE -> R.string.color_purple
    PaletteColor.ORANGE -> R.string.color_orange
    PaletteColor.ROSE -> R.string.color_rose
    PaletteColor.GRAY -> R.string.color_gray
    PaletteColor.RED -> R.string.color_red
}

/** Spoken and shown name of a saved color: the palette name, or "Cor personalizada, #RRGGBB". */
@Composable
fun itemColorName(stored: Int): String = when (val color = ItemColors.decode(stored)) {
    is ItemColor.Palette -> stringResource(paletteColorName(color.entry))
    is ItemColor.Custom -> stringResource(R.string.color_custom_named, ItemColors.formatHex(color.rgb))
    null -> stringResource(paletteColorName(PaletteColor.GREEN))
}
