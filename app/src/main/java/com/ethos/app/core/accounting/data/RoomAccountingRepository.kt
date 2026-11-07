package com.ethos.app.core.accounting.data

import androidx.room.withTransaction
import com.ethos.app.core.accounting.domain.*
import com.ethos.app.core.tasks.data.TaskDatabase
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.habits.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map

class RoomAccountingRepository(
    private val database: TaskDatabase, private val clock: TaskClock,
) : AccountingRepository {
    private val store = AccountingStore(database, clock)
    override fun observe(): Flow<AccountingSnapshot> = flow {
        database.withTransaction { store.initialize() }
        emitAll(database.invalidationTracker.createFlow("accounting_prices", "accounting_entitlements",
            "accounting_ledger", emitInitialState = true).map { database.withTransaction { store.snapshot() } })
    }

    override suspend fun savePrices(prices: List<Price>) {
        require(prices.size == 3 && prices.map { it.category }.toSet() == setOf(0, 1, 2))
        prices.forEach { it.validate() }
        database.withTransaction {
            store.initialize()
            database.accounting().prices(prices.map { PriceEntity(it.category, it.coins, it.xp, it.penalty) })
        }
    }

    override suspend fun previewTask(action: TaskAction): FinancialPreview = database.withTransaction {
        store.initialize()
        val before = store.snapshot()
        val source = TaskRules.apply(store.tasks(), action, clock.today(), clock.nowMillis())
        val id = when (action) {
            is TaskAction.Resolve -> action.id
            is TaskAction.Undo -> action.id
            else -> error("Not a financial task action")
        }
        val owner = "task:$id"
        val after = AccountingRules.reconcile(before,
            AccountingProjection.tasks(source).filter { it.owner == owner }, setOf(owner), clock.nowMillis())
        FinancialPreview(after.entries.drop(before.entries.size), after.balance, after.totalXp)
    }

    override suspend fun previewHabit(action: HabitAction): FinancialPreview = database.withTransaction {
        store.initialize()
        val before = store.snapshot()
        val old = store.habits()
        val source = HabitRules.apply(old, action, clock.today(), clock.nowMillis(),
            store.tasks().activeLists.map { it.id }.toSet())
        val id = when (action) {
            is HabitAction.Record -> action.habitId
            is HabitAction.Correct -> old.records.first { it.id == action.id }.habitId
            is HabitAction.Review -> source.audits.last().habitId
            is HabitAction.Save -> action.habit.id
            is HabitAction.Status -> action.id
        }
        val owner = "habit:$id"
        val after = AccountingRules.reconcile(before,
            store.habitOutcomes(source).filter { it.owner == owner },
            setOf(owner), clock.nowMillis(), AccountingProjection.practiceOrigins(old, source, action, clock.today()))
        FinancialPreview(after.entries.drop(before.entries.size), after.balance, after.totalXp)
    }
}
