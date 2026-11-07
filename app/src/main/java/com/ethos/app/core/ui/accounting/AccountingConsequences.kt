package com.ethos.app.core.ui.accounting

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

@Composable
fun AccountingExplanation(prices: List<Price>) {
    Text(stringResource(R.string.finance_explanation))
    prices.forEach { price ->
        Text(stringResource(R.string.finance_table_line,
            stringResource(listOf(R.string.easy, R.string.medium, R.string.hard)[price.category]),
            price.coins, price.xp,
            stringResource(listOf(R.string.low, R.string.medium, R.string.high)[price.category]), price.penalty))
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
        else -> R.string.finance_gain
    }
    Text(stringResource(kind), style = MaterialTheme.typography.titleSmall)
    Text(entry.title)
    val reason = when {
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
