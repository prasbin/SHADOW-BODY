package com.shadowbody.app

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Phase 4 instrumented flow on a real emulator.
 *
 * Covers what a user can actually observe: readiness is recorded and
 * validated, every recommended target carries an explanation, adopting turns
 * the snapshot into a real plan, and skipping changes nothing about history.
 *
 * Each test starts from an empty workout/adaptive state so the suite does not
 * depend on execution order. Scrolling goes through the list container and a
 * matcher, because lazy content that is off screen is not composed at all.
 */
class AdaptiveFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearTrainingState() {
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as ShadowBodyApp
        val db = app.database.openHelper.writableDatabase
        listOf(
            "workout_recommendation",
            "readiness_report",
            "missed_workout",
            "exercise_adaptation",
            "adaptation_checkpoint",
            "workout_session",
            "workout_plan",
        ).forEach { table -> db.execSQL("DELETE FROM $table") }
    }

    private fun awaitNode(matcher: SemanticsMatcher, timeoutMillis: Long = 10_000) {
        composeRule.waitUntil(timeoutMillis) {
            runCatching {
                composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
    }

    private fun awaitTag(tag: String, timeoutMillis: Long = 10_000) =
        awaitNode(hasTestTag(tag), timeoutMillis)

    private fun awaitText(text: String, timeoutMillis: Long = 10_000) =
        awaitNode(hasText(text, substring = true), timeoutMillis)

    /** Brings a lazy-list child into view by matcher, then asserts it is shown. */
    private fun showInList(listTag: String, matcher: SemanticsMatcher) {
        awaitTag(listTag)
        composeRule.onNodeWithTag(listTag).performScrollToNode(matcher)
        composeRule.onNode(matcher).assertIsDisplayed()
    }

    private fun showTagInList(listTag: String, tag: String) =
        showInList(listTag, hasTestTag(tag))

    private fun showTextInList(listTag: String, text: String) =
        showInList(listTag, hasText(text, substring = true))

    private fun clickTagInList(listTag: String, tag: String) {
        showTagInList(listTag, tag)
        composeRule.onNodeWithTag(tag).performClick()
    }

    /** For controls outside the lazy list, such as top-app-bar actions. */
    private fun clickTag(tag: String) {
        awaitTag(tag)
        composeRule.onNodeWithTag(tag).performClick()
    }

    private fun openDashboardModule(moduleTag: String) {
        goToDashboard()
        clickTagInList("dashboardList", moduleTag)
    }

    private fun isShowing(tag: String): Boolean =
        runCatching {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)

    /**
     * Walks back to the dashboard. A configuration change restores the previous
     * route, so tests must never assume where they currently are.
     */
    private fun goToDashboard() {
        repeat(8) {
            if (isShowing("dashboardList")) return
            pressSystemBack()
        }
        awaitTag("dashboardList")
    }

    private fun pressSystemBack() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }

    private fun openAdaptive() {
        openDashboardModule("module:adaptive")
        awaitTag("adaptiveTitle")
    }

    private fun openHall() {
        openDashboardModule("module:workout")
        awaitText("TRAINING HALL")
    }

    @Test
    fun readinessCheckInIsValidatedAndStored() {
        // 1. The adaptive module is reachable and starts empty — no invented
        //    recommendation, and nothing adoptable yet.
        openAdaptive()
        showTagInList("adaptiveList", "adaptiveEmpty")
        assertNodeCount(hasTestTag("adaptiveAdopt"), 0)

        // 2. A refused check-in names the fields that are missing.
        clickTagInList("adaptiveList", "readinessSave")
        showTextInList("adaptiveList", "Rate fatigue 1-5.")
        showTextInList("adaptiveList", "Rate soreness 1-5.")

        // 3. A valid check-in is stored and shown back as the latest report.
        clickTagInList("adaptiveList", "readinessFatigue:2")
        clickTagInList("adaptiveList", "readinessSoreness:4")
        showTagInList("adaptiveList", "readinessNotes")
        composeRule.onNodeWithTag("readinessNotes").performTextInput("legs ok")
        clickTagInList("adaptiveList", "readinessSave")
        showTagInList("adaptiveList", "readinessLast")
        assertNodeCount(hasTestTag("readinessFatigue:error"), 0)

        // 4. It survives a restart: readiness is local and persistent.
        composeRule.activityRule.scenario.recreate()
        openAdaptive()
        showTextInList("adaptiveList", "Last: fatigue 2/5")
    }

    @Test
    fun generatedWorkoutExplainsEveryTargetAndCanBeAdopted() {
        // 1. Generate from the seeded bodyweight library (no profile needed).
        openAdaptive()
        clickTagInList("adaptiveList", "adaptiveGenerate")
        awaitTag("adaptiveName")
        showTagInList("adaptiveList", "adaptiveName")

        // 2. Every recommended exercise shows a target and a reason for it.
        awaitTag("adaptiveTarget:1")
        showTagInList("adaptiveList", "adaptiveTarget:1")
        // An untracked exercise is never described as earned progress.
        showTagInList("adaptiveList", "adaptiveReason:1")
        assertText("not enough recent performance data")

        // 3. Adopting converts the snapshot into a real Phase 3 plan.
        awaitTag("adaptiveAdopt")
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag("adaptiveAdopt").assertIsEnabled()
            }.isSuccess
        }
        clickTagInList("adaptiveList", "adaptiveAdopt")
        showTextInList("adaptiveList", "Saved as a plan")

        // 4. The plan really exists in the training hall.
        composeRule.activityRule.scenario.recreate()
        openHall()
        showTextInList("hallList", "ADAPTIVE SESSION")
    }

    @Test
    fun skipIsRecordedAndLeavesHistoryUntouched() {
        // 1. A real plan gives the generator something to adapt.
        openHall()
        clickTag("planNew") // FAB outside list
        awaitText("FORGE PLAN")
        composeRule.onNodeWithTag("planName").performTextInput("Skip Base")
        clickTagInList("editorList", "slotAdd")
        awaitText("SELECT EXERCISE")
        composeRule.onNodeWithTag("pickSearch").performTextInput("Push")
        composeRule.onNodeWithTag("pick:Push-Up").performClick()
        clickTag("planSave") // bottom bar, outside the list
        awaitText("SKIP BASE")

        // 2. Generate, then skip: an explicit user action, reported honestly.
        composeRule.activityRule.scenario.recreate()
        openAdaptive()
        clickTagInList("adaptiveList", "adaptiveGenerate")
        awaitTag("adaptiveName")
        awaitTag("adaptiveSkip")
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag("adaptiveSkip").assertIsEnabled()
            }.isSuccess
        }
        clickTagInList("adaptiveList", "adaptiveSkip")
        showTextInList("adaptiveList", "Skip recorded")

        // 3. A skipped recommendation can no longer be adopted.
        showTagInList("adaptiveList", "adaptiveAdopt")
        composeRule.onNodeWithTag("adaptiveAdopt").assertIsNotEnabled()

        // 4. No session was created or deleted: history is still empty and the
        //    plan is untouched.
        composeRule.activityRule.scenario.recreate()
        openHall()
        showTextInList("hallList", "No workouts recorded yet")
        showTextInList("hallList", "SKIP BASE")
    }

    private fun assertNodeCount(matcher: SemanticsMatcher, expected: Int) {
        assertEquals(
            "unexpected number of nodes matching $matcher",
            expected,
            composeRule.onAllNodes(matcher).fetchSemanticsNodes().size,
        )
    }

    /** The text of a node that is already on screen. */
    private fun assertText(text: String, substring: Boolean = true) {
        val nodes = composeRule.onAllNodes(hasText(text, substring = substring))
            .fetchSemanticsNodes()
        assertTrue("no node with text '$text' is present", nodes.isNotEmpty())
    }
}
