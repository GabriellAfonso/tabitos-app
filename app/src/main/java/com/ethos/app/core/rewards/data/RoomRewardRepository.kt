package com.ethos.app.core.rewards.data

import androidx.room.withTransaction
import com.ethos.app.core.accounting.data.AccountingStore
import com.ethos.app.core.accounting.data.LedgerEntity
import com.ethos.app.core.accounting.domain.LedgerEntry
import com.ethos.app.core.rewards.domain.*
import com.ethos.app.core.tasks.data.TaskDatabase
import com.ethos.app.core.tasks.domain.TaskClock
import java.util.UUID
import kotlinx.coroutines.flow.*

class RoomRewardRepository(private val database: TaskDatabase, private val clock: TaskClock) : RewardRepository {
    private val dao get() = database.rewards()
    private val store = AccountingStore(database, clock)
    private val refreshes = MutableStateFlow(0L)

    override fun observe(): Flow<RewardSnapshot> = flow {
        database.withTransaction { store.initialize() }
        emitAll(combine(database.invalidationTracker.createFlow("rewards", "reward_intents",
            "reward_purchases", "accounting_ledger", emitInitialState = true), refreshes) { _, _ ->
            database.withTransaction {
                RewardSnapshot(dao.rewards().map { it.value }.filter { !it.deleted },
                    dao.purchases().map { it.domain() }, store.snapshot(), dao.pending()?.domain(), clock.today())
            }
        })
    }

    override suspend fun refresh() { refreshes.value++ }

    override suspend fun save(reward: Reward) {
        RewardRules.validate(reward)
        database.withTransaction {
            val all = dao.rewards().map { it.value }
            val old = all.firstOrNull { it.id == reward.id }
            require(old?.deleted != true)
            val value = reward.copy(name = reward.name.trim(), description = reward.description.trim(),
                position = old?.position ?: Math.addExact(all.maxOfOrNull { it.position } ?: -1, 1),
                revision = Math.addExact(old?.revision ?: -1, 1), deleted = false)
            dao.save(RewardEntity(value.id, value))
        }
    }

    override suspend fun move(id: String, direction: Int) {
        require(direction == -1 || direction == 1)
        database.withTransaction {
            val all = dao.rewards().map { it.value }.filter { !it.deleted }
            val index = all.indexOfFirst { it.id == id }
            if (index < 0) return@withTransaction
            val target = all.getOrNull(index + direction) ?: return@withTransaction
            val current = all[index]
            dao.save(RewardEntity(id, current.copy(position = target.position)))
            dao.save(RewardEntity(target.id, target.copy(position = current.position)))
        }
    }

    override suspend fun delete(id: String) {
        database.withTransaction {
            val old = dao.rewards().firstOrNull { it.id == id }?.value ?: return@withTransaction
            dao.save(RewardEntity(id, old.copy(deleted = true, revision = Math.addExact(old.revision, 1))))
        }
    }

    override suspend fun preview(id: String) {
        database.withTransaction {
            store.initialize()
            // Never hide an uncertain committed result until the owner acknowledges it.
            if (dao.pending() != null) return@withTransaction
            val reward = dao.rewards().first { it.id == id && !it.value.deleted }.value
            val balance = store.snapshot().balance
            val used = RewardRules.used(reward, dao.purchases().map { it.domain() }, clock.today())
            dao.intent(PurchaseIntent(UUID.randomUUID().toString(), reward, clock.today(), balance, used).entity())
        }
    }

    override suspend fun confirm(expected: PurchaseIntent): PurchaseResult = database.withTransaction {
        val intentId = expected.id
        store.initialize()
        if (dao.purchase(intentId) != null) return@withTransaction PurchaseResult.PURCHASED
        val intent = dao.intent(intentId)?.domain() ?: return@withTransaction PurchaseResult.UNAVAILABLE
        if (intent.closed) return@withTransaction PurchaseResult.UNAVAILABLE
        // A second rapid tap may still carry the old rendered preview after a stale refresh.
        if (intent != expected) return@withTransaction PurchaseResult.STALE
        val reward = dao.rewards().firstOrNull { it.id == intent.reward.id }?.value
            ?: return@withTransaction PurchaseResult.UNAVAILABLE
        val wallet = store.snapshot()
        val today = clock.today()
        val used = RewardRules.used(reward, dao.purchases().map { it.domain() }, today)
        val current = intent.copy(reward = reward, date = today, balance = wallet.balance, used = used)
        if (reward.deleted) {
            dao.intent(current.entity())
            return@withTransaction PurchaseResult.UNAVAILABLE
        }
        if (current != intent) {
            dao.intent(current.entity())
            return@withTransaction PurchaseResult.STALE
        }
        if (!RewardRules.available(reward, wallet.balance, used)) return@withTransaction PurchaseResult.UNAVAILABLE
        RewardRules.validate(reward)
        val sequence = Math.addExact(wallet.entries.lastOrNull()?.sequence ?: 0, 1)
        val time = clock.nowMillis()
        val entry = LedgerEntry(sequence, "purchase:$intentId", reward.name, today, time, "PURCHASE",
            -reward.cost, -reward.cost, 0, Math.subtractExact(wallet.balance, reward.cost), wallet.totalXp)
        database.accounting().append(listOf(LedgerEntity(sequence, entry)))
        dao.purchase(PurchaseEntity(intentId, reward, today, time, sequence))
        PurchaseResult.PURCHASED
    }

    override suspend fun close(intentId: String) { database.withTransaction { dao.close(intentId) } }
}
