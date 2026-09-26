package com.shadowbody.app.domain.adaptive

import com.shadowbody.app.domain.model.ProgressionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4 rule tests for [WorkoutAdaptationEngine].
 *
 * Pure JUnit: the engine has no clock, no I/O and no Android types, so every
 * rule is verified without a device.
 */
class WorkoutAdaptationEngineTest {

    private val exerciseId = 1L

    private fun progress(
        sets: Int = 3,
        reps: Int? = 10,
        durationSec: Int? = null,
        restSec: Int = 60,
        sessionsAtTarget: Int = 0,
        lastResult: PerformanceRecord? = null,
        state: ProgressionState = ProgressionState.MAINTAINING,
    ) = ExerciseProgress(
        exerciseId = exerciseId,
        currentSets = sets,
        currentReps = reps,
        currentDurationSec = durationSec,
        restSec = restSec,
        sessionsAtTarget = sessionsAtTarget,
        lastResult = lastResult,
        state = state,
    )

    /** A session where every set was completed at target. */
    private fun cleanReps(sets: Int = 3, reps: Int = 10) = PerformanceRecord(
        completedSets = sets,
        targetSets = sets,
        actualReps = reps,
        targetReps = reps,
    )

    private fun cleanTime(sets: Int = 3, seconds: Int = 45) = PerformanceRecord(
        completedSets = sets,
        targetSets = sets,
        actualDurationSec = seconds,
        targetDurationSec = seconds,
    )

    // --- Maintenance --------------------------------------------------------

    @Test
    fun `no recorded session maintains and stays untracked`() {
        val decision = WorkoutAdaptationEngine.nextTarget(progress(), null, AdaptationContext())
        assertEquals(ProgressionState.UNTRACKED, decision.state)
        assertEquals(AdaptationReason.NO_DATA, decision.reason)
        assertEquals(3, decision.sets)
        assertEquals(10, decision.reps)
    }

    @Test
    fun `one clean session does not progress`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(sessionsAtTarget = 0, lastResult = cleanReps()),
            null,
            AdaptationContext(),
        )
        assertEquals(ProgressionState.MAINTAINING, decision.state)
        assertEquals(AdaptationReason.SINGLE_SUCCESS, decision.reason)
        assertEquals(3, decision.sets)
    }

    @Test
    fun `weekly target met maintains even with a clean streak`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(sessionsAtTarget = 5, lastResult = cleanReps()),
            null,
            AdaptationContext(weeklyTargetMet = true),
        )
        assertEquals(ProgressionState.MAINTAINING, decision.state)
        assertEquals(AdaptationReason.SCHEDULE_MET, decision.reason)
    }

    @Test
    fun `target at every ceiling holds instead of escalating`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(
                sets = AdaptationLimits.MAX_SETS,
                reps = AdaptationLimits.MAX_REPS,
                sessionsAtTarget = 9,
                lastResult = cleanReps(AdaptationLimits.MAX_SETS, AdaptationLimits.MAX_REPS),
            ),
            null,
            AdaptationContext(),
        )
        assertEquals(ProgressionState.MAINTAINING, decision.state)
        assertEquals(AdaptationReason.AT_LIMIT, decision.reason)
        assertEquals(AdaptationLimits.MAX_SETS, decision.sets)
        assertEquals(AdaptationLimits.MAX_REPS, decision.reps)
    }

    // --- Progression --------------------------------------------------------

    @Test
    fun `two clean sessions progress sets by one step`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(sessionsAtTarget = 1, lastResult = cleanReps()),
            null,
            AdaptationContext(),
        )
        assertEquals(ProgressionState.PROGRESSING, decision.state)
        assertEquals(AdaptationReason.SETS_PROGRESSED, decision.reason)
        assertEquals(4, decision.sets)
        assertTrue(decision.isProgression)
    }

    @Test
    fun `sets at maximum progress reps instead`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(
                sets = AdaptationLimits.MAX_SETS,
                reps = 20,
                sessionsAtTarget = 1,
                lastResult = cleanReps(AdaptationLimits.MAX_SETS, 20),
            ),
            null,
            AdaptationContext(),
        )
        assertEquals(ProgressionState.PROGRESSING, decision.state)
        assertEquals(AdaptationReason.REPS_PROGRESSED, decision.reason)
        assertEquals(AdaptationLimits.MAX_SETS, decision.sets)
        assertEquals(20 + AdaptationLimits.MAX_REP_STEP, decision.reps)
    }

    @Test
    fun `time based targets progress duration not reps`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(
                sets = AdaptationLimits.MAX_SETS,
                reps = null,
                durationSec = 45,
                sessionsAtTarget = 1,
                lastResult = cleanTime(seconds = 45),
            ),
            null,
            AdaptationContext(),
        )
        assertEquals(ProgressionState.PROGRESSING, decision.state)
        assertEquals(AdaptationReason.TIME_PROGRESSED, decision.reason)
        assertEquals(null, decision.reps)
        assertEquals(45 + AdaptationLimits.MAX_DURATION_STEP_SEC, decision.durationSec)
    }

    // --- Regression from performance ---------------------------------------

    @Test
    fun `missing sets reduce volume`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(
                sets = 3,
                lastResult = PerformanceRecord(
                    completedSets = 1,
                    targetSets = 3,
                    actualReps = 10,
                    targetReps = 10,
                ),
            ),
            null,
            AdaptationContext(),
        )
        assertEquals(ProgressionState.REGRESSING, decision.state)
        assertEquals(AdaptationReason.SETS_REDUCED, decision.reason)
        assertEquals(2, decision.sets)
    }

    @Test
    fun `short reps reduce reps without touching sets`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(
                sets = 3,
                lastResult = PerformanceRecord(
                    completedSets = 3,
                    targetSets = 3,
                    actualReps = 6,
                    targetReps = 10,
                ),
            ),
            null,
            AdaptationContext(),
        )
        assertEquals(ProgressionState.REGRESSING, decision.state)
        assertEquals(AdaptationReason.REPS_REDUCED, decision.reason)
        assertEquals(3, decision.sets)
        assertEquals(10 - AdaptationLimits.MAX_REP_STEP, decision.reps)
    }

    @Test
    fun `short hold time reduces duration`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(
                sets = 3,
                reps = null,
                durationSec = 60,
                lastResult = PerformanceRecord(
                    completedSets = 3,
                    targetSets = 3,
                    actualDurationSec = 30,
                    targetDurationSec = 60,
                ),
            ),
            null,
            AdaptationContext(),
        )
        assertEquals(ProgressionState.REGRESSING, decision.state)
        assertEquals(AdaptationReason.TIME_REDUCED, decision.reason)
        assertEquals(60 - AdaptationLimits.MAX_DURATION_STEP_SEC, decision.durationSec)
    }

    @Test
    fun `reduction also grants rest`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(
                restSec = 60,
                lastResult = PerformanceRecord(completedSets = 1, targetSets = 3),
            ),
            null,
            AdaptationContext(),
        )
        assertEquals(60 + AdaptationLimits.REST_STEP_SEC, decision.restSec)
    }

    // --- Readiness ----------------------------------------------------------

    @Test
    fun `high fatigue reduces and never progresses`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(sessionsAtTarget = 9, lastResult = cleanReps()),
            ReadinessSnapshot(fatigue = 5, soreness = 1),
            AdaptationContext(),
        )
        assertEquals(ProgressionState.REGRESSING, decision.state)
        assertEquals(AdaptationReason.FATIGUE_REDUCED, decision.reason)
        assertEquals(2, decision.sets)
    }

    @Test
    fun `high soreness reduces and never progresses`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(sessionsAtTarget = 9, lastResult = cleanReps()),
            ReadinessSnapshot(fatigue = 1, soreness = 4),
            AdaptationContext(),
        )
        assertEquals(ProgressionState.REGRESSING, decision.state)
        assertEquals(AdaptationReason.SORENESS_REDUCED, decision.reason)
    }

    @Test
    fun `readiness below the threshold changes nothing`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(sessionsAtTarget = 9, lastResult = cleanReps()),
            ReadinessSnapshot(fatigue = 3, soreness = 3),
            AdaptationContext(),
        )
        assertEquals(ProgressionState.PROGRESSING, decision.state)
    }

    @Test
    fun `readiness outranks a clean streak but not the explanation`() {
        // Fatigue is checked first, so the reason names fatigue, not progress.
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(sessionsAtTarget = 1, lastResult = cleanReps()),
            ReadinessSnapshot(fatigue = 4, soreness = 4),
            AdaptationContext(),
        )
        assertEquals(AdaptationReason.FATIGUE_REDUCED, decision.reason)
    }

    // --- Missed sessions ----------------------------------------------------

    @Test
    fun `a single miss does not reduce anything`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(sessionsAtTarget = 9, lastResult = cleanReps()),
            null,
            AdaptationContext(missedSessions = 1),
        )
        assertEquals(ProgressionState.PROGRESSING, decision.state)
    }

    @Test
    fun `two misses reduce by exactly one set and no more`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(sets = 3, lastResult = cleanReps()),
            null,
            AdaptationContext(missedSessions = 2),
        )
        assertEquals(ProgressionState.REGRESSING, decision.state)
        assertEquals(AdaptationReason.MISSED_REDUCED, decision.reason)
        assertEquals(3 - AdaptationLimits.MAX_MISSED_SET_PENALTY, decision.sets)
    }

    // --- Safety bounds ------------------------------------------------------

    @Test
    fun `repeated reductions stop at the floor`() {
        var current = progress(
            sets = AdaptationLimits.MIN_SETS,
            reps = AdaptationLimits.MIN_REPS + 4,
            lastResult = cleanReps(),
        )
        repeat(10) {
            val decision = WorkoutAdaptationEngine.nextTarget(
                current,
                ReadinessSnapshot(fatigue = 5, soreness = 5),
                AdaptationContext(),
            )
            current = current.copy(
                currentSets = decision.sets,
                currentReps = decision.reps,
            )
        }
        assertEquals(AdaptationLimits.MIN_SETS, current.currentSets)
        assertEquals(AdaptationLimits.MIN_REPS, current.currentReps)
    }

    @Test
    fun `out of range targets are clamped before any rule runs`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(sets = 99, reps = 999, restSec = 9999, lastResult = null),
            null,
            AdaptationContext(),
        )
        assertTrue(decision.clamped)
        assertEquals(AdaptationLimits.MAX_SETS, decision.sets)
        assertEquals(AdaptationLimits.MAX_REPS, decision.reps)
        assertEquals(AdaptationLimits.MAX_REST_SEC, decision.restSec)
    }

    @Test
    fun `absent rep target is lifted to the time floor`() {
        val decision = WorkoutAdaptationEngine.nextTarget(
            progress(reps = null, durationSec = null),
            null,
            AdaptationContext(),
        )
        assertEquals(null, decision.reps)
        assertEquals(AdaptationLimits.MIN_DURATION_SEC, decision.durationSec)
    }

    @Test
    fun `initial target clamps and never claims progress`() {
        val decision = WorkoutAdaptationEngine.initialTarget(
            exerciseId = exerciseId,
            sets = 40,
            reps = null,
            durationSec = 5000,
            restSec = 5,
        )
        assertEquals(ProgressionState.UNTRACKED, decision.state)
        assertEquals(AdaptationReason.NO_DATA, decision.reason)
        assertEquals(AdaptationLimits.MAX_SETS, decision.sets)
        assertEquals(AdaptationLimits.MAX_DURATION_SEC, decision.durationSec)
        assertEquals(AdaptationLimits.MIN_REST_SEC, decision.restSec)
        assertTrue(decision.clamped)
    }

    // --- Determinism --------------------------------------------------------

    @Test
    fun `identical input always yields an identical decision`() {
        val input = progress(sessionsAtTarget = 1, lastResult = cleanReps())
        val first = WorkoutAdaptationEngine.nextTarget(input, null, AdaptationContext())
        val second = WorkoutAdaptationEngine.nextTarget(input, null, AdaptationContext())
        assertEquals(first, second)
    }

    @Test
    fun `target text is compact for both rep and time targets`() {
        assertEquals(
            "3 x 12",
            WorkoutAdaptationEngine.nextTarget(progress(sets = 3, reps = 12), null).targetText(),
        )
        assertEquals(
            "4 x 45s",
            WorkoutAdaptationEngine.nextTarget(
                progress(sets = 4, reps = null, durationSec = 45),
                null,
            ).targetText(),
        )
    }

    @Test
    fun `maintenance never changes the volume`() {
        // No data yet.
        val untracked = WorkoutAdaptationEngine.nextTarget(
            progress(sets = 3, reps = 10, lastResult = null),
            null,
            AdaptationContext(),
        )
        assertEquals(AdaptationReason.NO_DATA, untracked.reason)
        assertEquals(3, untracked.sets)
        assertEquals(10, untracked.reps)

        // Week already complete.
        val scheduled = WorkoutAdaptationEngine.nextTarget(
            progress(sets = 3, reps = 10, lastResult = null),
            null,
            AdaptationContext(weeklyTargetMet = true),
        )
        assertEquals(AdaptationReason.SCHEDULE_MET, scheduled.reason)
        assertEquals(3, scheduled.sets)
        assertEquals(10, scheduled.reps)
    }
}
