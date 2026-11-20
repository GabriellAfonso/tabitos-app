package com.tabitos.app.features.timers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.tabitos.app.core.rewards.domain.*
import com.tabitos.app.features.rewards.*
import com.tabitos.app.core.timers.domain.*
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TimerUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sessionCardsShowRemainingAndActionsAtDoubleFont() {
        val running = TimerSession("r", TimerKind.TASK, "o", "Leitura", TimerState.RUNNING, created = 0)
        val reward = TimerSession("p", TimerKind.REWARD, "x", "Jogo", TimerState.PAUSED, limitMillis = 7_200_000,
            created = 0)
        val state = TimerUiState(loading = false, snapshot = TimerSnapshot(listOf(running, reward), listOf(
            TimerSegment("a", "r", 0, 90_000, created = 0), TimerSegment("b", "p", 0, 600_000, created = 0))))
        var paused: String? = null
        var resumed: TimerSession? = null
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
                    TimerSessionCards(state, listOf(reward, running), ZoneId.of("UTC"),
                        TimerCardActions(pause = { paused = it }, resume = { resumed = it }))
                } }
            }
        }
        compose.onNodeWithText("Sessão ativa").assertExists()
        compose.onNodeWithText("Decorrido: 01:30").assertExists()
        compose.onNodeWithText("Restante: 1:50:00").assertExists()
        compose.onNodeWithText("Pausar").performScrollTo().performClick()
        compose.onNodeWithText("Retomar").performScrollTo().performClick()
        assertEquals("r", paused)
        assertEquals("p", resumed?.id)
    }

    @Test fun timedPurchaseExplainsCountdownAndPauseDialogCancelDoesNotConfirm() {
        val reward = Reward("r", "Jogo", cost = 10, minutes = 120)
        var confirmed = false
        var cancelled = false
        val state = RewardsUiState(loading = false, pauseRequired = "Leitura", snapshot = RewardSnapshot(
            rewards = listOf(reward)))
        compose.setContent { MaterialTheme { RewardsContent(state, RewardActions(
            confirmPause = { confirmed = true }, cancelPause = { cancelled = true })) } }
        compose.onNodeWithText("Pausar, comprar e iniciar").assertExists()
        compose.onNodeWithText("Voltar").performClick()
        assertTrue(cancelled)
        assertFalse(confirmed)
    }
}
