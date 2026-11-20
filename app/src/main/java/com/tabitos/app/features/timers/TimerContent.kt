package com.tabitos.app.features.timers

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tabitos.app.core.ui.components.ScrollableTimeField
import com.tabitos.app.R
import com.tabitos.app.core.timers.domain.*
import com.tabitos.app.core.ui.format.TabitosDates
import com.tabitos.app.core.ui.format.label
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private const val SECONDS_PER_HOUR = 3600

/** Hours appear only when there is at least one, so half an hour reads 30:00 instead of 0:30:00. */
fun clockText(millis: Long): String {
    val seconds = millis.coerceAtLeast(0) / 1000
    val minutesAndSeconds = "%02d:%02d".format((seconds / 60) % 60, seconds % 60)
    return if (seconds < SECONDS_PER_HOUR) minutesAndSeconds else "${seconds / SECONDS_PER_HOUR}:$minutesAndSeconds"
}

fun minutesLabel(millis: Long): String {
    val (h, m) = TimerRules.minutesText(millis)
    return if (h > 0) "$h h ${"%02d".format(m)} min" else "$m min"
}

private val hourMinuteFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pt-BR"))
private fun hourMinute(millis: Long, zone: ZoneId) = Instant.ofEpochMilli(millis).atZone(zone).format(hourMinuteFormat)

/** Ticks the display once per second only while the UI is visible. */
@Composable
fun TimerTicker(viewModel: TimerViewModel) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { viewModel.tick(); delay(1000) }
        }
    }
}

data class TimerCardActions(
    val open: (TimerSession) -> Unit = {}, val pause: (String) -> Unit = {}, val resume: (TimerSession) -> Unit = {},
    val discard: (String) -> Unit = {},
)

/** Active and paused sessions (filtered by the host to sources still open), running first. */
@Composable
fun TimerSessionCards(state: TimerUiState, sessions: List<TimerSession>, zone: ZoneId, actions: TimerCardActions) {
    sessions.sortedByDescending { it.state == TimerState.RUNNING }.forEach { session ->
        val total = TimerRules.sessionTotal(state.snapshot, session.id, state.now)
        val remaining = TimerRules.remaining(state.snapshot, session, state.now, zone)
        OutlinedCard(onClick = { actions.open(session) }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(when {
                        session.kind == TimerKind.REWARD -> R.string.timer_reward_session
                        session.state == TimerState.RUNNING -> R.string.timer_active
                        else -> R.string.timer_paused
                    }), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    if (TimerRules.discardable(state.snapshot, session) != null) {
                        TimerCardMenu(enabled = !state.busy) { actions.discard(session.id) }
                    }
                }
                Text(session.title, style = MaterialTheme.typography.titleMedium)
                Text(if (remaining != null) stringResource(R.string.timer_remaining_line, clockText(remaining))
                    else stringResource(R.string.timer_elapsed_line, clockText(total)))
                if (remaining == 0L) Text(stringResource(if (session.kind == TimerKind.REWARD)
                    R.string.timer_exhausted else R.string.timer_reached))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (session.state == TimerState.RUNNING) {
                        Button({ actions.pause(session.id) }, enabled = !state.busy) {
                            Text(stringResource(R.string.timer_pause)) }
                    } else if (remaining != 0L) {
                        Button({ actions.resume(session) }, enabled = !state.busy) {
                            Text(stringResource(R.string.timer_resume)) }
                    }
                    TextButton({ actions.open(session) }) { Text(stringResource(R.string.timer_open)) }
                }
            }
        }
    }
}

/** Kept behind ⋮ rather than beside Pausar, so discarding is never one more accidental tap (spec 018). */
@Composable
private fun TimerCardMenu(enabled: Boolean, discard: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.timer_more_actions)
    Box {
        IconButton({ expanded = true }, enabled = enabled,
            modifier = Modifier.semantics { contentDescription = description }) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.timer_discard)) },
                onClick = { expanded = false; discard() })
        }
    }
}

@Composable
private fun DiscardDialog(state: TimerUiState, discard: TimerDiscard, vm: TimerViewModel) {
    AlertDialog(onDismissRequest = { if (!state.busy) vm.cancelDiscard() },
        title = { Text(stringResource(R.string.timer_discard_title)) },
        text = { Text(stringResource(R.string.timer_discard_body, minutesLabel(discard.millis))) },
        confirmButton = { TextButton(vm::confirmDiscard, enabled = !state.busy) {
            Text(stringResource(R.string.timer_discard_confirm), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(vm::cancelDiscard, enabled = !state.busy) {
            Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun TimerError(state: TimerUiState, retry: () -> Unit) {
    state.problem?.let { problem ->
        Text(stringResource(when (problem) {
            TimerProblem.INVALID_INTERVAL -> R.string.timer_problem_interval
            TimerProblem.FUTURE -> R.string.timer_problem_future
            TimerProblem.TOO_LONG -> R.string.timer_problem_long
            TimerProblem.BEFORE_SOURCE -> R.string.timer_problem_before
            TimerProblem.INACTIVE -> R.string.timer_problem_inactive
            TimerProblem.OVERLAP -> R.string.timer_problem_overlap
            TimerProblem.NO_REMAINING -> R.string.timer_reached
        }), color = MaterialTheme.colorScheme.error)
    }
    if (state.error) {
        Text(stringResource(R.string.timer_error), color = MaterialTheme.colorScheme.error)
        TextButton(retry, enabled = !state.busy) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun ConflictDialog(state: TimerUiState, request: TimerStart, vm: TimerViewModel) {
    AlertDialog(onDismissRequest = { if (!state.busy) vm.cancelConflict() },
        title = { Text(stringResource(R.string.timer_conflict_title)) },
        text = { Text(stringResource(R.string.timer_conflict_body, state.snapshot.running?.title.orEmpty(),
            request.title)) },
        confirmButton = { TextButton({ vm.start(request, pauseOther = true) }, enabled = !state.busy) {
            Text(stringResource(R.string.timer_conflict_confirm)) } },
        dismissButton = { TextButton(vm::cancelConflict, enabled = !state.busy) {
            Text(stringResource(R.string.cancel)) } })
}

/** The sheet stays open underneath, so confirmations and the manual editor stack above it. */
@Composable
fun TimerDialogs(
    state: TimerUiState, vm: TimerViewModel, openSource: (TimerKind, String) -> Unit,
    requestPermission: () -> Unit,
) {
    state.target?.let { TimerSessionSheet(state, it, vm, openSource, requestPermission) }
    state.manual?.let { ManualEditor(state, it, vm) }
    state.conflict?.let { ConflictDialog(state, it, vm) }
    state.discard?.let { DiscardDialog(state, it, vm) }
    state.alarm?.let { TimerAlarmDialog(it.title, vm::stopAlarm) }
}

/** Any way of leaving the popup silences the alarm: it only exists to stop the sound (spec 026). */
@Composable
private fun TimerAlarmDialog(title: String, stop: () -> Unit) {
    AlertDialog(onDismissRequest = stop,
        icon = { Icon(painterResource(R.drawable.ic_alarm), contentDescription = null) },
        title = { Text(stringResource(R.string.timer_alarm_title)) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(stringResource(R.string.timer_alarm_body), textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            val description = stringResource(R.string.timer_alarm_stop_description, title)
            Button(stop, Modifier.fillMaxWidth().semantics { contentDescription = description }) {
                Text(stringResource(R.string.timer_alarm_stop))
            }
        })
}

/** "Abrir" on a session: countdown ring, one main control, recent segments; full list behind "Ver tudo". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerSessionSheet(
    state: TimerUiState, target: TimerTarget, vm: TimerViewModel, openSource: (TimerKind, String) -> Unit,
    requestPermission: () -> Unit,
) {
    val model = timerSheetModel(state, target, vm.zone)
    val busy by rememberUpdatedState(state.busy)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !busy })
    var showsAll by rememberSaveable(target.start.kind, target.start.sourceId) { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = { vm.open(null) }, sheetState = sheetState) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (showsAll) {
                AllSegments(model, state.busy, vm, back = { showsAll = false })
            } else {
                SessionOverview(model, state, vm, target.start, SessionLinks(
                    openSource = { vm.open(null); openSource(target.start.kind, target.start.sourceId) },
                    requestPermission = requestPermission, seeAll = { showsAll = true }))
            }
        }
    }
}

private data class SessionLinks(val openSource: () -> Unit, val requestPermission: () -> Unit, val seeAll: () -> Unit)

@Composable
private fun SessionOverview(
    model: TimerSheetModel, state: TimerUiState, vm: TimerViewModel, request: TimerStart, links: SessionLinks,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusChip(model.status)
        Spacer(Modifier.weight(1f))
        SessionMenu(model, state.busy, vm)
    }
    Column {
        Text(model.title, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(kindLabel(model.kind)), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    TimerError(state, vm::retry)
    SessionNotices(state, links.requestPermission)
    TimerRing(model)
    model.objective?.let { ObjectiveLine(it) }
    SessionOutcome(model)
    PrimaryControls(model, state.busy, vm, request)
    if (model.belowGoal) {
        Text(stringResource(R.string.timer_below_goal), color = MaterialTheme.colorScheme.error)
        TextButton(links.openSource) { Text(stringResource(R.string.timer_open_source)) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.timer_records), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        if (model.hasMore) TextButton(links.seeAll) { Text(stringResource(R.string.timer_see_all)) }
    }
    SegmentDays(model.recent, model.today, state.busy, vm)
}

private fun kindLabel(kind: TimerKind) = when (kind) {
    TimerKind.TASK -> R.string.timer_kind_task
    TimerKind.HABIT -> R.string.timer_kind_habit
    TimerKind.REWARD -> R.string.timer_reward_session
}

@Composable
private fun StatusChip(status: TimerSheetStatus) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (status) {
        TimerSheetStatus.RUNNING -> colors.secondaryContainer to colors.onSecondaryContainer
        TimerSheetStatus.REACHED -> colors.tertiaryContainer to colors.onTertiaryContainer
        TimerSheetStatus.PAUSED, TimerSheetStatus.IDLE -> colors.surfaceContainerHighest to colors.onSurfaceVariant
    }
    Surface(shape = MaterialTheme.shapes.small, color = container, contentColor = content) {
        Text(stringResource(when (status) {
            TimerSheetStatus.RUNNING -> R.string.timer_state_running
            TimerSheetStatus.PAUSED -> R.string.timer_state_paused
            TimerSheetStatus.REACHED -> R.string.timer_state_reached
            TimerSheetStatus.IDLE -> R.string.timer_state_idle
        }), Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelLarge)
    }
}

/** Discard and finish sit behind ⋮ so neither is one accidental tap away from Pausar (spec 018). */
@Composable
private fun SessionMenu(model: TimerSheetModel, busy: Boolean, vm: TimerViewModel) {
    val discardId = model.discardSessionId
    val finishId = model.finishSessionId
    if (discardId == null && finishId == null) return
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.timer_more_actions)
    Box {
        IconButton({ expanded = true }, enabled = !busy,
            modifier = Modifier.semantics { contentDescription = description }) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            if (discardId != null) DropdownMenuItem(
                text = { Text(stringResource(R.string.timer_discard), color = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; vm.requestDiscard(discardId) })
            if (finishId != null) DropdownMenuItem(
                text = { Column {
                    Text(stringResource(R.string.timer_finish))
                    Text(stringResource(R.string.timer_finish_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } },
                onClick = { expanded = false; vm.finish(finishId) })
        }
    }
}

@Composable
private fun SessionNotices(state: TimerUiState, requestPermission: () -> Unit) {
    if (!state.notificationsVisible) Surface(shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer) {
        Column(Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp)) {
            Text(stringResource(R.string.timer_notifications_off), style = MaterialTheme.typography.bodySmall)
            TextButton(requestPermission, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.timer_allow_notifications)) }
        }
    }
    if (state.serviceBlocked) Text(stringResource(R.string.timer_service_blocked),
        style = MaterialTheme.typography.bodySmall)
    if (state.unavailable) Text(stringResource(R.string.timer_reached))
}

@Composable
private fun TimerRing(model: TimerSheetModel) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(RING_SIZE), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { model.objective?.fraction ?: 0f }, modifier = Modifier.fillMaxSize(),
                strokeWidth = 14.dp, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round, gapSize = 0.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(if (model.showsRemaining) R.string.timer_remaining else R.string.timer_elapsed),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(clockText(model.clockMillis), style = MaterialTheme.typography.displaySmall, softWrap = false,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
    }
}

private val RING_SIZE = 196.dp

@Composable
private fun ObjectiveLine(objective: TimerObjective) {
    val done = minutesLabel(objective.doneMillis)
    val goal = minutesLabel(objective.objectiveMillis)
    Text(stringResource(when (objective.period) {
        TimerPeriod.DAY -> R.string.timer_progress_day
        TimerPeriod.WEEK -> R.string.timer_progress_week
        TimerPeriod.TOTAL -> R.string.timer_progress_total
        null -> R.string.timer_progress_reward
    }, done, goal), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SessionOutcome(model: TimerSheetModel) {
    val text = when {
        model.status == TimerSheetStatus.REACHED -> if (model.kind == TimerKind.REWARD) R.string.timer_exhausted
            else R.string.timer_completed
        model.sourceClosed -> R.string.timer_source_closed
        else -> return
    }
    Text(stringResource(text), Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}

@Composable
private fun PrimaryControls(model: TimerSheetModel, busy: Boolean, vm: TimerViewModel, request: TimerStart) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically) {
        when (model.primary) {
            TimerPrimaryAction.PAUSE -> MainButton(R.drawable.ic_pause, R.string.timer_pause, busy) {
                model.sessionId?.let(vm::pause) }
            TimerPrimaryAction.RESUME -> MainButton(R.drawable.ic_play_arrow, R.string.timer_resume, busy) {
                vm.start(request) }
            TimerPrimaryAction.START -> MainButton(R.drawable.ic_play_arrow, R.string.timer_start, busy) {
                vm.start(request) }
            TimerPrimaryAction.NONE -> Unit
        }
        if (model.canAddManual) {
            val description = stringResource(R.string.timer_add_manual)
            FilledTonalIconButton(vm::newManual, enabled = !busy, modifier = Modifier.size(MAIN_BUTTON_HEIGHT)
                .semantics { contentDescription = description }) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null)
            }
        }
    }
}

private val MAIN_BUTTON_HEIGHT = 52.dp

@Composable
private fun MainButton(@DrawableRes icon: Int, @StringRes label: Int, busy: Boolean, onClick: () -> Unit) {
    Button(onClick, enabled = !busy, modifier = Modifier.heightIn(min = MAIN_BUTTON_HEIGHT).widthIn(min = 160.dp)) {
        Icon(painterResource(icon), contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(label))
    }
}

@Composable
private fun AllSegments(model: TimerSheetModel, busy: Boolean, vm: TimerViewModel, back: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(back) { Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back)) }
        Column(Modifier.padding(start = 4.dp)) {
            Text(stringResource(R.string.timer_records), style = MaterialTheme.typography.titleMedium)
            Text(model.title, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (model.canAddManual) OutlinedButton(vm::newManual, enabled = !busy) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.timer_add_manual))
    }
    SegmentDays(model.all, model.today, busy, vm)
}

@Composable
private fun SegmentDays(days: List<TimerSegmentDay>, today: LocalDate, busy: Boolean, vm: TimerViewModel) {
    if (days.isEmpty()) {
        Text(stringResource(R.string.timer_no_sessions), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    days.forEach { day ->
        Text(TabitosDates.short(day.date, today).label(), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.clip(MaterialTheme.shapes.large), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            day.rows.forEach { SegmentRow(it, busy, vm) }
        }
    }
}

@Composable
private fun SegmentRow(row: TimerSegmentRow, busy: Boolean, vm: TimerViewModel) {
    val zone = vm.zone
    val start = hourMinute(row.startWall, zone)
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).background(MaterialTheme.colorScheme.surface)
        .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val tint = if (row.open) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(painterResource(if (row.segment.manual) R.drawable.ic_edit else R.drawable.ic_play_arrow),
            contentDescription = null, tint = tint)
        Column(Modifier.weight(1f)) {
            Text(if (row.open) stringResource(R.string.timer_segment_now, start)
                else stringResource(R.string.timer_segment_range, start, hourMinute(row.endWall, zone)),
                style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(when {
                row.open -> R.string.timer_segment_counting
                row.segment.manual -> R.string.timer_segment_typed
                else -> R.string.timer_segment_counted
            }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(minutesLabel(row.millis), style = MaterialTheme.typography.bodyMedium, color = tint)
        if (row.open) Spacer(Modifier.width(48.dp)) else SegmentMenu(row.segment, start, busy, vm)
    }
}

@Composable
private fun SegmentMenu(segment: TimerSegment, start: String, busy: Boolean, vm: TimerViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.timer_segment_actions, start)
    Box {
        IconButton({ expanded = true }, enabled = !busy,
            modifier = Modifier.semantics { contentDescription = description }) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.timer_correct)) },
                onClick = { expanded = false; vm.editSegment(segment) })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.timer_delete), color = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; vm.deleteSegment(segment.id) })
        }
    }
}

@Composable
private fun ManualEditor(state: TimerUiState, draft: ManualDraft, vm: TimerViewModel) {
    AlertDialog(onDismissRequest = { if (!state.busy) vm.manual(null) },
        title = { Text(stringResource(if (draft.segmentId == null) R.string.timer_add_manual
            else R.string.timer_correct)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TimerError(state, vm::retry)
            Text(stringResource(R.string.timer_manual_hint))
            OutlinedTextField(draft.date, { vm.manual(draft.copy(date = it)) }, enabled = !state.busy,
                singleLine = true,
                label = { Text(stringResource(R.string.timer_manual_date)) }, modifier = Modifier.fillMaxWidth())
            ScrollableTimeField(R.string.timer_manual_start, draft.start, !state.busy,
                onChange = { vm.manual(draft.copy(start = it)) })
            ScrollableTimeField(R.string.timer_manual_end, draft.end, !state.busy,
                onChange = { vm.manual(draft.copy(end = it)) })
        } },
        confirmButton = { TextButton(vm::saveManual, enabled = !state.busy) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton({ vm.manual(null) }, enabled = !state.busy) {
            Text(stringResource(R.string.cancel)) } })
}

@Preview
@Composable
private fun TimerAlarmDialogPreview() {
    MaterialTheme { TimerAlarmDialog("Ler 30 minutos", stop = {}) }
}

@Preview
@Composable
private fun TimerCardsPreview() {
    val session = TimerSession("p", TimerKind.TASK, "o", "Ler 30 minutos", TimerState.PAUSED, created = 0)
    val state = TimerUiState(snapshot = TimerSnapshot(listOf(session), listOf(TimerSegment("s", "p", 0, 754_000,
        created = 0))), loading = false)
    MaterialTheme { Column { TimerSessionCards(state, listOf(session), ZoneId.of("UTC"), TimerCardActions()) } }
}
