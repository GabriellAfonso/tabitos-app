package com.ethos.app.core.rewards.domain

import kotlinx.coroutines.flow.Flow

interface RewardRepository {
    fun observe(): Flow<RewardSnapshot>
    suspend fun refresh()
    suspend fun save(reward: Reward)
    suspend fun move(id: String, direction: Int)
    suspend fun delete(id: String)
    suspend fun preview(id: String)
    /** Timed purchases return RUNNING_CONFLICT, uncharged, while another timer runs unless [pauseRunning]. */
    suspend fun confirm(expected: PurchaseIntent, pauseRunning: Boolean = false): PurchaseResult
    suspend fun close(intentId: String)
}
