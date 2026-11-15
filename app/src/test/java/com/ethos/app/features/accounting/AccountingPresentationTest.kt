package com.ethos.app.features.accounting

import com.ethos.app.core.accounting.domain.LedgerEntry
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountingPresentationTest {
    private val zone = ZoneOffset.UTC
    private fun entry(sequence: Long, day: LocalDate, hour: Int) = LedgerEntry(
        sequence = sequence, key = "task:$sequence", title = "Tarefa $sequence", date = day,
        time = day.atTime(hour, 0).toInstant(zone).toEpochMilli(), kind = "GAIN", nominalCoins = 10, coins = 10,
        xp = 20, balance = sequence * 10, totalXp = sequence * 20,
    )

    @Test fun historyGroupsByRecordedDayNewestFirst() {
        val monday = LocalDate.of(2026, 11, 9)
        val tuesday = monday.plusDays(1)
        val days = historyDays(listOf(entry(1, monday, 9), entry(2, monday, 18), entry(3, tuesday, 8)), zone)
        assertEquals(listOf(tuesday, monday), days.map { it.date })
        assertEquals(listOf(2L, 1L), days[1].entries.map { it.sequence })
    }
}
