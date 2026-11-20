package com.tabitos.app.core.ui.format

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class TabitosDatesTest {
    private val today = LocalDate.of(2026, 11, 10) // Tuesday

    @Test fun relativeDays() {
        assertEquals(DateText.Today, TabitosDates.short(today, today))
        assertEquals(DateText.Tomorrow, TabitosDates.short(today.plusDays(1), today))
        assertEquals(DateText.Yesterday, TabitosDates.long(today.minusDays(1), today))
    }

    @Test fun sameYearOmitsYear() {
        assertEquals(DateText.Plain("20 nov"), TabitosDates.short(LocalDate.of(2026, 11, 20), today))
        assertEquals(DateText.Plain("sexta, 20 de novembro"), TabitosDates.long(LocalDate.of(2026, 11, 20), today))
        assertEquals("5 mai", TabitosDates.shortPlain(LocalDate.of(2026, 5, 5), today))
    }

    @Test fun otherYearAddsYear() {
        assertEquals(DateText.Plain("10 nov 2027"), TabitosDates.short(LocalDate.of(2027, 11, 10), today))
        assertEquals(DateText.Plain("quarta, 10 de novembro de 2027"),
            TabitosDates.long(LocalDate.of(2027, 11, 10), today))
        assertEquals("sábado, 1 de janeiro de 2028", TabitosDates.longPlain(LocalDate.of(2028, 1, 1), today))
    }

    @Test fun monthTitleAndTime() {
        assertEquals("novembro de 2026", TabitosDates.monthTitle(YearMonth.of(2026, 11)))
        assertEquals("09:05", TabitosDates.time(9 * 60 + 5))
    }

    @Test fun weekRanges() {
        assertEquals("9–15 nov", TabitosDates.weekRange(today, DayOfWeek.SUNDAY))
        assertEquals("9–13 nov", TabitosDates.weekRange(today, DayOfWeek.FRIDAY))
        assertEquals("26 out – 1 nov", TabitosDates.weekRange(LocalDate.of(2026, 10, 29), DayOfWeek.SUNDAY))
    }
}
