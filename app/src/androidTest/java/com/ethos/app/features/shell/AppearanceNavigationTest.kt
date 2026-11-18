package com.ethos.app.features.shell

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ethos.app.MainActivity
import org.junit.Rule
import org.junit.Test

class AppearanceNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    /** Tema is a row that opens the options (spec 025); the dialog shows which one is saved. */
    @Test fun appearanceRemainsSelectedAfterRecreationAndNavigation() {
        compose.onNodeWithContentDescription("Abrir configurações").performClick()
        choose("Escuro")
        assertSaved("Escuro")
        compose.activityRule.scenario.recreate()
        assertSaved("Escuro")
        compose.onNodeWithContentDescription("Abrir Hoje").performClick()
        compose.onNodeWithContentDescription("Abrir configurações").performClick()
        assertSaved("Escuro")
        choose("Claro")
        assertSaved("Claro")
        choose("Seguir o sistema")
        assertSaved("Seguir o sistema")
    }

    private fun choose(text: String) {
        compose.onNodeWithText("Tema").performClick()
        compose.onNode(hasText(text) and isSelectable()).performClick()
    }

    private fun assertSaved(text: String) {
        compose.onNodeWithText("Tema").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodes(hasText(text) and isSelected()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasText(text) and isSelectable()).assertIsSelected()
        compose.onNodeWithText("Cancelar").performClick()
    }
}
