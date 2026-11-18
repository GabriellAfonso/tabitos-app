package com.ethos.app.features.trackers

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.habits.domain.Habit
import com.ethos.app.core.icons.AppIconTables
import com.ethos.app.core.trackers.domain.QuestionType
import com.ethos.app.core.trackers.domain.TrackerAnalytics
import com.ethos.app.core.trackers.domain.TrackerQuestion
import com.ethos.app.core.trackers.domain.TrackerRules
import com.ethos.app.core.ui.components.EthosDatePickerDialog
import com.ethos.app.core.ui.components.SegmentedControl
import com.ethos.app.core.ui.components.itemColors
import com.ethos.app.core.ui.components.option
import com.ethos.app.core.ui.format.EthosDates
import com.ethos.app.core.ui.format.label
import com.ethos.app.core.ui.theme.EthosTheme
import java.time.LocalDate

enum class TrackerTab(@StringRes val title: Int) {
    RECORDS(R.string.tracker_entries), EVOLUTION(R.string.tracker_tab_evolution),
    QUESTIONS(R.string.tracker_tab_questions),
}

/** Callbacks of the tracker page; the ViewModel owns every rule behind them. */
data class TrackerPageActions(
    val close: () -> Unit,
    val create: () -> Unit,
    val editQuestions: () -> Unit,
    val fill: (LocalDate) -> Unit,
    val openEntry: (String) -> Unit,
    val showMore: () -> Unit,
    val period: (Int) -> Unit,
    val setActive: (Boolean) -> Unit,
    val retry: () -> Unit,
)

/** Everything the page draws, already derived by the ViewModel (spec 034). */
data class TrackerPageModel(
    val habit: Habit?,
    val today: LocalDate,
    val created: LocalDate?,
    val canCreate: Boolean,
    val canArchive: Boolean,
    val active: Boolean,
    val summary: TodaySummary?,
    val records: RecordList?,
    val analytics: TrackerAnalytics?,
    val frequency: List<FrequencyGroup>,
    val period: Int,
    val questions: List<TrackerQuestion>,
    val version: Int,
)

@Composable
fun TrackerPage(model: TrackerPageModel, state: TrackerUiState, actions: TrackerPageActions) {
    var menu by rememberSaveable { mutableStateOf(false) }
    TrackerFullScreen(
        title = stringResource(R.string.tracker), subtitle = model.habit?.name.orEmpty(), busy = state.busy,
        onClose = actions.close, closeIcon = R.drawable.ic_arrow_back, closeLabel = R.string.back,
        actions = {
            if (model.canArchive) Box {
                val more = stringResource(R.string.more_options)
                IconButton({ menu = true }, enabled = !state.busy,
                    modifier = Modifier.semantics { contentDescription = more }) {
                    Icon(painterResource(R.drawable.ic_more_vert), contentDescription = null)
                }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(
                        if (model.active) R.string.tracker_archive else R.string.tracker_restore)) },
                        onClick = { menu = false; actions.setActive(!model.active) })
                }
            }
        },
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            TrackerError(state.storageError, state.problem, state.problemQuestion, actions.retry)
        }
        when {
            state.loading -> Text(stringResource(R.string.tracker_loading), Modifier.padding(16.dp))
            model.summary == null -> TrackerEmptyState(model, !state.busy, actions.create)
            else -> TrackerTabs(model, model.summary, !state.busy, actions)
        }
    }
}

@Composable
private fun TrackerEmptyState(model: TrackerPageModel, enabled: Boolean, create: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
        model.habit?.let { HabitTile(it, 64) }
        Text(stringResource(R.string.tracker_empty_title), style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center)
        Text(stringResource(R.string.tracker_none), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (model.canCreate) Button(create, enabled = enabled) { Text(stringResource(R.string.tracker_create)) }
        Text(stringResource(R.string.tracker_zero_effect), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HabitTile(habit: Habit, size: Int) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape((size / 3).dp)).background(itemColors[habit.color]),
        contentAlignment = Alignment.Center) {
        Icon(painterResource(AppIconTables.habit[habit.icon].option.icon), contentDescription = null,
            Modifier.size((size / 2).dp), tint = Color.White)
    }
}

@Composable
private fun ColumnScope.TrackerTabs(
    model: TrackerPageModel, summary: TodaySummary, enabled: Boolean, actions: TrackerPageActions,
) {
    var tab by rememberSaveable { mutableStateOf(TrackerTab.RECORDS) }
    Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
        TodayStrip(summary, model.today, enabled, actions)
    }
    PrimaryTabRow(selectedTabIndex = TrackerTab.entries.indexOf(tab)) {
        TrackerTab.entries.forEach { entry ->
            Tab(selected = tab == entry, onClick = { tab = entry }, text = { Text(stringResource(entry.title)) })
        }
    }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (tab) {
            TrackerTab.RECORDS -> RecordsTab(model, enabled, actions)
            TrackerTab.EVOLUTION -> EvolutionTab(model, actions.period)
            TrackerTab.QUESTIONS -> QuestionsTab(model, enabled, actions.editQuestions)
        }
    }
}

@Composable
private fun TodayStrip(summary: TodaySummary, today: LocalDate, enabled: Boolean, actions: TrackerPageActions) {
    val readOnly = when (summary.kind) {
        TodayKind.ARCHIVED -> R.string.tracker_archived
        TodayKind.HABIT_EXCLUDED -> R.string.tracker_readonly_habit
        TodayKind.EMPTY, TodayKind.FILLED -> null
    }
    val filled = summary.kind == TodayKind.FILLED
    Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
        color = if (filled || readOnly != null) MaterialTheme.colorScheme.surfaceContainer
            else MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            summary.primary?.let { ValuePill(it.toString()) }
            Column(Modifier.weight(1f)) {
                Text(EthosDates.longPlain(today, today).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall)
                Text(readOnly?.let { stringResource(it) } ?: todayText(summary),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (readOnly != null) return@Row
            if (summary.opensExisting) TextButton({ summary.latestEntryId?.let(actions.openEntry) },
                enabled = enabled) { Text(stringResource(R.string.reminder_open)) }
            else Button({ actions.fill(today) }, enabled = enabled) { Text(stringResource(R.string.tracker_fill)) }
        }
    }
}

@Composable
private fun todayText(summary: TodaySummary): String = when {
    summary.kind == TodayKind.EMPTY -> stringResource(R.string.tracker_today_empty)
    summary.allowsAnother -> pluralStringResource(R.plurals.tracker_today_entries, summary.entries, summary.entries)
    else -> stringResource(R.string.tracker_today_filled)
}

@Composable
private fun ValuePill(text: String, soft: Boolean = false, dashed: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        dashed -> Color.Transparent
        soft -> colors.secondaryContainer
        else -> colors.primary
    }
    val content = when {
        dashed -> colors.onSurfaceVariant
        soft -> colors.onSecondaryContainer
        else -> colors.onPrimary
    }
    Surface(shape = CircleShape, color = background, contentColor = content,
        border = if (dashed) BorderStroke(1.dp, colors.outline) else null) {
        Box(Modifier.sizeIn(minWidth = 32.dp, minHeight = 28.dp).padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun RecordsTab(model: TrackerPageModel, enabled: Boolean, actions: TrackerPageActions) {
    val records = model.records ?: return
    var picking by rememberSaveable { mutableStateOf(false) }
    records.weeks.forEach { week ->
        Text(weekTitleText(weekTitle(week.start, model.today), model.today).uppercase(),
            Modifier.padding(top = 6.dp).semantics { heading() }, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        week.days.forEach { day -> RecordDayRows(day, model.today, enabled, actions) }
    }
    if (records.hasMore) TextButton(actions.showMore, enabled = enabled) {
        Text(stringResource(R.string.tracker_show_more))
    }
    val created = model.created
    if (model.active && model.canArchive && created != null) {
        TextButton({ picking = true }, enabled = enabled) {
            Icon(painterResource(R.drawable.ic_calendar_month), contentDescription = null, Modifier.size(18.dp))
            Text(stringResource(R.string.tracker_fill_other_day), Modifier.padding(start = 8.dp))
        }
        if (picking) EthosDatePickerDialog(initial = model.today, min = created, max = model.today,
            onConfirm = { picking = false; actions.fill(it) }, onDismiss = { picking = false })
    }
}

@Composable
private fun weekTitleText(title: WeekTitle, today: LocalDate): String = when (title) {
    WeekTitle.Current -> stringResource(R.string.tracker_week_current)
    WeekTitle.Previous -> stringResource(R.string.tracker_week_previous)
    is WeekTitle.Of -> stringResource(R.string.tracker_week_of, EthosDates.shortPlain(title.start, today))
}

@Composable
private fun RecordDayRows(day: RecordDay, today: LocalDate, enabled: Boolean, actions: TrackerPageActions) {
    val dayText = recordDayLabel(day.date, today).label()
    if (day.entries.isEmpty()) {
        val detail = stringResource(if (day.fillable) R.string.tracker_no_entry_fill else R.string.tracker_no_entry)
        RecordRow(dayText, detail, enabled && day.fillable, onClick = { actions.fill(day.date) }) {
            ValuePill("–", dashed = true)
        }
        return
    }
    day.entries.forEach { entry ->
        val detail = entry.choices.joinToString(" · ").ifEmpty { stringResource(R.string.tracker_no_marks) }
        RecordRow(dayText, detail, enabled, onClick = { actions.openEntry(entry.id) }) {
            ValuePill(entry.primary?.toString() ?: "✓")
        }
    }
}

@Composable
private fun RecordRow(
    title: String, detail: String, enabled: Boolean, onClick: (() -> Unit)?, leading: @Composable () -> Unit,
) {
    val click = onClick?.let { Modifier.clickable(enabled = enabled, role = Role.Button, onClick = it) } ?: Modifier
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).then(click)
        .heightIn(min = 52.dp).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.widthIn(min = 40.dp), contentAlignment = Alignment.Center) { leading() }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EvolutionTab(model: TrackerPageModel, choose: (Int) -> Unit) {
    val analytics = model.analytics ?: return
    SegmentedControl(TrackerRules.PERIODS, model.period,
        label = { pluralStringResource(R.plurals.tracker_period, it, it) }, onSelect = choose)
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (analytics.primaryTitle == null) {
                Text(stringResource(R.string.tracker_no_primary), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(analytics.primaryTitle, style = MaterialTheme.typography.titleSmall)
                TrackerChart(analytics)
                if (analytics.rangesDiffer) Text(stringResource(R.string.tracker_ranges_differ),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            Text(stringResource(R.string.tracker_period_summary, analytics.points.size, analytics.missingDays.size),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    model.frequency.forEach { group -> FrequencyCard(group) }
    Text(stringResource(R.string.tracker_analysis_note), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FrequencyCard(group: FrequencyGroup) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(group.question, style = MaterialTheme.typography.titleSmall)
            group.bars.forEach { bar ->
                val description = stringResource(R.string.tracker_option_count, bar.label, bar.count)
                Row(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(bar.label, Modifier.weight(0.4f), style = MaterialTheme.typography.bodySmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.weight(0.6f).height(8.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(bar.fraction).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary))
                    }
                    Text(bar.count.toString(), Modifier.widthIn(min = 20.dp), style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun QuestionsTab(model: TrackerPageModel, enabled: Boolean, edit: () -> Unit) {
    model.questions.forEachIndexed { index, q ->
        RecordRow(q.title, questionSummary(q), enabled = false, onClick = null) {
            ValuePill((index + 1).toString(), soft = true)
        }
    }
    if (model.active && model.canArchive) FilledTonalButton(edit, enabled = enabled) {
        Icon(painterResource(R.drawable.ic_edit), contentDescription = null, Modifier.size(18.dp))
        Text(stringResource(R.string.tracker_edit_form), Modifier.padding(start = 8.dp))
    }
    Text(stringResource(R.string.tracker_version_note, model.version), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun questionSummary(q: TrackerQuestion): String {
    val parts = buildList {
        add(stringResource(q.kind.label))
        if (q.kind == QuestionKind.SCALE) add(stringResource(R.string.tracker_range, q.min, q.max))
        if (q.required) add(stringResource(R.string.tracker_marker_required))
        if (q.primary) add(stringResource(R.string.tracker_marker_chart))
    }
    return parts.joinToString(" · ")
}

@get:StringRes
internal val QuestionKind.label: Int get() = when (this) {
    QuestionKind.SCALE -> R.string.tracker_type_scale
    QuestionKind.SINGLE -> R.string.tracker_type_single
    QuestionKind.MULTIPLE -> R.string.tracker_type_multiple
    QuestionKind.TEXT -> R.string.tracker_type_text
}

@Preview
@Composable
private fun TrackerPagePreview() {
    val today = LocalDate.of(2026, 11, 18)
    val q = TrackerQuestion("u", QuestionType.SCALE, "Como está sua energia?",
        required = true, primary = true)
    val records = RecordList(listOf(RecordWeek(weekStart(today), listOf(
        RecordDay(today, emptyList(), true),
        RecordDay(today.minusDays(1), listOf(RecordEntry("e", 6, listOf("Trabalho"))), true)))), hasMore = true)
    val model = TrackerPageModel(Habit("h", today.minusDays(30), "Meditar"), today, today.minusDays(30),
        canCreate = true, canArchive = true, active = true, summary = TodaySummary(TodayKind.EMPTY), records = records,
        analytics = null, frequency = emptyList(), period = 7, questions = listOf(q), version = 1)
    EthosTheme {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TodayStrip(TodaySummary(TodayKind.EMPTY), today, true, previewActions)
                RecordsTab(model, true, previewActions)
            }
        }
    }
}

private val previewActions = TrackerPageActions({}, {}, {}, {}, {}, {}, {}, {}, {})
