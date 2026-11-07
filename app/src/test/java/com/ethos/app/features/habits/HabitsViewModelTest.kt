package com.ethos.app.features.habits

import androidx.lifecycle.SavedStateHandle
import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.tasks.domain.TaskClock
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = object : TaskClock {
        override fun today(): LocalDate = LocalDate.of(2026, 11, 6)
        override fun nowMillis(): Long = 100
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun failedSaveRetainsInputAndStableRetryCreatesOneHabit() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = HabitsViewModel(repo, clock, SavedStateHandle())
        advanceUntilIdle()
        vm.newHabit(null)
        val d = vm.state.value.draft!!
        vm.draft(d.copy(habit = d.habit.copy(name = "Ler")))
        vm.preview()
        repo.failWrite = true
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.storageError)
        assertNotNull(vm.state.value.draft)
        assertTrue(repo.snapshot.value.habits.isEmpty())
        repo.failWrite = false
        vm.retry(); vm.retry()
        advanceUntilIdle()
        assertEquals(1, repo.snapshot.value.habits.size)
        assertNull(vm.state.value.draft)
        assertFalse(vm.state.value.storageError)
    }
    @Test fun readFailureIsNotSuccessfulEmptyAndManualImportanceRestores() = runTest(dispatcher) {
        val repo = FakeRepository().apply { failRead = true }
        val saved = SavedStateHandle()
        val vm = HabitsViewModel(repo, clock, saved)
        advanceUntilIdle()
        assertTrue(vm.state.value.storageError)
        vm.newHabit(null)
        val d = vm.state.value.draft!!
        vm.draft(d.copy(habit = d.habit.copy(importance = 0, importanceManual = true)))
        vm.difficulty(2)
        assertEquals(0, vm.state.value.draft!!.habit.importance)
        val restored = HabitsViewModel(repo, clock, saved)
        assertEquals(vm.state.value.draft, restored.state.value.draft)
        repo.failRead = false
        vm.retry()
        advanceUntilIdle()
        assertFalse(vm.state.value.storageError)
    }
    private inner class FakeRepository : HabitRepository {
        val snapshot = MutableStateFlow(HabitSnapshot())
        var failWrite = false
        var failRead = false
        override fun observe() = flow {
            if (failRead) throw IOException("Injected read failure")
            snapshot.collect { emit(it) }
        }
        override suspend fun execute(action: HabitAction) {
            if (failWrite) throw IOException("Injected write failure")
            snapshot.value = HabitRules.apply(snapshot.value, action, clock.today(), clock.nowMillis(), emptySet())
        }
    }
}
