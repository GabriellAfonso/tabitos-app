package com.ethos.app.features.tasks

import androidx.lifecycle.SavedStateHandle
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.timers.domain.*
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Spec 016: timed variant of tasks and automatic completion when the duration is reached. */
@OptIn(ExperimentalCoroutinesApi::class)
class TimedTaskTest {
    private val dispatcher = StandardTestDispatcher()
    private val day = LocalDate.of(2026, 11, 11)
    private val clock = object : TaskClock {
        override fun today(): LocalDate = day
        override fun nowMillis(): Long = 100
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun timedTaskSavesGoalDropsChecklistAndAsksBeforeManualCompletion() = runTest(dispatcher) {
        val tasks = Tasks()
        val timers = Timers()
        val vm = TasksViewModel(tasks, clock, SavedStateHandle(), timers = timers)
        advanceUntilIdle()
        vm.newTask()
        val draft = vm.state.value.draft!!
        vm.updateDraft(draft.copy(fields = draft.fields.copy(title = "Estudar", checklist = listOf("a")),
            timed = true, durationText = "30"))
        vm.saveDraft()
        advanceUntilIdle()
        val definition = tasks.data.value.definitions.single()
        assertTrue(definition.fields.checklist.isEmpty())
        assertEquals(TimerGoal("task:${definition.id}", 30), timers.data.value.goals.single())
        val occurrence = tasks.data.value.activeOccurrences.single()
        vm.quickCheck(occurrence.id)
        assertEquals(occurrence.id, vm.state.value.timedConfirmation)
        assertEquals(TaskStatus.OPEN, tasks.data.value.activeOccurrences.single().status)
        vm.confirmTimed(true)
        advanceUntilIdle()
        assertEquals(TaskStatus.DONE, tasks.data.value.occurrences.single().status)
    }

    @Test fun reachedDurationCompletesOnceWithUndo() = runTest(dispatcher) {
        val tasks = Tasks()
        tasks.execute(TaskAction.Create("t", TaskFields(title = "Ler", deadline = DeadlineKind.DAY, date = day),
            RepeatRule(start = day)))
        val vm = TasksViewModel(tasks, clock, SavedStateHandle())
        advanceUntilIdle()
        vm.timerCompleted("t:1", day)
        advanceUntilIdle()
        val done = tasks.data.value.occurrences.single()
        assertEquals(TaskStatus.DONE, done.status)
        assertEquals(day, done.completedDate)
        vm.timerCompleted("t:1", day)
        advanceUntilIdle()
        assertEquals(1, tasks.data.value.history.count { it.kind == AuditKind.COMPLETED })
        vm.undoTimerDone()
        advanceUntilIdle()
        assertEquals(TaskStatus.OPEN, tasks.data.value.occurrences.single().status)
        assertNull(vm.state.value.timerDone)
    }

    @Test fun listCreatedFromEditorReceivesTheTaskBeingWritten() = runTest(dispatcher) {
        val tasks = Tasks()
        val vm = TasksViewModel(tasks, clock, SavedStateHandle())
        advanceUntilIdle()
        vm.newTask()
        vm.newListForDraft()
        val list = vm.state.value.listDraft!!
        vm.listDraft(list.copy(name = "Estudos"))
        vm.saveList()
        advanceUntilIdle()
        assertEquals(list.id, vm.state.value.draft!!.fields.listId)
        assertNull(vm.state.value.listDraft)
    }

    @Test fun onlyReachedAuditsWithoutLaterConfirmationArePending() {
        val reached = TimerAudit("r1", "s1", TimerAuditKind.REACHED, 10)
        val confirmed = TimerAudit("c1", "s1", TimerAuditKind.CONFIRMED, 11)
        val other = TimerAudit("r2", "s2", TimerAuditKind.REACHED, 12)
        assertEquals(listOf("r2"), TimerRules.unconfirmedReached(
            TimerSnapshot(audits = listOf(reached, confirmed, other))).map { it.id })
    }

    private inner class Tasks : TaskRepository {
        val data = MutableStateFlow(TaskSnapshot())
        override fun observe() = data
        override suspend fun execute(action: TaskAction) {
            data.value = TaskRules.apply(data.value, action, clock.today(), clock.nowMillis())
        }
    }

    private class Timers : TimerRepository {
        val data = MutableStateFlow(TimerSnapshot())
        override fun observe() = data
        override suspend fun start(request: TimerStart, pauseOther: Boolean) = StartResult.STARTED
        override suspend fun pause(sessionId: String) = Unit
        override suspend fun finish(sessionId: String) = Unit
        override suspend fun confirmed(sessionId: String) = Unit
        override suspend fun sync(closedSources: Set<Pair<TimerKind, String>>) = TimerSync(null, null, emptyList())
        override suspend fun checkpoint() = Unit
        override suspend fun saveGoal(sourceKey: String, minutes: Int?) {
            val others = data.value.goals.filterNot { it.sourceKey == sourceKey }
            data.value = data.value.copy(goals = others + listOfNotNull(minutes?.let { TimerGoal(sourceKey, it) }))
        }
        override suspend fun addManual(request: TimerStart, start: Long, end: Long, bounds: TimerBounds) = Unit
        override suspend fun correct(segmentId: String, start: Long, end: Long, bounds: TimerBounds) = Unit
        override suspend fun delete(segmentId: String) = Unit
    }
}
