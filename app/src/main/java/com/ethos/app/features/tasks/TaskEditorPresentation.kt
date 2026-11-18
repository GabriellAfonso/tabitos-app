package com.ethos.app.features.tasks

import com.ethos.app.core.tasks.domain.DeadlineKind
import com.ethos.app.core.tasks.domain.RepeatKind
import com.ethos.app.core.tasks.domain.RepeatRule
import com.ethos.app.core.tasks.domain.TaskOccurrence
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

/** A recurring task always has a date, so "Sem data" is offered only for single tasks. */
fun deadlineKindsFor(draft: TaskDraft): List<DeadlineKind> =
    DeadlineKind.entries.filter { !draft.isRecurring || it != DeadlineKind.NONE }

/** Draft after choosing a deadline kind: no date clears date and time; only a specific day keeps a time. */
fun withDeadline(draft: TaskDraft, kind: DeadlineKind, today: LocalDate): TaskDraft =
    if (kind == DeadlineKind.NONE) draft.copy(fields = draft.fields.copy(deadline = kind), dateText = "", timeText = "")
    else draft.copy(fields = draft.fields.copy(deadline = kind), dateText = draft.dateText.ifBlank { today.toString() },
        timeText = if (kind == DeadlineKind.DAY) draft.timeText else "")

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
