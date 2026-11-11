package com.ethos.app.features.shell

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ethos.app.core.ui.theme.EthosTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

/** The shell only frames destinations; each page shows a marker so navigation can be asserted. */
class ShellNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun launch(pendingCount: Int = 0) {
        compose.setContent {
            EthosTheme {
                EthosShell(
                    today = LocalDate.of(2026, 11, 11),
                    pendingCount = pendingCount,
                    createActions = CreateActions(taskEnabled = true, habitEnabled = true, {}, {}, {}),
                    drawerLists = {},
                ) { destination, _ -> Text("page:${destination.route} " + stringResource(destination.title)) }
            }
        }
    }

    @Test fun repeatedRootsReturnToTodayWithoutDuplicates() {
        launch()
        repeat(10) { compose.onNodeWithContentDescription("Abrir Tarefas").performClick() }
        compose.onNodeWithText("page:tasks", substring = true).assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("page:today", substring = true).assertIsDisplayed()
    }

    @Test fun drawerClosesOnBackAndOpensSecondaryDestinations() {
        launch(pendingCount = 2)
        compose.onNodeWithContentDescription("Abrir menu").performClick()
        compose.onNodeWithText("2").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("2").assertIsNotDisplayed()
        compose.onNodeWithContentDescription("Abrir menu").performClick()
        compose.onNodeWithText("Histórico de moedas").performClick()
        compose.onNodeWithText("page:history", substring = true).assertIsDisplayed()
    }

    @Test fun createMenuOffersTaskHabitAndBreakingHabit() {
        launch()
        compose.onNodeWithContentDescription("Criar", substring = true).performClick()
        compose.onNodeWithText("Nova tarefa").assertIsDisplayed()
        compose.onNodeWithText("Novo hábito").assertIsDisplayed()
        compose.onNodeWithText("Quebrar hábito").assertIsDisplayed()
    }
}
