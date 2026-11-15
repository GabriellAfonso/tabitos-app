package com.ethos.app.features.habits

import com.ethos.app.core.preferences.FixedGameMode
import androidx.lifecycle.SavedStateHandle
import com.ethos.app.core.habits.domain.*
import androidx.compose.ui.graphics.Color
import com.ethos.app.R
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.tasks.domain.TaskList
import com.ethos.app.core.ui.components.itemColors
import com.ethos.app.features.tasks.listIcons
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

    @Test fun unwantedHabitStillRequiresConfirmationAndCancelNeverWrites() = runTest(dispatcher) {
        val repo = FakeRepository()
        repo.execute(HabitAction.Save(Habit("h", clock.today(), "Evitar"),
            HabitRule("r", "h", clock.today(), HabitType.AVOID, target = 0)))
        val accounting = object : com.ethos.app.core.accounting.domain.AccountingRepository {
            override fun observe() = MutableStateFlow(com.ethos.app.core.accounting.domain.AccountingSnapshot())
            override suspend fun savePrices(prices: List<com.ethos.app.core.accounting.domain.Price>) = Unit
            override suspend fun previewTask(action: com.ethos.app.core.tasks.domain.TaskAction):
                com.ethos.app.core.accounting.domain.FinancialPreview = error("Unexpected task")
            override suspend fun previewHabit(action: HabitAction) =
                com.ethos.app.core.accounting.domain.FinancialPreview(emptyList(), -10, 0)
        }
        val vm = HabitsViewModel(repo, clock, SavedStateHandle(), accounting)
        advanceUntilIdle()
        vm.quickRecord("h")
        advanceUntilIdle()
        assertNotNull(vm.state.value.financialPreview)
        assertTrue(repo.snapshot.value.records.isEmpty())
        vm.confirmFinancial(false)
        advanceUntilIdle()
        assertTrue(repo.snapshot.value.records.isEmpty())
        vm.quickRecord("h")
        advanceUntilIdle()
        vm.confirmFinancial(true)
        advanceUntilIdle()
        assertEquals(1, repo.snapshot.value.records.size)
    }

    @Test fun gameModeOffRecordsUnwantedHabitWithoutConfirmation() = runTest(dispatcher) {
        val repo = FakeRepository()
        repo.execute(HabitAction.Save(Habit("h", clock.today(), "Evitar"),
            HabitRule("r", "h", clock.today(), HabitType.AVOID, target = 0)))
        val accounting = object : com.ethos.app.core.accounting.domain.AccountingRepository {
            override fun observe() = MutableStateFlow(com.ethos.app.core.accounting.domain.AccountingSnapshot())
            override suspend fun savePrices(prices: List<com.ethos.app.core.accounting.domain.Price>) = Unit
            override suspend fun previewTask(action: com.ethos.app.core.tasks.domain.TaskAction):
                com.ethos.app.core.accounting.domain.FinancialPreview = error("Unexpected task")
            override suspend fun previewHabit(action: HabitAction):
                com.ethos.app.core.accounting.domain.FinancialPreview = error("No preview while off")
        }
        val vm = HabitsViewModel(repo, clock, SavedStateHandle(), accounting, gameMode = FixedGameMode(on = false))
        advanceUntilIdle()
        vm.quickRecord("h")
        advanceUntilIdle()
        assertNull(vm.state.value.financialPreview)
        assertEquals(1, repo.snapshot.value.records.size)
    }

    @Test fun quickPracticeTogglesWithoutConfirmationAndIgnoresConcurrentTap() = runTest(dispatcher) {
        val repo = FakeRepository()
        repo.execute(HabitAction.Save(Habit("h", clock.today(), "Ler"), HabitRule("r", "h", clock.today())))
        val accounting = object : com.ethos.app.core.accounting.domain.AccountingRepository {
            override fun observe() = flow<com.ethos.app.core.accounting.domain.AccountingSnapshot> { }
            override suspend fun savePrices(prices: List<com.ethos.app.core.accounting.domain.Price>) = Unit
            override suspend fun previewTask(action: com.ethos.app.core.tasks.domain.TaskAction):
                com.ethos.app.core.accounting.domain.FinancialPreview = error("Unexpected task preview")
            override suspend fun previewHabit(action: HabitAction):
                com.ethos.app.core.accounting.domain.FinancialPreview = error("Practice must not ask confirmation")
        }
        val vm = HabitsViewModel(repo, clock, SavedStateHandle(), accounting)
        advanceUntilIdle()
        vm.quickRecord("h"); vm.quickRecord("h")
        advanceUntilIdle()
        assertEquals(1, vm.state.value.snapshot.records.count { !it.deleted })
        assertNull(vm.state.value.financialPreview)
        assertFalse(vm.state.value.storageError)
        repo.failWrite = true
        vm.quickRecord("h")
        advanceUntilIdle()
        assertTrue(vm.state.value.storageError)
        assertEquals(1, vm.state.value.snapshot.records.count { !it.deleted })
        repo.failWrite = false
        vm.retry()
        advanceUntilIdle()
        assertEquals(0, vm.state.value.snapshot.records.count { !it.deleted })
        vm.quickRecord("h")
        advanceUntilIdle()
        assertEquals(1, vm.state.value.snapshot.records.count { !it.deleted })
    }

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
    @Test fun badHabitStartsRedWithProhibitionIconAndNewestIconSaves() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = HabitsViewModel(repo, clock, SavedStateHandle())
        advanceUntilIdle()
        vm.newHabit(null, breaking = true)
        val d = vm.state.value.draft!!
        assertEquals(R.drawable.ic_block, habitIcons[d.habit.icon].icon)
        assertEquals(Color(0xFFB3261E), itemColors[d.habit.color])
        vm.draft(d.copy(habit = d.habit.copy(name = "Rede social", icon = habitIcons.lastIndex)))
        vm.preview()
        vm.save()
        advanceUntilIdle()
        assertEquals(habitIcons.lastIndex, repo.snapshot.value.habits.single().icon)
    }
    @Test fun editorChoicesMatchTheIndicesTheRulesAccept() {
        assertEquals(Habit.COLOR_COUNT, itemColors.size)
        assertEquals(Habit.ICON_COUNT, habitIcons.size)
        assertEquals(TaskList.COLOR_COUNT, itemColors.size)
        assertEquals(TaskList.ICON_COUNT, listIcons.size)
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
