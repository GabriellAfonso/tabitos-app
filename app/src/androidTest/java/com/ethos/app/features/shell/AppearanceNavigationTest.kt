package com.ethos.app.features.shell

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ethos.app.MainActivity
import org.junit.Rule
import org.junit.Test

class AppearanceNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun appearanceRemainsSelectedAfterRecreationAndNavigation() {
        compose.onNodeWithContentDescription("Abrir configurações").performClick()
        compose.onNodeWithText("Escuro").performScrollTo().performClick()
        waitSelected("Escuro")
        compose.activityRule.scenario.recreate()
        waitSelected("Escuro")
        compose.onNodeWithContentDescription("Abrir Hoje").performClick()
        compose.onNodeWithContentDescription("Abrir configurações").performClick()
        compose.onNodeWithText("Escuro").assertIsSelected()
        compose.onNodeWithText("Claro").performScrollTo().performClick()
        waitSelected("Claro")
        compose.onNodeWithText("Seguir o sistema").performScrollTo().performClick()
        waitSelected("Seguir o sistema")
    }

    private fun waitSelected(text: String) {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodes(hasText(text) and isSelected()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(text).assertIsSelected()
    }
}
