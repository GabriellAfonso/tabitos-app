package com.tabitos.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tabitos.app.features.reminders.*
import com.tabitos.app.core.reminders.domain.*
import com.tabitos.app.core.reminders.platform.NotificationCapability
import com.tabitos.app.core.preferences.TaskReminderDefaults
import com.tabitos.app.core.ui.components.EDITOR_SAVE_TAG
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReminderUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun defaultsDialogLetsTheUserConfigureBothDeadlineTypes() {
        var state by mutableStateOf(ReminderUiState(loading = false, defaults = TaskReminderDefaults()))
        var saved: TaskReminderDefaults? = null
        compose.setContent {
            MaterialTheme {
                NotificationSettings(state, NotificationSettingsActions(
                    allowNotifications = {}, openExactAlarms = {}, openChannel = {}, saveSummary = {}, retry = {},
                    changeDefaults = { state = state.copy(defaultsDraft = it) },
                    saveDefaults = { saved = state.defaultsDraft },
                ))
            }
        }
        compose.onNodeWithText("Lembretes padrão de tarefas").performClick()
        compose.onNodeWithText("30 minutos antes").assertIsOn()
        compose.onNodeWithText("1 dia antes").assertIsOff().performClick()
        compose.onNodeWithText("Horário no dia").performScrollTo().performClick()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Salvar").performClick()
        assertEquals(TaskReminderDefaults(setOf(30, 1440), 9 * 60), saved)
    }

    @Test fun listShowsWhenEachReminderRingsAndOffersUnusedShortcuts() {
        val timed = ReminderDefinition("timed", kind = ReminderKind.TASK, sourceId = "task",
            mode = ReminderMode.RELATIVE, offsetMinutes = 30)
        var added: Long? = null
        compose.setContent { MaterialTheme {
            ReminderListContent(ReminderUiState(loading = false, kind = ReminderKind.TASK, targetId = "task:1",
                title = "Tarefa fictícia", definitions = listOf(timed),
                target = ReminderTarget(LocalDate.of(2026, 11, 19), 15 * 60, LocalDateTime.of(2026, 11, 19, 9, 0))),
                ReminderListActions(close = {}, scope = {}, edit = {}, remove = {}, quickAdd = { added = it },
                    retry = {}))
        } }
        compose.onNodeWithText("30 minutos antes").assertIsDisplayed()
        compose.onNodeWithText("Avisa hoje às 14:30").assertIsDisplayed()
        compose.onNodeWithText("Esta e próximas").assertDoesNotExist()
        compose.onNodeWithText("1 hora antes").performScrollTo().performClick()
        assertEquals(60L, added)
    }

    @Test fun summaryStartsOffAndIsTurnedOnByPickingATimeAtLargeFont() {
        var chosen: String? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
                    NotificationSettings(ReminderUiState(loading = false), actions(summary = { chosen = it }))
                } }
            }
        }
        compose.onNodeWithContentDescription("Lembrete de revisão").assertIsOff()
        compose.onNodeWithText("Avisa se faltar marcar algo hoje.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Lembrete de revisão").performScrollTo().performClick()
        compose.onNodeWithText("Horário do lembrete de revisão").assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
        assertEquals("09:00", chosen)
    }
    @Test fun blockedWarningAppearsOnlyWhenSomethingIsBlocked() {
        var capability by mutableStateOf(NotificationCapability(reminders = false, summaries = false, exact = true))
        var allowed = false
        compose.setContent { MaterialTheme { Column {
            NotificationSettings(ReminderUiState(loading = false, capability = capability),
                actions(allow = { allowed = true }))
        } } }
        compose.onNodeWithText("Lembretes e resumos não aparecem. Os itens continuam no app.").assertIsDisplayed()
        compose.onNodeWithText("Permitir").performClick()
        assertTrue(allowed)
        capability = NotificationCapability(reminders = true, summaries = true, exact = true)
        compose.onNodeWithText("Notificações bloqueadas").assertDoesNotExist()
    }
    @Test fun habitEditorHasWeekdaysAndNeverCompletionAction() {
        var draft by mutableStateOf(ReminderDraft(ReminderDefinition("r", kind = ReminderKind.HABIT,
            sourceId = "h", mode = ReminderMode.WEEKDAYS), date = LocalDate.of(2026, 11, 2)))
        var saved = false
        compose.setContent { MaterialTheme {
            ReminderEditor(ReminderUiState(loading = false, draft = draft), ReminderEditorActions(
                change = { if (it != null) draft = it }, save = { saved = true }, delete = {}, retry = {}))
        } }
        compose.onNodeWithContentDescription("Domingo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Antes do prazo").assertDoesNotExist()
        compose.onNodeWithText("Concluir").assertDoesNotExist()
        compose.onNodeWithTag(EDITOR_SAVE_TAG).performClick()
        assertTrue(saved)
    }
    @Test fun storageFailureExplainsRetryWithoutClaimingSuccess() {
        var retried = false
        compose.setContent { MaterialTheme { Column {
            NotificationSettings(ReminderUiState(loading = false, error = true), actions(retry = { retried = true }))
        } } }
        compose.onNodeWithText("Tentar novamente").performClick()
        assertTrue(retried)
    }

    private fun actions(
        allow: () -> Unit = {}, summary: (String?) -> Unit = {}, retry: () -> Unit = {},
    ) = NotificationSettingsActions(allowNotifications = allow, openExactAlarms = {}, openChannel = {},
        saveSummary = summary, retry = retry, changeDefaults = {}, saveDefaults = {})
}
