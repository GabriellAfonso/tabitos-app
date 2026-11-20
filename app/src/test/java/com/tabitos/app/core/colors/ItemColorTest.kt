package com.tabitos.app.core.colors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemColorTest {
    @Test fun savedPositionsKeepTheirMeaning() {
        // Items saved before spec 038 hold these positions; they must keep painting the same family of color.
        val expected = listOf(PaletteColor.GREEN, PaletteColor.BLUE, PaletteColor.PURPLE, PaletteColor.ORANGE,
            PaletteColor.ROSE, PaletteColor.GRAY, PaletteColor.RED)
        expected.forEachIndexed { stored, entry ->
            assertEquals(ItemColor.Palette(entry), ItemColors.decode(stored))
            assertEquals(stored, ItemColors.encode(ItemColor.Palette(entry)))
        }
        assertEquals(0xA04D68, ItemColors.rgb(4))
        assertEquals(0x657078, ItemColors.rgb(5))
    }

    @Test fun customColorRoundTripsAndNeverCollidesWithPositions() {
        val stored = ItemColors.custom(0x1E7F86)
        assertTrue(stored < 0)
        assertEquals(ItemColor.Custom(0x1E7F86), ItemColors.decode(stored))
        assertEquals(0x1E7F86, ItemColors.rgb(stored))
        assertEquals(ItemColor.Custom(0x000000), ItemColors.decode(ItemColors.custom(0x000000)))
        assertEquals(ItemColor.Custom(0xFFFFFF), ItemColors.decode(ItemColors.custom(0xFFFFFF)))
    }

    @Test fun anythingElseIsInvalid() {
        assertFalse(ItemColors.isValid(7))
        assertFalse(ItemColors.isValid(0x1E7F86))
        assertFalse(ItemColors.isValid(0x801E7F86.toInt()))
        assertNull(ItemColors.decode(Int.MIN_VALUE))
        assertTrue(ItemColors.isValid(6))
    }

    @Test fun rowShowsRainbowOrderAndBadHabitsStartRed() {
        assertEquals(listOf(PaletteColor.RED, PaletteColor.ORANGE, PaletteColor.GREEN, PaletteColor.BLUE,
            PaletteColor.PURPLE), ItemColors.rowColors)
        assertEquals(6, ItemColors.badHabitStart)
        assertFalse(ItemColors.isRowColor(4))
        assertFalse(ItemColors.isRowColor(5))
        assertFalse(ItemColors.isRowColor(ItemColors.custom(0xD93025)))
        assertTrue(ItemColors.isRowColor(0))
    }

    @Test fun contrastMatchesKnownValues() {
        assertEquals(21.0, ItemColors.contrast(0x000000, 0xFFFFFF), 0.01)
        assertEquals(1.0, ItemColors.contrast(0x1E7F86, 0x1E7F86), 0.001)
        assertEquals(ItemColors.LIGHT_ON_COLOR, ItemColors.onColor(0x1B1B3A))
        assertEquals(ItemColors.DARK_ON_COLOR, ItemColors.onColor(0xF6D743))
    }


    @Test fun hsvRoundTripKeepsTheColor() {
        listOf(0x1E7F86, 0xD93025, 0x000000, 0xFFFFFF, 0x808080, 0x9254DE).forEach { rgb ->
            assertEquals(ItemColors.formatHex(rgb), ItemColors.formatHex(ItemColors.fromHsv(ItemColors.toHsv(rgb))))
        }
        assertEquals(0xFF0000, ItemColors.fromHsv(Hsv(0f, 1f, 1f)))
        assertEquals(0x0000FF, ItemColors.fromHsv(Hsv(240f, 1f, 1f)))
    }

    @Test fun hexAcceptsShortLongHashAndAnyCase() {
        assertEquals(0x1E7F86, ItemColors.parseHex("#1e7f86"))
        assertEquals(0x1E7F86, ItemColors.parseHex(" 1E7F86 "))
        assertEquals(0xFFCC00, ItemColors.parseHex("fc0"))
        assertNull(ItemColors.parseHex("12ZZ"))
        assertNull(ItemColors.parseHex("1E7F8"))
        assertNull(ItemColors.parseHex(""))
        assertEquals("0A0B0C", ItemColors.formatHex(0x0A0B0C))
    }

    @Test fun channelsAcceptOnlyZeroTo255() {
        assertEquals(255, ItemColors.parseChannel("255"))
        assertNull(ItemColors.parseChannel("256"))
        assertNull(ItemColors.parseChannel("-1"))
        assertNull(ItemColors.parseChannel(""))
        assertEquals(listOf(0x1E, 0x7F, 0x86), ItemColors.channels(0x1E7F86))
        assertEquals(0x1E7F86, ItemColors.fromChannels(0x1E, 0x7F, 0x86))
    }
}
