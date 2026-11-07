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
                    HabitInput(R.string.habit_name, d.habit.name, !state.busy) {
                        actions.change(d.copy(habit = d.habit.copy(name = it))) }
                    HabitInput(R.string.habit_description, d.habit.description, !state.busy) {
                        actions.change(d.copy(habit = d.habit.copy(description = it))) }
                    HabitChoices(R.string.habit_type, listOf(R.string.habit_practice, R.string.habit_avoid,
                        R.string.habit_limit), d.rule.type.ordinal, !state.busy) { index ->
                        actions.change(d.copy(rule = d.rule.copy(type = HabitType.entries[index],
                            unit = if (index == 1) HabitUnit.DAY else d.rule.unit),
                            target = if (index == 1) "0" else "1")) }
                    if (d.editing) Text(stringResource(R.string.habit_future_type_hint))
                    if (d.rule.type != HabitType.AVOID) {
                        HabitChoices(R.string.habit_period, listOf(R.string.habit_day, R.string.habit_week,
                            R.string.habit_month), d.rule.unit.ordinal, !state.busy) {
                            actions.change(d.copy(rule = d.rule.copy(unit = HabitUnit.entries[it]), target = "1")) }
                        if (d.rule.type != HabitType.PRACTICE || d.rule.unit != HabitUnit.DAY) {
                            HabitInput(if (d.rule.type == HabitType.PRACTICE) R.string.habit_distinct_target
                                else R.string.habit_occurrence_limit, d.target, !state.busy) {
                                actions.change(d.copy(target = it)) }
                        }
                    }
                    if (d.rule.type == HabitType.PRACTICE && d.rule.unit == HabitUnit.DAY) {
                        Text(stringResource(R.string.habit_weekdays))
                        val days = listOf(R.string.habit_mon, R.string.habit_tue, R.string.habit_wed,
                            R.string.habit_thu, R.string.habit_fri, R.string.habit_sat, R.string.habit_sun)
                        days.forEachIndexed { index, label -> Row {
                            Checkbox(d.rule.weekdays and (1 shl index) != 0, {
                                actions.change(d.copy(rule = d.rule.copy(weekdays = d.rule.weekdays xor (1 shl index))))
                            }, enabled = !state.busy)
                            Text(stringResource(label), Modifier.padding(top = 12.dp))
                        } }
                    }
                    if (d.rule.unit != HabitUnit.DAY) {
                        HabitInput(R.string.habit_partial_target, d.partial, !state.busy) {
                            actions.change(d.copy(partial = it)) }
                        Text(stringResource(R.string.habit_partial_hint))
                    }
                    if (d.editing) HabitChoices(R.string.habit_goal_effect,
                        listOf(R.string.habit_current_period, R.string.habit_next_period),
                        if (d.nextPeriod) 1 else 0, !state.busy) { actions.change(d.copy(nextPeriod = it == 1)) }
                    HabitChoices(R.string.habit_difficulty, listOf(R.string.habit_easy, R.string.habit_medium,
                        R.string.habit_hard), d.habit.difficulty, !state.busy, actions.difficulty)
                    HabitChoices(R.string.habit_importance, listOf(R.string.habit_low, R.string.habit_medium,
                        R.string.habit_high), d.habit.importance, !state.busy) {
                        actions.change(d.copy(habit = d.habit.copy(importance = it, importanceManual = true))) }
                    HabitChoices(R.string.habit_color, listOf(R.string.habit_sage, R.string.habit_blue,
                        R.string.habit_purple, R.string.habit_orange, R.string.habit_rose, R.string.habit_gray),
                        d.habit.color, !state.busy) { actions.change(d.copy(habit = d.habit.copy(color = it))) }
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
                        Text(stringResource(R.string.habit_preview)) }
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
