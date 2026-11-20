package com.tabitos.app.core.accounting.domain

import kotlinx.coroutines.flow.Flow
import com.tabitos.app.core.tasks.domain.TaskAction
import com.tabitos.app.core.habits.domain.HabitAction

interface AccountingRepository {
    fun observe(): Flow<AccountingSnapshot>
    suspend fun savePrices(prices: List<Price>)
    suspend fun previewTask(action: TaskAction): FinancialPreview
    suspend fun previewHabit(action: HabitAction): FinancialPreview
}
