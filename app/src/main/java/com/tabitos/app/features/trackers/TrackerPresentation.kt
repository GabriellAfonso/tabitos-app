package com.tabitos.app.features.trackers

import com.tabitos.app.core.habits.domain.HabitSnapshot
import com.tabitos.app.core.trackers.domain.QuestionType
import com.tabitos.app.core.trackers.domain.Tracker
import com.tabitos.app.core.trackers.domain.TrackerAnalytics
import com.tabitos.app.core.trackers.domain.TrackerEntry
import com.tabitos.app.core.trackers.domain.TrackerOption
import com.tabitos.app.core.trackers.domain.TrackerQuestion
import com.tabitos.app.core.trackers.domain.TrackerRules
import com.tabitos.app.core.trackers.domain.TrackerSnapshot
import com.tabitos.app.core.ui.format.DateText
import com.tabitos.app.core.ui.format.TabitosDates
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Question kinds offered by the editor; CHOICE splits into one or many answers (spec 034). */
enum class QuestionKind(val type: QuestionType, val multiple: Boolean) {
    SCALE(QuestionType.SCALE, false),
    SINGLE(QuestionType.CHOICE, false),
    MULTIPLE(QuestionType.CHOICE, true),
    TEXT(QuestionType.TEXT, false),
}

val TrackerQuestion.kind: QuestionKind
    get() = when (type) {
        QuestionType.SCALE -> QuestionKind.SCALE
        QuestionType.CHOICE -> if (multiple) QuestionKind.MULTIPLE else QuestionKind.SINGLE
        QuestionType.TEXT -> QuestionKind.TEXT
    }

/**
 * Changes the question kind keeping what the user typed. Only scales can be the primary indicator, and a choice
 * needs at least one option, so switching drops the indicator and adds an empty option when required.
 */
fun TrackerQuestion.withKind(kind: QuestionKind, newOption: () -> TrackerOption): TrackerQuestion {
    if (kind == this.kind) return this
    val needsOption = kind.type == QuestionType.CHOICE && options.isEmpty()
    return copy(type = kind.type, multiple = kind.multiple, primary = primary && kind == QuestionKind.SCALE,
        options = if (needsOption) listOf(newOption()) else options)
}

/** Marks [questionId] as the chart indicator; only one scale can be it. */
fun List<TrackerQuestion>.withPrimary(questionId: String, primary: Boolean): List<TrackerQuestion> = map {
    when {
        it.id == questionId -> it.copy(primary = primary)
        primary -> it.copy(primary = false)
        else -> it
    }
}

/** How a scale is answered: a single row of buttons, wrapped chips, or a number for very long ranges. */
enum class ScaleInput { TRACK, CHIPS, NUMBER }

private const val TRACK_MAX_VALUES = 11
private const val CHIPS_MAX_VALUES = 21

fun scaleInput(q: TrackerQuestion): ScaleInput {
    val values = q.max - q.min + 1
    return when {
        values <= TRACK_MAX_VALUES -> ScaleInput.TRACK
        values <= CHIPS_MAX_VALUES -> ScaleInput.CHIPS
        else -> ScaleInput.NUMBER
    }
}

/** Options with descriptions need room for the second line, so they keep list rows instead of chips. */
fun choicesAsChips(q: TrackerQuestion): Boolean = q.options.all { it.description.isBlank() }

enum class TodayKind { EMPTY, FILLED, ARCHIVED, HABIT_EXCLUDED }

/** The "Hoje" strip at the top of the tracker page. */
data class TodaySummary(
    val kind: TodayKind, val entries: Int = 0, val latestEntryId: String? = null, val primary: Int? = null,
    val allowsAnother: Boolean = false,
) {
    /** One entry per day opens the existing entry; "Vários por dia" always starts a new one. */
    val opensExisting: Boolean get() = kind == TodayKind.FILLED && !allowsAnother
}

fun todaySummary(s: TrackerSnapshot, tracker: Tracker, habits: HabitSnapshot, today: LocalDate): TodaySummary {
    if (!tracker.active) return TodaySummary(TodayKind.ARCHIVED)
    if (!TrackerRules.editable(tracker, habits)) return TodaySummary(TodayKind.HABIT_EXCLUDED)
    val entries = s.entries(tracker.id).filter { it.date == today }
    val latest = entries.maxWithOrNull(compareBy({ it.filledAt }, { it.id }))
        ?: return TodaySummary(TodayKind.EMPTY, allowsAnother = tracker.multiple)
    return TodaySummary(TodayKind.FILLED, entries.size, latest.id, primaryValue(s, tracker, latest),
        tracker.multiple)
}

private fun primaryValue(s: TrackerSnapshot, tracker: Tracker, entry: TrackerEntry): Int? {
    val primaryId = s.current(tracker.id)?.questions?.firstOrNull { it.primary }?.id ?: return null
    return entry.answers.firstOrNull { it.questionId == primaryId }?.scale
}

/** One entry as a row: the primary value and the labels of the options it marked. */
data class RecordEntry(val id: String, val primary: Int?, val choices: List<String>)
data class RecordDay(val date: LocalDate, val entries: List<RecordEntry>, val fillable: Boolean)
data class RecordWeek(val start: LocalDate, val days: List<RecordDay>)
data class RecordList(val weeks: List<RecordWeek>, val hasMore: Boolean)

/** "Hoje", "Ontem" or "ter, 17 nov" for a row of the records list. */
fun recordDayLabel(date: LocalDate, today: LocalDate): DateText = when (val text = TabitosDates.short(date, today)) {
    is DateText.Plain -> DateText.Plain(TabitosDates.shortWithWeekday(date, today))
    else -> text
}

sealed interface WeekTitle {
    data object Current : WeekTitle
    data object Previous : WeekTitle
    data class Of(val start: LocalDate) : WeekTitle
}

fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun weekTitle(start: LocalDate, today: LocalDate): WeekTitle = when (start) {
    weekStart(today) -> WeekTitle.Current
    weekStart(today).minusWeeks(1) -> WeekTitle.Previous
    else -> WeekTitle.Of(start)
}

/** Days from today back to the tracker creation, newest first, grouped by Monday–Sunday weeks. */
fun recordList(
    s: TrackerSnapshot, tracker: Tracker, habits: HabitSnapshot, today: LocalDate, weeks: Int,
): RecordList {
    val windowStart = weekStart(today).minusWeeks(weeks - 1L)
    val first = maxOf(windowStart, tracker.created)
    val byDate = s.entries(tracker.id).groupBy { it.date }
    val days = generateSequence(today) { it.minusDays(1) }.takeWhile { it >= first }.map { date ->
        val entries = byDate[date].orEmpty().sortedWith(compareByDescending<TrackerEntry> { it.filledAt }
            .thenByDescending { it.id })
        RecordDay(date, entries.map { recordEntry(s, tracker, it) }, TrackerRules.fillable(tracker, habits, date, today))
    }.toList()
    val grouped = days.groupBy { weekStart(it.date) }.map { (start, list) -> RecordWeek(start, list) }
    return RecordList(grouped, hasMore = tracker.created < windowStart)
}

private fun recordEntry(s: TrackerSnapshot, tracker: Tracker, entry: TrackerEntry): RecordEntry {
    val questions = s.version(tracker.id, entry.version)?.questions.orEmpty()
    val labels = entry.answers.flatMap { answer ->
        val options = questions.firstOrNull { it.id == answer.questionId }?.options.orEmpty()
        answer.choices.mapNotNull { id -> options.firstOrNull { it.id == id }?.label }
    }
    return RecordEntry(entry.id, primaryValue(s, tracker, entry), labels)
}

/** Option counts of one question, with each bar relative to the most chosen option of that question. */
data class FrequencyBar(val label: String, val count: Int, val fraction: Float)
data class FrequencyGroup(val question: String, val bars: List<FrequencyBar>)

fun frequencyGroups(a: TrackerAnalytics): List<FrequencyGroup> = a.options.groupBy { it.questionId }.values
    .map { counts ->
        val max = counts.maxOf { it.count }
        FrequencyGroup(counts.first().question, counts.sortedByDescending { it.count }.map {
            FrequencyBar(it.label, it.count, if (max == 0) 0f else it.count.toFloat() / max)
        })
    }
