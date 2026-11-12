package com.ethos.app.features.timers

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockTextTest {
    @Test fun hidesHoursBelowOneHour() {
        assertEquals("30:00", clockText(30 * 60_000L))
        assertEquals("00:05", clockText(5_000L))
        assertEquals("59:59", clockText(3_599_000L))
    }

    @Test fun showsHoursFromOneHourOn() {
        assertEquals("1:00:00", clockText(3_600_000L))
        assertEquals("2:05:09", clockText((2 * 3600 + 5 * 60 + 9) * 1000L))
    }

    @Test fun negativeRemainingShowsZero() {
        assertEquals("00:00", clockText(-1L))
    }
}
