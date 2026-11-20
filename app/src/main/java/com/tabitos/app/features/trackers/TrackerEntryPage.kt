package com.tabitos.app.features.trackers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tabitos.app.R
import com.tabitos.app.core.trackers.domain.QuestionType
import com.tabitos.app.core.trackers.domain.TrackerAnswer
import com.tabitos.app.core.trackers.domain.TrackerEntry
import com.tabitos.app.core.trackers.domain.TrackerOption
import com.tabitos.app.core.trackers.domain.TrackerQuestion
import com.tabitos.app.core.trackers.domain.TrackerVersion
import com.tabitos.app.core.ui.format.label
import com.tabitos.app.core.ui.theme.TabitosTheme
import java.time.LocalDate

/** Callbacks of the entry page. */
data class TrackerEntryActions(
    val answer: (TrackerAnswer) -> Unit,
    val save: () -> Unit,
    val cancel: () -> Unit,
    val delete: () -> Unit,
    val retry: () -> Unit,
)

/** Full-screen entry (spec 034): one card per question, actions fixed at the bottom. */
@Composable
fun TrackerEntryPage(
    draft: TrackerEntryDraft, version: TrackerVersion?, habitName: String, saved: TrackerEntry?, editable: Boolean,
    currentVersion: Int?, state: TrackerUiState, actions: TrackerEntryActions,
) {
    TrackerFullScreen(
        title = recordDayLabel(draft.date, state.today).label(), subtitle = habitName, busy = state.busy,
        onClose = actions.cancel, closeIcon = R.drawable.ic_close, closeLabel = R.string.close,
        bottomBar = {
            if (draft.editing && editable) TextButton(actions.delete, enabled = !state.busy) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
            Box(Modifier.weight(1f))
            TextButton(actions.cancel, enabled = !state.busy) { Text(stringResource(R.string.cancel)) }
            Button(actions.save, enabled = editable && !state.busy && version != null) {
                Text(stringResource(R.string.save))
            }
        },
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TrackerError(state.storageError, state.problem, state.problemQuestion, actions.retry)
            saved?.let { Note(stringResource(R.string.tracker_filled_at, formatMoment(it.filledAt))) }
            if (version != null && draft.version != currentVersion) {
                Note(stringResource(R.string.tracker_old_version, draft.version))
            }
            version?.let { TrackerEntryContent(it, draft, editable && !state.busy, state.problemQuestion, actions.answer) }
            Text(stringResource(R.string.tracker_zero_effect), Modifier.fillMaxWidth().padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun TrackerEntryContent(
    version: TrackerVersion, draft: TrackerEntryDraft, enabled: Boolean, problemQuestion: String?,
    answer: (TrackerAnswer) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        version.questions.forEach { q ->
            QuestionAnswerCard(q, draft.answer(q.id), enabled, problemQuestion == q.id, answer)
        }
    }
}

@Composable
private fun QuestionAnswerCard(
    q: TrackerQuestion, current: TrackerAnswer?, enabled: Boolean, problem: Boolean, answer: (TrackerAnswer) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceContainerLow,
        border = BorderStroke(if (problem) 2.dp else 1.dp, if (problem) colors.error else colors.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val title = if (q.required) stringResource(R.string.tracker_required_mark, q.title) else q.title
                    Text(title, style = MaterialTheme.typography.titleSmall,
                        color = if (problem) colors.error else colors.onSurface)
                    if (q.kind == QuestionKind.MULTIPLE) Text(stringResource(R.string.tracker_choose_many),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                if (current != null && !current.empty && !q.required) TextButton({ answer(TrackerAnswer(q.id)) },
                    enabled = enabled) { Text(stringResource(R.string.tracker_clear)) }
            }
            when (q.type) {
                QuestionType.SCALE -> ScaleAnswer(q, current?.scale, enabled) { answer(TrackerAnswer(q.id, scale = it)) }
                QuestionType.CHOICE -> ChoiceAnswer(q, current?.choices.orEmpty(), enabled) {
                    answer(TrackerAnswer(q.id, choices = it))
                }
                QuestionType.TEXT -> OutlinedTextField(current?.text.orEmpty(), {
                    answer(TrackerAnswer(q.id, text = it)) }, Modifier.fillMaxWidth(), enabled = enabled, minLines = 3,
                    placeholder = { Text(stringResource(R.string.tracker_text_hint)) })
            }
        }
    }
}

@Composable
private fun ScaleAnswer(q: TrackerQuestion, value: Int?, enabled: Boolean, choose: (Int) -> Unit) {
    if (q.minLabel.isNotBlank() || q.maxLabel.isNotBlank()) Text(stringResource(
        R.string.tracker_scale_extremes, q.min, q.minLabel, q.max, q.maxLabel),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    // Discrete buttons work with TalkBack and large fonts, unlike drag-only sliders.
    when (scaleInput(q)) {
        ScaleInput.TRACK -> ScaleTrack(q, value, enabled, choose)
        ScaleInput.CHIPS -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (q.min..q.max).forEach { v ->
                val description = stringResource(R.string.tracker_scale_value, v, q.min, q.max)
                FilterChip(value == v, { choose(v) }, { Text(v.toString()) },
                    Modifier.semantics { contentDescription = description }, enabled = enabled)
            }
        }
        ScaleInput.NUMBER -> OutlinedTextField((value ?: q.min).toString(),
            { text -> text.trim().toIntOrNull()?.let(choose) }, Modifier.fillMaxWidth(), enabled = enabled,
            singleLine = true, label = { Text(stringResource(R.string.tracker_scale_range, q.min, q.max)) })
    }
}

@Composable
private fun ScaleTrack(q: TrackerQuestion, value: Int?, enabled: Boolean, choose: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainerHigh)
        .selectableGroup()) {
        (q.min..q.max).forEach { v ->
            val selected = value == v
            val description = stringResource(R.string.tracker_scale_value, v, q.min, q.max)
            val background = when {
                selected -> colors.primary
                value != null && v < value -> colors.secondaryContainer
                else -> colors.surfaceContainerHigh
            }
            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(background)
                .selectable(selected, enabled, Role.RadioButton) { choose(v) }
                .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
                Text(v.toString(), style = MaterialTheme.typography.labelLarge,
                    color = if (selected) colors.onPrimary else colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChoiceAnswer(q: TrackerQuestion, chosen: List<String>, enabled: Boolean, choose: (List<String>) -> Unit) {
    fun toggled(o: TrackerOption): List<String> = when {
        !q.multiple -> listOf(o.id)
        o.id in chosen -> chosen - o.id
        else -> chosen + o.id
    }
    if (choicesAsChips(q)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            q.options.forEach { o ->
                val selected = o.id in chosen
                FilterChip(selected, { choose(toggled(o)) }, { Text(o.label) }, enabled = enabled,
                    leadingIcon = if (selected) {
                        { Icon(painterResource(R.drawable.ic_check), contentDescription = null) }
                    } else null)
            }
        }
        return
    }
    q.options.forEach { o ->
        val selected = o.id in chosen
        Row(Modifier.fillMaxWidth().selectable(selected = selected, enabled = enabled,
            role = if (q.multiple) Role.Checkbox else Role.RadioButton) { choose(toggled(o)) }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (q.multiple) Checkbox(selected, null, enabled = enabled) else RadioButton(selected, null, enabled = enabled)
            Column(Modifier.padding(start = 8.dp)) {
                Text(o.label)
                if (o.description.isNotBlank()) Text(o.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview
@Composable
private fun TrackerEntryPreview() {
    val version = TrackerVersion("t", 1, 0, listOf(
        TrackerQuestion("u", QuestionType.SCALE, "Como está sua energia?", required = true, primary = true,
            minLabel = "Esgotado", maxLabel = "Muito disposto"),
        TrackerQuestion("c", QuestionType.CHOICE, "O que atrapalhou hoje?", multiple = true, options = listOf(
            TrackerOption("a", "Sono ruim"), TrackerOption("b", "Trabalho"), TrackerOption("d", "Barulho"))),
        TrackerQuestion("r", QuestionType.CHOICE, "Como estava sua rotina?", options = listOf(
            TrackerOption("a", "Tranquila"), TrackerOption("b", "Corrida", "Pouco tempo livre"))),
        TrackerQuestion("n", QuestionType.TEXT, "Observações")))
    TabitosTheme {
        Surface {
            Column(Modifier.padding(16.dp)) {
                TrackerEntryContent(version, TrackerEntryDraft("e", "t", LocalDate.of(2026, 11, 18), 1,
                    listOf(TrackerAnswer("u", scale = 6), TrackerAnswer("c", choices = listOf("b")))), true, null) {}
            }
        }
    }
}
