package com.tabitos.app.core.ui.components

import com.tabitos.app.core.colors.Hsv
import com.tabitos.app.core.colors.ItemColors
import com.tabitos.app.core.colors.PaletteColor
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

enum class ColorInputMode { HEX, RGB }

/** Which typed field holds an invalid value; the wheel and preview keep the last valid color meanwhile. */
enum class ColorInputError { HEX, RGB }

/** Color being chosen in the "Cor personalizada" sheet; nothing reaches the item until [result] is used. */
data class CustomColorDraft(
    val hsv: Hsv,
    val mode: ColorInputMode = ColorInputMode.HEX,
    val hexText: String = ItemColors.formatHex(ItemColors.fromHsv(hsv)),
    val rgbTexts: List<String> = ItemColors.channels(ItemColors.fromHsv(hsv)).map { it.toString() },
    val error: ColorInputError? = null,
) : java.io.Serializable {
    val rgb: Int get() = ItemColors.fromHsv(hsv)

    /** Saved value to apply, or null while a typed field is invalid. */
    val result: Int? get() = if (error == null) ItemColors.custom(rgb) else null

    val canUse: Boolean get() = error == null

    companion object {
        /** Opens on the item's current color, palette or custom. */
        fun fromStored(stored: Int): CustomColorDraft = fromRgb(ItemColors.rgb(stored))

        fun fromRgb(rgb: Int): CustomColorDraft = CustomColorDraft(ItemColors.toHsv(rgb))
    }
}

fun CustomColorDraft.withWheel(hue: Float, saturation: Float): CustomColorDraft =
    CustomColorDraft(hsv.copy(hue = hue, saturation = saturation.coerceIn(0f, 1f)), mode)

fun CustomColorDraft.withValue(value: Float): CustomColorDraft =
    CustomColorDraft(hsv.copy(value = value.coerceIn(0f, 1f)), mode)

fun CustomColorDraft.withStart(entry: PaletteColor): CustomColorDraft =
    CustomColorDraft(ItemColors.toHsv(entry.rgb), mode)

/** Switching shows the current color in the other format and drops any invalid text. */
fun CustomColorDraft.withMode(mode: ColorInputMode): CustomColorDraft = CustomColorDraft(hsv, mode)

fun CustomColorDraft.withHexText(text: String): CustomColorDraft {
    val parsed = ItemColors.parseHex(text)
        ?: return copy(hexText = text, error = ColorInputError.HEX)
    return CustomColorDraft(ItemColors.toHsv(parsed), mode, hexText = text)
}

fun CustomColorDraft.withRgbText(channel: Int, text: String): CustomColorDraft {
    val texts = rgbTexts.toMutableList().also { it[channel] = text }
    val values = texts.map { ItemColors.parseChannel(it) }
    if (values.any { it == null }) return copy(rgbTexts = texts, error = ColorInputError.RGB)
    val (r, g, b) = values.map { it!! }
    return CustomColorDraft(ItemColors.toHsv(ItemColors.fromChannels(r, g, b)), mode, rgbTexts = texts)
}

/** Hue and saturation at a point of the wheel, measured from its center. */
fun wheelSelection(dx: Float, dy: Float, radius: Float): Pair<Float, Float> {
    val degrees = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
    return (degrees + FULL_TURN) % FULL_TURN to min(1f, hypot(dx, dy) / radius)
}

private const val FULL_TURN = 360f
