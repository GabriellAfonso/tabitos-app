package com.tabitos.app.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberStepperTest {
    private val prices = 1..99_999

    @Test fun typedNumberInsideRangeIsKept() {
        assertEquals(137, parseTypedNumber("137", prices))
    }

    @Test fun typedNumberOutsideRangeIsClamped() {
        assertEquals(1, parseTypedNumber("0", prices))
        assertEquals(99_999, parseTypedNumber("12345678901", prices))
    }

    @Test fun blankTextKeepsPreviousValue() {
        assertNull(parseTypedNumber("", prices))
        assertNull(parseTypedNumber("  ", prices))
    }
}
