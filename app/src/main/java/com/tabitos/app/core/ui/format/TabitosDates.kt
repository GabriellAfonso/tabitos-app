package com.tabitos.app.core.ui.format

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** User-facing date wording; relative words are resolved from resources by the caller. */
sealed interface DateText {
    data object Today : DateText
    data object Tomorrow : DateText
    data object Yesterday : DateText
    data class Plain(val text: String) : DateText
}

/** Pure pt-BR date formatting shared by task and habit screens (spec 013). */
object TabitosDates {
    private val ptBr: Locale = Locale.forLanguageTag("pt-BR")
    private val monthShort = DateTimeFormatter.ofPattern("MMM", ptBr)
    private val monthLong = DateTimeFormatter.ofPattern("MMMM", ptBr)
    private val weekday = DateTimeFormatter.ofPattern("EEEE", ptBr)
    private val weekdayShort = DateTimeFormatter.ofPattern("EEE", ptBr)

    fun short(date: LocalDate, today: LocalDate): DateText = relative(date, today) ?: DateText.Plain(
        shortPlain(date, today))

    fun long(date: LocalDate, today: LocalDate): DateText = relative(date, today) ?: DateText.Plain(
        longPlain(date, today))

    /** "10 nov", or "10 nov 2027" outside the current year. */
    fun shortPlain(date: LocalDate, today: LocalDate): String {
        val base = "${date.dayOfMonth} ${shortMonth(date)}"
        return if (date.year == today.year) base else "$base ${date.year}"
    }

    /** "ter, 17 nov", with the year outside the current one. */
    fun shortWithWeekday(date: LocalDate, today: LocalDate): String =
        "${date.format(weekdayShort).removeSuffix(".")}, ${shortPlain(date, today)}"

    /** "segunda, 10 de novembro", with " de 2027" outside the current year. */
    fun longPlain(date: LocalDate, today: LocalDate): String {
        val day = date.format(weekday).removeSuffix("-feira")
        val base = "$day, ${date.dayOfMonth} de ${date.format(monthLong)}"
        return if (date.year == today.year) base else "$base de ${date.year}"
    }

    /** "novembro de 2026". */
    fun monthTitle(month: YearMonth): String = "${month.atDay(1).format(monthLong)} de ${month.year}"

    /** Range of the week containing [today], ending on [last]: "10–16 nov" or "28 out – 3 nov". */
    fun weekRange(today: LocalDate, last: DayOfWeek): String {
        val start = today.with(DayOfWeek.MONDAY)
        val end = start.plusDays((last.value - 1).toLong())
        return if (start.month == end.month) "${start.dayOfMonth}–${end.dayOfMonth} ${shortMonth(end)}"
        else "${start.dayOfMonth} ${shortMonth(start)} – ${end.dayOfMonth} ${shortMonth(end)}"
    }

    fun time(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

    private fun shortMonth(date: LocalDate) = date.format(monthShort).removeSuffix(".")

    private fun relative(date: LocalDate, today: LocalDate): DateText? = when (date) {
        today -> DateText.Today
        today.plusDays(1) -> DateText.Tomorrow
        today.minusDays(1) -> DateText.Yesterday
        else -> null
    }
}
