package com.tabitos.app.core.ui.accounting

import com.tabitos.app.core.accounting.domain.AccountingRules
import com.tabitos.app.core.accounting.domain.FinancialPreview
import com.tabitos.app.core.accounting.domain.Progress
import com.tabitos.app.core.habits.domain.HabitAction
import com.tabitos.app.core.tasks.domain.TaskAction

/** Which action the coin confirmation is about; names the dialog and its confirm button. */
enum class FinancialConfirmationKind { TASK_DONE, TASK_NOT_DONE, UNDO, BAD_HABIT_EVENT, OTHER }

fun financialConfirmationKind(action: TaskAction): FinancialConfirmationKind = when (action) {
    is TaskAction.Resolve -> if (action.done) FinancialConfirmationKind.TASK_DONE
        else FinancialConfirmationKind.TASK_NOT_DONE
    is TaskAction.Undo -> FinancialConfirmationKind.UNDO
    else -> FinancialConfirmationKind.OTHER
}

/** Practice records never ask for confirmation, so a confirmed [HabitAction.Record] is a bad habit event. */
fun financialConfirmationKind(action: HabitAction): FinancialConfirmationKind = when (action) {
    is HabitAction.Record -> FinancialConfirmationKind.BAD_HABIT_EVENT
    else -> FinancialConfirmationKind.OTHER
}

sealed interface StatementLine {
    val coins: Long

    /** One ledger entry at its full (nominal) value; [kind] is the ledger kind ("GAIN", "PENALTY"...). */
    data class Entry(val title: String, val kind: String, override val coins: Long) : StatementLine

    /** Part of a penalty that the −300 ◈ floor gave back, so the lines still add up to the new balance. */
    data class FloorLimit(override val coins: Long) : StatementLine
}

/** Coin confirmation as a bank statement: current balance, one line per change, new balance, then XP. */
data class FinancialStatement(
    val currentBalance: Long,
    val lines: List<StatementLine>,
    val newBalance: Long,
    val xpChange: Long,
    val levelBefore: Progress,
    val levelAfter: Progress,
) {
    val isEmpty get() = lines.isEmpty()
    val losesCoins get() = newBalance < currentBalance
    val changesLevel get() = levelAfter.level != levelBefore.level
}

fun FinancialPreview.toStatement(): FinancialStatement {
    val lines = entries.flatMap { entry ->
        val nominal = StatementLine.Entry(entry.title, entry.kind, entry.nominalCoins)
        val floorGap = entry.coins - entry.nominalCoins
        if (floorGap == 0L) listOf(nominal) else listOf(nominal, StatementLine.FloorLimit(floorGap))
    }
    val xpChange = entries.sumOf { it.xp }
    return FinancialStatement(
        currentBalance = balance - entries.sumOf { it.coins },
        lines = lines,
        newBalance = balance,
        xpChange = xpChange,
        levelBefore = AccountingRules.progress(maxOf(0L, totalXp - xpChange)),
        levelAfter = AccountingRules.progress(totalXp),
    )
}

/** "+20", "−20" (true minus sign, as elsewhere in the app) or "0". */
fun signedAmount(value: Long): String = when {
    value > 0 -> "+$value"
    value < 0 -> "−${-value}"
    else -> "0"
}
