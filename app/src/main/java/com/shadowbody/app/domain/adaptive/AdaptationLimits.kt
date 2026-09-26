package com.shadowbody.app.domain.adaptive

/**
 * Phase 4 safety boundaries for the adaptive engine.
 *
 * Every number here is a hard, deterministic, unit-tested bound. The engine
 * never proposes a target outside these limits, and it never infers
 * anything about the user's health — these are training-load guardrails only,
 * not medical advice or a safety certification.
 */
object AdaptationLimits {

    // --- Volume (sets) ---
    const val MIN_SETS = 1
    const val MAX_SETS = 6
    const val SETS_STEP = 1

    // --- Reps (only used by rep-based targets) ---
    const val MIN_REPS = 5
    const val MAX_REPS = 30
    /** Hard cap on a single progression jump. */
    const val MAX_REP_STEP = 2

    // --- Time (only used by time-based targets, e.g. planks) ---
    const val MIN_DURATION_SEC = 15
    const val MAX_DURATION_SEC = 600
    const val MAX_DURATION_STEP_SEC = 30

    // --- Rest ---
    const val MIN_REST_SEC = 30
    const val MAX_REST_SEC = 180
    const val REST_STEP_SEC = 15

    // --- Evidence required to progress ---
    /**
     * Successful sessions at the *current* target before any increase. Two is
     * the minimum that prevents escalation from a single lucky session.
     */
    const val SESSIONS_TO_PROGRESS = 2

    // --- Evidence required to reduce ---
    /** Completed sets / target sets below this => reduce. */
    const val REGRESSION_COMPLETION_RATIO = 0.6
    /** Actual / target reps below this => reduce. */
    const val REGRESSION_REP_RATIO = 0.7
    /** Actual / target seconds below this => reduce. */
    const val REGRESSION_DURATION_RATIO = 0.7

    // --- Readiness (user-reported, 1..5) ---
    const val READINESS_MIN = 1
    const val READINESS_MAX = 5
    /** 4 or 5 on this scale triggers a reduction, never a progression. */
    const val HIGH_FATIGUE_THRESHOLD = 4
    const val HIGH_SORENESS_THRESHOLD = 4

    // --- Missed sessions ---
    /** Consecutive misses before demand is lowered at all. */
    const val MISSED_SESSIONS_TO_REDUCE = 2
    /** A miss may never cost more than this many sets, ever. */
    const val MAX_MISSED_SET_PENALTY = 1

    // --- Scheduling ---
    const val WEEK_WINDOW_MS = 7L * 24 * 60 * 60 * 1000
    /** Upper bound on a generated workout, whatever the profile says. */
    const val MAX_GENERATED_EXERCISES = 6
    const val MIN_GENERATED_EXERCISES = 1

    /** Seconds assumed per rep when estimating session length. */
    const val SECONDS_PER_REP = 4
    /** Fixed overhead per exercise (setup, walk to station). */
    const val SECONDS_PER_EXERCISE_OVERHEAD = 30
    const val SECONDS_PER_MINUTE = 60
}
