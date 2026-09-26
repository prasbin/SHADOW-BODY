package com.shadowbody.app.domain.model

/**
 * Phase 5 step categories for the Morning Activation routine.
 *
 * Purely descriptive: a category says what kind of action a step is, never
 * what it treats or prevents. Nothing here is a medical classification.
 */
enum class MorningStepCategory {
    /** Plain-language cue such as drinking water. No physical target. */
    HYDRATION,

    /** Slow, controlled breathing. */
    BREATHING,

    /** Standing alignment and core bracing. */
    POSTURE,

    /** General range-of-motion work. */
    MOBILITY,

    /** Joint-by-joint movement. */
    JOINT_MOBILITY,

    /** Bodyweight activation of the main movement patterns. */
    ACTIVATION,

    /** Short continuous movement such as marching or walking. */
    MOVEMENT,

    /** Calm-down and transition out of the routine. */
    COOLDOWN,
}
