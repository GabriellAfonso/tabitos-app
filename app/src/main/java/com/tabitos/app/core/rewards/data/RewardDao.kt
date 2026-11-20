package com.tabitos.app.core.rewards.data

import androidx.room.*

@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards ORDER BY reward_position, id") suspend fun rewards(): List<RewardEntity>
    @Upsert suspend fun save(value: RewardEntity)
    @Query("SELECT * FROM reward_purchases ORDER BY ledgerSequence") suspend fun purchases(): List<PurchaseEntity>
    @Query("SELECT * FROM reward_purchases WHERE id = :id") suspend fun purchase(id: String): PurchaseEntity?
    @Insert suspend fun purchase(value: PurchaseEntity)
    @Query("SELECT * FROM reward_intents WHERE closed = 0 ORDER BY rowid DESC LIMIT 1")
    suspend fun pending(): PurchaseIntentEntity?
    @Query("SELECT * FROM reward_intents WHERE id = :id") suspend fun intent(id: String): PurchaseIntentEntity?
    @Upsert suspend fun intent(value: PurchaseIntentEntity)
    @Query("UPDATE reward_intents SET closed = 1 WHERE closed = 0") suspend fun closeAll()
    @Query("UPDATE reward_intents SET closed = 1 WHERE id = :id") suspend fun close(id: String)
}
