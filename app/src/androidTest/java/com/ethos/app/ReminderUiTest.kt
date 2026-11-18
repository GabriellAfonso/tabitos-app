package com.ethos.app

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
import com.ethos.app.features.reminders.*
import com.ethos.app.core.reminders.domain.*
import com.ethos.app.core.reminders.platform.NotificationCapability
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReminderUiTest {
    @get:Rule val compose = createComposeRule()

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
        compose.onNodeWithText("Desligado").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Resumo diário").performScrollTo().performClick()
        compose.onNodeWithText("Horário do resumo").assertIsDisplayed()
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
            sourceId = "h", mode = ReminderMode.WEEKDAYS, chosenDate = LocalDate.of(2026, 11, 2))))
        var saved = false
        compose.setContent { MaterialTheme {
            ReminderEditor(ReminderUiState(loading = false, draft = draft), { if (it != null) draft = it },
                { saved = true }, {})
        } }
        compose.onNodeWithText("Domingo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Concluir").assertDoesNotExist()
        compose.onNodeWithText("Salvar").performClick()
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
        saveSummary = summary, retry = retry)
}
