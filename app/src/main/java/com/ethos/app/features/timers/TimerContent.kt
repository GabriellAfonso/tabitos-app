package com.ethos.app.features.timers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.ethos.app.R
import com.ethos.app.core.timers.domain.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

fun clockText(millis: Long): String {
    val seconds = millis.coerceAtLeast(0) / 1000
    return "%d:%02d:%02d".format(seconds / 3600, (seconds / 60) % 60, seconds % 60)
}

fun minutesLabel(millis: Long): String {
    val (h, m) = TimerRules.minutesText(millis)
    return if (h > 0) "$h h ${"%02d".format(m)} min" else "$m min"
}

private val stamp = DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"))
private fun at(millis: Long, zone: ZoneId) = Instant.ofEpochMilli(millis).atZone(zone).format(stamp)

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
)

/** Active and paused sessions (filtered by the host to sources still open), running first. */
@Composable
fun TimerSessionCards(state: TimerUiState, sessions: List<TimerSession>, zone: ZoneId, actions: TimerCardActions) {
    sessions.sortedByDescending { it.state == TimerState.RUNNING }.forEach { session ->
        val total = TimerRules.sessionTotal(state.snapshot, session.id, state.now)
        val remaining = TimerRules.remaining(state.snapshot, session, state.now, zone)
        OutlinedCard(onClick = { actions.open(session) }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(when {
                    session.kind == TimerKind.REWARD -> R.string.timer_reward_session
                    session.state == TimerState.RUNNING -> R.string.timer_active
                    else -> R.string.timer_paused
                }), style = MaterialTheme.typography.labelLarge)
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

private val auditLabels = mapOf(
    TimerAuditKind.START to R.string.timer_audit_start, TimerAuditKind.PAUSE to R.string.timer_audit_pause,
    TimerAuditKind.RESUME to R.string.timer_audit_resume, TimerAuditKind.RECOVERED to R.string.timer_audit_recovered,
    TimerAuditKind.REACHED to R.string.timer_audit_reached, TimerAuditKind.EXHAUSTED to R.string.timer_audit_exhausted,
    TimerAuditKind.MANUAL to R.string.timer_audit_manual, TimerAuditKind.CORRECTED to R.string.timer_audit_corrected,
    TimerAuditKind.DELETED to R.string.timer_audit_deleted, TimerAuditKind.FINISHED to R.string.timer_audit_finished,
    TimerAuditKind.CONFIRMED to R.string.timer_audit_confirmed,
    TimerAuditKind.SOURCE_CLOSED to R.string.timer_audit_source_closed,
)

@Composable
fun TimerDialogs(
    state: TimerUiState, vm: TimerViewModel, openSource: (TimerKind, String) -> Unit,
    requestPermission: () -> Unit,
) {
    val zone = vm.zone
    state.conflict?.let { request ->
        val running = state.snapshot.running
        AlertDialog(onDismissRequest = { if (!state.busy) vm.cancelConflict() },
            title = { Text(stringResource(R.string.timer_conflict_title)) },
            text = { Text(stringResource(R.string.timer_conflict_body, running?.title.orEmpty(), request.title)) },
            confirmButton = { TextButton({ vm.start(request, pauseOther = true) }, enabled = !state.busy) {
                Text(stringResource(R.string.timer_conflict_confirm)) } },
            dismissButton = { TextButton(vm::cancelConflict, enabled = !state.busy) {
                Text(stringResource(R.string.cancel)) } })
        return
    }
    state.manual?.let { draft -> ManualEditor(state, draft, vm); return }
    state.reachedPrompt?.let { id ->
        val session = state.snapshot.sessions.firstOrNull { it.id == id }
        if (session != null && state.target == null) {
            AlertDialog(onDismissRequest = vm::dismissReached,
                title = { Text(stringResource(R.string.timer_reached)) },
                text = { Text(stringResource(R.string.timer_reached_body, session.title)) },
                confirmButton = { TextButton({ vm.confirm(session.kind, session.sourceId, session.id) },
                    enabled = !state.busy) { Text(stringResource(R.string.timer_confirm_complete)) } },
                dismissButton = { TextButton(vm::dismissReached) { Text(stringResource(R.string.timer_not_now)) } })
            return
        }
    }
    val target = state.target ?: return
    val request = target.start
    val session = state.snapshot.open(request.kind, request.sourceId)
    val today = Instant.ofEpochMilli(state.now.wall).atZone(zone).toLocalDate()
    val goal = state.snapshot.goal(request.goalKey)
    val progress = TimerRules.progress(state.snapshot, request.kind, request.sourceId, request.period, today,
        state.now, zone)
    val remaining = session?.let { TimerRules.remaining(state.snapshot, it, state.now, zone) }
        ?: goal?.let { (it.minutes * 60_000L - progress).coerceAtLeast(0) }
    val total = session?.let { TimerRules.sessionTotal(state.snapshot, it.id, state.now) } ?: 0L
    AlertDialog(onDismissRequest = { if (!state.busy) vm.open(null) }, title = { Text(request.title) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TimerError(state, vm::retry)
            val showRemaining = remaining != null && (state.countdown || request.kind == TimerKind.REWARD)
            Text(stringResource(if (showRemaining) R.string.timer_remaining else R.string.timer_elapsed),
                style = MaterialTheme.typography.labelLarge)
            Text(clockText(if (showRemaining) remaining!! else if (goal != null) progress else total),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (remaining != null && request.kind != TimerKind.REWARD) Row {
                FilterChip(state.countdown, { vm.countdown(true) },
                    label = { Text(stringResource(R.string.timer_mode_countdown)) })
                Spacer(Modifier.width(8.dp))
                FilterChip(!state.countdown, { vm.countdown(false) },
                    label = { Text(stringResource(R.string.timer_mode_countup)) })
            }
            if (goal != null) Text(stringResource(when (request.period) {
                TimerPeriod.DAY -> R.string.timer_progress_day
                TimerPeriod.WEEK -> R.string.timer_progress_week
                TimerPeriod.TOTAL -> R.string.timer_progress_total
            }, minutesLabel(progress), minutesLabel(goal.minutes * 60_000L)))
            when {
                session?.state == TimerState.RUNNING -> Button({ vm.pause(session.id) }, enabled = !state.busy) {
                    Text(stringResource(R.string.timer_pause)) }
                remaining == 0L -> if (request.kind != TimerKind.REWARD && target.canStart && !target.sourceDone) {
                    Text(stringResource(R.string.timer_reached))
                    Button({ vm.confirm(request.kind, request.sourceId, session?.id) }, enabled = !state.busy) {
                        Text(stringResource(R.string.timer_confirm_complete)) }
                } else if (request.kind == TimerKind.REWARD) Text(stringResource(R.string.timer_exhausted))
                target.canStart -> Button({ vm.start(request) }, enabled = !state.busy) {
                    Text(stringResource(if (session != null) R.string.timer_resume else R.string.timer_start)) }
                else -> Text(stringResource(R.string.timer_source_closed))
            }
            if (state.unavailable) Text(stringResource(R.string.timer_reached))
            if (session != null && session.kind != TimerKind.REWARD && session.state == TimerState.PAUSED) {
                TextButton({ vm.finish(session.id) }, enabled = !state.busy) {
                    Text(stringResource(R.string.timer_finish)) }
            }
            if (!state.notificationsVisible) {
                Text(stringResource(R.string.timer_notifications_off))
                TextButton(requestPermission) { Text(stringResource(R.string.timer_allow_notifications)) }
            }
            if (state.serviceBlocked) Text(stringResource(R.string.timer_service_blocked))
            if (target.sourceDone && goal != null && progress < goal.minutes * 60_000L) {
                Text(stringResource(R.string.timer_below_goal), color = MaterialTheme.colorScheme.error)
                TextButton({ vm.open(null); openSource(request.kind, request.sourceId) }) {
                    Text(stringResource(R.string.timer_open_source)) }
            }
            if (target.goalAllowed && request.goalKey != null) {
                HorizontalDivider()
                OutlinedTextField(state.goalText, vm::goal, enabled = !state.busy, singleLine = true,
                    label = { Text(stringResource(if (request.period == TimerPeriod.WEEK) R.string.timer_goal_week
                        else if (request.period == TimerPeriod.DAY) R.string.timer_goal_day
                        else R.string.timer_goal)) },
                    modifier = Modifier.fillMaxWidth())
                TextButton(vm::saveGoal, enabled = !state.busy) { Text(stringResource(R.string.timer_goal_save)) }
            }
            val segments = TimerRules.sourceSegments(state.snapshot, request.kind, request.sourceId)
            if (request.period != TimerPeriod.TOTAL && segments.isNotEmpty()) {
                Text(stringResource(R.string.timer_periods), style = MaterialTheme.typography.titleSmall)
                TimerRules.byPeriod(segments, request.period, state.now, zone).entries.take(6).forEach { (date, ms) ->
                    Text(stringResource(R.string.timer_period_line, date.toString(), minutesLabel(ms)))
                }
            }
            HorizontalDivider()
            Text(stringResource(R.string.timer_sessions), style = MaterialTheme.typography.titleSmall)
            if (request.kind != TimerKind.REWARD) TextButton(vm::newManual, enabled = !state.busy && target.canStart) {
                Text(stringResource(R.string.timer_add_manual)) }
            if (segments.isEmpty()) Text(stringResource(R.string.timer_no_sessions))
            segments.sortedByDescending { it.startWall }.forEach { segment ->
                val end = TimerRules.endWall(segment, state.now)
                Text(stringResource(if (segment.manual) R.string.timer_segment_manual else R.string.timer_segment,
                    at(segment.startWall, zone), at(end, zone), minutesLabel(end - segment.startWall)))
                if (segment.endWall != null) Row {
                    TextButton({ vm.editSegment(segment) }, enabled = !state.busy) {
                        Text(stringResource(R.string.timer_correct)) }
                    TextButton({ vm.deleteSegment(segment.id) }, enabled = !state.busy) {
                        Text(stringResource(R.string.timer_delete)) }
                }
            }
            val ids = state.snapshot.sessions.filter { it.kind == request.kind && it.sourceId == request.sourceId }
                .map { it.id }.toSet()
            val audits = state.snapshot.audits.filter { it.sessionId in ids }
            if (audits.isNotEmpty()) {
                Text(stringResource(R.string.timer_history), style = MaterialTheme.typography.titleSmall)
                audits.asReversed().take(30).forEach { audit ->
                    Text(stringResource(R.string.timer_audit_line, stringResource(auditLabels.getValue(audit.kind)),
                        at(audit.at, zone)))
                    if (audit.beforeStart != null && audit.beforeEnd != null) Text(stringResource(
                        R.string.timer_audit_before, at(audit.beforeStart, zone), at(audit.beforeEnd, zone)))
                }
            }
        }
    }, confirmButton = { TextButton({ vm.open(null) }, enabled = !state.busy) {
        Text(stringResource(R.string.close)) } })
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
            OutlinedTextField(draft.start, { vm.manual(draft.copy(start = it)) }, enabled = !state.busy,
                singleLine = true, label = { Text(stringResource(R.string.timer_manual_start)) },
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.end, { vm.manual(draft.copy(end = it)) }, enabled = !state.busy, singleLine = true,
                label = { Text(stringResource(R.string.timer_manual_end)) }, modifier = Modifier.fillMaxWidth())
        } },
        confirmButton = { TextButton(vm::saveManual, enabled = !state.busy) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton({ vm.manual(null) }, enabled = !state.busy) {
            Text(stringResource(R.string.cancel)) } })
}

@Preview
@Composable
private fun TimerCardsPreview() {
    val session = TimerSession("p", TimerKind.TASK, "o", "Ler 30 minutos", TimerState.PAUSED, created = 0)
    val state = TimerUiState(snapshot = TimerSnapshot(listOf(session), listOf(TimerSegment("s", "p", 0, 754_000,
        created = 0))), loading = false)
    MaterialTheme { Column { TimerSessionCards(state, listOf(session), ZoneId.of("UTC"), TimerCardActions()) } }
}
