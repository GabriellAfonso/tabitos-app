package com.ethos.app.features.settings

import com.ethos.app.core.preferences.GameModeRepository
import com.ethos.app.core.timers.domain.StartResult
import com.ethos.app.core.timers.domain.TimerBounds
import com.ethos.app.core.timers.domain.TimerKind
import com.ethos.app.core.timers.domain.TimerRepository
import com.ethos.app.core.timers.domain.TimerSession
import com.ethos.app.core.timers.domain.TimerSnapshot
import com.ethos.app.core.timers.domain.TimerStart
import com.ethos.app.core.timers.domain.TimerState
import com.ethos.app.core.timers.domain.TimerSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameModeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val preference = FakeGameMode()
    private val timers = FakeTimers()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun turningOffAsksFirstAndCancelKeepsItOn() = runTest(dispatcher) {
        val vm = GameModeViewModel(preference, timers)
        advanceUntilIdle()
        vm.request(false)
        advanceUntilIdle()
        assertTrue(vm.state.value.confirmingOff)
        assertTrue(preference.enabled.value)
        vm.confirmOff(false)
        advanceUntilIdle()
        assertFalse(vm.state.value.confirmingOff)
        assertTrue(preference.enabled.value)
        vm.request(false)
        vm.confirmOff(true)
        advanceUntilIdle()
        assertFalse(preference.enabled.value)
        assertFalse(vm.state.value.enabled)
    }

    @Test fun turningOnAppliesWithoutDialog() = runTest(dispatcher) {
        preference.enabled.value = false
        val vm = GameModeViewModel(preference, timers)
        advanceUntilIdle()
        vm.request(true)
        advanceUntilIdle()
        assertFalse(vm.state.value.confirmingOff)
        assertTrue(preference.enabled.value)
    }

    @Test fun runningRewardTimerBlocksSwitchUntilPaused() = runTest(dispatcher) {
        val reward = TimerSession("s", TimerKind.REWARD, "p", "Série", TimerState.RUNNING, created = 0)
        timers.data.value = TimerSnapshot(sessions = listOf(reward))
        val vm = GameModeViewModel(preference, timers)
        advanceUntilIdle()
        assertFalse(vm.state.value.canSwitch)
        vm.request(false)
        advanceUntilIdle()
        assertFalse(vm.state.value.confirmingOff)
        timers.data.value = TimerSnapshot(sessions = listOf(reward.copy(state = TimerState.PAUSED)))
        advanceUntilIdle()
        assertTrue(vm.state.value.canSwitch)
    }

    @Test fun runningTaskTimerDoesNotBlock() = runTest(dispatcher) {
        timers.data.value = TimerSnapshot(sessions = listOf(
            TimerSession("s", TimerKind.TASK, "t", "Leitura", TimerState.RUNNING, created = 0)))
        val vm = GameModeViewModel(preference, timers)
        advanceUntilIdle()
        assertTrue(vm.state.value.canSwitch)
    }

    private class FakeGameMode : GameModeRepository {
        override val enabled = MutableStateFlow(true)
        override suspend fun setEnabled(enabled: Boolean) { this.enabled.value = enabled }
    }

    private class FakeTimers : TimerRepository {
        val data = MutableStateFlow(TimerSnapshot())
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
}
