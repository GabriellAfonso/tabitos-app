package com.ethos.app.core.accounting.data

import androidx.room.*

@Dao
interface AccountingDao {
    @Query("SELECT * FROM accounting_activation WHERE id = 1") suspend fun activation(): ActivationEntity?
    @Insert suspend fun activate(value: ActivationEntity)
    @Query("SELECT * FROM accounting_prices ORDER BY category") suspend fun prices(): List<PriceEntity>
    @Upsert suspend fun prices(values: List<PriceEntity>)
    @Query("SELECT * FROM accounting_entitlements ORDER BY id") suspend fun entitlements(): List<EntitlementEntity>
    @Upsert suspend fun entitlements(values: List<EntitlementEntity>)
    @Query("SELECT * FROM accounting_ledger ORDER BY id") suspend fun entries(): List<LedgerEntity>
    @Insert suspend fun append(values: List<LedgerEntity>)
}
