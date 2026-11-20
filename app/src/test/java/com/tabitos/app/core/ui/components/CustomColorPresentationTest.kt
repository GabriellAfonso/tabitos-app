package com.tabitos.app.core.ui.components

import com.tabitos.app.core.colors.ItemColors
import com.tabitos.app.core.colors.PaletteColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomColorPresentationTest {
    private val teal = CustomColorDraft.fromRgb(0x1E7F86)

    @Test fun opensOnTheItemColor() {
        assertEquals("1E7F86", CustomColorDraft.fromStored(ItemColors.custom(0x1E7F86)).hexText)
        assertEquals("657078", CustomColorDraft.fromStored(PaletteColor.GRAY.position).hexText)
        assertEquals(listOf("217", "48", "37"), CustomColorDraft.fromStored(PaletteColor.RED.position).rgbTexts)
    }

    @Test fun validHexMovesTheColorAndKeepsWhatWasTyped() {
        val d = teal.withHexText("#f6d743")
        assertEquals(0xF6D743, d.rgb)
        assertEquals("#f6d743", d.hexText)
        assertEquals(listOf("246", "215", "67"), d.rgbTexts)
        assertEquals(ItemColors.custom(0xF6D743), d.result)
    }

    @Test fun invalidHexBlocksUseAndKeepsLastValidColor() {
        val d = teal.withHexText("12ZZ")
        assertEquals(ColorInputError.HEX, d.error)
        assertFalse(d.canUse)
        assertNull(d.result)
        assertEquals(0x1E7F86, d.rgb)
        assertTrue(d.withHexText("1E7").canUse)
    }

    @Test fun rgbOutOfRangeBlocksUntilFixed() {
        val bad = teal.withMode(ColorInputMode.RGB).withRgbText(0, "300")
        assertEquals(ColorInputError.RGB, bad.error)
        assertEquals(0x1E7F86, bad.rgb)
        val fixed = bad.withRgbText(0, "255")
        assertNull(fixed.error)
        assertEquals(0xFF7F86, fixed.rgb)
        assertEquals("FF7F86", fixed.hexText)
    }

    @Test fun switchingModeDropsInvalidTextAndShowsCurrentColor() {
        val d = teal.withHexText("zz").withMode(ColorInputMode.RGB)
        assertNull(d.error)
        assertEquals(listOf("30", "127", "134"), d.rgbTexts)
        assertEquals("1E7F86", d.hexText)
    }

    @Test fun wheelBrightnessAndStartUpdateFieldsTogether() {
        val red = teal.withWheel(0f, 1f).withValue(1f)
        assertEquals("FF0000", red.hexText)
        assertEquals("000000", red.withValue(0f).hexText)
        assertEquals("FF0000", red.withValue(0f).withValue(1f).hexText)
        val start = red.withMode(ColorInputMode.RGB).withStart(PaletteColor.BLUE)
        assertEquals(ColorInputMode.RGB, start.mode)
        assertEquals("1F6FD6", start.hexText)
    }

    @Test fun wheelPointMapsAngleToHueAndDistanceToSaturation() {
        assertEquals(0f to 1f, wheelSelection(100f, 0f, 100f))
        assertEquals(90f to 0.5f, wheelSelection(0f, 50f, 100f))
        assertEquals(180f, wheelSelection(-30f, 0f, 100f).first)
        assertEquals(1f, wheelSelection(0f, -500f, 100f).second)
    }
}
