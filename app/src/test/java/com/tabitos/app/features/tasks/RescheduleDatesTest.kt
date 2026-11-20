package com.tabitos.app.features.tasks

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RescheduleDatesTest {
    private val today = LocalDate.of(2026, 11, 17)
    private val tomorrow = today.plusDays(1)

    @Test fun shortcutsExcludeTheDeadlineAndThePickerStartsOnAnAvailableDay() {
        fun check(deadline: LocalDate, days: List<LocalDate>, initial: LocalDate) {
            val choices = rescheduleDates(today, deadline)
            assertEquals(days, choices.shortcuts)
            assertEquals(initial, choices.initial)
        }
        check(today, listOf(tomorrow), tomorrow)
        check(tomorrow, listOf(today), today)
        check(today.plusDays(4), listOf(today, tomorrow), tomorrow)
        check(today.minusDays(1), listOf(today, tomorrow), tomorrow)
    }
}
