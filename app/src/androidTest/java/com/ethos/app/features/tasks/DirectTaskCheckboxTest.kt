package com.ethos.app.features.tasks

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import com.ethos.app.core.accounting.data.RoomAccountingRepository
import com.ethos.app.core.tasks.data.RoomTaskRepository
import com.ethos.app.core.tasks.data.TaskDatabase
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.ui.theme.EthosTheme
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DirectTaskCheckboxTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val clock = object : TaskClock {
        override fun today(): LocalDate = LocalDate.of(2026, 11, 10)
        override fun nowMillis() = 100L
    }

    @Test fun todayMovesToCompletedBelowHabitsAndUncheckingReturnsWithoutDialogs() {
        val db = Room.inMemoryDatabaseBuilder(compose.activity, TaskDatabase::class.java).build()
        try {
            val tasks = RoomTaskRepository(db, clock)
            val wallet = RoomAccountingRepository(db, clock)
            runBlocking { tasks.execute(TaskAction.Create("t", TaskFields(title = "Preparar mochila",
                deadline = DeadlineKind.DAY, date = clock.today(), checklist = listOf("Chaves")),
                RepeatRule(start = clock.today()))) }
            compose.setContent {
                val vm = remember { TasksViewModel(tasks, clock, SavedStateHandle(), wallet) }
                EthosTheme { TasksScreen(vm) { state ->
                    TasksContent(state, TaskPage.TODAY, 0, {}, vm::detail, vm::action, {}, vm::retry,
                        vm::dismissError, extraItems = { item("habits-test") { Text("Hábitos do dia") } },
                        onComplete = vm::quickCheck)
                } }
            }
            compose.waitUntil(15000) {
                compose.onAllNodesWithContentDescription("Concluir Preparar mochila").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Concluir Preparar mochila").assertIsOff().performClick()
            compose.waitUntil(15000) {
                compose.onAllNodesWithText("Concluídas (1)").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Concluídas (1)").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Desmarcar Preparar mochila").assertIsOn()
            assertTrue(compose.onNodeWithText("Hábitos do dia").getUnclippedBoundsInRoot().top <
                compose.onNodeWithText("Concluídas (1)").getUnclippedBoundsInRoot().top)
            compose.onNodeWithText("Confirmar efeito financeiro").assertDoesNotExist()
            compose.onNodeWithText("Concluir mesmo assim").assertDoesNotExist()
            compose.waitUntil(15000) { runBlocking { wallet.observe().first().balance == 10L } }
            compose.onNodeWithContentDescription("Desmarcar Preparar mochila").performClick()
            compose.waitUntil(15000) {
                compose.onAllNodesWithContentDescription("Concluir Preparar mochila").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Concluir Preparar mochila").assertIsOff()
            compose.onNodeWithText("Concluídas (1)").assertDoesNotExist()
            compose.waitUntil(15000) { runBlocking { wallet.observe().first().balance == 0L } }
            assertEquals(0L, runBlocking { wallet.observe().first().totalXp })
        } finally { db.close() }
    }
}
