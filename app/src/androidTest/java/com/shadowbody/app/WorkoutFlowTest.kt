package com.shadowbody.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/**
 * Phase 3 instrumented flow on a real emulator: hall -> create plan with
 * two exercises -> start -> log + complete a set -> rest timer controls ->
 * finish -> result -> history -> restart persists.
 */
class WorkoutFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun fullWorkoutFlow() {
        // 1-2. Launch, open the workout section.
        composeRule.onNodeWithText("SHADOW BODY").assertIsDisplayed()
        composeRule.onNodeWithTag("module:workout").performClick()
        composeRule.onNodeWithText("TRAINING HALL").assertIsDisplayed()

        // 3-4. Empty plans, library seeded.
        composeRule.onNodeWithText("[ NO PLANS FORGED ]").assertIsDisplayed()

        // 5-7. Create a plan with two exercises.
        composeRule.onNodeWithTag("planNew").performClick()
        composeRule.onNodeWithText("FORGE PLAN").assertIsDisplayed()
        composeRule.onNodeWithTag("planName").performTextInput("Test Plan")
        composeRule.onNodeWithTag("slotAdd").performClick()
        composeRule.onNodeWithText("SELECT EXERCISE").assertIsDisplayed()
        composeRule.onNodeWithTag("pickSearch").performTextInput("Push")
        composeRule.onNodeWithTag("pick:Push-Up").performClick()
        composeRule.onNodeWithTag("slotReps:0").performScrollTo()
        composeRule.onNodeWithTag("slotReps:0").performTextClearance()
        composeRule.onNodeWithTag("slotReps:0").performTextInput("12")
        composeRule.onNodeWithTag("slotAdd").performClick()
        composeRule.onNodeWithText("SELECT EXERCISE").assertIsDisplayed()
        composeRule.onNodeWithTag("pickSearch").performTextInput("Squat")
        composeRule.onNodeWithTag("pick:Bodyweight Squat").performClick()
        composeRule.onNodeWithTag("planSave").performClick()

        // 8. Plan detail shows both slots.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("TEST PLAN").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("1. PUSH-UP").assertIsDisplayed()
        composeRule.onNodeWithText("2. BODYWEIGHT SQUAT").assertIsDisplayed()

        // 9. Start the workout.
        composeRule.onNodeWithTag("planStart").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("0/6 SETS").assertIsDisplayed()
            }.isSuccess
        }

        // 10-11. Log actuals on set 1 and complete it.
        composeRule.onNodeWithTag("setReps:0:1").performScrollTo()
        composeRule.onNodeWithTag("setReps:0:1").performTextInput("10")
        composeRule.onNodeWithTag("setDone:0:1").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("1/6 SETS").assertIsDisplayed()
            }.isSuccess
        }

        // 12. Rest timer: start, pause, resume, reset.
        composeRule.onNodeWithTag("activeList").performScrollToIndex(3)
        composeRule.onNodeWithTag("restStart:30").performClick()
        composeRule.onNodeWithTag("restRemaining").assertIsDisplayed()
        composeRule.onNodeWithTag("restPause").performClick()
        composeRule.onNodeWithTag("restResume").assertIsDisplayed()
        composeRule.onNodeWithTag("restResume").performClick()
        composeRule.onNodeWithTag("restReset").performClick()

        // 13-14. Finish, result screen appears.
        composeRule.onNodeWithTag("sessionFinish").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("QUEST COMPLETE").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithTag("resultSets").assertIsDisplayed()

        // 15. Back to hall: history shows the completed session.
        composeRule.onNodeWithTag("resultDone").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("TRAINING HALL").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("DONE").assertIsDisplayed()

        // 16-17. Restart persists history (plan + DONE session both present).
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 8_000) {
            runCatching {
                composeRule.onNodeWithText("TRAINING HALL").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("DONE").assertIsDisplayed()
    }
}
