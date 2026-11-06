package com.ethos.app.features.shell

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.ethos.app.core.ui.theme.EthosTheme
import com.ethos.app.R
import org.junit.Rule
import org.junit.Test

class ShellNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun launch() {
        compose.setContent { EthosTheme { EthosShell(settingsContent = {}) } }
    }

    @Test fun repeatedRootsReturnToTodayWithoutDuplicates() {
        launch()
        repeat(10) { compose.onNodeWithContentDescription("Abrir Tarefas").performClick() }
        compose.onNodeWithText("Suas tarefas, no seu ritmo").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Um passo de cada vez").assertIsDisplayed()
    }

    @Test fun drawerBackAndSecondaryDestinations() {
        launch()
        compose.onNodeWithContentDescription("Abrir menu").performClick()
        compose.onNodeWithText("Histórico de moedas").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Histórico de moedas").assertIsNotDisplayed()
        compose.onNodeWithContentDescription("Abrir menu").performClick()
        compose.onNodeWithText("Revisar pendências").performClick()
        compose.onNodeWithText("Tudo revisado").assertIsDisplayed()
        compose.onNodeWithTag("filter_HABITS").performClick()
        compose.onNodeWithTag("filter_HABITS").assertIsSelected()
        compose.onNodeWithText("Nenhuma pendência de hábitos.").assertIsDisplayed()
    }

    @Test fun balanceOpensHonestRewards() {
        launch()
        compose.onNodeWithContentDescription("Abrir recompensas").performClick()
        compose.onNodeWithText("Suas próximas conquistas").assertIsDisplayed()
        compose.onNodeWithText("120").assertDoesNotExist()
    }

    @Test fun independentFiltersRestoreAcrossDestinations() {
        launch()
        compose.onNodeWithTag("filter_TASKS").performClick()
        compose.onNodeWithContentDescription("Abrir menu").performClick()
        compose.onNodeWithText("Revisar pendências").performClick()
        compose.onNodeWithTag("filter_ALL").assertIsSelected()
        compose.onNodeWithTag("filter_HABITS").performClick()
        compose.onNodeWithContentDescription("Abrir Hoje").performClick()
        compose.onNodeWithTag("filter_TASKS").assertIsSelected()
        compose.onNodeWithContentDescription("Abrir menu").performClick()
        compose.onNodeWithText("Revisar pendências").performClick()
        compose.onNodeWithTag("filter_HABITS").assertIsSelected()
    }

    @Test fun populatedSamplesFilterAndShowActualPendingCount() {
        val task = ShellItem("task", ContentFilter.TASKS, R.string.sample_task, R.string.sample_task_meta)
        val habit = ShellItem("habit", ContentFilter.HABITS, R.string.sample_habit, R.string.sample_habit_meta)
        val sample = ShellPresentation(todayItems = listOf(task, habit), pendingItems = listOf(task, habit))
        compose.setContent { EthosTheme { EthosShell(sample, settingsContent = {}) } }
        compose.onNodeWithText("Você tem 2 pendências para revisar").assertIsDisplayed()
        compose.onNodeWithTag("filter_TASKS").performClick()
        compose.onNodeWithText("Caminhar por 20 minutos").assertExists()
        compose.onNodeWithText("Ler algumas páginas").assertDoesNotExist()
        compose.onNodeWithTag("filter_HABITS").performClick()
        compose.onNodeWithText("Ler algumas páginas").assertExists()
        compose.onNodeWithText("Caminhar por 20 minutos").assertDoesNotExist()
    }
}
