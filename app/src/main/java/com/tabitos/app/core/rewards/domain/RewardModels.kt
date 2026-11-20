package com.tabitos.app.core.rewards.domain

import com.tabitos.app.core.accounting.domain.AccountingSnapshot
import java.time.LocalDate

data class Reward(
    val id: String, val name: String, val description: String = "", val icon: String = "GIFT",
    val cost: Long, val limit: Int? = null, val period: String = "DAY",
    val position: Long = 0, val revision: Long = 0, val deleted: Boolean = false,
    /** Optional purchased duration; null keeps the immediate non-timed behavior. */
    val minutes: Int? = null,
)

data class PurchaseIntent(
    val id: String, val reward: Reward, val date: LocalDate, val balance: Long, val used: Int,
    val closed: Boolean = false,
)

data class Purchase(
    val id: String, val reward: Reward, val date: LocalDate, val time: Long, val ledgerSequence: Long,
)

data class RewardSnapshot(
    val rewards: List<Reward> = emptyList(), val purchases: List<Purchase> = emptyList(),
    val wallet: AccountingSnapshot = AccountingSnapshot(), val intent: PurchaseIntent? = null,
    val today: LocalDate = LocalDate.of(1970, 1, 1),
)

enum class PurchaseResult { PURCHASED, STALE, UNAVAILABLE, RUNNING_CONFLICT }
