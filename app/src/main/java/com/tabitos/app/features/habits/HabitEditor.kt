package com.tabitos.app.features.habits

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tabitos.app.R
import com.tabitos.app.core.habits.domain.Habit
import com.tabitos.app.core.habits.domain.HabitType
import com.tabitos.app.core.habits.domain.HabitUnit
import com.tabitos.app.core.icons.AppIcon
import com.tabitos.app.core.icons.AppIconTables
import com.tabitos.app.core.tasks.domain.TaskList
import com.tabitos.app.core.ui.accounting.FinancialEstimate
import com.tabitos.app.core.ui.components.ChoiceSheet
import com.tabitos.app.core.ui.components.ColorPicker
import com.tabitos.app.core.ui.components.EditorChip
import com.tabitos.app.core.ui.components.EditorDescriptionLine
import com.tabitos.app.core.ui.components.EditorIconTile
import com.tabitos.app.core.ui.components.EditorMoreChip
import com.tabitos.app.core.ui.components.EditorNameField
import com.tabitos.app.core.ui.components.EditorToggleChip
import com.tabitos.app.core.ui.components.EditorValueRow
import com.tabitos.app.core.ui.components.FocusOnCreate
import com.tabitos.app.core.ui.components.GrowingSheet
import com.tabitos.app.core.ui.components.IconPicker
import com.tabitos.app.core.ui.components.NumberStepper
import com.tabitos.app.core.ui.components.RadioOptions
import com.tabitos.app.core.ui.components.SettingsSectionHeader
import com.tabitos.app.core.ui.components.itemColorNames
import com.tabitos.app.core.ui.components.itemColors
import com.tabitos.app.core.ui.components.option
import com.tabitos.app.core.ui.components.rememberExpandAction

/** Icons offered in the habit editor's row, by habit kind; the rest are under "see more". */
private val goodHabitPrincipalIcons =
    listOf(AppIcon.LEAF, AppIcon.WATER, AppIcon.GYM, AppIcon.STUDY, AppIcon.MEDITATION)
private val badHabitPrincipalIcons =
    listOf(AppIcon.BLOCK, AppIcon.CIGARETTE, AppIcon.DRINK, AppIcon.SCREEN_TIME, AppIcon.FAST_FOOD)

/** Persisted index of the prohibition sign; bad habits start with it. */
internal val BLOCK_HABIT_ICON = AppIconTables.habit.indexOf(AppIcon.BLOCK)

/** Drawing of the habit's persisted icon index. */
internal val Habit.iconDrawable: Int get() = AppIconTables.habit[icon].option.icon

private val weekdayShortLabels = listOf(R.string.monday, R.string.tuesday, R.string.wednesday,
    R.string.thursday, R.string.friday, R.string.saturday, R.string.sunday)
// Indexed by the stored difficulty/importance value (0..2).
private val difficultyLabels = listOf(R.string.habit_easy, R.string.habit_medium, R.string.habit_hard)
private val importanceLabels = listOf(R.string.habit_low, R.string.habit_medium, R.string.habit_high)
private val levelValues = difficultyLabels.indices.toList()

@Composable
fun HabitLists(lists: List<TaskList>, selected: String?, enabled: Boolean, change: (String?) -> Unit) {
    Text(stringResource(R.string.choose_list))
    FilterChip(selected == null, { change(null) }, enabled = enabled,
        label = { Text(stringResource(R.string.inbox)) })
    lists.forEach { list -> FilterChip(selected == list.id, { change(list.id) }, enabled = enabled,
        label = { Text(list.name) }) }
}

/**
 * Habit editor sheet (spec 029) for good and bad habits: compact with rule chips when creating, full screen with
 * value rows after "Mais" or when editing. Rule changes go through HabitEditorPresentation, so saving is unchanged.
 */
@Composable
fun HabitEditor(state: HabitsUiState, lists: List<TaskList>, actions: HabitEditorActions) {
    val d = state.draft ?: return
    val busy = state.busy
    var expanded by rememberSaveable(d.habit.id) { mutableStateOf(d.editing) }
    var sheet by rememberSaveable(d.habit.id) { mutableStateOf<HabitEditorSheet?>(null) }
    val expand = rememberExpandAction { expanded = true }
    val open: (HabitEditorSheet) -> Unit = { sheet = it }
    val focus = remember { FocusRequester() }
    FocusOnCreate(d.habit.id, !d.editing, focus)
    GrowingSheet(
        expanded = expanded,
        title = stringResource(when {
            d.editing -> R.string.edit_habit
            d.isPractice -> R.string.new_habit
            else -> R.string.editor_new_bad_habit
        }),
        busy = busy, onDismiss = { actions.change(null) },
        onCollapse = if (d.editing) null else ({ expanded = false }), onSave = actions.save,
        name = { HabitNameRow(d, busy, focus, actions.change) { open(HabitEditorSheet.LOOK) } },
    ) {
        HabitError(state, actions.retry)
        if (!expanded) EditorDescriptionLine(d.habit.description, !busy) { open(HabitEditorSheet.DESCRIPTION) }
        HabitChips(d, busy, actions.change, open, onMore = expand.takeUnless { expanded })
        if (expanded) {
            HabitRows(d, lists, busy, state.gameMode, open)
            if (state.gameMode) FinancialEstimate(d.habit.difficulty, d.habit.importance)
        }
        if (d.editing) {
            Text(stringResource(R.string.habit_future_type_hint))
            Text(stringResource(R.string.habit_goal_effect), style = MaterialTheme.typography.labelLarge)
            RadioOptions(listOf(false, true), d.nextPeriod, enabled = !busy,
                onSelect = { actions.change(d.copy(nextPeriod = it)) }, label = {
                    stringResource(if (it) R.string.habit_next_period else R.string.habit_current_period) })
        }
        sheet?.let { HabitChoiceSheet(it, d, lists, busy, actions) { sheet = null } }
    }
}

// Only callbacks cross into editor content; the screen owns its ViewModel connection.
data class HabitEditorActions(
    val change: (HabitDraft?) -> Unit, val difficulty: (Int) -> Unit,
    val save: () -> Unit, val retry: () -> Unit,
)

@Composable
private fun HabitNameRow(
    d: HabitDraft, busy: Boolean, focus: FocusRequester, change: (HabitDraft?) -> Unit, openLook: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EditorIconTile(d.habit.iconDrawable, itemColors[d.habit.color], Color.White,
            stringResource(R.string.editor_look), enabled = !busy, onClick = openLook)
        EditorNameField(d.habit.name, { change(d.copy(habit = d.habit.copy(name = it))) },
            stringResource(R.string.habit_name), !busy, focus, Modifier.weight(1f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitChips(
    d: HabitDraft, busy: Boolean, change: (HabitDraft?) -> Unit, open: (HabitEditorSheet) -> Unit,
    onMore: (() -> Unit)?,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (d.isPractice) {
            SheetChip(HabitEditorSheet.FREQUENCY, R.string.habit_frequency, frequencySummary(d),
                R.drawable.ic_repeat, busy, open)
            if (d.showsDaysChip) SheetChip(HabitEditorSheet.DAYS, R.string.editor_days, weekdaysSummary(d),
                R.drawable.ic_calendar_month, busy, open)
            if (d.showsTimesChip) SheetChip(HabitEditorSheet.TIMES, R.string.editor_times, timesSummary(d),
                R.drawable.ic_today, busy, open)
            EditorToggleChip(stringResource(if (d.timed) R.string.variant_timed else R.string.editor_timer_add),
                d.timed, R.drawable.ic_notification_timer, enabled = !busy,
                onClick = { change(withTimed(d, !d.timed)) })
            if (d.timed) SheetChip(HabitEditorSheet.MINUTES, R.string.editor_duration,
                stringResource(R.string.editor_minutes_value, minutesOf(d)), R.drawable.ic_schedule, busy, open)
        } else {
            SheetChip(HabitEditorSheet.BREAKING_TYPE, R.string.habit_type, stringResource(breakingLabel(d.rule.type)),
                R.drawable.ic_block, busy, open, tint = MaterialTheme.colorScheme.error)
            if (d.showsLimitChip) SheetChip(HabitEditorSheet.LIMIT, R.string.editor_limit, limitSummary(d),
                R.drawable.ic_block, busy, open, tint = MaterialTheme.colorScheme.error)
        }
        SheetChip(HabitEditorSheet.LOOK, R.string.editor_look, stringResource(itemColorNames[d.habit.color]),
            d.habit.iconDrawable, busy, open, tint = itemColors[d.habit.color])
        onMore?.let { EditorMoreChip(it, enabled = !busy) }
    }
}

@Composable
private fun SheetChip(
    sheet: HabitEditorSheet, @StringRes label: Int, value: String, @DrawableRes icon: Int, busy: Boolean,
    open: (HabitEditorSheet) -> Unit, tint: Color = MaterialTheme.colorScheme.primary,
) {
    EditorChip(stringResource(label), value, icon, { open(sheet) }, Modifier.testTag("chip:${sheet.name}"),
        enabled = !busy, tint = tint)
}

@Composable
private fun HabitRows(
    d: HabitDraft, lists: List<TaskList>, busy: Boolean, gameMode: Boolean, open: (HabitEditorSheet) -> Unit,
) {
    SettingsSectionHeader(stringResource(R.string.more_options))
    SheetRow(HabitEditorSheet.LIST, R.string.choose_list, listName(lists, d.habit.listId),
        R.drawable.ic_format_list_bulleted, busy, open)
    SheetRow(HabitEditorSheet.DESCRIPTION, R.string.editor_description,
        d.habit.description.ifBlank { stringResource(R.string.editor_none) }, R.drawable.ic_edit_note, busy, open)
    SheetRow(HabitEditorSheet.IMPORTANCE, R.string.habit_importance,
        stringResource(importanceLabels[d.habit.importance]), R.drawable.ic_flag, busy, open)
    if (gameMode) SheetRow(HabitEditorSheet.DIFFICULTY, R.string.habit_difficulty,
        stringResource(difficultyLabels[d.habit.difficulty]), R.drawable.ic_bolt, busy, open)
}

@Composable
private fun SheetRow(
    sheet: HabitEditorSheet, @StringRes label: Int, value: String, @DrawableRes icon: Int, busy: Boolean,
    open: (HabitEditorSheet) -> Unit,
) {
    EditorValueRow(stringResource(label), value, icon, { open(sheet) }, Modifier.testTag("row:${sheet.name}"),
        enabled = !busy)
}

@Composable
private fun HabitChoiceSheet(
    sheet: HabitEditorSheet, d: HabitDraft, lists: List<TaskList>, busy: Boolean, actions: HabitEditorActions,
    close: () -> Unit,
) {
    val change = actions.change
    ChoiceSheet(stringResource(sheetTitle(sheet)), close) {
        when (sheet) {
            HabitEditorSheet.FREQUENCY -> FrequencyOptions(d, busy, change)
            HabitEditorSheet.DAYS -> WeekdayOptions(d, busy, change)
            HabitEditorSheet.TIMES -> {
                val weekly = d.rule.unit == HabitUnit.WEEK
                NumberStepper(d.target.toIntOrNull() ?: 1, { change(d.copy(target = it.toString())) },
                    if (weekly) TIMES_PER_WEEK_RANGE else TIMES_PER_MONTH_RANGE,
                    stringResource(if (weekly) R.string.times_per_week else R.string.times_per_month),
                    enabled = !busy)
            }
            HabitEditorSheet.MINUTES -> NumberStepper(minutesOf(d), { change(d.copy(durationText = it.toString())) },
                MINUTES_RANGE, stringResource(if (d.isTimedWeekly) R.string.minutes_per_week
                    else R.string.minutes_per_day), step = MINUTES_STEP, enabled = !busy)
            HabitEditorSheet.BREAKING_TYPE -> RadioOptions(listOf(HabitType.AVOID, HabitType.LIMIT), d.rule.type,
                enabled = !busy, onSelect = { change(withBreakingType(d, it)) },
                label = { stringResource(breakingLabel(it)) })
            HabitEditorSheet.LIMIT -> LimitOptions(d, busy, change)
            HabitEditorSheet.LOOK -> {
                ColorPicker(d.habit.color, !busy) { change(d.copy(habit = d.habit.copy(color = it))) }
                IconPicker(R.string.habit_icon, if (d.isPractice) goodHabitPrincipalIcons else badHabitPrincipalIcons,
                    AppIconTables.habit[d.habit.icon], !busy, tint = itemColors[d.habit.color]) {
                    change(d.copy(habit = d.habit.copy(icon = AppIconTables.habit.indexOf(it)))) }
            }
            HabitEditorSheet.LIST -> RadioOptions(listOf<String?>(null) + lists.map { it.id }, d.habit.listId,
                enabled = !busy, onSelect = { change(d.copy(habit = d.habit.copy(listId = it))) },
                label = { listName(lists, it) })
            HabitEditorSheet.DESCRIPTION -> OutlinedTextField(d.habit.description,
                { change(d.copy(habit = d.habit.copy(description = it))) }, Modifier.fillMaxWidth(), enabled = !busy,
                label = { Text(stringResource(R.string.habit_description)) })
            HabitEditorSheet.IMPORTANCE -> RadioOptions(levelValues, d.habit.importance, enabled = !busy,
                onSelect = { change(d.copy(habit = d.habit.copy(importance = it, importanceManual = true))) },
                label = { stringResource(importanceLabels[it]) })
            HabitEditorSheet.DIFFICULTY -> RadioOptions(levelValues, d.habit.difficulty, enabled = !busy,
                onSelect = actions.difficulty, label = { stringResource(difficultyLabels[it]) })
        }
    }
}

@Composable
private fun FrequencyOptions(d: HabitDraft, busy: Boolean, change: (HabitDraft?) -> Unit) {
    // A timed habit is practiced once per day or week (spec 016).
    if (d.timed) RadioOptions(listOf(false, true), d.isTimedWeekly, enabled = !busy,
        onSelect = { change(withTimedWeekly(d, it)) },
        label = { stringResource(if (it) R.string.habit_per_week else R.string.habit_per_day) })
    else RadioOptions(PracticeFrequency.entries, practiceFrequencyOf(d.rule), enabled = !busy,
        onSelect = { change(withPracticeFrequency(d, it)) }, label = { stringResource(frequencyLabel(it)) })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdayOptions(d: HabitDraft, busy: Boolean, change: (HabitDraft?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        weekdayShortLabels.forEachIndexed { index, label ->
            FilterChip(d.rule.hasWeekday(index), { change(toggleHabitWeekday(d, index)) }, enabled = !busy,
                label = { Text(stringResource(label)) }, modifier = Modifier.sizeIn(minHeight = 48.dp))
        }
    }
}

@Composable
private fun LimitOptions(d: HabitDraft, busy: Boolean, change: (HabitDraft?) -> Unit) {
    NumberStepper(d.target.toIntOrNull() ?: 1, { change(d.copy(target = it.toString())) }, LIMIT_RANGE,
        stringResource(maxPerLabel(d.rule.unit)), prefix = stringResource(R.string.at_most), enabled = !busy)
    Text(stringResource(R.string.habit_period), style = MaterialTheme.typography.labelLarge)
    RadioOptions(HabitUnit.entries, d.rule.unit, enabled = !busy,
        onSelect = { change(d.copy(rule = d.rule.copy(unit = it))) }, label = { stringResource(unitLabel(it)) })
}

private fun sheetTitle(sheet: HabitEditorSheet): Int = when (sheet) {
    HabitEditorSheet.FREQUENCY -> R.string.habit_frequency
    HabitEditorSheet.DAYS -> R.string.habit_weekdays
    HabitEditorSheet.TIMES -> R.string.editor_times
    HabitEditorSheet.MINUTES -> R.string.editor_duration
    HabitEditorSheet.BREAKING_TYPE -> R.string.habit_type
    HabitEditorSheet.LIMIT -> R.string.editor_limit
    HabitEditorSheet.LOOK -> R.string.editor_look
    HabitEditorSheet.LIST -> R.string.choose_list
    HabitEditorSheet.DESCRIPTION -> R.string.editor_description
    HabitEditorSheet.IMPORTANCE -> R.string.habit_importance
    HabitEditorSheet.DIFFICULTY -> R.string.habit_difficulty
}

private fun frequencyLabel(frequency: PracticeFrequency): Int = when (frequency) {
    PracticeFrequency.DAILY -> R.string.habit_every_day
    PracticeFrequency.CHOSEN_DAYS -> R.string.habit_chosen_days
    PracticeFrequency.WEEKLY -> R.string.habit_times_week
    PracticeFrequency.MONTHLY -> R.string.habit_times_month
}

private fun breakingLabel(type: HabitType): Int =
    if (type == HabitType.AVOID) R.string.habit_avoid else R.string.habit_limit

private fun unitLabel(unit: HabitUnit): Int = when (unit) {
    HabitUnit.DAY -> R.string.habit_day
    HabitUnit.WEEK -> R.string.habit_week
    HabitUnit.MONTH -> R.string.habit_month
}

private fun maxPerLabel(unit: HabitUnit): Int = when (unit) {
    HabitUnit.DAY -> R.string.max_per_day
    HabitUnit.WEEK -> R.string.max_per_week
    HabitUnit.MONTH -> R.string.max_per_month
}

@Composable
private fun frequencySummary(d: HabitDraft): String = stringResource(
    if (d.timed) (if (d.isTimedWeekly) R.string.habit_per_week else R.string.habit_per_day)
    else frequencyLabel(practiceFrequencyOf(d.rule)))

@Composable
private fun weekdaysSummary(d: HabitDraft): String =
    if (d.rule.weekdays == ALL_WEEKDAYS) stringResource(R.string.habit_freq_every_day)
    else weekdayShortLabels.indices.filter { d.rule.hasWeekday(it) }
        .map { stringResource(weekdayShortLabels[it]) }.joinToString(", ")

@Composable
private fun timesSummary(d: HabitDraft): String {
    val times = d.target.toIntOrNull() ?: 1
    val plural = if (d.rule.unit == HabitUnit.WEEK) R.plurals.habit_freq_week else R.plurals.habit_freq_month
    return pluralStringResource(plural, times, times)
}

@Composable
private fun limitSummary(d: HabitDraft): String {
    val max = d.target.toIntOrNull() ?: 1
    return stringResource(when (d.rule.unit) {
        HabitUnit.DAY -> R.string.habit_freq_limit_day
        HabitUnit.WEEK -> R.string.habit_freq_limit_week
        HabitUnit.MONTH -> R.string.habit_freq_limit_month
    }, max)
}

@Composable
private fun listName(lists: List<TaskList>, id: String?): String =
    lists.firstOrNull { it.id == id }?.name ?: stringResource(R.string.inbox)

private fun minutesOf(d: HabitDraft): Int = d.durationText.toIntOrNull() ?: DEFAULT_MINUTES

private const val DEFAULT_MINUTES = 20
private const val MINUTES_STEP = 5
private val MINUTES_RANGE = 5..600
private val TIMES_PER_WEEK_RANGE = 1..7
private val TIMES_PER_MONTH_RANGE = 1..31
private val LIMIT_RANGE = 0..99
