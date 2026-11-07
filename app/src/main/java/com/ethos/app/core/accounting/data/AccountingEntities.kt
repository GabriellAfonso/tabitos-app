package com.ethos.app.core.accounting.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ethos.app.core.accounting.domain.*

@Entity(tableName = "accounting_activation")
data class ActivationEntity(@PrimaryKey val id: Int = 1, val time: Long)

@Entity(tableName = "accounting_prices")
data class PriceEntity(@PrimaryKey val category: Int, val coins: Long, val xp: Long, val penalty: Long) {
    fun domain() = Price(category, coins, xp, penalty)
}

@Entity(tableName = "accounting_entitlements")
data class EntitlementEntity(@PrimaryKey val id: String, @Embedded val value: Entitlement)

@Entity(tableName = "accounting_ledger")
data class LedgerEntity(@PrimaryKey val id: Long, @Embedded val value: LedgerEntry)
