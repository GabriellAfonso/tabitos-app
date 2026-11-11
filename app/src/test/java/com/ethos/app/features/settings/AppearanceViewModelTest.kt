package com.ethos.app.features.settings

import com.ethos.app.core.preferences.AppearanceMode
import com.ethos.app.core.preferences.AppearanceRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun readFailureRemainsVisibleUntilSuccessfulRetry() = runTest(dispatcher) {
        val repository = FakeRepository().apply { failRead = true }
        val model = AppearanceViewModel(repository)
        advanceUntilIdle()
        assertEquals(AppearanceError.READ, model.state.value.error)
        assertEquals(AppearanceMode.SYSTEM, model.state.value.mode)
        repository.failRead = false
        repository.saved.value = AppearanceMode.DARK
        model.retry()
        advanceUntilIdle()
        assertEquals(AppearanceMode.DARK, model.state.value.mode)
        assertNull(model.state.value.error)
    }

    @Test fun failedWriteKeepsSavedSelectionAndRetryCommitsAttempt() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = AppearanceViewModel(repository)
        advanceUntilIdle()
        repository.failWrite = true
        model.select(AppearanceMode.LIGHT)
        advanceUntilIdle()
        assertEquals(AppearanceMode.SYSTEM, model.state.value.mode)
        assertEquals(AppearanceError.WRITE, model.state.value.error)
        repository.failWrite = false
        model.retry()
        advanceUntilIdle()
        assertEquals(AppearanceMode.LIGHT, model.state.value.mode)
        assertEquals(AppearanceMode.LIGHT, repository.saved.value)
        assertNull(model.state.value.error)
    }

    @Test fun selectionIsBlockedUntilReadRecovery() = runTest(dispatcher) {
        val repository = FakeRepository().apply { failRead = true }
        val model = AppearanceViewModel(repository)
        advanceUntilIdle()
        model.select(AppearanceMode.DARK)
        advanceUntilIdle()
        assertEquals(AppearanceMode.SYSTEM, repository.saved.value)
        assertEquals(AppearanceError.READ, model.state.value.error)
    }

    private class FakeRepository : AppearanceRepository {
        val saved = MutableStateFlow(AppearanceMode.SYSTEM)
        var failRead = false
        var failWrite = false
        override val modes = flow {
            if (failRead) throw IOException("read")
            saved.collect { emit(it) }
        }
        override suspend fun setMode(mode: AppearanceMode) {
            if (failWrite) throw IOException("write")
            saved.value = mode
        }
        override val timerAlarm = MutableStateFlow(true)
        override suspend fun setTimerAlarm(enabled: Boolean) { timerAlarm.value = enabled }
    }
}
