@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.tabitos.app.core.ui.accounting

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tabitos.app.R
import com.tabitos.app.core.accounting.domain.*
import com.tabitos.app.core.ui.theme.TabitosTheme
import java.time.LocalDate

val LocalFinancialPrices = staticCompositionLocalOf<List<Price>?> { null }

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

/** Label of a ledger kind, shown under each statement line. */
@StringRes
private fun ledgerKindLabel(kind: String): Int = when (kind) {
    "REVERSAL" -> R.string.finance_reversal
    "REAPPLY" -> R.string.finance_reapply
    "CORRECTION" -> R.string.finance_correction
    "PENALTY" -> R.string.finance_failure
    "PURCHASE" -> R.string.finance_purchase
    else -> R.string.finance_gain
}

@StringRes
private fun FinancialConfirmationKind.title(): Int = when (this) {
    FinancialConfirmationKind.TASK_DONE -> R.string.finance_confirm_task_done
    FinancialConfirmationKind.TASK_NOT_DONE -> R.string.finance_confirm_task_not_done
    FinancialConfirmationKind.UNDO -> R.string.finance_confirm_undo
    FinancialConfirmationKind.BAD_HABIT_EVENT -> R.string.finance_confirm_bad_habit
    FinancialConfirmationKind.OTHER -> R.string.finance_confirm
}

@StringRes
private fun FinancialConfirmationKind.confirmLabel(): Int = when (this) {
    FinancialConfirmationKind.TASK_DONE -> R.string.finance_apply_task_done
    FinancialConfirmationKind.TASK_NOT_DONE -> R.string.finance_apply_task_not_done
    FinancialConfirmationKind.UNDO -> R.string.finance_apply_undo
    FinancialConfirmationKind.BAD_HABIT_EVENT -> R.string.finance_apply_bad_habit
    FinancialConfirmationKind.OTHER -> R.string.finance_apply
}

/** Coin confirmation laid out as a statement (option C of prototypes/confirmacao-moedas). */
@Composable
fun FinancialConfirmation(
    kind: FinancialConfirmationKind, preview: FinancialPreview, busy: Boolean,
    confirm: () -> Unit, cancel: () -> Unit,
) {
    val statement = remember(preview) { preview.toStatement() }
    AlertDialog(onDismissRequest = { if (!busy) cancel() },
        title = { Text(stringResource(kind.title())) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (statement.isEmpty) Text(stringResource(R.string.finance_no_change))
                else { StatementCard(statement); XpSummary(statement) }
            }
        },
        confirmButton = { ConfirmButton(kind, statement.losesCoins, busy, confirm) },
        dismissButton = { TextButton(cancel, enabled = !busy) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun ConfirmButton(kind: FinancialConfirmationKind, losesCoins: Boolean, busy: Boolean, confirm: () -> Unit) {
    // Red when coins go down, so a loss never looks like a routine confirmation.
    val colors = if (losesCoins) ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
    else ButtonDefaults.buttonColors()
    Button(confirm, enabled = !busy, colors = colors) { Text(stringResource(kind.confirmLabel())) }
}

@Composable
private fun StatementCard(statement: FinancialStatement) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        StatementRow(stringResource(R.string.finance_balance_now), null, coinsText(statement.currentBalance))
        statement.lines.forEach { line ->
            HorizontalDivider()
            StatementLineRow(line)
        }
        HorizontalDivider()
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)) {
            StatementRow(stringResource(R.string.finance_balance_after), null, coinsText(statement.newBalance),
                total = true)
        }
    }
}

@Composable
private fun StatementLineRow(line: StatementLine) {
    val amount = coinsText(line.coins, signed = true)
    when (line) {
        is StatementLine.Entry -> {
            val kindLabel = stringResource(ledgerKindLabel(line.kind))
            StatementRow(line.title.ifBlank { kindLabel }, kindLabel, amount, amountColor(line.coins))
        }
        is StatementLine.FloorLimit -> StatementRow(stringResource(R.string.finance_floor_limit),
            stringResource(R.string.finance_floor_limit_detail), amount, amountColor(line.coins))
    }
}

@Composable
private fun StatementRow(
    title: String, subtitle: String?, amount: String,
    amountColor: Color = MaterialTheme.colorScheme.onSurface, total: Boolean = false,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (total) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(amount, color = amountColor, fontWeight = if (total) FontWeight.Bold else FontWeight.Medium,
            style = if (total) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun XpSummary(statement: FinancialStatement) {
    if (statement.xpChange == 0L) {
        Text(stringResource(R.string.finance_xp_unchanged), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val after = statement.levelAfter
    val level = if (statement.changesLevel) {
        stringResource(R.string.finance_level_change, statement.levelBefore.level, after.level)
    } else stringResource(R.string.finance_level_within, after.level, after.within, after.required)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.finance_xp_change, signedAmount(statement.xpChange)), Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(level, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(progress = { after.within.toFloat() / after.required }, Modifier.fillMaxWidth())
    }
}

/** Balances keep their plain value ("135 ◈"); changes always carry a sign ("+20 ◈"). */
@Composable
private fun coinsText(value: Long, signed: Boolean = false): String = stringResource(R.string.finance_coins_amount,
    if (signed || value < 0) signedAmount(value) else value.toString())

@Composable
private fun amountColor(coins: Long): Color =
    if (coins < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

@Preview
@Composable
private fun FinancialConfirmationPreview() {
    val gain = LedgerEntry(key = "task:t:2026-11-18", title = "Revisar relatório trimestral",
        date = LocalDate.of(2026, 11, 18), time = 0, kind = "GAIN", nominalCoins = 20, coins = 20, xp = 40,
        balance = 155, totalXp = 470)
    TabitosTheme {
        FinancialConfirmation(FinancialConfirmationKind.TASK_DONE, FinancialPreview(listOf(gain), 155, 470),
            busy = false, confirm = {}, cancel = {})
    }
}

@Preview
@Composable
private fun EstimatePreview() {
    MaterialTheme { CompositionLocalProvider(LocalFinancialPrices provides Price.defaults()) {
        Column { FinancialEstimate(2, 0) }
    } }
}
