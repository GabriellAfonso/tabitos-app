package com.ethos.app.features.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.ethos.app.MainActivity
import com.ethos.app.R
import org.junit.Rule
import org.junit.Test

/** Uses the real Activity and navigation owner: content-only tests cannot detect a missing Hilt factory. */
class SettingsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun settingsOpensAndDemoResetCanBeCanceledWithoutWriting() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.open_settings)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.demo_reset)).performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.demo_confirmation)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.cancel)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.demo_title)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.demo_confirmation)).assertDoesNotExist()
    }
}
