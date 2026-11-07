package com.ethos.app.features.rewards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethos.app.core.rewards.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class RewardCard(val reward: Reward, val used: Int, val available: Boolean)
data class PurchasePresentation(
    val available: Boolean, val exhausted: Boolean, val balanceAfter: Long?, val allowanceAfter: Int?,
)

fun purchasePresentation(intent: PurchaseIntent): PurchasePresentation {
    val remaining = intent.reward.limit?.let { maxOf(0, it - intent.used) }
    return PurchasePresentation(RewardRules.available(intent.reward, intent.balance, intent.used),
        remaining == 0, if (intent.balance >= intent.reward.cost) intent.balance - intent.reward.cost else null,
        remaining?.let { maxOf(0, it - 1) })
}
data class RewardsUiState(
    val snapshot: RewardSnapshot = RewardSnapshot(), val cards: List<RewardCard> = emptyList(),
    val loading: Boolean = true, val error: Boolean = false, val busy: Boolean = false,
    val invalid: Boolean = false, val stale: Boolean = false, val unavailable: Boolean = false,
    val draft: List<String>? = null, val deleteId: String? = null,
)

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val repository: RewardRepository, private val saved: SavedStateHandle,
) : ViewModel() {
    private val mutable = MutableStateFlow(RewardsUiState(
        draft = saved.get<ArrayList<String>>("rewardDraft"), deleteId = saved["rewardDelete"]))
    val state = mutable.asStateFlow()
    private var observer: Job? = null
    private var failed: (() -> Unit)? = null
    private var readFailed = false
    init { observe() }

    private fun observe() {
        observer?.cancel()
        observer = viewModelScope.launch {
            try {
                repository.observe().collect { snapshot ->
                    readFailed = false
                    val cards = snapshot.rewards.map { reward ->
                        val used = RewardRules.used(reward, snapshot.purchases, snapshot.today)
                        RewardCard(reward, used, RewardRules.available(reward, snapshot.wallet.balance, used))
                    }
                    mutable.update { it.copy(snapshot = snapshot, cards = cards, loading = false,
                        error = failed != null) }
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                readFailed = true
                mutable.update { it.copy(loading = false, error = true) }
            }
        }
    }

    fun edit(reward: Reward? = null) {
        draft(reward?.let { listOf(it.id, it.name, it.description, it.icon, it.cost.toString(),
            it.limit?.toString().orEmpty(), it.period) }
            ?: listOf(UUID.randomUUID().toString(), "", "", "GIFT", "", "", "DAY"))
    }

    fun draft(value: List<String>?) {
        if (state.value.busy) return
        saved["rewardDraft"] = value?.let { ArrayList(it) }
        mutable.update { it.copy(draft = value, invalid = false) }
    }

    fun save() {
        val fields = state.value.draft ?: return
        val reward = try {
            require(fields.size == 7 && fields[4].isNotEmpty() && fields[4].all(Char::isDigit))
            require(fields[5].isEmpty() || fields[5].all(Char::isDigit))
            Reward(fields[0], fields[1], fields[2], fields[3], fields[4].toLong(),
                fields[5].takeIf { it.isNotEmpty() }?.toInt(), fields[6]).also(RewardRules::validate)
        } catch (_: IllegalArgumentException) {
            mutable.update { it.copy(invalid = true) }; return
        }
        write(::save) {
            repository.save(reward)
            saved["rewardDraft"] = null
            mutable.update { it.copy(draft = null) }
        }
    }

    fun deletion(id: String?) {
        if (state.value.busy) return
        saved["rewardDelete"] = id
        mutable.update { it.copy(deleteId = id) }
    }
    fun delete() {
        val id = state.value.deleteId ?: return
        write(::delete) {
            repository.delete(id)
            saved["rewardDelete"] = null
            mutable.update { it.copy(deleteId = null) }
        }
    }
    fun move(id: String, direction: Int) { write({ move(id, direction) }) { repository.move(id, direction) } }
    fun preview(id: String) {
        write({ preview(id) }) {
            repository.preview(id)
            mutable.update { it.copy(stale = false, unavailable = false) }
        }
    }
    fun confirm() {
        val intent = state.value.snapshot.intent ?: return
        write(::confirm) {
            val result = repository.confirm(intent)
            mutable.update { it.copy(stale = result == PurchaseResult.STALE,
                unavailable = result == PurchaseResult.UNAVAILABLE) }
        }
    }
    fun close() {
        val id = state.value.snapshot.intent?.id ?: return
        write(::close) {
            repository.close(id)
            mutable.update { it.copy(stale = false, unavailable = false) }
        }
    }

    private fun write(retry: () -> Unit, operation: suspend () -> Unit) {
        if (state.value.busy || readFailed || state.value.loading) return
        mutable.update { it.copy(busy = true, error = false) }
        viewModelScope.launch {
            try {
                operation()
                failed = null
                mutable.update { it.copy(busy = false, error = readFailed) }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                failed = retry
                mutable.update { it.copy(busy = false, error = true) }
            }
        }
    }
    fun retry() { observe(); failed?.invoke() }
    fun refresh() { viewModelScope.launch { repository.refresh() } }
}
