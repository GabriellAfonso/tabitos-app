package com.tabitos.app.core.rewards.data

import androidx.room.*
import com.tabitos.app.core.rewards.domain.*
import java.time.LocalDate

@Entity(tableName = "rewards")
data class RewardEntity(@PrimaryKey val id: String, @Embedded(prefix = "reward_") val value: Reward)

@Entity(tableName = "reward_intents")
data class PurchaseIntentEntity(
    @PrimaryKey val id: String, @Embedded(prefix = "reward_") val reward: Reward,
    val date: LocalDate, val balance: Long, val used: Int, val closed: Boolean,
) {
    fun domain() = PurchaseIntent(id, reward, date, balance, used, closed)
}

@Entity(tableName = "reward_purchases", indices = [Index(value = ["ledgerSequence"], unique = true)])
data class PurchaseEntity(
    @PrimaryKey val id: String, @Embedded(prefix = "reward_") val reward: Reward,
    val date: LocalDate, val time: Long, val ledgerSequence: Long,
) {
    fun domain() = Purchase(id, reward, date, time, ledgerSequence)
}

fun PurchaseIntent.entity() = PurchaseIntentEntity(id, reward, date, balance, used, closed)
