package com.ethos.app.core.accounting

import androidx.lifecycle.SavedStateHandle
import com.ethos.app.core.accounting.domain.*
import com.ethos.app.core.tasks.domain.TaskAction
import com.ethos.app.core.habits.domain.HabitAction
import com.ethos.app.features.accounting.AccountingViewModel
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun failedSaveRetainsDraftAcrossRecreationAndRetry() = runTest(dispatcher) {
        val repo = Fake()
        val saved = SavedStateHandle()
        val vm = AccountingViewModel(repo, saved)
        advanceUntilIdle()
        vm.edit()
        vm.draft(vm.state.value.draft!!.toMutableList().also { it[0] = "77" })
        repo.failWrite = true
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.error)
        assertEquals(5L, vm.state.value.snapshot.prices[0].coins)
        assertEquals("77", vm.state.value.draft!![0])
        val restored = AccountingViewModel(repo, saved)
        advanceUntilIdle()
        assertEquals("77", restored.state.value.draft!![0])
        repo.failWrite = false
        vm.retry()
        advanceUntilIdle()
        assertFalse(vm.state.value.error)
        assertNull(vm.state.value.draft)
        assertEquals(77L, vm.state.value.snapshot.prices[0].coins)
    }

    @Test fun invalidInputCannotWriteAndReadFailureCannotMasqueradeAsZero() = runTest(dispatcher) {
        val repo = Fake()
        val vm = AccountingViewModel(repo, SavedStateHandle())
        advanceUntilIdle()
        vm.edit()
        listOf("-1", "1.5", "10001", "99999999999999999999999999", "").forEach { invalid ->
            vm.draft(vm.state.value.draft!!.toMutableList().also { it[0] = invalid })
            vm.save()
            advanceUntilIdle()
            assertTrue(vm.state.value.invalid)
        }
        assertEquals(0, repo.writes)
        repo.failRead = true
        val broken = AccountingViewModel(repo, SavedStateHandle())
        advanceUntilIdle()
        assertTrue(broken.state.value.error)
        repo.failRead = false
        broken.retry()
        advanceUntilIdle()
        assertFalse(broken.state.value.error)
    }

    private class Fake : AccountingRepository {
        val saved = MutableStateFlow(AccountingSnapshot())
        var failRead = false
        var failWrite = false
        var writes = 0
        override fun observe() = flow {
            if (failRead) throw IOException("read")
            emitAll(saved)
        }
        override suspend fun savePrices(prices: List<Price>) {
            if (failWrite) throw IOException("write")
            writes++
            saved.value = saved.value.copy(prices = prices)
        }
        override suspend fun previewTask(action: TaskAction): FinancialPreview = error("unused")
        override suspend fun previewHabit(action: HabitAction): FinancialPreview = error("unused")
    }
}
