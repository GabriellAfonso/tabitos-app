package com.ethos.app.features.tasks

import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.ui.theme.EthosTheme
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Spec 013: quick capture, "Mais opções", date field semantics, review visibility and discard. */
class TaskEditorTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val day = LocalDate.of(2026, 11, 10)
    private val clock = object : TaskClock {
        override fun today(): LocalDate = day
        override fun nowMillis(): Long = 100
    }

    private fun show(repo: FakeRepository): TasksViewModel {
        lateinit var created: TasksViewModel
        compose.setContent {
            val vm = remember { TasksViewModel(repo, clock, SavedStateHandle()).also { created = it } }
            EthosTheme {
                TasksScreen(vm) { state ->
                    TasksContent(state, TaskPage.LIST, 0, vm::newTask, vm::detail, vm::action,
                        {}, vm::retry, vm::dismissError)
                }
            }
        }
        compose.waitForIdle()
        return created
    }

    @Test fun titleOnlyCaptureUsesDefaultsAndHidesAdvancedFields() {
        val repo = FakeRepository(clock)
        show(repo)
        compose.onNodeWithText("Nova tarefa").performClick()
        compose.onNodeWithText("Dificuldade").assertDoesNotExist()
        compose.onNodeWithContentDescription("Data, Hoje").assertExists()
        compose.onNodeWithText("Título").performTextInput("Comprar pão")
        compose.onNodeWithText("Salvar").performClick()
        compose.waitUntil(15_000) { repo.saved.value.activeOccurrences.size == 1 }
        val o = repo.saved.value.activeOccurrences.single()
        assertEquals(day, o.dueDate)
        assertEquals(1, o.fields.difficulty)
        assertEquals(RepeatKind.NONE, repo.saved.value.definitions.single().rule.kind)
        compose.onNodeWithTag("more-options").assertDoesNotExist()
    }

    @Test fun reviewConfirmationAppearsOnlyWhenOverdueDateChanges() {
        val past = day.minusDays(2)
        val repo = FakeRepository(clock)
        runBlocking { repo.execute(TaskAction.Create("late", TaskFields(title = "Atrasada",
            deadline = DeadlineKind.DAY, date = past), RepeatRule(start = past))) }
        val vm = show(repo)
        compose.runOnIdle { vm.edit(vm.state.value.snapshot.activeOccurrences.single()) }
        compose.onNodeWithText("Entendi, registrar o prazo perdido").assertDoesNotExist()
        compose.runOnIdle { val d = vm.state.value.draft!!; vm.updateDraft(d.copy(dateText = day.toString())) }
        compose.onNodeWithText("Entendi, registrar o prazo perdido").performScrollTo().assertIsDisplayed()
    }

    @Test fun backWithChangesAsksBeforeDiscarding() {
        val repo = FakeRepository(clock)
        show(repo)
        compose.onNodeWithText("Nova tarefa").performClick()
        compose.onNodeWithText("Título").performTextInput("Rascunho")
        compose.onNodeWithText("Voltar").performClick()
        compose.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        compose.onNodeWithText("Continuar editando").performClick()
        compose.onNodeWithText("Rascunho").assertExists()
        compose.onNodeWithText("Voltar").performClick()
        compose.onNodeWithText("Descartar").performClick()
        compose.onNodeWithText("Rascunho").assertDoesNotExist()
        assertEquals(0, repo.saved.value.activeOccurrences.size)
    }

    private class FakeRepository(private val clock: TaskClock) : TaskRepository {
        val saved = MutableStateFlow(TaskSnapshot())
        override fun observe() = saved
        override suspend fun execute(action: TaskAction) {
            saved.value = TaskRules.apply(saved.value, action, clock.today(), clock.nowMillis())
        }
    }
}
