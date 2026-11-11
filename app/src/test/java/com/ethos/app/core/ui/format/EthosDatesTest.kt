package com.ethos.app.core.ui.format

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class EthosDatesTest {
    private val today = LocalDate.of(2026, 11, 10) // Tuesday

    @Test fun relativeDays() {
        assertEquals(DateText.Today, EthosDates.short(today, today))
        assertEquals(DateText.Tomorrow, EthosDates.short(today.plusDays(1), today))
        assertEquals(DateText.Yesterday, EthosDates.long(today.minusDays(1), today))
    }

    @Test fun sameYearOmitsYear() {
        assertEquals(DateText.Plain("20 nov"), EthosDates.short(LocalDate.of(2026, 11, 20), today))
        assertEquals(DateText.Plain("sexta, 20 de novembro"), EthosDates.long(LocalDate.of(2026, 11, 20), today))
        assertEquals("5 mai", EthosDates.shortPlain(LocalDate.of(2026, 5, 5), today))
    }

    @Test fun otherYearAddsYear() {
        assertEquals(DateText.Plain("10 nov 2027"), EthosDates.short(LocalDate.of(2027, 11, 10), today))
        assertEquals(DateText.Plain("quarta, 10 de novembro de 2027"),
            EthosDates.long(LocalDate.of(2027, 11, 10), today))
        assertEquals("sábado, 1 de janeiro de 2028", EthosDates.longPlain(LocalDate.of(2028, 1, 1), today))
    }

    @Test fun monthTitleAndTime() {
        assertEquals("novembro de 2026", EthosDates.monthTitle(YearMonth.of(2026, 11)))
        assertEquals("09:05", EthosDates.time(9 * 60 + 5))
    }

    @Test fun weekRanges() {
        assertEquals("9–15 nov", EthosDates.weekRange(today, DayOfWeek.SUNDAY))
        assertEquals("9–13 nov", EthosDates.weekRange(today, DayOfWeek.FRIDAY))
        assertEquals("26 out – 1 nov", EthosDates.weekRange(LocalDate.of(2026, 10, 29), DayOfWeek.SUNDAY))
    }
}
