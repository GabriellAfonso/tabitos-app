package com.ethos.app.features.habits

import com.ethos.app.core.ui.components.NumberStepper
import com.ethos.app.core.ui.accounting.FinancialEstimate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ethos.app.R
import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.tasks.domain.TaskList
import com.ethos.app.core.ui.components.ColorPicker
import com.ethos.app.core.ui.components.IconOption
import com.ethos.app.core.ui.components.IconPicker
import com.ethos.app.core.ui.components.itemColors

// Index is the persisted Habit.icon value; keep the order stable.
internal val habitIcons = listOf(IconOption("❧", R.string.habit_icon_leaf),
    IconOption("▤", R.string.habit_icon_book), IconOption("⌂", R.string.habit_icon_home),
    IconOption("▣", R.string.habit_icon_work), IconOption("♡", R.string.habit_icon_heart),
    IconOption("☆", R.string.habit_icon_star))

@Composable
fun HabitInput(label: Int, value: String, enabled: Boolean = true, change: (String) -> Unit) {
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), enabled = enabled,
        label = { Text(stringResource(label)) })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitChoices(label: Int, options: List<Int>, selected: Int, enabled: Boolean = true, change: (Int) -> Unit) {
    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { index, title ->
            FilterChip(selected == index, { change(index) }, enabled = enabled,
                label = { Text(stringResource(title)) })
        }
    }
}

@Composable
fun HabitLists(lists: List<TaskList>, selected: String?, enabled: Boolean, change: (String?) -> Unit) {
    Text(stringResource(R.string.choose_list))
    FilterChip(selected == null, { change(null) }, enabled = enabled,
        label = { Text(stringResource(R.string.inbox)) })
    lists.forEach { list -> FilterChip(selected == list.id, { change(list.id) }, enabled = enabled,
        label = { Text(list.name) }) }
}

@Composable
fun HabitEditor(state: HabitsUiState, lists: List<TaskList>, actions: HabitEditorActions) {
    val d = state.draft ?: return
    Dialog(onDismissRequest = { if (!state.busy) actions.change(null) },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(if (d.editing) R.string.edit_habit else R.string.new_habit),
                    style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HabitError(state, actions.retry)
                    if (state.gameMode) FinancialEstimate(d.habit.difficulty, d.habit.importance)
                    HabitInput(R.string.habit_name, d.habit.name, !state.busy) {
                        actions.change(d.copy(habit = d.habit.copy(name = it))) }
                    HabitInput(R.string.habit_description, d.habit.description, !state.busy) {
                        actions.change(d.copy(habit = d.habit.copy(description = it))) }
                    if (d.rule.type == HabitType.PRACTICE) {
                        HabitChoices(R.string.variant, listOf(R.string.variant_normal, R.string.variant_timed),
                            if (d.timed) 1 else 0, !state.busy) { index ->
                            actions.change(if (index == 1) d.copy(timed = true, target = "1",
                                durationText = d.durationText.ifBlank { "20" },
                                rule = d.rule.copy(unit = if (d.rule.unit == HabitUnit.MONTH) HabitUnit.DAY else d.rule.unit))
                                else d.copy(timed = false))
                        }
                        if (d.timed) TimedFrequency(d, !state.busy, actions.change)
                        else PracticeFrequency(d, !state.busy, actions.change)
                    }
                    else BreakingRule(d, !state.busy, actions.change)
                    if (d.editing) Text(stringResource(R.string.habit_future_type_hint))
                    if (d.editing) HabitChoices(R.string.habit_goal_effect,
                        listOf(R.string.habit_current_period, R.string.habit_next_period),
                        if (d.nextPeriod) 1 else 0, !state.busy) { actions.change(d.copy(nextPeriod = it == 1)) }
                    if (state.gameMode) HabitChoices(R.string.habit_difficulty, listOf(R.string.habit_easy,
                        R.string.habit_medium, R.string.habit_hard), d.habit.difficulty, !state.busy, actions.difficulty)
                    HabitChoices(R.string.habit_importance, listOf(R.string.habit_low, R.string.habit_medium,
                        R.string.habit_high), d.habit.importance, !state.busy) {
                        actions.change(d.copy(habit = d.habit.copy(importance = it, importanceManual = true))) }
                    ColorPicker(d.habit.color, !state.busy) {
                        actions.change(d.copy(habit = d.habit.copy(color = it))) }
                    IconPicker(R.string.habit_icon, habitIcons, d.habit.icon, !state.busy,
                        tint = itemColors[d.habit.color]) {
                        actions.change(d.copy(habit = d.habit.copy(icon = it))) }
                    HabitLists(lists, d.habit.listId, !state.busy) {
                        actions.change(d.copy(habit = d.habit.copy(listId = it))) }
                }
                Row {
                    TextButton({ actions.change(null) }, enabled = !state.busy) {
                        Text(stringResource(R.string.cancel)) }
                    Button(actions.save, enabled = !state.busy) {
                        Text(stringResource(R.string.save)) }
                }
            }
        }
    }
}

// Only callbacks cross into editor content; the screen owns its ViewModel connection.
data class HabitEditorActions(
    val change: (HabitDraft?) -> Unit, val difficulty: (Int) -> Unit,
    val save: () -> Unit, val retry: () -> Unit,
)

/** Practice frequency in plain words, mapped onto the existing day/weekday/week/month rules (spec 014). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeFrequency(d: HabitDraft, enabled: Boolean, change: (HabitDraft?) -> Unit) {
    val r = d.rule
    val selected = when {
        r.unit == HabitUnit.DAY && r.weekdays == 127 -> 0
        r.unit == HabitUnit.DAY -> 1
        r.unit == HabitUnit.WEEK -> 2
        else -> 3
    }
    HabitChoices(R.string.habit_frequency, listOf(R.string.habit_every_day, R.string.habit_chosen_days,
        R.string.habit_times_week, R.string.habit_times_month), selected, enabled) { index ->
        if (index == selected) return@HabitChoices
        change(when (index) {
            0 -> d.copy(rule = r.copy(unit = HabitUnit.DAY, weekdays = 127), target = "1")
            1 -> d.copy(rule = r.copy(unit = HabitUnit.DAY, weekdays = 0b0011111), target = "1")
            2 -> d.copy(rule = r.copy(unit = HabitUnit.WEEK, weekdays = 127), target = "3")
            else -> d.copy(rule = r.copy(unit = HabitUnit.MONTH, weekdays = 127), target = "10")
        })
    }
    if (selected == 1) {
        Text(stringResource(R.string.habit_weekdays), style = MaterialTheme.typography.labelLarge)
        val days = listOf(R.string.habit_mon, R.string.habit_tue, R.string.habit_wed,
            R.string.habit_thu, R.string.habit_fri, R.string.habit_sat, R.string.habit_sun)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEachIndexed { index, label ->
                FilterChip(r.weekdays and (1 shl index) != 0, {
                    val bits = r.weekdays xor (1 shl index)
                    if (bits != 0) change(d.copy(rule = r.copy(weekdays = bits)))
                }, enabled = enabled, label = { Text(stringResource(label)) })
            }
        }
    }
    if (selected >= 2) NumberStepper(d.target.toIntOrNull() ?: 1, { change(d.copy(target = it.toString())) },
        if (selected == 2) 1..7 else 1..31,
        stringResource(if (selected == 2) R.string.times_per_week else R.string.times_per_month), enabled = enabled)
}

/** "Quebrar hábito": avoid entirely or limit how often, never mixed with practice. */
@Composable
private fun BreakingRule(d: HabitDraft, enabled: Boolean, change: (HabitDraft?) -> Unit) {
    val r = d.rule
    HabitChoices(R.string.habit_type, listOf(R.string.habit_avoid, R.string.habit_limit),
        if (r.type == HabitType.AVOID) 0 else 1, enabled) { index ->
        change(if (index == 0) d.copy(rule = r.copy(type = HabitType.AVOID, unit = HabitUnit.DAY), target = "0")
            else d.copy(rule = r.copy(type = HabitType.LIMIT), target = "1"))
    }
    if (r.type == HabitType.LIMIT) {
        HabitChoices(R.string.habit_period, listOf(R.string.habit_day, R.string.habit_week,
            R.string.habit_month), r.unit.ordinal, enabled) {
            change(d.copy(rule = r.copy(unit = HabitUnit.entries[it]))) }
        NumberStepper(d.target.toIntOrNull() ?: 1, { change(d.copy(target = it.toString())) }, 0..99,
            stringResource(when (r.unit) {
                HabitUnit.DAY -> R.string.max_per_day
                HabitUnit.WEEK -> R.string.max_per_week
                HabitUnit.MONTH -> R.string.max_per_month
            }), prefix = stringResource(R.string.at_most), enabled = enabled)
    }
}

/** Timed practice: minutes per day (on chosen weekdays) or per week; one practice per period (spec 016). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimedFrequency(d: HabitDraft, enabled: Boolean, change: (HabitDraft?) -> Unit) {
    val r = d.rule
    val weekly = r.unit == HabitUnit.WEEK
    HabitChoices(R.string.habit_frequency, listOf(R.string.habit_per_day, R.string.habit_per_week),
        if (weekly) 1 else 0, enabled) { index ->
        change(d.copy(target = "1", rule = r.copy(unit = if (index == 1) HabitUnit.WEEK else HabitUnit.DAY,
            weekdays = if (index == 1) 127 else r.weekdays)))
    }
    if (!weekly) {
        Text(stringResource(R.string.habit_weekdays), style = MaterialTheme.typography.labelLarge)
        val days = listOf(R.string.habit_mon, R.string.habit_tue, R.string.habit_wed,
            R.string.habit_thu, R.string.habit_fri, R.string.habit_sat, R.string.habit_sun)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEachIndexed { index, label ->
                FilterChip(r.weekdays and (1 shl index) != 0, {
                    val bits = r.weekdays xor (1 shl index)
                    if (bits != 0) change(d.copy(rule = r.copy(weekdays = bits)))
                }, enabled = enabled, label = { Text(stringResource(label)) })
            }
        }
    }
    NumberStepper(d.durationText.toIntOrNull() ?: DEFAULT_MINUTES, { change(d.copy(durationText = it.toString())) },
        MINUTES_RANGE, stringResource(if (weekly) R.string.minutes_per_week else R.string.minutes_per_day),
        step = MINUTES_STEP, enabled = enabled)
}

private const val DEFAULT_MINUTES = 20
private const val MINUTES_STEP = 5
private val MINUTES_RANGE = 5..600
