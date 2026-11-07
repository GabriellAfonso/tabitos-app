package com.ethos.app.core.rewards.domain

import kotlinx.coroutines.flow.Flow

interface RewardRepository {
    fun observe(): Flow<RewardSnapshot>
    suspend fun refresh()
    suspend fun save(reward: Reward)
    suspend fun move(id: String, direction: Int)
    suspend fun delete(id: String)
    suspend fun preview(id: String)
    suspend fun confirm(expected: PurchaseIntent): PurchaseResult
    suspend fun close(intentId: String)
}
