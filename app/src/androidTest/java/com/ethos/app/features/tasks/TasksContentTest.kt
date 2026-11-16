package com.ethos.app.features.tasks

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.ethos.app.core.ui.components.FilterRow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import com.ethos.app.R
import com.ethos.app.core.ui.components.ContentFilter
import com.ethos.app.features.shell.TodayCreateButton
import com.ethos.app.features.shell.CreateActions
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.ui.theme.EthosTheme
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TasksContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val day = LocalDate.of(2026, 11, 6)
    private val clock = object : TaskClock {
        override fun today(): LocalDate = day
        override fun nowMillis(): Long = 100
    }

    @Test fun captureChecklistConfirmationAndUndo() {
        val repo = FakeRepository(clock)
        compose.setContent {
            val vm = remember { TasksViewModel(repo, clock, SavedStateHandle()) }
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
        compose.onNodeWithContentDescription("Criar tarefa ou hábito").performClick()
        compose.onNodeWithText("Nova tarefa").performClick()
        compose.onNodeWithText("Título").performTextInput("Preparar mochila")
        compose.onNodeWithTag("more-options").performClick()
        compose.onNodeWithText("+ Adicionar item").performScrollTo().performClick()
        compose.onNodeWithTag("checklist:0").performTextInput("Chaves")
        compose.onNodeWithText("+ Adicionar item").performScrollTo().performClick()
        compose.onNodeWithTag("checklist:1").performTextInput("Água")
        compose.onNodeWithText("Salvar").performClick()
        compose.waitUntil(15_000) { repo.saved.value.activeOccurrences.size == 1 }
        compose.onNodeWithText("Preparar mochila", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("Concluir").performScrollTo().performClick()
        compose.onNodeWithText("Restam 2 itens. Concluir mesmo assim?").assertIsDisplayed()
        compose.onNodeWithText("Concluir mesmo assim").performClick()
        compose.waitUntil(15_000) { repo.saved.value.occurrences.single().status == TaskStatus.DONE }
        compose.onNodeWithText("Desfazer").performScrollTo().performClick()
        compose.waitUntil(15_000) { repo.saved.value.occurrences.single().status == TaskStatus.OPEN }
        assertEquals(1, repo.saved.value.history.count { it.kind == AuditKind.UNDONE })
    }

    @Test fun weeklySectionIsSeparateAndHabitsFilterDoesNotShowTasks() {
        var s = TaskRules.apply(TaskSnapshot(), TaskAction.Create("day", TaskFields(title = "Dia",
            deadline = DeadlineKind.DAY, date = day), RepeatRule(start = day)), day, 10)
        s = TaskRules.apply(s, TaskAction.Create("week", TaskFields(title = "Semana",
            deadline = DeadlineKind.WEEK, date = day), RepeatRule(start = day)), day, 10)
        compose.setContent {
            var filter by remember { mutableStateOf(ContentFilter.ALL) }
            EthosTheme {
                TasksContent(TasksUiState(s, day, loading = false), TaskPage.TODAY, filter,
                    {}, {}, {}, {}) {
                    FilterRow(filter) { filter = it }
                }
            }
        }
        compose.onNodeWithText("Dia").assertExists()
        compose.onNodeWithText("Nesta semana · sem dia definido").assertExists()
        compose.onNodeWithText("Semana").assertExists()
        compose.onNodeWithText("Hábitos").performClick()
        compose.onNodeWithText("Dia").assertDoesNotExist()
        compose.onNodeWithText("Semana").assertDoesNotExist()
    }

    private class FakeRepository(private val clock: TaskClock) : TaskRepository {
        val saved = MutableStateFlow(TaskSnapshot())
        override fun observe() = saved
        override suspend fun execute(action: TaskAction) {
            saved.value = TaskRules.apply(saved.value, action, clock.today(), clock.nowMillis())
        }
    }
}
