package com.shadowbody.app.ui.nutrition

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shadowbody.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NutritionFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun nutritionDashboardAndGoalFlow() {
        // Navigate from dashboard to nutrition
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag("module:nutrition").performClick()
                true
            }.getOrDefault(false)
        }
        composeRule.waitForIdle()

        // Verify nutrition screen title is displayed
        composeRule.onNodeWithTag("nutritionTitle").assertIsDisplayed()

        // Open goal configuration dialog
        composeRule.onNodeWithTag("nutritionGoalButton").performClick()
        composeRule.onNodeWithTag("goalDialog").assertIsDisplayed()

        // Save goals
        composeRule.onNodeWithTag("saveGoalButton").performClick()
        composeRule.waitForIdle()

        // Add food
        composeRule.onNodeWithTag("addFoodFab").performClick()
        composeRule.onNodeWithTag("addFoodDialog").assertIsDisplayed()
        composeRule.onNodeWithTag("inputFoodName").performTextInput("Chicken Breast")
        composeRule.onNodeWithTag("inputFoodCalories").performTextInput("250")
        composeRule.onNodeWithTag("inputFoodProtein").performTextInput("45")
        composeRule.onNodeWithTag("inputFoodCarbs").performTextInput("0")
        composeRule.onNodeWithTag("inputFoodFat").performTextInput("5")
        composeRule.onNodeWithTag("saveFoodButton").performClick()
        composeRule.waitForIdle()

        // Verify food item is logged and displayed
        composeRule.onNodeWithTag("nutritionList").assertIsDisplayed()
    }
}
