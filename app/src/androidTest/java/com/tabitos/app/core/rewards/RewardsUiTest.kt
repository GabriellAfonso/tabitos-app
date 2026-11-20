package com.tabitos.app.core.rewards

import com.tabitos.app.core.preferences.FixedGameMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tabitos.app.core.rewards.data.RoomRewardRepository
import com.tabitos.app.core.tasks.data.*
import com.tabitos.app.core.tasks.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.tabitos.app.core.rewards.domain.*
import com.tabitos.app.features.rewards.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RewardsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyAndErrorStatesAreHonest() {
        var retried = false
        compose.setContent { MaterialTheme {
            RewardsContent(RewardsUiState(loading = false, error = true), RewardActions(retry = { retried = true }))
        } }
        compose.onNodeWithText("Nova recompensa").assertDoesNotExist()
        compose.onNodeWithText("Tentar novamente").performClick()
        assertTrue(retried)
    }

    @Test fun confirmationAtLargeFontShowsPriceAllowanceAndCancelWithoutPurchase() {
        var confirmed = 0
        var closed = false
        val reward = Reward("r", "Descanso", cost = 5, limit = 2)
        val date = LocalDate.of(2026, 11, 7)
        val intent = PurchaseIntent("intent", reward, date, 10, 0)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme { RewardsContent(RewardsUiState(loading = false,
                    cards = listOf(rewardCard(reward, emptyList(), 10, date)),
                    snapshot = RewardSnapshot(rewards = listOf(reward), intent = intent)),
                    RewardActions(confirm = { confirmed++ }, close = { closed = true })) }
            }
        }
        compose.onNodeWithText("Saldo 10 → 5 moedas").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Depois desta: 1 de 2 por dia").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Voltar").performClick()
        assertTrue(closed)
        assertEquals(0, confirmed)
    }

    @Test fun committedResultHasAcknowledgementAndNoRefundOrUseLater() {
        val reward = Reward("r", "Descanso", cost = 5)
        val date = LocalDate.of(2026, 11, 7)
        val intent = PurchaseIntent("intent", reward, date, 10, 0)
        var acknowledged = false
        compose.setContent { MaterialTheme { RewardsContent(RewardsUiState(loading = false,
            snapshot = RewardSnapshot(intent = intent,
                purchases = listOf(Purchase("intent", reward, date, 1, 1)))),
            RewardActions(close = { acknowledged = true })) } }
        compose.onNodeWithText("compra registrada", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Pausar e comprar").assertDoesNotExist()
        compose.onNodeWithText("Voltar").assertDoesNotExist()
        compose.onNodeWithText("Fechar").performClick()
        assertTrue(acknowledged)
    }

    @Test fun realRepositoryPurchaseNeedsConfirmationAndRecordsImmediateUse() {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
            TaskDatabase::class.java).build()
        val clock = object : TaskClock {
            override fun today() = LocalDate.of(2026, 11, 7)
            override fun nowMillis() = 100L
        }
        try {
            val repo = RoomRewardRepository(db, clock)
            runBlocking {
                val tasks = RoomTaskRepository(db, clock, FixedGameMode(on = true))
                tasks.execute(TaskAction.Create("work", TaskFields(title = "Trabalho fictício"),
                    RepeatRule(start = clock.today())))
                tasks.execute(TaskAction.Resolve(tasks.observe().first().occurrences.single().id,
                    true, clock.today()))
                repo.save(Reward("r", "Descanso", cost = 5, limit = 2))
            }
            compose.setContent {
                val vm = remember { RewardsViewModel(repo, SavedStateHandle()) }
                MaterialTheme { RewardsScreen(vm, openHistory = {}) }
            }
            val buy = "Comprar Descanso por 5 moedas"
            compose.waitUntil(15000) {
                compose.onAllNodesWithContentDescription(buy).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription(buy).performScrollTo().performClick()
            compose.onNodeWithText("Usa na hora · sem devolução").assertIsDisplayed()
            compose.onNodeWithText("Voltar").performClick()
            compose.waitUntil(15000) { runBlocking { repo.observe().first().intent == null } }
            assertTrue(runBlocking { repo.observe().first().purchases.isEmpty() })
            compose.onNodeWithContentDescription(buy).performScrollTo().performClick()
            compose.onNodeWithText("Comprar", substring = true).performClick()
            compose.waitUntil(15000) {
                compose.onAllNodesWithText("compra registrada", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            val snapshot = runBlocking { repo.observe().first() }
            assertEquals(5L, snapshot.wallet.balance)
            assertEquals(20L, snapshot.wallet.totalXp)
            assertEquals(1, snapshot.purchases.size)
            compose.onNodeWithText("Fechar").performClick()
            compose.waitUntil(15000) { runBlocking { repo.observe().first().intent == null } }
        } finally { db.close() }
    }
}
