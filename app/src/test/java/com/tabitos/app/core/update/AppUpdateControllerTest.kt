package com.tabitos.app.core.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import com.tabitos.app.core.update.data.AppUpdateController
import com.tabitos.app.core.update.data.AppUpdateSource
import com.tabitos.app.core.update.domain.AppUpdatePhase
import com.tabitos.app.core.update.domain.StoreUpdate
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateControllerTest {
    @Test fun noUpdateShowsNothing() = runTest {
        val controller = AppUpdateController(FakeSource(StoreUpdate.NOT_AVAILABLE))
        running(controller)
        assertEquals(AppUpdatePhase.IDLE, controller.phase.value)
    }

    @Test fun acceptedPromptDownloadsThenShowsReady() = runTest {
        val source = FakeSource(StoreUpdate.AVAILABLE)
        val controller = AppUpdateController(source)
        running(controller)
        assertEquals(AppUpdatePhase.PROMPT, controller.phase.value)
        controller.launchPrompt(FakeLauncher)
        controller.promptResult(accepted = true)
        assertEquals(AppUpdatePhase.DOWNLOADING, controller.phase.value)
        source.states.emit(StoreUpdate.DOWNLOADED)
        runCurrent()
        assertEquals(AppUpdatePhase.READY, controller.phase.value)
        controller.install()
        assertEquals(1, source.completed)
    }

    @Test fun promptStartsOnlyOnceEvenIfAskedAgain() = runTest {
        val source = FakeSource(StoreUpdate.AVAILABLE)
        val controller = AppUpdateController(source)
        running(controller)
        controller.launchPrompt(FakeLauncher)
        controller.launchPrompt(FakeLauncher)
        assertEquals(1, source.started)
    }

    @Test fun declinedPromptStaysDismissedForThisOpening() = runTest {
        val source = FakeSource(StoreUpdate.AVAILABLE)
        val controller = AppUpdateController(source)
        running(controller)
        controller.launchPrompt(FakeLauncher)
        controller.promptResult(accepted = false)
        source.states.emit(StoreUpdate.DOWNLOADED)
        runCurrent()
        assertEquals(AppUpdatePhase.DISMISSED, controller.phase.value)
    }

    @Test fun promptThatCannotStartCountsAsDeclined() = runTest {
        val controller = AppUpdateController(FakeSource(StoreUpdate.AVAILABLE, startable = false))
        running(controller)
        controller.launchPrompt(FakeLauncher)
        assertEquals(AppUpdatePhase.DISMISSED, controller.phase.value)
    }

    @Test fun downloadFromAnEarlierOpeningGoesStraightToReady() = runTest {
        val source = FakeSource(StoreUpdate.DOWNLOADED)
        val controller = AppUpdateController(source)
        running(controller)
        assertEquals(AppUpdatePhase.READY, controller.phase.value)
        controller.dismiss()
        assertEquals(AppUpdatePhase.DISMISSED, controller.phase.value)
        controller.install()
        assertEquals(0, source.completed)
    }

    @Test fun failedDownloadShowsNothing() = runTest {
        val source = FakeSource(StoreUpdate.DOWNLOADING)
        val controller = AppUpdateController(source)
        running(controller)
        source.states.emit(StoreUpdate.FAILED)
        runCurrent()
        assertEquals(AppUpdatePhase.IDLE, controller.phase.value)
    }

    @Test fun storeIsAskedOncePerProcess() = runTest {
        val source = FakeSource(StoreUpdate.NOT_AVAILABLE)
        val controller = AppUpdateController(source)
        running(controller)
        running(controller)
        assertEquals(1, source.checks)
    }

    private fun TestScope.running(controller: AppUpdateController) {
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { controller.run() }
        runCurrent()
    }

    private class FakeSource(private val update: StoreUpdate, private val startable: Boolean = true) :
        AppUpdateSource {
        val states = MutableSharedFlow<StoreUpdate>()
        var checks = 0
        var started = 0
        var completed = 0

        override suspend fun check(): StoreUpdate = update.also { checks++ }
        override fun installStates(): Flow<StoreUpdate> = states
        override fun startFlexible(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean =
            startable.also { started++ }
        override fun complete() { completed++ }
    }

    private object FakeLauncher : ActivityResultLauncher<IntentSenderRequest>() {
        override val contract: ActivityResultContract<IntentSenderRequest, *> =
            ActivityResultContracts.StartIntentSenderForResult()
        override fun launch(input: IntentSenderRequest, options: ActivityOptionsCompat?) = Unit
        override fun unregister() = Unit
    }
}
