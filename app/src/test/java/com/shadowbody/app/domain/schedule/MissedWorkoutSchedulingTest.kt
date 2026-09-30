package com.shadowbody.app.domain.schedule

import com.shadowbody.app.domain.adaptive.AdaptationContext
import com.shadowbody.app.domain.adaptive.AdaptationLimits
import com.shadowbody.app.domain.adaptive.ExerciseProgress
import com.shadowbody.app.domain.adaptive.PerformanceRecord
import com.shadowbody.app.domain.adaptive.ReadinessSnapshot
import com.shadowbody.app.domain.adaptive.WorkoutAdaptationEngine
import com.shadowbody.app.domain.model.ProgressionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissedWorkoutSchedulingTest {

    private fun progress(
        sessionsAtTarget: Int = 2,
        lastResult: PerformanceRecord? = PerformanceRecord(
            completedSets = 3,
            targetSets = 3,
            actualReps = 10,
            targetReps = 10,
        ),
        state: ProgressionState = ProgressionState.PROGRESSING,
    ) = ExerciseProgress(
        exerciseId = 1L,
        currentSets = 3,
        currentReps = 10,
        currentDurationSec = null,
        restSec = 60,
        sessionsAtTarget = sessionsAtTarget,
        lastResult = lastResult,
        state = state,
    )

    @Test
    fun missedSessionsReduceVolumeConservatively() {
        val result = WorkoutAdaptationEngine.nextTarget(
            progress = progress(),
            readiness = null,
            context = AdaptationContext(missedSessions = AdaptationLimits.MISSED_SESSIONS_TO_REDUCE),
        )

        assertEquals(ProgressionState.REGRESSING, result.state)
        assertTrue(result.sets < 3)
    }

    @Test
    fun noMissedSessionsAllowsProgression() {
        val result = WorkoutAdaptationEngine.nextTarget(
            progress = progress(sessionsAtTarget = AdaptationLimits.SESSIONS_TO_PROGRESS),
            readiness = null,
            context = AdaptationContext(missedSessions = 0),
        )

        assertEquals(ProgressionState.PROGRESSING, result.state)
    }

    @Test
    fun missedWorkoutDoesNotRequireManualPlanRebuild() {
        val result = WorkoutAdaptationEngine.nextTarget(
            progress = progress(
                sessionsAtTarget = 0,
                lastResult = null,
                state = ProgressionState.UNTRACKED,
            ),
            readiness = null,
            context = AdaptationContext(missedSessions = 1),
        )

        assertEquals(ProgressionState.UNTRACKED, result.state)
    }

    @Test
    fun highFatigueWithMissedSessionsStillReduces() {
        val result = WorkoutAdaptationEngine.nextTarget(
            progress = progress(),
            readiness = ReadinessSnapshot(fatigue = 5, soreness = 2, notes = ""),
            context = AdaptationContext(missedSessions = 2),
        )

        assertEquals(ProgressionState.REGRESSING, result.state)
    }
}
