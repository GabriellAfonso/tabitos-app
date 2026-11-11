package com.ethos.app.features.settings

import com.ethos.app.core.demo.domain.DemoRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun cancelNeverWritesAndConfirmedResetCannotRunTwice() = runTest(dispatcher) {
        var writes = 0
        val vm = DemoViewModel(object : DemoRepository { override suspend fun restore() { writes++ } })
        vm.restore()
        vm.ask(); vm.cancel(); vm.restore()
        advanceUntilIdle()
        assertEquals(0, writes)
        vm.ask(); vm.restore(); vm.restore()
        advanceUntilIdle()
        assertEquals(1, writes)
        assertEquals(DemoResult.RESTORED, vm.state.value.result)
        assertFalse(vm.state.value.busy)
    }

    @Test fun failedRestoreDoesNotClaimSuccessAndCanBeRetriedExplicitly() = runTest(dispatcher) {
        var failing = true
        val vm = DemoViewModel(object : DemoRepository {
            override suspend fun restore() { if (failing) throw IOException("Injected failure") }
        })
        vm.ask(); vm.restore()
        advanceUntilIdle()
        assertEquals(DemoResult.ERROR, vm.state.value.result)
        failing = false
        vm.ask(); vm.restore()
        advanceUntilIdle()
        assertEquals(DemoResult.RESTORED, vm.state.value.result)
    }
}
