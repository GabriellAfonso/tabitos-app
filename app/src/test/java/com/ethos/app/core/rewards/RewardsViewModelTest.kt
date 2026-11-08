package com.ethos.app.core.rewards

import androidx.lifecycle.SavedStateHandle
import com.ethos.app.core.rewards.domain.*
import com.ethos.app.features.rewards.*
import com.ethos.app.core.timers.domain.*
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
        listOf("", "0", "-1", "1.5", "9223372036854775808").forEach { cost ->
            vm.draft(vm.state.value.draft!!.toMutableList().also { it[1] = "Pausa"; it[4] = cost })
            vm.save()
            advanceUntilIdle()
            assertTrue(vm.state.value.invalid)
        }
        assertEquals(0, repo.writes)
        vm.draft(vm.state.value.draft!!.toMutableList().also { it[4] = "5" })
        repo.failWrite = true
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.error)
        assertEquals("5", vm.state.value.draft!![4])
        val restored = RewardsViewModel(repo, saved)
        advanceUntilIdle()
        assertEquals("Pausa", restored.state.value.draft!![1])
        repo.failWrite = false
        vm.retry()
        advanceUntilIdle()
        assertNull(vm.state.value.draft)
        assertEquals(1, repo.writes)
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
        override suspend fun move(id: String, direction: Int) { }
        override suspend fun delete(id: String) { }
        override suspend fun preview(id: String) { }
        var confirms = mutableListOf<Boolean>()
        override suspend fun confirm(expected: PurchaseIntent, pauseRunning: Boolean): PurchaseResult {
            confirms += pauseRunning
            return PurchaseResult.PURCHASED
        }
        override suspend fun close(intentId: String) { }
    }
}
