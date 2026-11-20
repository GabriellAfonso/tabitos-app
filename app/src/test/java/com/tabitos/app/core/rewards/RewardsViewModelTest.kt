package com.tabitos.app.core.rewards

import androidx.lifecycle.SavedStateHandle
import com.tabitos.app.core.rewards.domain.*
import com.tabitos.app.features.rewards.*
import com.tabitos.app.core.timers.domain.*
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RewardsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun invalidAndFailedSaveRetainDraftAcrossRecreation() = runTest(dispatcher) {
        val repo = Fake()
        val saved = SavedStateHandle()
        val vm = RewardsViewModel(repo, saved)
        advanceUntilIdle()
        vm.edit()
        assertEquals(DEFAULT_REWARD_COST, vm.state.value.draft!!.cost)
        listOf("", "   ").forEach { name ->
            vm.draft(vm.state.value.draft!!.copy(name = name))
            vm.save()
            advanceUntilIdle()
            assertTrue(vm.state.value.invalid)
        }
        assertEquals(0, repo.writes)
        vm.draft(vm.state.value.draft!!.copy(name = " Pausa ", cost = 5))
        repo.failWrite = true
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.error)
        assertEquals(5L, vm.state.value.draft!!.cost)
        val restored = RewardsViewModel(repo, saved)
        advanceUntilIdle()
        assertEquals(" Pausa ", restored.state.value.draft!!.name)
        repo.failWrite = false
        vm.retry()
        advanceUntilIdle()
        assertNull(vm.state.value.draft)
        assertEquals(1, repo.writes)
    }

    @Test fun editingKeepsEveryFieldAndSavesTrimmedText() {
        val reward = Reward("r", "Série", "Um episódio", "MOVIE", 40, limit = 2, period = "WEEK", minutes = 45)
        val draft = RewardDraft.of(reward)
        assertTrue(draft.editing)
        assertEquals(reward, draft.toReward())
        assertEquals("Série", draft.copy(name = "  Série ").toReward().name)
    }

    @Test fun readErrorDoesNotBecomeZeroWalletAndRetryRecovers() = runTest(dispatcher) {
        val repo = Fake().also { it.failRead = true }
        val vm = RewardsViewModel(repo, SavedStateHandle())
        advanceUntilIdle()
        assertTrue(vm.state.value.error)
        repo.failRead = false
        vm.retry()
        advanceUntilIdle()
        assertFalse(vm.state.value.error)
        assertFalse(vm.state.value.loading)
    }

    @Test fun presentationDoesNotOverflowOrInventUnlimitedQuota() {
        val r = Reward("r", "Pausa", cost = Long.MAX_VALUE)
        val p = purchasePresentation(PurchaseIntent("p", r, LocalDate.of(2026, 1, 1), -300, 0))
        assertFalse(p.available)
        assertNull(p.balanceAfter)
        assertNull(p.allowanceAfter)
        val exact = purchasePresentation(PurchaseIntent("p", r.copy(cost = 5, limit = 2),
            LocalDate.of(2026, 1, 1), 5, 1))
        assertTrue(exact.available)
        assertEquals(0L, exact.balanceAfter)
        assertEquals(0, exact.allowanceAfter)
    }

    @Test fun timedPurchaseWithRunningTimerAsksBeforeChargingAndCancelChargesNothing() = runTest(dispatcher) {
        val reward = Reward("r", "Jogo", cost = 10, minutes = 120)
        val repo = Fake()
        repo.data.value = RewardSnapshot(rewards = listOf(reward),
            intent = PurchaseIntent("i", reward, LocalDate.of(2026, 11, 7), 50, 0))
        val running = TimerSession("s", TimerKind.TASK, "o", "Leitura", TimerState.RUNNING, created = 0)
        val timers = FakeTimers(MutableStateFlow(TimerSnapshot(sessions = listOf(running))))
        val vm = RewardsViewModel(repo, SavedStateHandle(), timers)
        advanceUntilIdle()
        vm.confirm()
        advanceUntilIdle()
        assertEquals("Leitura", vm.state.value.pauseRequired)
        assertTrue(repo.confirms.isEmpty())
        vm.cancelPause()
        assertNull(vm.state.value.pauseRequired)
        assertTrue(repo.confirms.isEmpty())
        vm.confirm()
        advanceUntilIdle()
        vm.confirm(true)
        advanceUntilIdle()
        assertEquals(listOf(true), repo.confirms)
        // Non-timed rewards keep point-6 behavior even while a timer runs.
        repo.data.value = repo.data.value.copy(intent = PurchaseIntent("j", reward.copy(minutes = null),
            LocalDate.of(2026, 11, 7), 50, 0))
        advanceUntilIdle()
        vm.confirm()
        advanceUntilIdle()
        assertEquals(listOf(true, false), repo.confirms)
    }

    @Test fun runningTimerIsKnownBeforeConfirmAndAnotherPriceReplacesTheOpenStrip() = runTest(dispatcher) {
        val reward = Reward("r", "Jogo", cost = 10, minutes = 120)
        val repo = Fake()
        repo.data.value = RewardSnapshot(rewards = listOf(reward),
            intent = PurchaseIntent("i", reward, LocalDate.of(2026, 11, 7), 50, 0))
        val running = TimerSession("s", TimerKind.TASK, "o", "Leitura", TimerState.RUNNING, created = 0)
        val timers = FakeTimers(MutableStateFlow(TimerSnapshot(sessions = listOf(running))))
        val vm = RewardsViewModel(repo, SavedStateHandle(), timers)
        advanceUntilIdle()
        assertEquals("Leitura", vm.state.value.runningTimer)
        timers.data.value = TimerSnapshot()
        advanceUntilIdle()
        assertNull(vm.state.value.runningTimer)
        vm.preview("other")
        advanceUntilIdle()
        assertEquals(listOf("i"), repo.closes)
        assertEquals(listOf("other"), repo.previews)
    }

    private class FakeTimers(val data: MutableStateFlow<TimerSnapshot>) : TimerRepository {
        override fun observe() = data
        override suspend fun start(request: TimerStart, pauseOther: Boolean) = StartResult.STARTED
        override suspend fun pause(sessionId: String) { }
        override suspend fun finish(sessionId: String) { }
        override suspend fun confirmed(sessionId: String) { }
        override suspend fun sync(closedSources: Set<Pair<TimerKind, String>>) = TimerSync(null, null, emptyList())
        override suspend fun checkpoint() { }
        override suspend fun saveGoal(sourceKey: String, minutes: Int?) { }
        override suspend fun addManual(request: TimerStart, start: Long, end: Long, bounds: TimerBounds) { }
        override suspend fun correct(segmentId: String, start: Long, end: Long, bounds: TimerBounds) { }
        override suspend fun delete(segmentId: String) { }
        override suspend fun discardLatest(sessionId: String) = Unit
    }

    private class Fake : RewardRepository {
        val data = MutableStateFlow(RewardSnapshot())
        var failRead = false
        var failWrite = false
        var writes = 0
        override fun observe() = flow {
            if (failRead) throw IOException("read")
            emitAll(data)
        }
        override suspend fun save(reward: Reward) {
            if (failWrite) throw IOException("write")
            writes++
            data.value = data.value.copy(rewards = listOf(reward))
        }
        override suspend fun refresh() { }
        override suspend fun order(ids: List<String>) { }
        override suspend fun delete(id: String) { }
        val previews = mutableListOf<String>()
        override suspend fun preview(id: String) { previews += id }
        var confirms = mutableListOf<Boolean>()
        override suspend fun confirm(expected: PurchaseIntent, pauseRunning: Boolean): PurchaseResult {
            confirms += pauseRunning
            return PurchaseResult.PURCHASED
        }
        val closes = mutableListOf<String>()
        override suspend fun close(intentId: String) { closes += intentId }
    }
}
