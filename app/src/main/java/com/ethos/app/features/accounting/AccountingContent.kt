package com.ethos.app.features.accounting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.ethos.app.R
import com.ethos.app.core.ui.accounting.*
import com.ethos.app.core.accounting.domain.*
import com.ethos.app.core.ui.components.CoinAmount
import com.ethos.app.core.ui.components.Eyebrow
import com.ethos.app.core.ui.format.EthosDates
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun AccountingStatus(state: AccountingUiState, retry: () -> Unit) {
    if (state.loading) Text(stringResource(R.string.finance_loading))
    if (state.error) {
        Text(stringResource(R.string.finance_error), color = MaterialTheme.colorScheme.error)
        TextButton(retry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
fun WalletTiles(state: AccountingUiState, rewards: () -> Unit, retry: () -> Unit) {
    AccountingStatus(state, retry)
    if (state.loading || state.error) return
    val s = state.snapshot
    val shape = RoundedCornerShape(16.dp)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    // Model B wallet (spec 010): amber coin amount, link to rewards, level with XP bar.
    val balance: @Composable (Modifier) -> Unit = { modifier ->
        val description = stringResource(R.string.wallet_description, s.balance)
        Card(rewards, modifier.semantics(mergeDescendants = true) { contentDescription = description },
            shape = shape, colors = cardColors, border = border) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow(stringResource(R.string.wallet_coins))
                CoinAmount(s.balance.toString())
                Text(stringResource(R.string.wallet_rewards), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    val level: @Composable (Modifier) -> Unit = { modifier ->
        Card(modifier, shape = shape, colors = cardColors, border = border) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow(stringResource(R.string.wallet_progress))
                Text(stringResource(R.string.finance_level, s.progress.level), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                val xpDescription = stringResource(R.string.finance_progress,
                    s.progress.within, s.progress.required, s.totalXp)
                Text(stringResource(R.string.finance_progress_compact, s.progress.within, s.progress.required),
                    modifier = Modifier.semantics { contentDescription = xpDescription },
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { s.progress.within.toFloat() / s.progress.required.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    trackColor = MaterialTheme.colorScheme.outlineVariant, drawStopIndicator = {})
            }
        }
    }
    BoxWithConstraints {
        if (maxWidth >= 300.dp && LocalDensity.current.fontScale < 1.5f) {
            // Same height side by side: the row takes the taller tile's height and both fill it.
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                balance(Modifier.weight(1f).fillMaxHeight())
                level(Modifier.weight(1f).fillMaxHeight())
            }
        } else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            balance(Modifier.fillMaxWidth())
            level(Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun AccountingSettings(state: AccountingUiState, edit: () -> Unit, change: (List<String>?) -> Unit,
    save: () -> Unit, retry: () -> Unit) {
    Text(stringResource(R.string.earnings), style = MaterialTheme.typography.titleLarge)
    AccountingStatus(state, retry)
    if (!state.loading && !state.error) AccountingExplanation(state.snapshot.prices)
    if (!state.loading && !state.error && state.draft == null) {
        TextButton(edit) { Text(stringResource(R.string.finance_edit_table)) }
    }
    val draft = state.draft
    if (draft != null) {
        Text(stringResource(R.string.finance_bounds))
        (0..2).forEach { i ->
            Text(stringResource(listOf(R.string.easy, R.string.medium, R.string.hard)[i]),
                style = MaterialTheme.typography.titleSmall)
            val labels = listOf(R.string.finance_coin_gain, R.string.finance_xp_gain, R.string.finance_penalty)
            labels.forEachIndexed { j, label ->
                val index = i * 3 + j
                val importance = stringResource(listOf(R.string.low, R.string.medium, R.string.high)[i])
                OutlinedTextField(draft[index], { value -> change(draft.toMutableList().also { it[index] = value }) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.saving,
                    label = { Text(if (j == 2) stringResource(R.string.finance_penalty_label, importance)
                        else stringResource(label)) })
            }
        }
        if (state.invalid) Text(stringResource(R.string.finance_invalid), color = MaterialTheme.colorScheme.error)
        TextButton(save, enabled = !state.saving && !state.loading) { Text(stringResource(R.string.save)) }
        TextButton({ change(null) }, enabled = !state.saving) { Text(stringResource(R.string.cancel)) }
    }
}

/** Coin history kept simple: one line per entry under a day header, amounts on the right. */
@Composable
fun AccountingHistory(state: AccountingUiState, retry: () -> Unit, today: LocalDate, zone: ZoneId) {
    val days = remember(state.snapshot.entries, zone) { historyDays(state.snapshot.entries, zone) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
        item { AccountingStatus(state, retry) }
        if (!state.loading && !state.error && days.isEmpty()) {
            item { Text(stringResource(R.string.finance_history_empty)) }
        }
        days.forEach { day ->
            item(key = "day:${day.date}") {
                Eyebrow(EthosDates.longPlain(day.date, today), Modifier.padding(top = 16.dp, bottom = 4.dp))
            }
            items(day.entries, key = { it.sequence }) { entry ->
                HistoryRow(entry, zone)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: LedgerEntry, zone: ZoneId) {
    val moment = Instant.ofEpochMilli(entry.time).atZone(zone)
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.history_line, stringResource(historyKind(entry.kind)),
                EthosDates.time(moment.hour * MINUTES_PER_HOUR + moment.minute)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            CoinAmount(signed(entry.coins), style = MaterialTheme.typography.titleSmall)
            if (entry.xp != 0L) Text(stringResource(R.string.history_xp, signed(entry.xp)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val MINUTES_PER_HOUR = 60

private fun signed(value: Long): String = if (value > 0) "+$value" else value.toString()

private fun historyKind(kind: String): Int = when (kind) {
    "REVERSAL" -> R.string.history_kind_reversal
    "REAPPLY" -> R.string.history_kind_reapply
    "CORRECTION" -> R.string.history_kind_correction
    "PENALTY" -> R.string.history_kind_penalty
    "PURCHASE" -> R.string.history_kind_purchase
    else -> R.string.history_kind_gain
}

@Preview
@Composable
private fun WalletPreview() { MaterialTheme { WalletTiles(AccountingUiState(loading = false), {}, {}) } }
