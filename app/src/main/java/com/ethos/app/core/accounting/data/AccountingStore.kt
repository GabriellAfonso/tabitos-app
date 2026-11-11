package com.ethos.app.core.accounting.data

import com.ethos.app.core.accounting.domain.*
import com.ethos.app.core.tasks.data.*
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.habits.domain.*

/** Caller must hold the shared database transaction for activation and all source mutations. */
class AccountingStore(private val database: TaskDatabase, private val clock: TaskClock) {
    suspend fun tasks(): TaskSnapshot {
        val dao = database.tasks()
        val checks = dao.checks().groupBy { it.ownerId }
        return TaskSnapshot(dao.lists().map { it.domain() },
            dao.definitions().map { it.domain(checks["template:${it.id}"].orEmpty()) },
            dao.occurrences().map { it.domain(checks[it.id].orEmpty()) }, dao.history().map { it.domain() })
    }

    suspend fun habits(): HabitSnapshot {
        val dao = database.habits()
        return HabitSnapshot(dao.habits().map { it.value }, dao.rules().map { it.value },
            dao.intervals().map { it.value }, dao.overrides().map { it.value }, dao.records().map { it.value },
            dao.reviews().map { it.value }, dao.audits().map { it.value })
    }

    suspend fun initialize() {
        val dao = database.accounting()
        if (dao.activation() != null) return
        dao.prices(Price.defaults().map { PriceEntity(it.category, it.coins, it.xp, it.penalty) })
        val oldHabits = habits()
        val outcomes = AccountingProjection.tasks(tasks()) + AccountingProjection.habits(oldHabits, clock.today())
        dao.entitlements(outcomes.filter { it.active }.map { o -> EntitlementEntity(o.key,
            Entitlement(o.key, o.owner, o.title, o.date, 0, 0, active = true, exempt = true,
                priced = false, reward = o.reward)) })
        // Baseline record identities remain unpriced even if corrections move them to another day/limit slot.
        dao.entitlements(oldHabits.records.map { record ->
            val key = "baseline-record:${record.id}"
            EntitlementEntity(key, Entitlement(key, "baseline:habit:${record.habitId}", "", record.date,
                0, 0, active = false, exempt = true, priced = false))
        })
        dao.activate(ActivationEntity(time = clock.nowMillis()))
    }

    suspend fun snapshot(): AccountingSnapshot {
        val dao = database.accounting()
        return AccountingSnapshot(dao.prices().map { it.domain() }, dao.entries().map { it.value },
            dao.entitlements().map { it.value })
    }

    suspend fun habitOutcomes(source: HabitSnapshot): List<FinancialOutcome> {
        val baseline = snapshot().entitlements.filter { it.key.startsWith("baseline-record:") }
            .map { it.key.removePrefix("baseline-record:") }.toSet()
        return AccountingProjection.habits(source, clock.today()).filter { outcome ->
            when {
                outcome.key.startsWith("event:") -> outcome.key.removePrefix("event:") !in baseline
                outcome.key.startsWith("practice:") -> source.records.none {
                    it.id in baseline && !it.deleted && "habit:${it.habitId}" == outcome.owner &&
                        it.date == outcome.date
                }
                else -> true
            }
        }
    }

    suspend fun reconcile(
        outcomes: List<FinancialOutcome>, owners: Set<String>, priceOrigins: Map<String, String> = emptyMap(),
        scoring: Boolean,
    ) {
        val before = snapshot()
        val after = AccountingRules.reconcile(before, outcomes, owners, clock.nowMillis(), priceOrigins, scoring)
        val changed = after.entitlements.filter { value -> before.entitlements.none { it == value } }
        if (changed.isNotEmpty()) database.accounting().entitlements(changed.map { EntitlementEntity(it.key, it) })
        val added = after.entries.drop(before.entries.size)
        if (added.isNotEmpty()) database.accounting().append(added.map { LedgerEntity(it.sequence, it) })
    }
}
