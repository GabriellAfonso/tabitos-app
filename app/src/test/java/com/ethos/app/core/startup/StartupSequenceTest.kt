package com.ethos.app.core.startup

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupSequenceTest {
    @Test fun deferredSourcesWaitForToday() = runTest {
        val sequence = StartupSequence()
        val calendar = async(start = CoroutineStart.UNDISPATCHED) { sequence.awaitTurn(DeferredSource.CALENDAR) }
        runCurrent()
        assertFalse(calendar.isCompleted)
        sequence.markTodayShown()
        runCurrent()
        assertTrue(calendar.isCompleted)
    }

    @Test fun sourcesStartOneAtATimeInOrder() = runTest {
        val sequence = StartupSequence()
        val rewards = async(start = CoroutineStart.UNDISPATCHED) { sequence.awaitTurn(DeferredSource.REWARDS) }
        val reminders = async(start = CoroutineStart.UNDISPATCHED) { sequence.awaitTurn(DeferredSource.REMINDERS) }
        sequence.markTodayShown()
        runCurrent()
        assertFalse(rewards.isCompleted)
        sequence.markLoaded(DeferredSource.CALENDAR)
        runCurrent()
        assertTrue(rewards.isCompleted)
        assertFalse(reminders.isCompleted)
        sequence.markLoaded(DeferredSource.REWARDS)
        runCurrent()
        assertTrue(reminders.isCompleted)
    }

    @Test fun requestedSourceSkipsTheQueue() = runTest {
        val sequence = StartupSequence()
        val reminders = async(start = CoroutineStart.UNDISPATCHED) { sequence.awaitTurn(DeferredSource.REMINDERS) }
        sequence.request(setOf(DeferredSource.REMINDERS))
        runCurrent()
        assertTrue(reminders.isCompleted)
    }

    @Test fun todayWaitersResumeOnceShown() = runTest {
        val sequence = StartupSequence()
        val refresh = async(start = CoroutineStart.UNDISPATCHED) { sequence.awaitTodayShown() }
        assertFalse(refresh.isCompleted)
        sequence.markTodayShown()
        sequence.markTodayShown()
        runCurrent()
        assertTrue(refresh.isCompleted)
    }

    @Test fun releasedNeverWaits() = runTest {
        val sequence = StartupSequence.released()
        sequence.awaitTodayShown()
        DeferredSource.entries.forEach { sequence.awaitTurn(it) }
    }
}
