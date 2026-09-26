package com.shadowbody.app.domain.adaptive

import com.shadowbody.app.domain.model.ProgressionState

/**
 * Phase 4: what the engine knew about one exercise *before* deciding.
 *
 * Pure data, no Room or Android types, so every rule in
 * [WorkoutAdaptationEngine] is unit-testable without a device.
 */
data class ExerciseProgress(
    val exerciseId: Long,
    /** Current target volume, already inside the adaptive bounds. */
    val currentSets: Int,
    /** Null for time-based targets. */
    val currentReps: Int? = null,
    /** Null for rep-based targets. */
    val currentDurationSec: Int? = null,
    val restSec: Int = 60,
    /** Successful sessions completed at the *current* target. */
    val sessionsAtTarget: Int = 0,
    /** Result of the most recent completed session; null when untracked. */
    val lastResult: PerformanceRecord? = null,
    val state: ProgressionState = ProgressionState.UNTRACKED,
) {
    val isTimeBased: Boolean get() = currentReps == null
}

/** One exercise's outcome inside a completed session. */
data class PerformanceRecord(
    val completedSets: Int,
    val targetSets: Int,
    /** Best recorded set. */
    val actualReps: Int? = null,
    val targetReps: Int? = null,
    /** Best recorded set, seconds. */
    val actualDurationSec: Int? = null,
    val targetDurationSec: Int? = null,
) {
    val completionRatio: Double
        get() = if (targetSets <= 0) {
            0.0
        } else {
            completedSets.toDouble() / targetSets.toDouble()
        }
}

/** Latest user-reported readiness. Both scales are 1..5. */
data class ReadinessSnapshot(
    val fatigue: Int,
    val soreness: Int,
    val notes: String = "",
)

/** Non-performance inputs that also influence the next target. */
data class AdaptationContext(
    /**
     * User-reported missed sessions inside the current 7-day window
     * ([AdaptationLimits.WEEK_WINDOW_MS]). Missed sessions are only ever
     * counted when the user records them, and can never remove more than a
     * single set.
     */
    val missedSessions: Int = 0,
    /**
     * True when the planned sessions for the current week are already done.
     * Only evaluated while generating a recommendation — never when folding a
     * completed session into state, which is purely evidence-driven.
     */
    val weeklyTargetMet: Boolean = false,
)
