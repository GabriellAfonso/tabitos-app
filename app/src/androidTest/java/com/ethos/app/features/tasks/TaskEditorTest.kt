package com.ethos.app.features.tasks

import com.ethos.app.core.ui.components.ContentFilter
import com.ethos.app.features.shell.TodayCreateButton
import com.ethos.app.features.shell.CreateActions
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
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

/** Specs 013/029: compact capture, "Mais" growing the sheet, review visibility, edit layout and discard. */
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
                    Column {
                        TodayCreateButton(CreateActions(
                            taskEnabled = true, habitEnabled = true,
                            onNewTask = vm::newTask, onNewHabit = {}, onBreakHabit = {},
                        ))
                        TasksContent(state, TaskPage.LIST, ContentFilter.ALL, vm::detail, vm::action,
                            vm::retry, vm::dismissError)
                    }
                }
            }
        }
        compose.waitForIdle()
        return created
    }

    @Test fun titleOnlyCaptureUsesDefaultsAndHidesAdvancedFields() {
        val repo = FakeRepository(clock)
        show(repo)
        compose.onNodeWithContentDescription("Criar tarefa ou hábito").performClick()
        compose.onNodeWithText("Nova tarefa").performClick()
        compose.onNodeWithTag("row:IMPORTANCE").assertDoesNotExist()
        compose.onNodeWithContentDescription("Prazo: Hoje").assertExists()
        compose.onNodeWithTag("editor-name").performTextInput("Comprar pão")
        compose.onNodeWithTag("editor-save").performClick()
        compose.waitUntil(15_000) { repo.saved.value.activeOccurrences.size == 1 }
        val o = repo.saved.value.activeOccurrences.single()
        assertEquals(day, o.dueDate)
        assertEquals(1, o.fields.difficulty)
        assertEquals(RepeatKind.NONE, repo.saved.value.definitions.single().rule.kind)
        compose.onNodeWithTag("editor-sheet").assertDoesNotExist()
    }

    @Test fun moreExpandsTheSheetAndCollapseKeepsTheDraft() {
        val repo = FakeRepository(clock)
        show(repo)
        compose.onNodeWithContentDescription("Criar tarefa ou hábito").performClick()
        compose.onNodeWithText("Nova tarefa").performClick()
        compose.onNodeWithTag("editor-name").performTextInput("Pagar luz")
        compose.onNodeWithTag("editor-more").performClick()
        compose.onNodeWithTag("row:IMPORTANCE").performScrollTo().performClick()
        compose.onNodeWithText("Alta").performClick()
        compose.onNodeWithText("Pronto").performClick()
        compose.onNodeWithTag("row:IMPORTANCE").assertTextContains("Alta", substring = true)
        compose.onNodeWithContentDescription("Recolher").performClick()
        compose.onNodeWithTag("editor-more").assertExists()
        compose.onNodeWithText("Pagar luz").assertExists()
    }

    @Test fun editingOpensExpandedWithoutCollapse() {
        val repo = FakeRepository(clock)
        runBlocking { repo.execute(TaskAction.Create("t", TaskFields(title = "Existente"), RepeatRule(start = day))) }
        val vm = show(repo)
        compose.runOnIdle { vm.edit(vm.state.value.snapshot.activeOccurrences.single()) }
        compose.onNodeWithText("Editar tarefa").assertExists()
        compose.onNodeWithTag("editor-more").assertDoesNotExist()
        compose.onNodeWithTag("row:DESCRIPTION").assertExists()
        compose.onNodeWithContentDescription("Fechar").assertExists()
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
        compose.onNodeWithContentDescription("Criar tarefa ou hábito").performClick()
        compose.onNodeWithText("Nova tarefa").performClick()
        compose.onNodeWithTag("editor-name").performTextInput("Rascunho")
        tapOutsideSheet()
        compose.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        compose.onNodeWithText("Continuar editando").performClick()
        compose.onNodeWithText("Rascunho").assertExists()
        tapOutsideSheet()
        compose.onNodeWithText("Descartar").performClick()
        compose.onNodeWithText("Rascunho").assertDoesNotExist()
        assertEquals(0, repo.saved.value.activeOccurrences.size)
    }

    /** The compact sheet sits at the bottom of its dialog; a tap near the top lands on the area outside it. */
    private fun tapOutsideSheet() {
        compose.onAllNodes(isRoot()).onLast().performTouchInput { click(Offset(centerX, top + OUTSIDE_TAP_Y)) }
    }

    private class FakeRepository(private val clock: TaskClock) : TaskRepository {
        val saved = MutableStateFlow(TaskSnapshot())
        override fun observe() = saved
        override suspend fun execute(action: TaskAction) {
            saved.value = TaskRules.apply(saved.value, action, clock.today(), clock.nowMillis())
        }
    }
}

private const val OUTSIDE_TAP_Y = 120f
