package com.tabitos.app.core.colors

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Color of a list or habit: a palette entry or a custom opaque RGB (`0xRRGGBB`). */
sealed interface ItemColor {
    data class Palette(val entry: PaletteColor) : ItemColor
    data class Custom(val rgb: Int) : ItemColor
}

/**
 * App palette. Lists and habits save [position] in their color column: never change or reuse a position.
 * Spec 038 changed the tones in place (GREEN was sage); ROSE and GRAY left the color row but still paint the
 * items that use them.
 */
enum class PaletteColor(val position: Int, val rgb: Int) {
    GREEN(0, 0x2E8B3E), BLUE(1, 0x1F6FD6), PURPLE(2, 0x9254DE), ORANGE(3, 0xDB6400), ROSE(4, 0xA04D68),
    GRAY(5, 0x657078), RED(6, 0xD93025),
}

/** Background tones of the light and dark themes, where the custom color sheet previews the color. */
enum class ThemeBackground(val rgb: Int) { LIGHT(0xFBF8FC), DARK(0x171419) }

/** Hue (0–360), saturation and value (0–1). */
data class Hsv(val hue: Float, val saturation: Float, val value: Float) : java.io.Serializable

object ItemColors {
    /** Order of the color row in the editors (rainbow); ROSE and GRAY are kept only for existing items. */
    val rowColors = listOf(PaletteColor.RED, PaletteColor.ORANGE, PaletteColor.GREEN, PaletteColor.BLUE,
        PaletteColor.PURPLE)

    /** Bad habits start with red so the card reads as "stop". */
    val badHabitStart: Int = encode(ItemColor.Palette(PaletteColor.RED))

    private const val OPAQUE = 0xFF shl 24
    private const val RGB_MASK = 0xFFFFFF
    const val LIGHT_ON_COLOR = 0xFFFFFF
    const val DARK_ON_COLOR = 0x1E1A23

    /**
     * Saved value: non-negative = palette position; opaque ARGB (`0xFFRRGGBB`, always negative as Int) = custom.
     * Returns null for any other value.
     */
    fun decode(stored: Int): ItemColor? = when {
        stored >= 0 -> PaletteColor.entries.firstOrNull { it.position == stored }?.let { ItemColor.Palette(it) }
        stored and OPAQUE == OPAQUE -> ItemColor.Custom(stored and RGB_MASK)
        else -> null
    }

    fun encode(color: ItemColor): Int = when (color) {
        is ItemColor.Palette -> color.entry.position
        is ItemColor.Custom -> OPAQUE or (color.rgb and RGB_MASK)
    }

    fun custom(rgb: Int): Int = encode(ItemColor.Custom(rgb))

    fun isValid(stored: Int): Boolean = decode(stored) != null

    /** RGB to paint; an invalid value (refused by the rules when saving) paints as the first palette entry. */
    fun rgb(stored: Int): Int = when (val color = decode(stored)) {
        is ItemColor.Palette -> color.entry.rgb
        is ItemColor.Custom -> color.rgb
        null -> PaletteColor.GREEN.rgb
    }

    /** Whether the color is one of the editor row entries (the others show in the custom circle). */
    fun isRowColor(stored: Int): Boolean = (decode(stored) as? ItemColor.Palette)?.entry in rowColors

    /** Icon color drawn on top of [rgb]: light or dark, whichever contrasts more. */
    fun onColor(rgb: Int): Int =
        if (contrast(rgb, LIGHT_ON_COLOR) >= contrast(rgb, DARK_ON_COLOR)) LIGHT_ON_COLOR else DARK_ON_COLOR

    /** WCAG contrast ratio, from 1 to 21. */
    fun contrast(first: Int, second: Int): Double {
        val a = luminance(first)
        val b = luminance(second)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private fun luminance(rgb: Int): Double {
        fun channel(shift: Int): Double {
            val c = (rgb shr shift and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    fun toHsv(rgb: Int): Hsv {
        val r = (rgb shr 16 and 0xFF) / 255f
        val g = (rgb shr 8 and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        val high = max(r, max(g, b))
        val delta = high - min(r, min(g, b))
        val hue = when {
            delta == 0f -> 0f
            high == r -> 60f * (((g - b) / delta).mod(6f))
            high == g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }
        return Hsv(hue, if (high == 0f) 0f else delta / high, high)
    }

    fun fromHsv(hsv: Hsv): Int {
        val (h, s, v) = hsv
        fun channel(n: Int): Int {
            val k = (n + h / 60f).mod(6f)
            val c = v - v * s * max(0f, min(k, min(4f - k, 1f)))
            return (c * 255f + 0.5f).toInt().coerceIn(0, 255)
        }
        return channel(5) shl 16 or (channel(3) shl 8) or channel(1)
    }

    /** Accepts `RRGGBB` or `RGB`, with or without `#`, any case; null when not a color. */
    fun parseHex(text: String): Int? {
        val digits = text.trim().removePrefix("#")
        if (digits.any { it.digitToIntOrNull(16) == null }) return null
        return when (digits.length) {
            6 -> digits.toInt(16)
            3 -> digits.map { "$it$it" }.joinToString("").toInt(16)
            else -> null
        }
    }

    /** Six upper-case digits without `#`. */
    fun formatHex(rgb: Int): String = "%06X".format(rgb and RGB_MASK)

    /** One RGB channel typed by the person (0–255), or null. */
    fun parseChannel(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 0..255 }

    fun channels(rgb: Int): List<Int> = listOf(rgb shr 16 and 0xFF, rgb shr 8 and 0xFF, rgb and 0xFF)

    fun fromChannels(r: Int, g: Int, b: Int): Int = r shl 16 or (g shl 8) or b
}
