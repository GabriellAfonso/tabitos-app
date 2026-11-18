@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ethos.app.core.ui.accounting

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.accounting.domain.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val LocalFinancialPrices = staticCompositionLocalOf<List<Price>?> { null }

fun financialTime(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

private val difficultyLabels = listOf(R.string.easy, R.string.medium, R.string.hard)

/** Name of a price row's difficulty ("Fácil", "Médio", "Difícil"). */
@StringRes
fun difficultyLabel(price: Price): Int = difficultyLabels[price.category]
private val importanceLabels = listOf(R.string.low, R.string.medium, R.string.high)

/** How coins and XP work, as two small tables and short facts instead of a paragraph. */
@Composable
fun AccountingExplanation(prices: List<Price>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PriceTable(R.string.finance_gain_title, R.string.finance_col_difficulty,
            listOf(R.string.finance_col_coins, R.string.finance_col_xp),
            prices.map { listOf(stringResource(difficultyLabel(it)), "+${it.coins}", "+${it.xp}") })
        PriceTable(R.string.finance_penalty_title, R.string.finance_col_importance, listOf(R.string.finance_col_coins),
            prices.map { listOf(stringResource(importanceLabels[it.category]), "−${it.penalty}") })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.finance_fact_xp, R.string.finance_fact_habits, R.string.finance_fact_floor,
                R.string.finance_fact_level).forEach { fact ->
                AssistChip(onClick = {}, label = { Text(stringResource(fact)) }, enabled = false,
                    colors = AssistChipDefaults.assistChipColors(
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface))
            }
        }
    }
}

@Composable
private fun PriceTable(title: Int, firstColumn: Int, columns: List<Int>, rows: List<List<String>>) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            TableRow((listOf(firstColumn) + columns).map { stringResource(it) }, header = true)
            HorizontalDivider()
            rows.forEach { TableRow(it, header = false) }
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean) {
    Row(Modifier.fillMaxWidth()) {
        cells.forEachIndexed { index, cell ->
            Text(cell, Modifier.weight(if (index == 0) 1.4f else 1f),
                style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyLarge,
                color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun FinancialEstimate(difficulty: Int, importance: Int) {
    val prices = LocalFinancialPrices.current
    if (prices == null) { Text(stringResource(R.string.finance_estimate_unavailable)); return }
    val gain = prices[difficulty]
    val penalty = prices[importance]
    Text(stringResource(R.string.finance_estimate, gain.coins, gain.xp, penalty.penalty))
    var expanded by rememberSaveable { mutableStateOf(false) }
    TextButton({ expanded = !expanded }) { Text(stringResource(R.string.finance_how)) }
    if (expanded) AccountingExplanation(prices)
}

@Composable
fun FinancialEntryContent(entry: LedgerEntry) {
    val kind = when (entry.kind) {
        "REVERSAL" -> R.string.finance_reversal
        "REAPPLY" -> R.string.finance_reapply
        "CORRECTION" -> R.string.finance_correction
        "PENALTY" -> R.string.finance_failure
        "PURCHASE" -> R.string.finance_purchase
        else -> R.string.finance_gain
    }
    Text(stringResource(kind), style = MaterialTheme.typography.titleSmall)
    Text(entry.title)
    val reason = when {
        entry.key.startsWith("purchase:") -> R.string.finance_source_purchase
        entry.key.startsWith("task:") -> R.string.finance_source_task
        entry.key.startsWith("practice:") -> R.string.finance_source_day
        entry.key.startsWith("event:") -> R.string.finance_source_event
        else -> R.string.finance_source_period
    }
    Text(stringResource(reason, entry.date.toString()))
    Text(stringResource(R.string.finance_effect, entry.coins, entry.xp, entry.balance))
    Text(stringResource(R.string.finance_entered, financialTime(entry.time)))
    if (entry.nominalCoins != entry.coins) Text(stringResource(R.string.finance_clipped,
        entry.nominalCoins, entry.coins))
}

@Composable
fun FinancialConfirmation(preview: FinancialPreview, busy: Boolean, confirm: () -> Unit, cancel: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) cancel() },
        title = { Text(stringResource(R.string.finance_confirm)) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (preview.entries.isEmpty()) Text(stringResource(R.string.finance_no_change))
                preview.entries.forEach { FinancialEntryContent(it) }
                Text(stringResource(R.string.finance_result, preview.balance, preview.totalXp))
            }
        }, confirmButton = { TextButton(confirm, enabled = !busy) { Text(stringResource(R.string.finance_apply)) } },
        dismissButton = { TextButton(cancel, enabled = !busy) { Text(stringResource(R.string.cancel)) } })
}

@Preview
@Composable
private fun EstimatePreview() {
    MaterialTheme { CompositionLocalProvider(LocalFinancialPrices provides Price.defaults()) {
        Column { FinancialEstimate(2, 0) }
    } }
}
