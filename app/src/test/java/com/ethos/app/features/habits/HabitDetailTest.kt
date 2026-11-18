package com.ethos.app.features.habits

import androidx.lifecycle.SavedStateHandle
import com.ethos.app.core.habits.domain.Habit
import com.ethos.app.core.habits.domain.HabitAction
import com.ethos.app.core.habits.domain.HabitRecord
import com.ethos.app.core.habits.domain.HabitRepository
import com.ethos.app.core.habits.domain.HabitRule
import com.ethos.app.core.habits.domain.HabitRules
import com.ethos.app.core.habits.domain.HabitSnapshot
import com.ethos.app.core.habits.domain.HabitStatus
import com.ethos.app.core.habits.domain.HabitType
import com.ethos.app.core.habits.domain.HabitUnit
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.timers.domain.StartResult
import com.ethos.app.core.timers.domain.TimerBounds
import com.ethos.app.core.timers.domain.TimerGoal
import com.ethos.app.core.timers.domain.TimerKind
import com.ethos.app.core.timers.domain.TimerRepository
import com.ethos.app.core.timers.domain.TimerSnapshot
import com.ethos.app.core.timers.domain.TimerStart
import com.ethos.app.core.timers.domain.TimerSync
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Spec 023: week strip, main area and day corrections of the habit detail popup. */
@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailTest {
    private val dispatcher = StandardTestDispatcher()
    /** A Tuesday: the previous week (Mon 9 – Sun 15) is still in its 2 late days. */
    private val today = LocalDate.of(2026, 11, 17)
    private val clock = object : TaskClock {
        override fun today(): LocalDate = today
        override fun nowMillis(): Long = 100
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun dailyPracticeMarksOnlyOpenDaysAndShowsMissedOnes() = runTest(dispatcher) {
        val vm = viewModel(habit(HabitType.PRACTICE, HabitUnit.DAY, 1), past = listOf(1L, 2L, 4L, 5L, 6L))
        val model = habitDetailModel(vm.state.value, ID)!!
        assertEquals((6L downTo 0).map { today.minusDays(it) }, model.week.map { it.date })
        assertEquals(listOf(false, false, false, false, true, true, true), model.week.map { it.editable })
        assertEquals(HabitDayMark.MISSED, model.week.single { it.date == today.minusDays(3) }.mark)
        assertEquals(HabitDayMark.DONE, model.week.single { it.date == today.minusDays(1) }.mark)
        assertEquals(HabitDayMark.OPEN, model.week.last().mark)
        assertEquals(HabitDetailMain.MARK, model.main)
        assertNull(model.progress)

        vm.setPractice(ID, today.minusDays(1), done = false)
        advanceUntilIdle()
        val unmarked = habitDetailModel(vm.state.value, ID)!!
        assertEquals(HabitDayMark.OPEN, unmarked.week.single { it.date == today.minusDays(1) }.mark)

        vm.setPractice(ID, today, done = true)
        advanceUntilIdle()
        assertEquals(HabitDetailMain.DONE_TODAY, habitDetailModel(vm.state.value, ID)!!.main)
    }

    @Test fun weeklyPracticeKeepsThePreviousWeekOpenDuringItsLateDays() = runTest(dispatcher) {
        val vm = viewModel(habit(HabitType.PRACTICE, HabitUnit.WEEK, 3), past = listOf(1L))
        val model = habitDetailModel(vm.state.value, ID)!!
        assertTrue(model.week.all { it.editable })
        assertEquals(HabitDetailProgress(1, 3, HabitUnit.WEEK, limit = false), model.progress)
        assertTrue(model.week.none { it.mark == HabitDayMark.MISSED })
    }

    @Test fun avoidHabitCountsRecordsPerDayAndCorrectsThemFromTheSheet() = runTest(dispatcher) {
        val vm = viewModel(habit(HabitType.AVOID, HabitUnit.DAY, 0), past = listOf(1L, 1L, 5L))
        val model = habitDetailModel(vm.state.value, ID)!!
        val yesterday = model.week.single { it.date == today.minusDays(1) }
        assertEquals(HabitDayMark.OVER_LIMIT to 2, yesterday.mark to yesterday.count)
        assertEquals(HabitDayMark.CLEAN, model.week.single { it.date == today.minusDays(4) }.mark)
        assertEquals(HabitDetailMain.BREAKING, model.main)
        assertTrue(model.breaking)

        vm.removeOccurrence(ID, today.minusDays(1))
        advanceUntilIdle()
        assertEquals(1, habitDetailModel(vm.state.value, ID)!!.week.single { it.date == today.minusDays(1) }.count)
        vm.addOccurrence(ID, today)
        advanceUntilIdle()
        assertEquals(1, habitDetailModel(vm.state.value, ID)!!.todayCount)
    }

    @Test fun limitHabitShowsTodayAgainstItsLimit() = runTest(dispatcher) {
        val vm = viewModel(habit(HabitType.LIMIT, HabitUnit.DAY, 2), past = listOf(0L, 3L, 3L, 3L))
        val model = habitDetailModel(vm.state.value, ID)!!
        assertEquals(HabitDetailProgress(1, 2, HabitUnit.DAY, limit = true), model.progress)
        assertEquals(HabitDayMark.COUNTED, model.week.last().mark)
        assertEquals(HabitDayMark.OVER_LIMIT, model.week.single { it.date == today.minusDays(3) }.mark)
    }

    @Test fun pausedHabitHasNoTappableDays() = runTest(dispatcher) {
        val repo = repository(habit(HabitType.PRACTICE, HabitUnit.DAY, 1), past = emptyList())
        repo.execute(HabitAction.Status(ID, HabitStatus.PAUSED))
        val vm = HabitsViewModel(repo, clock, SavedStateHandle())
        advanceUntilIdle()
        val model = habitDetailModel(vm.state.value, ID)!!
        assertEquals(HabitDetailMain.PAUSED, model.main)
        assertFalse(model.week.any { it.editable })
    }

    @Test fun timedHabitConfirmsBeforeMarkingAPastDayByHand() = runTest(dispatcher) {
        val repo = repository(habit(HabitType.PRACTICE, HabitUnit.DAY, 1), past = emptyList())
        val timers = Timers(TimerSnapshot(goals = listOf(TimerGoal("habit:$ID", 10))))
        val vm = HabitsViewModel(repo, clock, SavedStateHandle(), timers = timers)
        advanceUntilIdle()
        vm.setPractice(ID, today.minusDays(1), done = true)
        assertEquals(TimedPractice(ID, today.minusDays(1)), vm.state.value.timedConfirmation)
        assertTrue(repo.snapshot.value.records.isEmpty())
        vm.confirmTimed(true)
        advanceUntilIdle()
        assertEquals(listOf(today.minusDays(1)), repo.snapshot.value.records.map { it.date })
    }

    private fun habit(type: HabitType, unit: HabitUnit, target: Int) =
        HabitAction.Save(Habit(ID, today.minusDays(CREATED_DAYS_AGO), "Hábito"),
            HabitRule("r", ID, today.minusDays(CREATED_DAYS_AGO), type, unit, target))

    /** Creates the habit [CREATED_DAYS_AGO] days ago and one record per entry of [past] (days before today). */
    private fun repository(save: HabitAction.Save, past: List<Long>): Repository {
        val created = HabitRules.apply(HabitSnapshot(), save, save.habit.created, 1, emptySet())
        val records = past.mapIndexed { i, back -> HabitRecord("rec$i", ID, today.minusDays(back), i.toLong()) }
        return Repository(created.copy(records = records))
    }

    private fun TestScope.viewModel(save: HabitAction.Save, past: List<Long>): HabitsViewModel {
        val vm = HabitsViewModel(repository(save, past), clock, SavedStateHandle())
        advanceUntilIdle()
        return vm
    }

    private inner class Repository(initial: HabitSnapshot) : HabitRepository {
        val snapshot = MutableStateFlow(initial)
        override fun observe() = snapshot
        override suspend fun execute(action: HabitAction) {
            snapshot.value = HabitRules.apply(snapshot.value, action, clock.today(), clock.nowMillis(), emptySet())
        }
        override suspend fun erase(habitId: String) = Unit
    }

    private class Timers(initial: TimerSnapshot) : TimerRepository {
        val data = MutableStateFlow(initial)
        override fun observe() = data
        override suspend fun start(request: TimerStart, pauseOther: Boolean) = StartResult.STARTED
        override suspend fun pause(sessionId: String) = Unit
        override suspend fun finish(sessionId: String) = Unit
        override suspend fun confirmed(sessionId: String) = Unit
        override suspend fun sync(closedSources: Set<Pair<TimerKind, String>>) = TimerSync(null, null, emptyList())
        override suspend fun checkpoint() = Unit
        override suspend fun saveGoal(sourceKey: String, minutes: Int?) = Unit
        override suspend fun addManual(request: TimerStart, start: Long, end: Long, bounds: TimerBounds) = Unit
        override suspend fun correct(segmentId: String, start: Long, end: Long, bounds: TimerBounds) = Unit
        override suspend fun delete(segmentId: String) = Unit
        override suspend fun discardLatest(sessionId: String) = Unit
    }

    private companion object {
        const val ID = "h"
        const val CREATED_DAYS_AGO = 20L
    }
}
