package com.ethos.app.features.accounting

import com.ethos.app.core.accounting.domain.LedgerEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One day of the coin history: newest day first, newest entry first inside it. */
data class HistoryDay(val date: LocalDate, val entries: List<LedgerEntry>)

/** Groups by the moment each entry was recorded, so a correction appears on the day it was made. */
internal fun historyDays(entries: List<LedgerEntry>, zone: ZoneId): List<HistoryDay> =
    entries.asReversed()
        .groupBy { Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate() }
        .map { (date, dayEntries) -> HistoryDay(date, dayEntries) }
