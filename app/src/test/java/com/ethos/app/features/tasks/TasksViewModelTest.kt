package com.ethos.app.features.tasks

import androidx.lifecycle.SavedStateHandle
import com.ethos.app.core.accounting.domain.*
import com.ethos.app.core.habits.domain.HabitAction
import com.ethos.app.core.tasks.domain.*
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
class TasksViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = object : TaskClock {
        override fun today(): LocalDate = LocalDate.of(2026, 11, 6)
        override fun nowMillis(): Long = 100
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun quickCheckboxSkipsBothConfirmationsAndRetainsDirectRetryAfterFailure() = runTest(dispatcher) {
        val repo = FakeRepository(clock)
        repo.execute(TaskAction.Create("quick", TaskFields(title = "Quick", checklist = listOf("Pending")),
            RepeatRule(start = clock.today())))
        val accounting = object : AccountingRepository {
            override fun observe() = MutableStateFlow(AccountingSnapshot())
            override suspend fun savePrices(prices: List<Price>) = Unit
            override suspend fun previewTask(action: TaskAction): FinancialPreview = error("Unexpected modal")
            override suspend fun previewHabit(action: HabitAction): FinancialPreview = error("Unexpected habit")
        }
        val vm = TasksViewModel(repo, clock, SavedStateHandle(), accounting)
        advanceUntilIdle()
        vm.quickCheck("quick:1"); vm.quickCheck("quick:1")
        advanceUntilIdle()
        assertEquals(TaskStatus.DONE, vm.state.value.snapshot.occurrences.single().status)
        assertNull(vm.state.value.uncheckedConfirmation)
        assertNull(vm.state.value.financialPreview)
        assertNull(vm.state.value.detailId)
        assertFalse(vm.state.value.snapshot.occurrences.single().checks.single().checked)
        repo.failWrite = true
        vm.quickCheck("quick:1")
        advanceUntilIdle()
        assertTrue(vm.state.value.storageError)
        assertEquals(TaskStatus.DONE, vm.state.value.snapshot.occurrences.single().status)
        repo.failWrite = false
        vm.retry()
        advanceUntilIdle()
        assertEquals(TaskStatus.OPEN, vm.state.value.snapshot.occurrences.single().status)
        assertNull(vm.state.value.financialPreview)
        assertFalse(vm.state.value.storageError)
        assertEquals(1, repo.snapshot.value.history.count { it.kind == AuditKind.COMPLETED })
        assertEquals(1, repo.snapshot.value.history.count { it.kind == AuditKind.UNDONE })
    }

    @Test fun failedSaveKeepsDraftAndRetryCreatesExactlyOneTask() = runTest(dispatcher) {
        val repo = FakeRepository(clock)
        val vm = TasksViewModel(repo, clock, SavedStateHandle())
        advanceUntilIdle()
        vm.newTask()
        vm.updateDraft(vm.state.value.draft!!.copy(fields = TaskFields(title = "Remember")))
        repo.failWrite = true
        vm.saveDraft()
        advanceUntilIdle()
        assertTrue(vm.state.value.storageError)
        assertNotNull(vm.state.value.draft)
        assertTrue(repo.snapshot.value.occurrences.isEmpty())
        repo.failWrite = false
        vm.retry()
        advanceUntilIdle()
        assertNull(vm.state.value.draft)
        assertEquals(1, repo.snapshot.value.occurrences.size)
        assertFalse(vm.state.value.storageError)
    }

    @Test fun rapidSaveIsSerializedAndManualImportanceWins() = runTest(dispatcher) {
        val repo = FakeRepository(clock)
        val vm = TasksViewModel(repo, clock, SavedStateHandle())
        advanceUntilIdle()
        vm.newTask()
        vm.updateDraft(vm.state.value.draft!!.copy(fields = TaskFields(title = "One",
            importance = 0, importanceManual = true)))
        vm.difficulty(2)
        assertEquals(0, vm.state.value.draft!!.fields.importance)
        vm.saveDraft(); vm.saveDraft()
        advanceUntilIdle()
        assertEquals(1, repo.snapshot.value.occurrences.size)
    }

    @Test fun readFailureRequiresRetryAndDraftRestoresFromSavedState() = runTest(dispatcher) {
        val repo = FakeRepository(clock).apply { failRead = true }
        val handle = SavedStateHandle()
        val vm = TasksViewModel(repo, clock, handle)
        advanceUntilIdle()
        assertTrue(vm.state.value.storageError)
        vm.newTask()
        vm.updateDraft(vm.state.value.draft!!.copy(fields = TaskFields(title = "Draft")))
        val restored = TasksViewModel(repo, clock,
            SavedStateHandle(mapOf("taskDraft" to handle.get<TaskDraft>("taskDraft"))))
        assertEquals("Draft", restored.state.value.draft!!.fields.title)
        repo.failRead = false
        restored.retry()
        advanceUntilIdle()
        assertFalse(restored.state.value.loading)
        assertFalse(restored.state.value.storageError)
    }

    @Test fun incompleteChecklistRequiresConfirmationAndCancelLeavesOpen() = runTest(dispatcher) {
        val repo = FakeRepository(clock)
        repo.execute(TaskAction.Create("a", TaskFields(title = "Checklist", checklist = listOf("One")),
            RepeatRule(start = clock.today())))
        val vm = TasksViewModel(repo, clock, SavedStateHandle())
        advanceUntilIdle()
        vm.detail("a:1")
        vm.complete(repo.snapshot.value.occurrences.single(), true)
        assertEquals(1, vm.state.value.uncheckedConfirmation)
        vm.confirmCompletion(false)
        assertEquals(TaskStatus.OPEN, repo.snapshot.value.occurrences.single().status)
        vm.complete(repo.snapshot.value.occurrences.single(), true)
        vm.confirmCompletion(true)
        advanceUntilIdle()
        assertEquals(TaskStatus.DONE, repo.snapshot.value.occurrences.single().status)
    }

    @Test fun changedDraftAfterWriteFailureDoesNotRetryStaleInput() = runTest(dispatcher) {
        val repo = FakeRepository(clock)
        val vm = TasksViewModel(repo, clock, SavedStateHandle())
        advanceUntilIdle()
        vm.newTask()
        vm.updateDraft(vm.state.value.draft!!.copy(fields = TaskFields(title = "Old input")))
        repo.failWrite = true
        vm.saveDraft()
        advanceUntilIdle()
        repo.failWrite = false
        vm.updateDraft(vm.state.value.draft!!.copy(fields = TaskFields(title = "")))
        vm.saveDraft()
        advanceUntilIdle()
        assertEquals(TaskProblem.INVALID_INPUT, vm.state.value.error)
        assertFalse(vm.state.value.storageError)
        vm.retry()
        advanceUntilIdle()
        assertTrue(repo.snapshot.value.occurrences.isEmpty())
        assertNotNull(vm.state.value.draft)
    }

    @Test fun notificationCompletionRevalidatesAndPreservesUncheckedConfirmation() = runTest(dispatcher) {
        val repo = FakeRepository(clock)
        repo.execute(TaskAction.Create("a", TaskFields(title = "Fictitious", checklist = listOf("One")),
            RepeatRule(start = clock.today())))
        val vm = TasksViewModel(repo, clock, SavedStateHandle())
        advanceUntilIdle()
        vm.notification("a:1", true)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.uncheckedConfirmation)
        assertEquals(TaskStatus.OPEN, repo.snapshot.value.occurrences.single().status)
        vm.confirmCompletion(true)
        advanceUntilIdle()
        vm.notification("a:1", true)
        advanceUntilIdle()
        assertEquals(1, repo.snapshot.value.history.count { it.kind == AuditKind.COMPLETED })
    }

    @Test fun notificationReadFailureAndMissingSourceAreRecoverable() = runTest(dispatcher) {
        val repo = FakeRepository(clock)
        val vm = TasksViewModel(repo, clock, SavedStateHandle())
        advanceUntilIdle()
        vm.notification("missing", true)
        advanceUntilIdle()
        assertEquals(TaskProblem.NOT_OPEN, vm.state.value.error)
        repo.failRead = true
        vm.notification("missing", true)
        advanceUntilIdle()
        assertTrue(vm.state.value.storageError)
    }
    @Test fun notificationCompletionRequiresExistingFinancialConfirmation() = runTest(dispatcher) {
        val repo = FakeRepository(clock)
        repo.execute(TaskAction.Create("a", TaskFields(title = "Fictitious"), RepeatRule(start = clock.today())))
        var previews = 0
        val accounting = object : AccountingRepository {
            override fun observe() = MutableStateFlow(AccountingSnapshot())
            override suspend fun savePrices(prices: List<Price>) = Unit
            override suspend fun previewTask(action: TaskAction): FinancialPreview {
                previews++
                return FinancialPreview(emptyList(), 10, 10)
            }
            override suspend fun previewHabit(action: HabitAction) = error("Unexpected habit mutation")
        }
        val vm = TasksViewModel(repo, clock, SavedStateHandle(), accounting)
        advanceUntilIdle()
        vm.notification("a:1", true)
        advanceUntilIdle()
        assertNotNull(vm.state.value.financialPreview)
        assertEquals(TaskStatus.OPEN, repo.snapshot.value.occurrences.single().status)
        vm.confirmFinancial(false)
        advanceUntilIdle()
        assertEquals(TaskStatus.OPEN, repo.snapshot.value.occurrences.single().status)
        vm.notification("a:1", true)
        advanceUntilIdle()
        vm.confirmFinancial(true)
        advanceUntilIdle()
        assertEquals(2, previews)
        assertEquals(TaskStatus.DONE, repo.snapshot.value.occurrences.single().status)
        assertEquals(1, repo.snapshot.value.history.count { it.kind == AuditKind.COMPLETED })
    }

    private class FakeRepository(val clock: TaskClock) : TaskRepository {
        val snapshot = MutableStateFlow(TaskSnapshot())
        var failRead = false
        var failWrite = false
        override fun observe() = flow {
            if (failRead) throw IOException()
            snapshot.collect { emit(it) }
        }
        override suspend fun execute(action: TaskAction) {
            if (failWrite) throw IOException()
            snapshot.value = TaskRules.apply(snapshot.value, action, clock.today(), clock.nowMillis())
        }
    }
}
