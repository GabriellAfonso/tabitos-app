package com.ethos.app.features.trackers

import com.ethos.app.core.ui.components.DragHandle
import com.ethos.app.core.ui.components.ReorderableColumn
import com.ethos.app.core.habits.domain.HabitStatus
import com.ethos.app.core.ui.components.ActionBanner
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.trackers.domain.*
import com.ethos.app.core.ui.theme.EthosTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val timeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
fun formatDay(date: LocalDate): String = date.format(dayFormat)
fun formatMoment(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFormat)

fun trackerProblemText(problem: TrackerProblem, question: String?): Int = when (problem) {
    TrackerProblem.EMPTY_FORM -> R.string.tracker_error_empty
    TrackerProblem.QUESTION -> if (question == "reminder") R.string.tracker_error_reminder
        else R.string.tracker_error_question
    TrackerProblem.SCALE -> R.string.tracker_error_scale
    TrackerProblem.PRIMARY -> R.string.tracker_error_primary
    TrackerProblem.OPTIONS -> R.string.tracker_error_options
    TrackerProblem.DATE -> R.string.tracker_error_date
    TrackerProblem.REQUIRED -> R.string.tracker_error_required
    TrackerProblem.ANSWER -> R.string.tracker_error_answer
    TrackerProblem.INACTIVE -> R.string.tracker_error_inactive
    TrackerProblem.DUPLICATE -> R.string.tracker_error_duplicate
    TrackerProblem.HABIT -> R.string.tracker_error_habit
}

@Composable
fun TrackerError(storage: Boolean, problem: TrackerProblem?, question: String?, retry: () -> Unit) {
    if (!storage && problem == null) return
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(if (storage) R.string.tracker_storage_error else trackerProblemText(problem!!, question)),
                color = MaterialTheme.colorScheme.error)
            if (storage) TextButton(retry) { Text(stringResource(R.string.retry)) }
        }
    }
}

/** Re-derives the date-dependent pending state while visible. */
@Composable
fun TrackerTicker(vm: TrackerViewModel) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { vm.refresh(); delay(60_000) } }
    }
}

@Composable
fun TrackerDialogs(state: TrackerUiState, vm: TrackerViewModel, requestPermission: () -> Unit) {
    val hub = state.hubHabitId
    if (hub != null && state.form == null && state.entry == null) TrackerHubDialog(state, vm)
    state.form?.let { draft ->
        AlertDialog(onDismissRequest = {}, title = { Text(stringResource(R.string.tracker_form_title)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                TrackerError(state.storageError, state.problem, state.problemQuestion, vm::retry)
                TrackerFormContent(draft, !state.busy, vm::form, vm::addQuestion, vm::newOption)
            } },
            confirmButton = { TextButton({ vm.saveForm(requestPermission) }, enabled = !state.busy) {
                Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton({ vm.form(null) }, enabled = !state.busy) {
                Text(stringResource(R.string.cancel)) } })
    }
    state.entry?.let { draft ->
        val version = state.snapshot.version(draft.trackerId, draft.version)
        val habit = state.snapshot.tracker(draft.trackerId)?.habitId
        val name = state.habits.habits.firstOrNull { it.id == habit }?.name.orEmpty()
        val old = state.snapshot.entries.firstOrNull { it.id == draft.entryId }
        val editable = state.snapshot.tracker(draft.trackerId)?.let(state::editable) == true && !state.busy
        AlertDialog(onDismissRequest = {}, title = { Text(stringResource(R.string.tracker_entry_title,
            formatDay(draft.date))) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                TrackerError(state.storageError, state.problem, state.problemQuestion, vm::retry)
                old?.let { Text(stringResource(R.string.tracker_filled_at, formatMoment(it.filledAt)),
                    style = MaterialTheme.typography.bodySmall) }
                if (version != null) {
                    if (draft.version != state.snapshot.tracker(draft.trackerId)?.version)
                        Text(stringResource(R.string.tracker_old_version, draft.version),
                            style = MaterialTheme.typography.bodySmall)
                    TrackerEntryContent(version, draft, editable, state.problemQuestion, vm::answer)
                }
            } },
            confirmButton = { TextButton(vm::saveEntry, enabled = editable && version != null) {
                Text(stringResource(R.string.save)) } },
            dismissButton = { Row {
                if (draft.editing && editable) TextButton({ vm.requestDelete(draft.entryId) }) {
                    Text(stringResource(R.string.delete)) }
                TextButton({ vm.entry(null) }, enabled = !state.busy) { Text(stringResource(R.string.cancel)) }
            } })
    }
    state.deleteEntryId?.let {
        AlertDialog(onDismissRequest = { if (!state.busy) vm.requestDelete(null) },
            text = { Column {
                TrackerError(state.storageError, state.problem, state.problemQuestion, vm::retry)
                Text(stringResource(R.string.tracker_delete_confirm))
            } },
            confirmButton = { TextButton(vm::confirmDelete, enabled = !state.busy) {
                Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton({ vm.requestDelete(null) }, enabled = !state.busy) {
                Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun TrackerHubDialog(state: TrackerUiState, vm: TrackerViewModel) {
    val habitId = state.hubHabitId ?: return
    val t = state.hubTracker
    AlertDialog(onDismissRequest = { if (!state.busy) vm.open(null) },
        title = { Text(stringResource(R.string.tracker) + " · " + state.hubHabitName) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TrackerError(state.storageError, state.problem, state.problemQuestion, vm::retry)
            if (state.loading) Text(stringResource(R.string.tracker_loading))
            Text(stringResource(R.string.tracker_zero_effect), style = MaterialTheme.typography.bodySmall)
            val habitOpen = state.habits.habits.firstOrNull { it.id == habitId }?.status !=
                HabitStatus.EXCLUDED
            if (t == null) {
                Text(stringResource(R.string.tracker_none))
                if (habitOpen) Button({ vm.editForm(habitId) }, enabled = !state.busy && !state.loading) {
                    Text(stringResource(R.string.tracker_create)) }
                return@Column
            }
            if (!t.active) Text(stringResource(R.string.tracker_archived))
            else if (!habitOpen) Text(stringResource(R.string.tracker_readonly_habit))
            if (state.pending.any { it.id == t.id }) Text(stringResource(R.string.tracker_pending_today),
                color = MaterialTheme.colorScheme.primary)
            if (state.editable(t)) {
                Button({ vm.fill(t.id, state.today) }, enabled = !state.busy) {
                    Text(stringResource(R.string.tracker_fill_today)) }
                OutlinedTextField(state.dateText, vm::dateText, Modifier.fillMaxWidth(), enabled = !state.busy,
                    label = { Text(stringResource(R.string.tracker_date)) }, singleLine = true)
                Row {
                    TextButton({ vm.dateText(state.today.minusDays(1).toString()) }, enabled = !state.busy) {
                        Text(stringResource(R.string.tracker_yesterday)) }
                    TextButton({ vm.fillTyped(t.id) }, enabled = !state.busy) {
                        Text(stringResource(R.string.tracker_fill_date)) }
                }
                TextButton({ vm.editForm(habitId) }, enabled = !state.busy) {
                    Text(stringResource(R.string.tracker_edit_form)) }
            }
            state.analytics?.let { TrackerAnalyticsContent(it, state.period, vm::period) }
            Text(stringResource(R.string.tracker_entries), style = MaterialTheme.typography.titleSmall)
            val entries = state.snapshot.entries(t.id).sortedWith(compareByDescending<TrackerEntry> { it.date }
                .thenByDescending { it.filledAt })
            if (entries.isEmpty()) Text(stringResource(R.string.tracker_no_entries))
            val primary = state.snapshot.current(t.id)?.questions?.firstOrNull { it.primary }
            entries.forEach { e ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(stringResource(R.string.tracker_entry_line, formatDay(e.date), formatMoment(e.filledAt)))
                        primary?.let { p -> e.answers.firstOrNull { it.questionId == p.id }?.scale?.let {
                            Text("${p.title}: $it", style = MaterialTheme.typography.bodySmall) } }
                        Row {
                            TextButton({ vm.editEntry(e.id) }, enabled = !state.busy) {
                                Text(stringResource(if (state.editable(t)) R.string.edit else R.string.reminder_open)) }
                            if (state.editable(t)) TextButton({ vm.requestDelete(e.id) }, enabled = !state.busy) {
                                Text(stringResource(R.string.delete)) }
                        }
                    }
                }
            }
            if (habitOpen) TextButton({ vm.setActive(t.id, !t.active) }, enabled = !state.busy) {
                Text(stringResource(if (t.active) R.string.tracker_archive else R.string.tracker_restore)) }
        } },
        confirmButton = { TextButton({ vm.open(null) }, enabled = !state.busy) { Text(stringResource(R.string.close)) } })
}


@Composable
fun TrackerFormContent(
    draft: TrackerFormDraft, enabled: Boolean, change: (TrackerFormDraft) -> Unit,
    add: (QuestionType, Boolean) -> Unit, newOption: () -> TrackerOption,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.tracker_versions_hint), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.tracker_mode), style = MaterialTheme.typography.titleSmall)
        Row {
            listOf(false to R.string.tracker_mode_single, true to R.string.tracker_mode_multiple).forEach { (m, label) ->
                FilterChip(draft.multiple == m, { change(draft.copy(multiple = m)) }, { Text(stringResource(label)) },
                    Modifier.padding(end = 8.dp), enabled = enabled)
            }
        }
        fun update(i: Int, q: TrackerQuestion) = change(draft.copy(questions = draft.questions.toMutableList()
            .also { it[i] = q }))
        ReorderableColumn(draft.questions, { it.id }, enabled,
            onReorder = { change(draft.copy(questions = it)) }) { q, questionHandle ->
            val i = draft.questions.indexOfFirst { it.id == q.id }
            val type = stringResource(when {
                q.type == QuestionType.SCALE -> R.string.tracker_type_scale
                q.type == QuestionType.TEXT -> R.string.tracker_type_text
                q.multiple -> R.string.tracker_type_multiple
                else -> R.string.tracker_type_single
            })
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DragHandle(questionHandle)
                        Text(stringResource(R.string.tracker_question_number, i + 1, type),
                            style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedTextField(q.title, { update(i, q.copy(title = it)) }, Modifier.fillMaxWidth(),
                        enabled = enabled, label = { Text(stringResource(R.string.tracker_question_title)) })
                    LabeledSwitch(R.string.tracker_required, q.required, enabled) { update(i, q.copy(required = it)) }
                    when (q.type) {
                        QuestionType.SCALE -> {
                            LabeledSwitch(R.string.tracker_primary, q.primary, enabled) { primary ->
                                change(draft.copy(questions = draft.questions.mapIndexed { j, other ->
                                    if (j == i) other.copy(primary = primary) else if (primary)
                                        other.copy(primary = false) else other }))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberField(R.string.tracker_scale_min, q.min, enabled, Modifier.weight(1f)) {
                                    update(i, q.copy(min = it)) }
                                NumberField(R.string.tracker_scale_max, q.max, enabled, Modifier.weight(1f)) {
                                    update(i, q.copy(max = it)) }
                            }
                            OutlinedTextField(q.minLabel, { update(i, q.copy(minLabel = it)) }, Modifier.fillMaxWidth(),
                                enabled = enabled, label = { Text(stringResource(R.string.tracker_scale_min_label)) })
                            OutlinedTextField(q.maxLabel, { update(i, q.copy(maxLabel = it)) }, Modifier.fillMaxWidth(),
                                enabled = enabled, label = { Text(stringResource(R.string.tracker_scale_max_label)) })
                        }
                        QuestionType.CHOICE -> {
                            LabeledSwitch(R.string.tracker_answer_multiple, q.multiple, enabled) {
                                update(i, q.copy(multiple = it)) }
                            ReorderableColumn(q.options, { it.id }, enabled,
                                onReorder = { update(i, q.copy(options = it)) }) { o, optionHandle ->
                                val k = q.options.indexOfFirst { it.id == o.id }
                                fun updateOption(value: TrackerOption) = update(i, q.copy(options =
                                    q.options.toMutableList().also { it[k] = value }))
                                OutlinedTextField(o.label, { updateOption(o.copy(label = it)) }, Modifier.fillMaxWidth(),
                                    enabled = enabled, label = { Text(stringResource(R.string.tracker_option)) })
                                OutlinedTextField(o.description, { updateOption(o.copy(description = it)) },
                                    Modifier.fillMaxWidth(), enabled = enabled,
                                    label = { Text(stringResource(R.string.tracker_option_description)) })
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DragHandle(optionHandle)
                                    TextButton({ update(i, q.copy(options = q.options - o)) }, enabled = enabled) {
                                        Text(stringResource(R.string.tracker_remove_option)) }
                                }
                            }
                            TextButton({ update(i, q.copy(options = q.options + newOption())) }, enabled = enabled) {
                                Text(stringResource(R.string.tracker_add_option)) }
                        }
                        QuestionType.TEXT -> Unit
                    }
                    TextButton({ change(draft.copy(questions = draft.questions - q)) }, enabled = enabled) {
                        Text(stringResource(R.string.tracker_remove_question)) }
                }
            }
        }
        listOf(Triple(QuestionType.SCALE, false, R.string.tracker_add_scale),
            Triple(QuestionType.CHOICE, false, R.string.tracker_add_single),
            Triple(QuestionType.CHOICE, true, R.string.tracker_add_multiple),
            Triple(QuestionType.TEXT, false, R.string.tracker_add_text)).forEach { (type, multiple, label) ->
            OutlinedButton({ add(type, multiple) }, Modifier.fillMaxWidth(), enabled = enabled) {
                Text(stringResource(label)) }
        }
        OutlinedTextField(draft.reminder, { change(draft.copy(reminder = it)) }, Modifier.fillMaxWidth(),
            enabled = enabled, singleLine = true, label = { Text(stringResource(R.string.tracker_reminder)) })
        Text(stringResource(R.string.tracker_reminder_hint), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LabeledSwitch(label: Int, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, enabled, Role.Switch, onValueChange = change).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), Modifier.weight(1f))
        Switch(checked, null, enabled = enabled)
    }
}

@Composable
private fun NumberField(label: Int, value: Int, enabled: Boolean, modifier: Modifier, change: (Int) -> Unit) {
    OutlinedTextField(value.toString(), { text -> text.trim().toIntOrNull()?.let(change) }, modifier,
        enabled = enabled, singleLine = true, label = { Text(stringResource(label)) })
}

@Composable
fun TrackerEntryContent(
    version: TrackerVersion, draft: TrackerEntryDraft, enabled: Boolean, problemQuestion: String?,
    answer: (TrackerAnswer) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        version.questions.forEach { q ->
            val current = draft.answer(q.id)
            val title = if (q.required) stringResource(R.string.tracker_required_mark, q.title) else q.title
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = if (problemQuestion == q.id) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            when (q.type) {
                QuestionType.SCALE -> {
                    if (q.minLabel.isNotBlank() || q.maxLabel.isNotBlank()) Text(stringResource(
                        R.string.tracker_scale_extremes, q.min, q.minLabel, q.max, q.maxLabel),
                        style = MaterialTheme.typography.bodySmall)
                    // Discrete buttons work with TalkBack and large fonts, unlike drag-only sliders.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val values = if (q.max - q.min <= 20) (q.min..q.max).toList() else listOfNotNull(
                            current?.scale?.minus(1)?.coerceAtLeast(q.min), current?.scale ?: q.min,
                            current?.scale?.plus(1)?.coerceAtMost(q.max)).distinct()
                        values.forEach { v ->
                            val description = stringResource(R.string.tracker_scale_value, v, q.min, q.max)
                            FilterChip(current?.scale == v, { answer(TrackerAnswer(q.id, scale = v)) },
                                { Text(v.toString()) }, Modifier.semantics { contentDescription = description },
                                enabled = enabled)
                        }
                    }
                    if (q.max - q.min > 20) NumberField(R.string.tracker_question_title, current?.scale ?: q.min,
                        enabled, Modifier.fillMaxWidth()) { answer(TrackerAnswer(q.id, scale = it)) }
                }
                QuestionType.CHOICE -> q.options.forEach { o ->
                    val selected = current?.choices?.contains(o.id) == true
                    Row(Modifier.fillMaxWidth().selectable(selected = selected, enabled = enabled,
                        role = if (q.multiple) Role.Checkbox else Role.RadioButton) {
                        val choices = if (q.multiple) {
                            if (selected) current!!.choices - o.id else current?.choices.orEmpty() + o.id
                        } else listOf(o.id)
                        answer(TrackerAnswer(q.id, choices = choices))
                    }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (q.multiple) Checkbox(selected, null, enabled = enabled)
                        else RadioButton(selected, null, enabled = enabled)
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(o.label)
                            if (o.description.isNotBlank()) Text(o.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                QuestionType.TEXT -> OutlinedTextField(current?.text.orEmpty(), {
                    answer(TrackerAnswer(q.id, text = it)) }, Modifier.fillMaxWidth(), enabled = enabled,
                    label = { Text(q.title) })
            }
            if (current != null && !current.empty && !q.required) TextButton({ answer(TrackerAnswer(q.id)) },
                enabled = enabled) { Text(stringResource(R.string.tracker_clear)) }
        }
    }
}

@Composable
fun TrackerAnalyticsContent(a: TrackerAnalytics, period: Int, choose: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.tracker_analysis), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TrackerRules.PERIODS.forEach { d ->
                FilterChip(period == d, { choose(d) }, { Text(pluralStringResource(R.plurals.tracker_period, d, d)) })
            }
        }
        if (a.primaryTitle == null) Text(stringResource(R.string.tracker_no_primary))
        else {
            TrackerChart(a)
            if (a.rangesDiffer) Text(stringResource(R.string.tracker_ranges_differ),
                color = MaterialTheme.colorScheme.tertiary)
            a.points.forEach { p -> Text(stringResource(R.string.tracker_point, formatDay(p.date),
                formatMoment(p.filledAt).takeLast(5), p.value, p.min, p.max), style = MaterialTheme.typography.bodySmall) }
        }
        Text(stringResource(R.string.tracker_missing_days, a.missingDays.size), style = MaterialTheme.typography.bodySmall)
        if (a.options.isNotEmpty()) {
            Text(stringResource(R.string.tracker_options_frequency), style = MaterialTheme.typography.titleSmall)
            a.options.groupBy { it.questionId }.values.forEach { group ->
                Text(group.first().question, style = MaterialTheme.typography.labelLarge)
                group.forEach { Text(stringResource(R.string.tracker_option_count, it.label, it.count)) }
            }
        }
        Text(stringResource(R.string.tracker_analysis_note), style = MaterialTheme.typography.bodySmall)
    }
}

/** One column per civil day; each entry is its own dot at its own range (no sums, averages or rescaled zeros). */
@Composable
fun TrackerChart(a: TrackerAnalytics) {
    val description = stringResource(R.string.tracker_chart_description, a.primaryTitle.orEmpty(), a.points.size,
        a.missingDays.size)
    val color = MaterialTheme.colorScheme.primary
    val gap = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(120.dp).semantics { contentDescription = description }) {
        val columns = a.days.coerceAtLeast(1)
        val step = size.width / columns
        for (i in 0 until columns) {
            val day = a.from.plusDays(i.toLong())
            val x = step * i + step / 2
            if (day in a.missingDays) {
                drawLine(gap, Offset(x, size.height - 2), Offset(x, size.height - 8), strokeWidth = 2f)
                continue
            }
            a.points.filter { it.date == day }.forEach { p ->
                val ratio = (p.value - p.min).toFloat() / (p.max - p.min).coerceAtLeast(1)
                drawCircle(color, radius = (step / 3).coerceIn(2f, 8f),
                    center = Offset(x, size.height - 10 - ratio * (size.height - 20)))
            }
        }
    }
}

@Composable
fun TrackerPendingCard(count: Int, open: () -> Unit) {
    if (count == 0) return
    ActionBanner(pluralStringResource(R.plurals.tracker_pending_count, count, count),
        "✎", open, outlined = true)
}

@Preview
@Composable
private fun TrackerEntryPreview() {
    val version = TrackerVersion("t", 1, 0, listOf(
        TrackerQuestion("u", QuestionType.SCALE, "Disposição para praticar", required = true, primary = true,
            minLabel = "Nenhuma", maxLabel = "Muita"),
        TrackerQuestion("c", QuestionType.CHOICE, "Como estava sua rotina?", options = listOf(
            TrackerOption("a", "Tranquila"), TrackerOption("b", "Corrida", "Pouco tempo livre"))),
        TrackerQuestion("n", QuestionType.TEXT, "Observações")))
    EthosTheme {
        Surface { TrackerEntryContent(version, TrackerEntryDraft("e", "t", LocalDate.of(2026, 11, 7), 1,
            listOf(TrackerAnswer("u", scale = 6))), true, null) {} }
    }
}

@Preview
@Composable
private fun TrackerChartPreview() {
    val today = LocalDate.of(2026, 11, 7)
    EthosTheme {
        Surface { TrackerAnalyticsContent(TrackerAnalytics(7, today.minusDays(6), today, "Disposição",
            listOf(TrackerPoint("a", today.minusDays(5), 0, 4, 0, 10), TrackerPoint("b", today, 0, 7, 0, 10)),
            (1..4).map { today.minusDays(it.toLong()) } + today.minusDays(6), false,
            listOf(OptionCount("c", "Rotina", "a", "Tranquila", 2))), 7) {} }
    }
}
