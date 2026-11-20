package com.tabitos.app.features.tasks

import com.tabitos.app.core.tasks.domain.DeadlineKind
import com.tabitos.app.core.tasks.domain.RepeatKind
import com.tabitos.app.core.tasks.domain.RepeatRule
import com.tabitos.app.core.tasks.domain.TaskOccurrence
import java.time.LocalDate

/** First-level repetition options shown in the editor (spec 013); each maps onto existing kinds. */
enum class RepeatChoice { NONE, DAILY, WEEKLY, MONTHLY, INTERVAL }

fun repeatChoiceOf(kind: RepeatKind): RepeatChoice = when (kind) {
    RepeatKind.NONE -> RepeatChoice.NONE
    RepeatKind.DAILY, RepeatKind.WEEKDAYS -> RepeatChoice.DAILY
    RepeatKind.WEEKLY, RepeatKind.CUSTOM -> RepeatChoice.WEEKLY
    RepeatKind.MONTHLY -> RepeatChoice.MONTHLY
    RepeatKind.FIXED_INTERVAL, RepeatKind.AFTER_COMPLETION -> RepeatChoice.INTERVAL
}

/** One selected weekday is the weekly kind; two or more are the custom-weekdays kind. */
fun weeklyKind(weekdays: Int): RepeatKind = if (Integer.bitCount(weekdays) <= 1) RepeatKind.WEEKLY
    else RepeatKind.CUSTOM

/** Toggles weekday [index] (0 = Monday) and never leaves the selection empty. */
fun toggleWeekday(weekdays: Int, index: Int): Int = (weekdays xor (1 shl index)).takeIf { it != 0 } ?: weekdays

/**
 * Rule after choosing a first-level option. Keeps the current sub-option when it already belongs to the
 * choice; a new weekly choice starts on the weekday of [start].
 */
fun ruleForChoice(rule: RepeatRule, choice: RepeatChoice, start: LocalDate): RepeatRule {
    if (repeatChoiceOf(rule.kind) == choice) return rule
    return when (choice) {
        RepeatChoice.NONE -> rule.copy(kind = RepeatKind.NONE)
        RepeatChoice.DAILY -> rule.copy(kind = RepeatKind.DAILY)
        RepeatChoice.WEEKLY -> rule.copy(kind = RepeatKind.WEEKLY, weekdays = 1 shl (start.dayOfWeek.value - 1))
        RepeatChoice.MONTHLY -> rule.copy(kind = RepeatKind.MONTHLY)
        RepeatChoice.INTERVAL -> rule.copy(kind = RepeatKind.FIXED_INTERVAL)
    }
}

/** Choice sheet opened from a chip or row of the task editor sheet (spec 029). */
enum class TaskEditorSheet { WHEN, REPEAT, ENDING, LIST, DURATION, DESCRIPTION, CHECKLIST, IMPORTANCE, DIFFICULTY }

/** Values of [TaskDraft.ending], as already read and written by the ViewModel. */
const val ENDING_NEVER = 0
const val ENDING_UNTIL_DATE = 1
const val ENDING_AFTER_COUNT = 2

val TaskDraft.isRecurring: Boolean get() = rule.kind != RepeatKind.NONE

/** A timed task has a duration and no checklist (spec 016). */
val TaskDraft.showsDurationChip: Boolean get() = timed
val TaskDraft.showsChecklistRow: Boolean get() = !timed
val TaskDraft.showsTimedChecklistWarning: Boolean get() = timed && fields.checklist.isNotEmpty()
val TaskDraft.showsEndingRow: Boolean get() = isRecurring

/** A new recurring task is anchored by its start date; editing moves this occurrence's date. */
val TaskDraft.editsStartDate: Boolean get() = isRecurring && !editing

/**
 * A recurring task always has a date, so "Sem data" is offered only for single tasks; "Até uma data" never
 * repeats (spec 035), so it is offered only for single tasks too.
 */
fun deadlineKindsFor(draft: TaskDraft): List<DeadlineKind> = DeadlineKind.entries.filter {
    !draft.isRecurring || (it != DeadlineKind.NONE && it != DeadlineKind.UNTIL_DATE)
}

val TaskDraft.isUntilDate: Boolean get() = fields.deadline == DeadlineKind.UNTIL_DATE

/** Repetition is unavailable while the deadline is "Até uma data" (spec 035). */
val TaskDraft.allowsRepeat: Boolean get() = !isUntilDate

/**
 * Draft after choosing a deadline kind: no date clears date and time; only kinds with a chosen day keep a time.
 * Entering "Até uma data" keeps the chosen day as the end and starts the window today (or on the end, if earlier).
 */
fun withDeadline(draft: TaskDraft, kind: DeadlineKind, today: LocalDate): TaskDraft {
    if (kind == DeadlineKind.NONE) {
        return draft.copy(fields = draft.fields.copy(deadline = kind), dateText = "", timeText = "")
    }
    // A new task's "Até domingo/sexta" means this week, which is the range shown beside the option.
    val enteringWeek = kind.isWeekWindow && draft.fields.deadline != kind && !draft.editing
    val end = if (enteringWeek) today.toString() else draft.dateText.ifBlank { today.toString() }
    val enteringWindow = kind == DeadlineKind.UNTIL_DATE && !draft.isUntilDate
    val start = if (enteringWindow) minOf(today, parseIsoDate(end) ?: today).toString() else draft.startText
    return draft.copy(fields = draft.fields.copy(deadline = kind), dateText = end, startText = start,
        timeText = if (kind.hasDayTime) draft.timeText else "")
}

private val DeadlineKind.isWeekWindow: Boolean get() = this == DeadlineKind.WEEK || this == DeadlineKind.WORK_WEEK

/** Day whose week the "Até domingo/sexta" options describe: the chosen week once picked, otherwise this week. */
fun weekReference(draft: TaskDraft, kind: DeadlineKind, today: LocalDate): LocalDate =
    if (draft.fields.deadline == kind) parseIsoDate(draft.dateText) ?: today else today

/**
 * The card under the deadline options has something to set: the series start, the day and time of a chosen-day
 * deadline, or the week being edited. A new single "Até domingo/sexta" needs nothing more (spec 035, opção C).
 */
val TaskDraft.showsDeadlineDetails: Boolean get() = fields.deadline != DeadlineKind.NONE &&
    (editsStartDate || fields.deadline.hasDayTime || editing)

/** Choosing "Até uma data" goes straight to its end date, as nothing else about it is set yet. */
fun opensUntilDatePicker(from: DeadlineKind, to: DeadlineKind): Boolean =
    to == DeadlineKind.UNTIL_DATE && from != DeadlineKind.UNTIL_DATE

/**
 * Bounds of the "Até uma data" pickers. The start is free from today on (an edited task keeps any past start), so
 * it never gets stuck on the default end; the end cannot come before the start.
 */
data class UntilDateLimits(val earliestStart: LocalDate?, val earliestEnd: LocalDate)

fun untilDateLimits(draft: TaskDraft, today: LocalDate) = UntilDateLimits(
    earliestStart = today.takeUnless { draft.editing },
    earliestEnd = parseIsoDate(draft.startText) ?: today,
)

/** Draft after picking the window's start; an end that would come before it moves to the start. */
fun withUntilDateStart(draft: TaskDraft, start: LocalDate): TaskDraft {
    val end = parseIsoDate(draft.dateText)
    val newEnd = if (end == null || end < start) start else end
    return draft.copy(startText = start.toString(), dateText = newEnd.toString())
}
/** A new task keeps the day already picked when it becomes (or stops being) recurring. */
fun withRepeatChoiceDates(draft: TaskDraft, next: RepeatChoice): TaskDraft {
    val current = repeatChoiceOf(draft.rule.kind)
    if (draft.editing || (current == RepeatChoice.NONE) == (next == RepeatChoice.NONE)) return draft
    return if (next == RepeatChoice.NONE) draft.copy(dateText = draft.startText)
        else draft.copy(startText = draft.dateText.ifBlank { draft.startText })
}

/** Rule after choosing [next]; a task that was not recurring anchors the new rule on its chosen day. */
fun ruleForRepeatChoice(draft: TaskDraft, next: RepeatChoice, today: LocalDate): RepeatRule {
    val start = repeatStart(draft, today)
    val anchor = if (draft.isRecurring) start else parseIsoDate(draft.dateText) ?: start
    return ruleForChoice(draft.rule, next, anchor)
}

/** First day of the series; also the earliest final date the ending can have. */
fun repeatStart(draft: TaskDraft, today: LocalDate): LocalDate = parseIsoDate(draft.startText) ?: today

private fun parseIsoDate(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()

/** Mirrors TaskRules.edit: confirmation is required only for an overdue occurrence whose date changes. */
fun needsReviewConfirmation(draft: TaskDraft, occurrence: TaskOccurrence?, today: LocalDate): Boolean {
    if (!draft.editing || occurrence == null || !occurrence.needsReview(today)) return false
    val date = if (draft.fields.deadline == DeadlineKind.NONE) null
        else runCatching { LocalDate.parse(draft.dateText) }.getOrNull()
    return draft.fields.deadline != occurrence.fields.deadline || date != occurrence.fields.date
}

fun addChecklistItem(items: List<String>, after: Int = items.lastIndex): List<String> =
    items.toMutableList().apply { add((after + 1).coerceIn(0, size), "") }

fun removeChecklistItem(items: List<String>, index: Int): List<String> =
    items.filterIndexed { i, _ -> i != index }
