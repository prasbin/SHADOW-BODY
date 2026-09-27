package com.shadowbody.app.ui.progression

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shadowbody.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressionFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun progressionDashboardAndRefresh() {
        // Navigate from dashboard to progression
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag("module:progression").performClick()
                true
            }.getOrDefault(false)
        }
        composeRule.waitForIdle()

        // Verify progression screen title is displayed
        composeRule.onNodeWithTag("progressionTitle").assertIsDisplayed()

        // Verify level card is displayed
        composeRule.onNodeWithTag("progressionLevel").assertIsDisplayed()

        // Verify XP bar is displayed
        composeRule.onNodeWithTag("progressionXpBar").assertIsDisplayed()

        // Verify attribute cards are displayed
        composeRule.onNodeWithTag("progressionList").assertIsDisplayed()

        // Tap refresh button
        composeRule.onNodeWithTag("progressionRefresh").performClick()
        composeRule.waitForIdle()

        // Verify screen still shows
        composeRule.onNodeWithTag("progressionTitle").assertIsDisplayed()
    }
}