package com.ethos.app.core.rewards

import com.ethos.app.core.preferences.FixedGameMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ethos.app.core.rewards.data.RoomRewardRepository
import com.ethos.app.core.tasks.data.*
import com.ethos.app.core.tasks.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.ethos.app.core.rewards.domain.*
import com.ethos.app.features.rewards.*
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
        val intent = PurchaseIntent("intent", Reward("r", "Descanso", cost = 5, limit = 2),
            LocalDate.of(2026, 11, 7), 10, 0)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme { RewardsContent(RewardsUiState(loading = false,
                    snapshot = RewardSnapshot(intent = intent)),
                    RewardActions(confirm = { confirmed++ }, close = { closed = true })) }
            }
        }
        compose.onNodeWithText("Preço em moedas: 5").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Saldo em moedas após a compra: 5").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Compras ainda permitidas APÓS esta compra: 1").performScrollTo().assertIsDisplayed()
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
        compose.onNodeWithText("Compra registrada").assertIsDisplayed()
        compose.onNodeWithText("Confirmar compra").assertDoesNotExist()
        compose.onNodeWithText("Voltar").assertDoesNotExist()
        compose.onNodeWithText("Entendi").performClick()
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
                MaterialTheme { RewardsScreen(vm) }
            }
            compose.waitUntil(15000) { compose.onAllNodesWithText("Comprar").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Comprar").performScrollTo().performClick()
            compose.onNodeWithText("Confirmar compra e uso?").assertIsDisplayed()
            compose.onNodeWithText("Voltar").performClick()
            compose.waitUntil(15000) { runBlocking { repo.observe().first().intent == null } }
            assertTrue(runBlocking { repo.observe().first().purchases.isEmpty() })
            compose.onNodeWithText("Comprar").performScrollTo().performClick()
            compose.onNodeWithText("Confirmar compra").performClick()
            compose.waitUntil(15000) {
                compose.onAllNodesWithText("Compra registrada").fetchSemanticsNodes().isNotEmpty()
            }
            val snapshot = runBlocking { repo.observe().first() }
            assertEquals(5L, snapshot.wallet.balance)
            assertEquals(20L, snapshot.wallet.totalXp)
            assertEquals(1, snapshot.purchases.size)
            compose.onNodeWithText("Entendi").performClick()
            compose.waitUntil(15000) { runBlocking { repo.observe().first().intent == null } }
        } finally { db.close() }
    }
}
