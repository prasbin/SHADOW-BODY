package com.shadowbody.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

/**
 * Phase 1 instrumented smoke test: dashboard launches, settings opens.
 * Run on an emulator (see README). Not part of the local unit suite.
 */
class LaunchSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardLaunchesWithTitle() {
        composeRule.onNodeWithText("SHADOW BODY").assertIsDisplayed()
        composeRule.onNodeWithText("[ SYSTEM AWAKENING ]").assertIsDisplayed()
    }

    @Test
    fun settingsOpensFromDashboard() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("SYSTEM CONFIG").assertIsDisplayed()
    }
}
