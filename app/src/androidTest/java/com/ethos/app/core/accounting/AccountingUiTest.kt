package com.ethos.app.core.accounting

import com.ethos.app.core.preferences.FixedGameMode
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import com.ethos.app.R
import com.ethos.app.core.ui.components.ContentFilter
import com.ethos.app.core.accounting.data.RoomAccountingRepository
import com.ethos.app.core.tasks.data.*
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.ui.theme.EthosTheme
import com.ethos.app.features.accounting.*
import com.ethos.app.features.tasks.*
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AccountingUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val clock = object : TaskClock {
        override fun today() = LocalDate.of(2026, 11, 6)
        override fun nowMillis() = 100L
    }

    @Test fun realTaskNeedsFinancialConfirmationAndCancellationDoesNotWrite() {
        val db = Room.inMemoryDatabaseBuilder(compose.activity, TaskDatabase::class.java).build()
        try {
            val tasks = RoomTaskRepository(db, clock, FixedGameMode(on = true))
            val wallet = RoomAccountingRepository(db, clock)
            runBlocking { tasks.execute(TaskAction.Create("t", TaskFields(title = "Preparar café", difficulty = 0),
                RepeatRule(start = clock.today()))) }
            compose.setContent {
                val vm = remember { TasksViewModel(tasks, clock, SavedStateHandle(), wallet) }
                EthosTheme { TasksScreen(vm) { state ->
                    TasksContent(state, TaskPage.LIST, ContentFilter.ALL, vm::detail, vm::action,
                        vm::retry, vm::dismissError)
                } }
            }
            compose.waitUntil(15000) {
                compose.onAllNodesWithText("Preparar café").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Preparar café", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Concluir tarefa").performScrollTo().performClick()
            compose.onNodeWithText("Confirmar efeito financeiro").assertIsDisplayed()
            assertTrue(runBlocking { wallet.observe().first().entries.isEmpty() })
            compose.onNodeWithText(compose.activity.getString(R.string.cancel)).performClick()
            assertTrue(runBlocking { wallet.observe().first().entries.isEmpty() })
            compose.onNodeWithText("Concluir tarefa").performScrollTo().performClick()
            compose.onNodeWithText("Confirmar").performClick()
            compose.waitUntil(15000) { runBlocking { wallet.observe().first().balance == 5L } }
            assertEquals(10L, runBlocking { wallet.observe().first().totalXp })
            compose.onNodeWithText("Desfazer registro").performScrollTo().performClick()
            compose.onNodeWithText("Estorno por correção").assertExists()
            compose.onNodeWithText("Confirmar").performClick()
            compose.waitUntil(15000) { runBlocking { wallet.observe().first().balance == 0L } }
        } finally { db.close() }
    }

    @Test fun tableValidationAndZeroValuesPersistThroughRealRepository() {
        val db = Room.inMemoryDatabaseBuilder(compose.activity, TaskDatabase::class.java).build()
        try {
            val repository = RoomAccountingRepository(db, clock)
            compose.setContent {
                val vm = remember { AccountingViewModel(repository, SavedStateHandle()) }
                EthosTheme { AccountingScreen(vm) { state ->
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        AccountingSettings(state, vm::edit, vm::draft, vm::save, vm::retry)
                    }
                } }
            }
            compose.waitUntil(15000) {
                compose.onAllNodesWithText("Editar ganhos e penalidades").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Editar ganhos e penalidades").performScrollTo().performClick()
            compose.onAllNodesWithText("Moedas por cumprimento")[0].performScrollTo().performTextReplacement("10001")
            compose.onNodeWithText("Salvar").performScrollTo().performClick()
            compose.onNodeWithText("Informe apenas números inteiros de 0 a 10.000 em todos os campos.")
                .performScrollTo().assertIsDisplayed()
            assertEquals(5L, runBlocking { repository.observe().first().prices[0].coins })
            compose.onAllNodesWithText("Moedas por cumprimento")[0].performScrollTo().performTextReplacement("0")
            compose.onNodeWithText("Salvar").performScrollTo().performClick()
            compose.waitUntil(15000) { runBlocking { repository.observe().first().prices[0].coins == 0L } }
        } finally { db.close() }
    }
}
