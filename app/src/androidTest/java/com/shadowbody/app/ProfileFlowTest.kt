package com.shadowbody.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import org.junit.Rule
import org.junit.Test

/**
 * Phase 2 instrumented flow on a real emulator:
 * launch -> profile empty -> create -> persists -> baseline -> history ->
 * edit -> updated -> activity restart -> still persisted.
 */
class ProfileFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun fullProfileAndBaselineFlow() {
        // 1-2. Launch shows honest empty state.
        composeRule.onNodeWithText("[ PROFILE NOT CONFIGURED ]").assertIsDisplayed()

        // 3. Open profile, create one.
        composeRule.onNodeWithTag("module:profile").performClick()
        composeRule.onNodeWithText("PLAYER PROFILE").assertIsDisplayed()
        composeRule.onNodeWithTag("profileCreate").performClick()

        composeRule.onNodeWithTag("profileAge").performTextInput("28")
        composeRule.onNodeWithTag("profileHeight").performTextInput("178")
        composeRule.onNodeWithTag("profileWeight").performTextInput("75")

        composeRule.onNodeWithTag("level:Intermediate").performScrollTo()
        composeRule.onNodeWithTag("level:Intermediate").performClick()
        composeRule.onNodeWithTag("equip:Bodyweight").performScrollTo()
        composeRule.onNodeWithTag("equip:Bodyweight").performClick()
        composeRule.onNodeWithTag("goal:Build strength").performScrollTo()
        composeRule.onNodeWithTag("goal:Build strength").performClick()
        composeRule.onNodeWithTag("day:Mon").performScrollTo()
        composeRule.onNodeWithTag("day:Mon").performClick()
        composeRule.onNodeWithTag("session:45").performScrollTo()
        composeRule.onNodeWithTag("session:45").performClick()

        // 4. Save, back on profile view with values.
        composeRule.onNodeWithTag("profileSave").performScrollTo()
        composeRule.onNodeWithTag("profileSave").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("INTERMEDIATE").assertIsDisplayed()
            }.isSuccess
        }

        // 5-6. Open history (empty archive honest state).
        composeRule.onNodeWithText("BASELINE HISTORY").performScrollTo()
        composeRule.onNodeWithText("BASELINE HISTORY").performClick()
        composeRule.onNodeWithText("BASELINE ARCHIVE").assertIsDisplayed()
        composeRule.onNodeWithText("[ ARCHIVE EMPTY ]").assertIsDisplayed()

        // 7-8. Add baseline, see it in history.
        composeRule.onNodeWithTag("baselineAdd").performClick()
        composeRule.onNodeWithText("NEW BASELINE").assertIsDisplayed()
        composeRule.onNodeWithTag("baselineWeight").performTextInput("80")
        composeRule.onNodeWithTag("baselineWaist").performTextInput("85")
        composeRule.onNodeWithTag("baselineSave").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("NEW BASELINE")
                .fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("1 TOTAL").assertIsDisplayed()
            }.isSuccess
        }

        // 9-10. Back to profile, edit age, verify updated value.
        Espresso.pressBack()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("PLAYER PROFILE").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithTag("profileEdit").performClick()
        composeRule.onNodeWithText("EDIT PROFILE").assertIsDisplayed()
        composeRule.onNodeWithTag("profileAge").performScrollTo()
        composeRule.onNodeWithTag("profileAge").performTextClearance()
        composeRule.onNodeWithTag("profileAge").performTextInput("29")
        composeRule.onNodeWithTag("profileSave").performScrollTo()
        composeRule.onNodeWithTag("profileSave").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("PLAYER PROFILE").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("29").assertIsDisplayed()

        // 11-12. Restart the activity: persisted data survives.
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("PLAYER PROFILE").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("29").assertIsDisplayed()
    }
}
