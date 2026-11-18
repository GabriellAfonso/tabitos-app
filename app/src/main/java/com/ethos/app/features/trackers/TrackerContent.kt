package com.ethos.app.features.trackers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.ethos.app.R
import com.ethos.app.core.trackers.domain.TrackerAnalytics
import com.ethos.app.core.trackers.domain.TrackerPoint
import com.ethos.app.core.trackers.domain.TrackerProblem
import com.ethos.app.core.ui.components.ActionBanner
import com.ethos.app.core.ui.theme.EthosTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val timeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
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

/** Tracker page, question editor, entry and delete confirmation; later windows stack above earlier ones. */
@Composable
fun TrackerDialogs(state: TrackerUiState, vm: TrackerViewModel, requestPermission: () -> Unit) {
    state.page?.let { model ->
        TrackerPage(model, state, TrackerPageActions(
            close = { vm.open(null) },
            create = { model.habit?.let { vm.editForm(it.id) } },
            editQuestions = { model.habit?.let { vm.editForm(it.id) } },
            fill = { date -> state.hubTracker?.let { vm.fill(it.id, date) } },
            openEntry = vm::editEntry,
            showMore = vm::showMoreRecords,
            period = vm::period,
            setActive = { active -> state.hubTracker?.let { vm.setActive(it.id, active) } },
            retry = vm::retry,
        ))
    }
    state.form?.let { draft ->
        TrackerFormPage(draft, state.habitName(draft.habitId), state, TrackerFormActions(
            change = vm::form,
            add = vm::addQuestion,
            newOption = vm::newOption,
            save = { vm.saveForm(requestPermission) },
            close = { vm.form(null) },
            retry = vm::retry,
        ))
    }
    state.entry?.let { draft ->
        val tracker = state.snapshot.tracker(draft.trackerId)
        TrackerEntryPage(
            draft = draft,
            version = state.snapshot.version(draft.trackerId, draft.version),
            habitName = state.habitName(tracker?.habitId),
            saved = state.snapshot.entries.firstOrNull { it.id == draft.entryId },
            editable = tracker?.let(state::editable) == true,
            currentVersion = tracker?.version,
            state = state,
            actions = TrackerEntryActions(
                answer = vm::answer,
                save = vm::saveEntry,
                cancel = { vm.entry(null) },
                delete = { vm.requestDelete(draft.entryId) },
                retry = vm::retry,
            ),
        )
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
        R.drawable.ic_edit, open, outlined = true)
}

@Preview
@Composable
private fun TrackerChartPreview() {
    val today = LocalDate.of(2026, 11, 7)
    EthosTheme {
        Surface {
            TrackerChart(TrackerAnalytics(7, today.minusDays(6), today, "Disposição",
                listOf(TrackerPoint("a", today.minusDays(5), 0, 4, 0, 10), TrackerPoint("b", today, 0, 7, 0, 10)),
                (1..4).map { today.minusDays(it.toLong()) } + today.minusDays(6), false, emptyList()))
        }
    }
}
