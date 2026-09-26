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
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Phase 5 instrumented flow on a real emulator.
 *
 * Covers what a user can actually observe: the built-in routine exists on a
 * fresh install, each step is recorded honestly, a skip is never counted as
 * work done, a timed step has a countdown that can be extended, paused and
 * resumed, a run that is only skipped is not recorded as a completion,
 * completing the routine locks the day, an unfinished run survives a restart,
 * recorded history is read-only, and a routine can be created and edited.
 *
 * Each test clears the morning state first, so the suite does not depend on
 * execution order. Progress waits on the stored row count rather than on
 * recomposition, so the assertions describe behaviour, not timing. Scrolling
 * goes through the list container and a matcher, because lazy content that is
 * off screen is not composed at all.
 */
class MorningFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearMorningState() {
        writableDatabase().apply {
            execSQL("DELETE FROM morning_routine_step_log")
            execSQL("DELETE FROM morning_routine_log")
            execSQL("DELETE FROM morning_routine_step WHERE isSeeded = 0")
            execSQL("DELETE FROM morning_routine WHERE seedKey IS NULL")
        }
    }

    // --- Storage helpers, so waits describe stored state, not composition ---

    private fun writableDatabase() =
        (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            as ShadowBodyApp).database.openHelper.writableDatabase

    private fun seedStepCount(): Int = count("SELECT COUNT(*) FROM morning_routine_step")

    private fun recordedStepCount(): Int = count("SELECT COUNT(*) FROM morning_routine_step_log")

    private fun storedRunCount(): Int = count("SELECT COUNT(*) FROM morning_routine_log")

    private fun completedCount(): Int =
        count("SELECT COUNT(*) FROM morning_routine_step_log WHERE outcome = 'COMPLETED'")

    private fun skippedCount(): Int =
        count("SELECT COUNT(*) FROM morning_routine_step_log WHERE outcome = 'SKIPPED'")

    private fun count(sql: String): Int = writableDatabase()
        .query(sql)
        .use { it.moveToFirst(); it.getInt(0) }

    private fun seededRoutineId(): Long = writableDatabase()
        .query("SELECT id FROM morning_routine WHERE seedKey IS NOT NULL")
        .use { it.moveToFirst(); it.getLong(0) }

    private fun routineId(name: String): Long = writableDatabase()
        .query("SELECT id FROM morning_routine WHERE name = '$name'")
        .use { it.moveToFirst(); it.getLong(0) }

    // --- Navigation and lookup helpers ---

    private fun awaitNode(matcher: SemanticsMatcher, timeoutMillis: Long = 10_000) {
        composeRule.waitUntil(timeoutMillis) {
            runCatching {
                composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
    }

    private fun awaitTag(tag: String, timeoutMillis: Long = 10_000) =
        awaitNode(hasTestTag(tag), timeoutMillis)

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

    /** For controls outside the lazy list, such as top-app-bar or bottom-bar actions. */
    private fun clickTag(tag: String) {
        awaitTag(tag)
        composeRule.onNodeWithTag(tag).performClick()
    }

    private fun isShowing(tag: String): Boolean =
        runCatching {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)

    private fun nodeCount(matcher: SemanticsMatcher): Int =
        composeRule.onAllNodes(matcher).fetchSemanticsNodes().size

    private fun assertText(text: String) {
        val nodes = composeRule.onAllNodesWithText(text, substring = true)
            .fetchSemanticsNodes()
        assertTrue("no node with text '$text' is present", nodes.isNotEmpty())
    }

    /** Whole-label match, for labels that are a substring of another label. */
    private fun assertTextExactly(text: String) {
        val nodes = composeRule.onAllNodesWithText(text, substring = false)
            .fetchSemanticsNodes()
        assertEquals("expected exactly one node with text '$text'", 1, nodes.size)
    }

    private fun assertNoText(text: String) {
        val nodes = composeRule.onAllNodesWithText(text, substring = true)
            .fetchSemanticsNodes()
        assertTrue("unexpected node with text '$text'", nodes.isEmpty())
    }

    private fun goToDashboard() {
        repeat(8) {
            if (isShowing("dashboardList")) return
            pressSystemBack()
        }
        awaitTag("dashboardList")
    }

    /**
     * System back, tolerant of the moment right after a recreation, when the
     * scenario has no live activity yet.
     */
    private fun pressSystemBack() {
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.activityRule.scenario.onActivity { activity ->
                    activity.onBackPressedDispatcher.onBackPressed()
                }
                true
            }.getOrDefault(false)
        }
        composeRule.waitForIdle()
    }

    private fun openMorning() {
        goToDashboard()
        clickTagInList("dashboardList", "module:activation")
        awaitTag("morningTitle")
    }

    /** Clicks COMPLETE on the current step and waits for it to be stored. */
    private fun completeCurrentStep() {
        val before = recordedStepCount()
        clickTagInList("morningRunList", "morningComplete")
        composeRule.waitUntil(10_000) { recordedStepCount() == before + 1 }
    }

    /** Clicks SKIP on the current step and waits for it to be stored. */
    private fun skipCurrentStep() {
        val before = recordedStepCount()
        clickTagInList("morningRunList", "morningSkip")
        composeRule.waitUntil(10_000) { recordedStepCount() == before + 1 }
    }

    /** Mirrors the run screen's `formatSeconds` so the display can be asserted. */
    private fun clock(total: Int): String =
        if (total >= 60) "${total / 60}:${(total % 60).toString().padStart(2, '0')}" else "$total"

    /**
     * The all-dealt-with panel is the first item, so it is off screen once the
     * list is scrolled to the last step: scroll to the top of the list rather
     * than searching for a node that is not composed.
     */
    private fun showAllDonePanel() {
        composeRule.onNodeWithTag("morningRunList").performScrollToIndex(0)
        composeRule.onNodeWithTag("morningRunAllDone").assertIsDisplayed()
    }

    private fun timerText(): String = composeRule
        .onAllNodesWithTag("morningTimerValue")
        .fetchSemanticsNodes()
        .first()
        .config[androidx.compose.ui.semantics.SemanticsProperties.Text]
        .joinToString { it.text }

    // --- Tests ---

    @Test
    fun theBuiltInRoutineIsSeededOnAFreshInstall() {
        // 1. The module is reachable from the dashboard and claims nothing.
        openMorning()
        showTagInList("morningList", "morningDayStatus")
        assertText("NOT_STARTED")
        showTextInList("morningList", "Nothing recorded yet today")

        // 2. The built-in routine is present and active, with no invented work.
        //    Matched by row, because the top bar carries the same name.
        val seeded = seededRoutineId()
        showTagInList("morningList", "morningRoutine:$seeded")
        assertTextExactly("ACTIVE")
        assertTrue(seedStepCount() > 1)

        // 3. History is empty on a first launch.
        showTagInList("morningList", "morningHistoryEmpty")
        assertText("NO RUNS YET")
        assertEquals(0, storedRunCount())
    }

    @Test
    fun aRunRecordsEachStepAndLocksTheDay() {
        val steps = seedStepCount()
        openMorning()

        // 1. Starting opens the run at the first step.
        clickTagInList("morningList", "morningStart")
        awaitTag("morningCurrentTitle")
        assertText("STEP 1")
        // The first step is a plain cue, so no countdown is invented for it.
        assertText("TARGET: REMINDER")
        assertEquals(0, nodeCount(hasTestTag("morningTimerValue")))

        // 2. Every step is completed explicitly.
        repeat(steps) { completeCurrentStep() }
        assertEquals(steps, completedCount())
        assertEquals(0, skippedCount())
        showAllDonePanel()

        // 3. The run can only be closed once every step is dealt with.
        clickTag("morningFinish")
        awaitTag("morningTitle")

        // 4. Today is complete, so it cannot be started again.
        showTagInList("morningList", "morningDayStatus")
        assertText("COMPLETED")
        showTagInList("morningList", "morningStart")
        composeRule.onNodeWithTag("morningStart").assertIsNotEnabled()

        // 5. The run is in history as a completion, and survives a restart.
        composeRule.activityRule.scenario.recreate()
        openMorning()
        assertText("DONE")
        assertText("ATTEMPT 1")
        assertNoText("NO RUNS YET")
    }

    @Test
    fun aSkippedStepIsRecordedButNeverCountedAsDone() {
        val steps = seedStepCount()

        openMorning()
        clickTagInList("morningList", "morningStart")
        awaitTag("morningCurrentTitle")

        // 1. Skipping is an honest record: stored, and not counted as done.
        skipCurrentStep()
        assertEquals(0, completedCount())
        assertEquals(1, skippedCount())
        showTagInList("morningRunList", "morningStepStatus:0")
        assertText("SKIPPED")

        // 2. A timed step shows a countdown that can be paused, extended and
        //    resumed. Pausing first makes the value deterministic.
        awaitTag("morningTimerValue")
        showTagInList("morningRunList", "morningTimerPause")
        composeRule.onNodeWithTag("morningTimerPause").performClick()
        showTagInList("morningRunList", "morningTimerResume")
        val pausedAt = timerText().toIntOrNull() ?: minutesToSeconds(timerText())
        clickTagInList("morningRunList", "morningTimerAdd")
        composeRule.waitUntil(10_000) { timerText() == clock(pausedAt + 30) }
        clickTagInList("morningRunList", "morningTimerResume")
        showTagInList("morningRunList", "morningTimerPause")
        composeRule.onNodeWithTag("morningTimerPause").assertIsEnabled()

        // 3. The rest is completed; one real completion plus a skip is a
        //    completion, and the skipped step is still shown as skipped.
        //    One step was skipped, so the other steps-1 are completed.
        repeat(steps - 1) { completeCurrentStep() }
        showAllDonePanel()
        clickTag("morningFinish")
        awaitTag("morningTitle")
        assertText("COMPLETED")
        assertEquals(steps - 1, completedCount())
        assertEquals(1, skippedCount())
    }

    private fun minutesToSeconds(display: String): Int {
        val parts = display.split(":")
        return if (parts.size == 2) {
            parts[0].toInt() * 60 + parts[1].toInt()
        } else {
            error("unexpected timer display: $display")
        }
    }

    @Test
    fun aRunThatIsOnlySkippedIsNeverRecordedAsACompletion() {
        val steps = seedStepCount()

        openMorning()
        clickTagInList("morningList", "morningStart")
        awaitTag("morningCurrentTitle")
        repeat(steps) { skipCurrentStep() }

        // The screen says so before it happens, in plain words.
        showAllDonePanel()
        assertText("recorded as abandoned")
        clickTag("morningFinish")
        awaitTag("morningTitle")

        showTagInList("morningList", "morningDayStatus")
        assertText("ABANDONED")
        assertEquals(0, completedCount())
        // An abandoned day is not a success, so another attempt is allowed.
        showTagInList("morningList", "morningStart")
        composeRule.onNodeWithTag("morningStart").assertIsEnabled()
    }

    @Test
    fun anUnfinishedRunResumesAfterARestart() {
        val steps = seedStepCount()

        openMorning()
        clickTagInList("morningList", "morningStart")
        awaitTag("morningCurrentTitle")
        skipCurrentStep()
        assertEquals(1, recordedStepCount())

        // 1. Leaving the run does not lose what was recorded, and does not
        //    silently start a second run.
        pressSystemBack()
        composeRule.activityRule.scenario.recreate()
        openMorning()
        assertText("Run in progress")
        showTagInList("morningList", "morningResume")
        assertEquals(1, storedRunCount())

        // 2. Resuming continues the same run and shows the recorded skip.
        clickTagInList("morningList", "morningResume")
        awaitTag("morningRunTitle")
        showTagInList("morningRunList", "morningStepStatus:0")
        assertText("SKIPPED")
        assertEquals(1, storedRunCount())

        // 3. And it can be completed from there.
        repeat(steps - 1) { completeCurrentStep() }
        clickTag("morningFinish")
        awaitTag("morningTitle")
        assertText("COMPLETED")
        assertEquals(1, storedRunCount())
    }

    @Test
    fun recordedHistoryIsReadOnly() {
        val steps = seedStepCount()

        openMorning()
        clickTagInList("morningList", "morningStart")
        awaitTag("morningCurrentTitle")
        skipCurrentStep()
        repeat(steps - 1) { completeCurrentStep() }
        clickTag("morningFinish")
        awaitTag("morningTitle")

        // 1. Opening a finished run shows the recorded result.
        val logId = writableDatabase()
            .query("SELECT id FROM morning_routine_log LIMIT 1")
            .use { it.moveToFirst(); it.getLong(0) }
        clickTagInList("morningList", "morningHistory:$logId")
        awaitTag("morningResultStatus")
        assertText("COMPLETED")
        assertText("ATTEMPT 1")
        assertText("cannot be changed")

        // 2. Nothing that could change it is offered: no decisions, no timer
        //    and no way to close it again.
        assertEquals(0, nodeCount(hasTestTag("morningFinish")))
        assertEquals(0, nodeCount(hasTestTag("morningAbandon")))
        assertEquals(0, nodeCount(hasTestTag("morningComplete")))
        assertEquals(0, nodeCount(hasTestTag("morningSkip")))
        assertEquals(0, nodeCount(hasTestTag("morningTimerValue")))
        showTagInList("morningRunList", "morningStepStatus:0")
        assertText("SKIPPED")
    }

    @Test
    fun aRoutineCanBeCreatedAndRun() {
        openMorning()

        // 1. A new routine is refused until it has a name and a usable step.
        clickTagInList("morningList", "morningNewRoutine")
        awaitTag("morningEditorTitle")
        assertText("NEW ROUTINE")
        clickTagInList("morningEditorList", "morningEditorSave")
        showTextInList("morningEditorList", "Name the routine.")
        showTextInList("morningEditorList", "Name the step.")
        showTextInList("morningEditorList", "Describe how to do it.")

        // 2. Fill it in: a counted step and a timed step.
        showTagInList("morningEditorList", "morningEditorName")
        composeRule.onNodeWithTag("morningEditorName").performTextInput("SHORT START")
        showTagInList("morningEditorList", "morningEditorStepTitle:0")
        composeRule.onNodeWithTag("morningEditorStepTitle:0").performTextInput("Wall press")
        showTagInList("morningEditorList", "morningEditorStepInstructions:0")
        composeRule.onNodeWithTag("morningEditorStepInstructions:0")
            .performTextInput("Press the wall for a count.")
        showTagInList("morningEditorList", "morningEditorStepReps:0")
        composeRule.onNodeWithTag("morningEditorStepReps:0").performTextInput("20")
        clickTagInList("morningEditorList", "morningEditorAddStep")
        showTagInList("morningEditorList", "morningEditorStepTitle:1")
        composeRule.onNodeWithTag("morningEditorStepTitle:1").performTextInput("Closing breath")
        showTagInList("morningEditorList", "morningEditorStepInstructions:1")
        composeRule.onNodeWithTag("morningEditorStepInstructions:1")
            .performTextInput("Three slow breaths.")
        showTagInList("morningEditorList", "morningEditorStepDuration:1")
        composeRule.onNodeWithTag("morningEditorStepDuration:1").performTextInput("45")
        clickTagInList("morningEditorList", "morningEditorCategory:1:BREATHING")
        clickTagInList("morningEditorList", "morningEditorSave")
        awaitTag("morningTitle")

        // 3. It is listed and runnable, and records only its own two steps.
        showTextInList("morningList", "SHORT START")
        val custom = routineId("SHORT START")
        clickTagInList("morningList", "morningRoutineStart:$custom")
        awaitTag("morningCurrentTitle")
        assertText("Wall press")
        assertText("TARGET: 20 reps")
        assertEquals(0, recordedStepCount())
        completeCurrentStep()
        awaitTag("morningCurrentTitle")
        assertText("Closing breath")
        assertText("TARGET: 45s")
        awaitTag("morningTimerValue")
        completeCurrentStep()
        clickTag("morningFinish")
        awaitTag("morningTitle")
        assertEquals(2, completedCount())
        assertEquals(1, storedRunCount())
    }

    @Test
    fun theBuiltInRoutineCanBeEditedWithoutBreakingOrder() {
        openMorning()
        val seeded = seededRoutineId()

        // 1. The built-in routine is editable, and says that edits stay local.
        clickTagInList("morningList", "morningRoutine:$seeded")
        awaitTag("morningEditorTitle")
        assertText("EDIT ROUTINE")
        showTagInList("morningEditorList", "morningEditorSeeded")

        // 2. The first step cannot move up, and the second can.
        showTagInList("morningEditorList", "morningEditorUp:0")
        composeRule.onNodeWithTag("morningEditorUp:0").assertIsNotEnabled()
        showTagInList("morningEditorList", "morningEditorDown:0")
        composeRule.onNodeWithTag("morningEditorDown:0").assertIsEnabled()

        clickTagInList("morningEditorList", "morningEditorUp:1")
        clickTagInList("morningEditorList", "morningEditorSave")
        awaitTag("morningTitle")

        // 3. Positions are still a gap-free 0..n-1 sequence, and nothing was
        //    duplicated by the reorder.
        val positions = writableDatabase().query(
            "SELECT position FROM morning_routine_step WHERE routineId = $seeded ORDER BY position",
        ).use { cursor ->
            val values = mutableListOf<Int>()
            while (cursor.moveToNext()) values.add(cursor.getInt(0))
            values
        }
        assertEquals(positions.indices.toList(), positions)
        assertEquals(positions.size, count("SELECT COUNT(*) FROM morning_routine_step WHERE routineId = $seeded"))
    }
}
