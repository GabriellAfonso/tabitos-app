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

/** "Mais opções" starts expanded when any advanced field differs from the new-task defaults. */
fun hasAdvancedOptions(draft: TaskDraft): Boolean = draft.fields.description.isNotBlank() ||
    draft.fields.checklist.any { it.isNotBlank() } || draft.fields.difficulty != 1 ||
    draft.fields.importance != 1 || draft.rule.kind != RepeatKind.NONE || draft.ending != 0

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
