package com.tabitos.app.features.shell

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tabitos.app.core.ui.theme.TabitosTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

/** The shell only frames destinations; each page shows a marker so navigation can be asserted. */
class ShellNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun launch(pendingCount: Int = 0) {
        compose.setContent {
            TabitosTheme {
                TabitosShell(
                    today = LocalDate.of(2026, 11, 11),
                    pendingCount = pendingCount,
                    gameMode = true,
                    createActions = CreateActions(taskEnabled = true, habitEnabled = true, {}, {}, {}),
                    drawerLists = {},
                ) { destination, navigate ->
                    Column {
                        Text("page:${destination.route} " + stringResource(destination.title))
                        if (destination == ShellDestination.SETTINGS) {
                            Button({ navigate(ShellDestination.EXCLUDED_HABITS) }) { Text("go:excluded") }
                        }
                    }
                }
            }
        }
    }

    @Test fun repeatedRootsReturnToTodayWithoutDuplicates() {
        launch()
        repeat(10) {
            compose.onNodeWithContentDescription("Abrir menu").performClick()
            compose.onNodeWithText("Calendário").performClick()
        }
        compose.onNodeWithText("page:calendar", substring = true).assertIsDisplayed()
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
        compose.onNodeWithText("Recompensas").performClick()
        compose.onNodeWithText("page:rewards", substring = true).assertIsDisplayed()
    }

    @Test fun settingsSubPageShowsBackAndReturnsToSettings() {
        launch()
        compose.onNodeWithContentDescription("Abrir configurações").performClick()
        compose.onNodeWithText("go:excluded").performClick()
        compose.onNodeWithText("page:excluded-habits", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("page:settings", substring = true).assertIsDisplayed()
    }

    @Test fun createMenuOffersTaskHabitAndBreakingHabit() {
        launch()
        compose.onNodeWithContentDescription("Criar", substring = true).performClick()
        compose.onNodeWithText("Criar tarefa").assertIsDisplayed()
        compose.onNodeWithText("Criar bom hábito").assertIsDisplayed()
        compose.onNodeWithText("Quebrar mau hábito").assertIsDisplayed()
    }
}
