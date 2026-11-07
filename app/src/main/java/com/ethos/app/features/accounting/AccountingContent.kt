package com.ethos.app.features.accounting

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.ethos.app.R
import com.ethos.app.core.ui.accounting.*
import com.ethos.app.core.accounting.domain.*

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
    val balance: @Composable (Modifier) -> Unit = { modifier ->
        Card(rewards, modifier) { Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.balance), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.finance_coins, s.balance), style = MaterialTheme.typography.titleMedium)
        } }
    }
    val level: @Composable (Modifier) -> Unit = { modifier ->
        OutlinedCard(modifier) { Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.finance_level, s.progress.level), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.finance_progress, s.progress.within, s.progress.required, s.totalXp))
            LinearProgressIndicator(progress = { s.progress.within.toFloat() / s.progress.required.toFloat() },
                modifier = Modifier.fillMaxWidth())
        } }
    }
    BoxWithConstraints {
        if (maxWidth >= 300.dp && LocalDensity.current.fontScale < 1.5f) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                balance(Modifier.weight(1f))
                level(Modifier.weight(1f))
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

@Composable
fun AccountingHistory(state: AccountingUiState, retry: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { AccountingStatus(state, retry) }
        if (!state.loading && !state.error && state.snapshot.entries.isEmpty()) {
            item { Text(stringResource(R.string.finance_history_empty)) }
        }
        items(state.snapshot.entries.asReversed(), key = { it.sequence }) { entry ->
            OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                FinancialEntryContent(entry)
                entry.related?.let { id ->
                    val original = state.snapshot.entries.firstOrNull { it.sequence == id }
                    if (original != null) Text(stringResource(R.string.finance_related, original.title,
                        financialTime(original.time)))
                }
            } }
        }
    }
}

@Preview
@Composable
private fun WalletPreview() { MaterialTheme { WalletTiles(AccountingUiState(loading = false), {}, {}) } }
