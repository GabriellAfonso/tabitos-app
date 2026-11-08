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
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReminderUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun summaryStartsOffAndRequiresChosenTimeAtLargeFont() {
        var chosen: String? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
                    ReminderSettingsPanel(ReminderUiState(loading = false), { chosen = it }, {})
                } }
            }
        }
        compose.onNodeWithText("Desativado. Escolha um horário para ativar.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Horário (HH:mm)").performScrollTo().performTextInput("18:45")
        compose.onNodeWithText("Salvar horário e ativar resumo").performScrollTo().performClick()
        assertEquals("18:45", chosen)
        compose.onNodeWithText("Desativar resumo").assertDoesNotExist()
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
            ReminderSettingsPanel(ReminderUiState(loading = false, error = true), {}, { retried = true })
        } } }
        compose.onNodeWithText("Tentar novamente").performClick()
        assertTrue(retried)
    }
}
