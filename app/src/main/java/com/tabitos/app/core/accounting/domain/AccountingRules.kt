package com.tabitos.app.core.accounting.domain

import java.math.BigInteger

object AccountingRules {
    private fun threshold(level: Long): BigInteger = BigInteger.valueOf(level - 1)
        .multiply(BigInteger.valueOf(level + 2)).multiply(BigInteger.valueOf(25))

    fun progress(xp: Long): Progress {
        require(xp >= 0)
        val value = BigInteger.valueOf(xp)
        var low = 1L
        var high = 1_000_000_000L // Above the highest reachable level for a Long XP total.
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (threshold(mid) <= value) low = mid else high = mid - 1
        }
        // Binary search guarantees threshold(low) <= xp <= Long.MAX_VALUE.
        val start = threshold(low).toLong()
        return Progress(low, xp - start, Math.addExact(100, Math.multiplyExact(50, low - 1)))
    }

    fun change(balance: Long, totalXp: Long, coins: Long, xp: Long): FinancialChange {
        val result = maxOf(-300L, Math.addExact(balance, coins))
        val nextXp = Math.addExact(totalXp, xp)
        require(nextXp >= 0)
        return FinancialChange(result - balance, xp, result, nextXp)
    }

    /**
     * Pure reconciliation; the persistence adapter assigns monotonically increasing entry sequences.
     * With [scoring] off (game mode off, spec 017) newly valid outcomes become exempt without a ledger entry, so
     * they never pay — not even after scoring returns — while removing an outcome still reverses what it paid.
     */
    fun reconcile(
        snapshot: AccountingSnapshot, outcomes: List<FinancialOutcome>, owners: Set<String>, now: Long,
        priceOrigins: Map<String, String> = emptyMap(), scoring: Boolean = true,
    ): AccountingSnapshot {
        val desired = outcomes.associateBy { it.key }
        val subjects = snapshot.entitlements.associateBy { it.key }.toMutableMap()
        val ledger = snapshot.entries.toMutableList()
        fun append(subject: Entitlement, active: Boolean): Entitlement {
            val last = subject.lastEntry?.let { id -> ledger.first { it.sequence == id } }
            if (subject.exempt) {
                // Baseline outcome is released only by an explicit new source transition.
                return subject.copy(active = active, exempt = active)
            }
            val nominal = when {
                !active -> -(last?.coins ?: 0L)
                subject.reward && last?.kind == "REVERSAL" -> -last.coins
                else -> subject.coins
            }
            val xp = if (active) subject.xp else -(last?.xp ?: 0L)
            val effect = change(ledger.lastOrNull()?.balance ?: 0, ledger.lastOrNull()?.totalXp ?: 0, nominal, xp)
            val id = Math.addExact(ledger.lastOrNull()?.sequence ?: 0, 1)
            ledger += LedgerEntry(id, subject.key, subject.title, subject.date, now,
                if (!active) "REVERSAL" else if (last != null) "REAPPLY"
                else if (!subject.reward) "PENALTY" else "GAIN",
                nominal, effect.coins, effect.xp, effect.balance, effect.totalXp, last?.sequence)
            return subject.copy(active = active, lastEntry = id)
        }
        // Always remove obsolete accounting before applying newly valid outcomes.
        subjects.values.filter { it.owner in owners && it.active && desired[it.key]?.active != true }
            .sortedBy { it.key }.forEach { subjects[it.key] = append(it, false) }
        outcomes.sortedBy { it.key }.forEach { o ->
            var subject = subjects[o.key]
            val originKey = priceOrigins[o.key]
            val origin = originKey?.let { subjects[it] }
            val movesPricedDay = o.active && subject?.active != true && origin != null
            if (movesPricedDay) {
                // Moving a unique practice day is a correction of its original award, not a new price.
                val wasExempt = snapshot.entitlements.first { it.key == originKey }.exempt
                subject = origin.copy(key = o.key, owner = o.owner, title = o.title, date = o.date,
                    active = false, exempt = wasExempt)
            }
            if (subject == null) {
                if (!o.active) return@forEach
                val price = snapshot.prices.first { it.category == o.category }
                subject = Entitlement(o.key, o.owner, o.title, o.date,
                    if (o.reward) price.coins else -price.penalty, if (o.reward) price.xp else 0,
                    active = false, exempt = false, reward = o.reward)
            } else if (subject.exempt && !subject.active) subject = subject.copy(exempt = false)
            // A released baseline is priced only on its first new financial application.
            if (o.active && !subject.active && !subject.priced && !subject.exempt) {
                val price = snapshot.prices.first { it.category == o.category }
                subject = subject.copy(coins = if (o.reward) price.coins else -price.penalty,
                    xp = if (o.reward) price.xp else 0, priced = true)
            }
            if (o.active && subject.active && subject.date != o.date && !subject.exempt) {
                // A date-only event correction keeps its applied charge and adds an attributable zero delta.
                val id = Math.addExact(ledger.lastOrNull()?.sequence ?: 0, 1)
                ledger += LedgerEntry(id, subject.key, o.title, o.date, now, "CORRECTION", 0, 0, 0,
                    ledger.lastOrNull()?.balance ?: 0, ledger.lastOrNull()?.totalXp ?: 0, subject.lastEntry)
                subject = subject.copy(title = o.title, date = o.date)
            }
            if (o.active && !subject.active) subject = subject.copy(title = o.title, date = o.date)
            if (o.active && !subject.active) {
                // A moved practice day keeps its original award even with scoring off: it is a correction.
                subject = if (scoring || movesPricedDay) append(subject, true)
                else subject.copy(active = true, exempt = true)
            }
            subjects[o.key] = subject
        }
        return snapshot.copy(entries = ledger, entitlements = subjects.values.toList())
    }
}
