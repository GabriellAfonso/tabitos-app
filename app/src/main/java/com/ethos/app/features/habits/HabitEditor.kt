package com.ethos.app.features.habits

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
                    com.ethos.app.core.ui.accounting.FinancialEstimate(d.habit.difficulty, d.habit.importance)
                    HabitInput(R.string.habit_name, d.habit.name, !state.busy) {
                        actions.change(d.copy(habit = d.habit.copy(name = it))) }
                    HabitInput(R.string.habit_description, d.habit.description, !state.busy) {
                        actions.change(d.copy(habit = d.habit.copy(description = it))) }
                    if (d.rule.type == HabitType.PRACTICE) PracticeFrequency(d, !state.busy, actions.change)
                    else BreakingRule(d, !state.busy, actions.change)
                    if (d.editing) Text(stringResource(R.string.habit_future_type_hint))
                    if (d.editing) HabitChoices(R.string.habit_goal_effect,
                        listOf(R.string.habit_current_period, R.string.habit_next_period),
                        if (d.nextPeriod) 1 else 0, !state.busy) { actions.change(d.copy(nextPeriod = it == 1)) }
                    HabitChoices(R.string.habit_difficulty, listOf(R.string.habit_easy, R.string.habit_medium,
                        R.string.habit_hard), d.habit.difficulty, !state.busy, actions.difficulty)
                    HabitChoices(R.string.habit_importance, listOf(R.string.habit_low, R.string.habit_medium,
                        R.string.habit_high), d.habit.importance, !state.busy) {
                        actions.change(d.copy(habit = d.habit.copy(importance = it, importanceManual = true))) }
                    ColorPicker(d.habit.color, !state.busy) {
                        actions.change(d.copy(habit = d.habit.copy(color = it))) }
                    HabitChoices(R.string.habit_icon, listOf(R.string.habit_icon_leaf, R.string.habit_icon_book,
                        R.string.habit_icon_home, R.string.habit_icon_work, R.string.habit_icon_heart,
                        R.string.habit_icon_star), d.habit.icon, !state.busy) {
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
    if (selected >= 2) HabitInput(R.string.habit_times, d.target, enabled) { change(d.copy(target = it)) }
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
        HabitInput(R.string.habit_occurrence_limit, d.target, enabled) { change(d.copy(target = it)) }
    }
}
