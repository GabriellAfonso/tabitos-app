package com.tabitos.app.features.rewards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabitos.app.core.icons.AppIcon
import com.tabitos.app.core.startup.DeferredSource
import com.tabitos.app.core.startup.StartupSequence
import com.tabitos.app.core.rewards.domain.*
import com.tabitos.app.core.timers.domain.TimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Store row: [missing] coins still needed and [exhausted] say why an unavailable reward cannot be bought. */
data class RewardCard(
    val reward: Reward, val used: Int, val available: Boolean, val missing: Long = 0, val exhausted: Boolean = false,
)

/** Reward editor fields; numbers come from steppers and choice sheets, so only the name can be left invalid. */
data class RewardDraft(
    val id: String, val name: String = "", val description: String = "", val icon: String = AppIcon.GIFT.name,
    val cost: Long = DEFAULT_REWARD_COST, val limit: Int? = null, val period: String = RewardRules.periods.first(),
    val minutes: Int? = null, val editing: Boolean = false,
) : java.io.Serializable {
    fun toReward() = Reward(id, name.trim(), description.trim(), icon, cost, limit, period, minutes = minutes)

    companion object {
        fun of(reward: Reward) = RewardDraft(reward.id, reward.name, reward.description, reward.icon, reward.cost,
            reward.limit, reward.period, reward.minutes, editing = true)
    }
}

const val DEFAULT_REWARD_COST = 20L

fun rewardCard(reward: Reward, purchases: List<Purchase>, balance: Long, today: LocalDate): RewardCard {
    val used = RewardRules.used(reward, purchases, today)
    return RewardCard(reward, used, RewardRules.available(reward, balance, used),
        missing = maxOf(0, reward.cost - balance), exhausted = reward.limit != null && used >= reward.limit)
}
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
    val draft: RewardDraft? = null, val deleteId: String? = null,
    /** Title of the running timer that must be paused, with consent, before a timed purchase is charged. */
    val pauseRequired: String? = null,
    /** Title of the timer counting now, so the purchase strip can say up front that it will be paused. */
    val runningTimer: String? = null,
)

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val repository: RewardRepository, private val saved: SavedStateHandle,
    private val timers: TimerRepository? = null,
    private val startup: StartupSequence = StartupSequence.released(),
) : ViewModel() {
    private val mutable = MutableStateFlow(RewardsUiState(
        draft = saved[DRAFT_KEY],
        deleteId = saved["rewardDelete"]))
    val state = mutable.asStateFlow()
    private var observer: Job? = null
    private var failed: (() -> Unit)? = null
    private var readFailed = false
    /** Hoje loads first; rewards start in their preload turn or when their page opens (spec 027). */
    init {
        viewModelScope.launch {
            startup.awaitTurn(DeferredSource.REWARDS)
            observe()
            timers?.let { observeRunning(it) }
        }
    }

    private suspend fun observeRunning(timers: TimerRepository) {
        try {
            timers.observe().map { it.running?.title }.distinctUntilChanged()
                .collect { title -> mutable.update { it.copy(runningTimer = title) } }
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) {
            // Without the notice the confirm still asks for consent through pauseRequired.
            mutable.update { it.copy(runningTimer = null) }
        }
    }

    private fun observe() {
        observer?.cancel()
        observer = viewModelScope.launch {
            try {
                repository.observe().collect { snapshot ->
                    readFailed = false
                    val cards = snapshot.rewards.map {
                        rewardCard(it, snapshot.purchases, snapshot.wallet.balance, snapshot.today)
                    }
                    mutable.update { it.copy(snapshot = snapshot, cards = cards, loading = false,
                        error = failed != null) }
                    startup.markLoaded(DeferredSource.REWARDS)
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                readFailed = true
                mutable.update { it.copy(loading = false, error = true) }
                startup.markLoaded(DeferredSource.REWARDS)
            }
        }
    }

    fun edit(reward: Reward? = null) {
        draft(reward?.let(RewardDraft::of) ?: RewardDraft(UUID.randomUUID().toString()))
    }

    fun draft(value: RewardDraft?) {
        if (state.value.busy) return
        saved[DRAFT_KEY] = value
        mutable.update { it.copy(draft = value, invalid = false) }
    }

    fun save() {
        val reward = state.value.draft?.toReward() ?: return
        if (runCatching { RewardRules.validate(reward) }.isFailure) {
            mutable.update { it.copy(invalid = true) }; return
        }
        write(::save) {
            repository.save(reward)
            saved[DRAFT_KEY] = null
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
            // An open purchase strip of the removed reward would have no row left to show it.
            state.value.snapshot.intent?.takeIf { it.reward.id == id }?.let { repository.close(it.id) }
            saved["rewardDelete"] = null
            mutable.update { it.copy(deleteId = null) }
        }
    }
    fun order(ids: List<String>) { write({ order(ids) }) { repository.order(ids) } }
    fun preview(id: String) {
        write({ preview(id) }) {
            // Tapping another price replaces the open strip; a shown receipt counts as acknowledged.
            state.value.snapshot.intent?.let { repository.close(it.id) }
            repository.preview(id)
            mutable.update { it.copy(stale = false, unavailable = false) }
        }
    }
    fun confirm() = confirm(false)

    /** A timed purchase while another timer runs first asks consent; [pause] carries that consent. */
    fun confirm(pause: Boolean) {
        val intent = state.value.snapshot.intent ?: return
        write({ confirm(pause) }) {
            if (!pause && intent.reward.minutes != null) {
                val running = timers?.observe()?.first()?.running
                if (running != null) {
                    mutable.update { it.copy(pauseRequired = running.title) }
                    return@write
                }
            }
            val result = repository.confirm(intent, pause)
            val conflict = if (result == PurchaseResult.RUNNING_CONFLICT)
                timers?.observe()?.first()?.running?.title.orEmpty() else null
            mutable.update { it.copy(stale = result == PurchaseResult.STALE,
                unavailable = result == PurchaseResult.UNAVAILABLE, pauseRequired = conflict) }
        }
    }
    fun cancelPause() { mutable.update { it.copy(pauseRequired = null) } }
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

private const val DRAFT_KEY = "rewardEditor"
