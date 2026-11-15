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

    @Test fun excludedHabitsOpenFromSettingsAndBackReturns() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.open_settings)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.habit_excluded)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).assertIsDisplayed()
            .performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.appearance)).assertExists()
    }
}
