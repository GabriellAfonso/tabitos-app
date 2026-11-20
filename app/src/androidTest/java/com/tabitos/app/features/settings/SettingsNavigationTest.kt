package com.tabitos.app.features.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.tabitos.app.MainActivity
import com.tabitos.app.R
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

    @Test fun earningsOpenFromSettingsAndBackReturns() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.open_settings)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.earnings_setting)).performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.finance_edit_table)).performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.settings_data)).performScrollTo().assertIsDisplayed()
    }
}
