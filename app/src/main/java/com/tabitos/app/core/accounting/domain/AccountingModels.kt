package com.tabitos.app.core.accounting.domain

import java.time.LocalDate

data class Price(val category: Int, val coins: Long, val xp: Long, val penalty: Long) {
    fun validate() { require(category in 0..2 && listOf(coins, xp, penalty).all { it in 0..10000 }) }
    companion object {
        fun defaults() = listOf(Price(0, 5, 10, 5), Price(1, 10, 20, 10), Price(2, 20, 40, 20))
    }
}

data class Progress(val level: Long, val within: Long, val required: Long)
data class FinancialChange(val coins: Long, val xp: Long, val balance: Long, val totalXp: Long)

/** Potential entitlement, including inactive events whose first-entry price must be retained. */
data class FinancialOutcome(
    val key: String, val owner: String, val title: String, val date: LocalDate,
    val reward: Boolean, val category: Int, val active: Boolean,
)

data class Entitlement(
    val key: String, val owner: String, val title: String, val date: LocalDate,
    val coins: Long, val xp: Long, val active: Boolean, val exempt: Boolean,
    val lastEntry: Long? = null, val priced: Boolean = true, val reward: Boolean = true,
)

data class LedgerEntry(
    val sequence: Long = 0, val key: String, val title: String, val date: LocalDate, val time: Long,
    val kind: String, val nominalCoins: Long, val coins: Long, val xp: Long,
    val balance: Long, val totalXp: Long, val related: Long? = null,
)

data class AccountingSnapshot(
    val prices: List<Price> = Price.defaults(), val entries: List<LedgerEntry> = emptyList(),
    val entitlements: List<Entitlement> = emptyList(),
) {
    val balance get() = entries.lastOrNull()?.balance ?: 0L
    val totalXp get() = entries.lastOrNull()?.totalXp ?: 0L
    val progress get() = AccountingRules.progress(totalXp)
}

data class FinancialPreview(val entries: List<LedgerEntry>, val balance: Long, val totalXp: Long)
