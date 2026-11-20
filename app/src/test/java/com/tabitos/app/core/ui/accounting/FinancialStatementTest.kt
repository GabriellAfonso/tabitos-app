package com.tabitos.app.core.ui.accounting

import com.tabitos.app.core.accounting.domain.FinancialPreview
import com.tabitos.app.core.accounting.domain.LedgerEntry
import com.tabitos.app.core.habits.domain.HabitAction
import com.tabitos.app.core.tasks.domain.TaskAction
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialStatementTest {
    private val today = LocalDate.of(2026, 11, 18)

    private fun entry(kind: String, nominal: Long, coins: Long = nominal, xp: Long = 0) = LedgerEntry(
        key = "task:t:$today", title = "Revisar relatório", date = today, time = 0, kind = kind,
        nominalCoins = nominal, coins = coins, xp = xp, balance = 0, totalXp = 0)

    @Test fun gainStartsFromBalanceBeforeTheActionAndCrossesLevel() {
        // 430 XP is level 3 (250 to 450); +40 reaches level 4.
        val statement = FinancialPreview(listOf(entry("GAIN", 20, xp = 40)), balance = 155, totalXp = 470)
            .toStatement()

        assertEquals(135L, statement.currentBalance)
        assertEquals(listOf(StatementLine.Entry("Revisar relatório", "GAIN", 20)), statement.lines)
        assertEquals(155L, statement.newBalance)
        assertEquals(40L, statement.xpChange)
        assertEquals(3L, statement.levelBefore.level)
        assertEquals(4L, statement.levelAfter.level)
        assertTrue(statement.changesLevel)
        assertFalse(statement.losesCoins)
    }

    @Test fun penaltyClippedByFloorShowsFullPenaltyAndTheLimitLine() {
        val statement = FinancialPreview(listOf(entry("PENALTY", nominal = -20, coins = -10)),
            balance = -300, totalXp = 430).toStatement()

        assertEquals(-290L, statement.currentBalance)
        assertEquals(listOf(StatementLine.Entry("Revisar relatório", "PENALTY", -20), StatementLine.FloorLimit(10)),
            statement.lines)
        assertEquals(statement.newBalance, statement.currentBalance + statement.lines.sumOf { it.coins })
        assertTrue(statement.losesCoins)
        assertFalse(statement.changesLevel)
    }

    @Test fun emptyPreviewHasNoLines() {
        val statement = FinancialPreview(emptyList(), balance = 135, totalXp = 430).toStatement()

        assertTrue(statement.isEmpty)
        assertEquals(135L, statement.currentBalance)
        assertEquals(0L, statement.xpChange)
    }

    @Test fun kindFollowsTheAction() {
        assertEquals(FinancialConfirmationKind.TASK_DONE,
            financialConfirmationKind(TaskAction.Resolve("t", done = true, actualDate = today)))
        assertEquals(FinancialConfirmationKind.TASK_NOT_DONE,
            financialConfirmationKind(TaskAction.Resolve("t", done = false, actualDate = today)))
        assertEquals(FinancialConfirmationKind.UNDO, financialConfirmationKind(TaskAction.Undo("t")))
        assertEquals(FinancialConfirmationKind.BAD_HABIT_EVENT,
            financialConfirmationKind(HabitAction.Record("r", "h", today)))
        assertEquals(FinancialConfirmationKind.OTHER, financialConfirmationKind(HabitAction.Settle))
    }

    @Test fun signedAmountUsesTrueMinus() {
        assertEquals("+20", signedAmount(20))
        assertEquals("−20", signedAmount(-20))
        assertEquals("0", signedAmount(0))
    }
}
