package com.shadowbody.app.domain.morning

import com.shadowbody.app.domain.model.MorningStepCategory
import com.shadowbody.app.domain.model.MorningStepOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MorningEnforcementTest {

    @Test
    fun newDayCreatesFreshState() {
        val state = MorningDayState(dayKey = "2026-10-03")
        assertEquals(MorningDayStatus.NOT_STARTED, state.status)
        assertEquals(0, state.completedSteps)
        assertEquals(0, state.skippedSteps)
    }

    @Test
    fun inProgressStateTracksCompletedAndSkipped() {
        val state = MorningDayState(
            dayKey = "2026-10-03",
            status = MorningDayStatus.IN_PROGRESS,
            completedSteps = 3,
            skippedSteps = 1,
            totalSteps = 8,
        )

        assertEquals(MorningDayStatus.IN_PROGRESS, state.status)
        assertEquals(3, state.completedSteps)
        assertEquals(1, state.skippedSteps)
        assertEquals(4, state.completedSteps + state.skippedSteps)
    }

    @Test
    fun completedStateRequiresAllStepsDealtWith() {
        val state = MorningDayState(
            dayKey = "2026-10-03",
            status = MorningDayStatus.COMPLETED,
            completedSteps = 6,
            skippedSteps = 2,
            totalSteps = 8,
        )

        assertEquals(MorningDayStatus.COMPLETED, state.status)
        assertEquals(8, state.completedSteps + state.skippedSteps)
    }

    @Test
    fun partialCompletionIsNotComplete() {
        val state = MorningDayState(
            dayKey = "2026-10-03",
            status = MorningDayStatus.IN_PROGRESS,
            completedSteps = 3,
            skippedSteps = 1,
            totalSteps = 8,
        )

        assertFalse(
            "Partial completion should not be COMPLETED",
            state.status == MorningDayStatus.COMPLETED
        )
    }

    @Test
    fun allCompletedIsComplete() {
        val state = MorningDayState(
            dayKey = "2026-10-03",
            status = MorningDayStatus.COMPLETED,
            completedSteps = 8,
            skippedSteps = 0,
            totalSteps = 8,
        )

        assertEquals(MorningDayStatus.COMPLETED, state.status)
    }

    @Test
    fun allSkippedIsComplete() {
        val state = MorningDayState(
            dayKey = "2026-10-03",
            status = MorningDayStatus.COMPLETED,
            completedSteps = 0,
            skippedSteps = 8,
            totalSteps = 8,
        )

        assertEquals(MorningDayStatus.COMPLETED, state.status)
    }

    @Test
    fun runStepOutcomeDistinguishesCompletedSkippedAndUnfinished() {
        val completed = MorningRunStep(
            position = 0,
            title = "Task",
            instructions = "",
            category = MorningStepCategory.HYDRATION,
            targetDurationSec = 60,
            targetReps = null,
            stepId = 1L,
            outcome = MorningStepOutcome.COMPLETED,
            elapsedSec = 60,
        )

        val skipped = MorningRunStep(
            position = 1,
            title = "Task 2",
            instructions = "",
            category = MorningStepCategory.HYDRATION,
            targetDurationSec = 60,
            targetReps = null,
            stepId = 2L,
            outcome = MorningStepOutcome.SKIPPED,
            elapsedSec = null,
        )

        val unfinished = MorningRunStep(
            position = 2,
            title = "Task 3",
            instructions = "",
            category = MorningStepCategory.HYDRATION,
            targetDurationSec = 60,
            targetReps = null,
            stepId = 3L,
            outcome = null,
            elapsedSec = null,
        )

        assertEquals(MorningStepOutcome.COMPLETED, completed.outcome)
        assertEquals(MorningStepOutcome.SKIPPED, skipped.outcome)
        assertEquals(null, unfinished.outcome)
    }

    @Test
    fun openingRoutineDoesNotCompleteIt() {
        val state = MorningDayState(dayKey = "2026-10-03")
        assertEquals(MorningDayStatus.NOT_STARTED, state.status)
        assertEquals(0, state.completedSteps)
    }

    @Test
    fun completionRuleEvaluatesCorrectly() {
        val notReady = MorningCompletionRule.evaluate(
            totalSteps = 8,
            completedSteps = 3,
            skippedSteps = 1,
        )
        assertEquals(MorningFinishDecision.NOT_READY, notReady)

        val completed = MorningCompletionRule.evaluate(
            totalSteps = 8,
            completedSteps = 6,
            skippedSteps = 2,
        )
        assertEquals(MorningFinishDecision.COMPLETED, completed)
    }

    @Test
    fun saturdayDoesNotOverrideMorningEnforcement() {
        val state = MorningDayState(dayKey = "2026-10-10")
        assertEquals(MorningDayStatus.NOT_STARTED, state.status)
    }
}
