package com.shadowbody.app.domain.adaptive

import com.shadowbody.app.domain.model.ProgressionState

/**
 * Phase 4: every adaptation outcome carries one of these codes, so the UI can
 * always explain *why* a target changed. Codes are stable strings (persisted
 * in Room); [message] is the user-facing sentence.
 *
 * No randomness, no hidden scoring: the code alone determines the sentence.
 */
enum class AdaptationReason(
    val code: String,
    val message: String,
    val state: ProgressionState,
) {
    // --- Maintenance ---
    NO_DATA(
        code = "NO_DATA",
        message = "Maintained because there is not enough recent performance data.",
        state = ProgressionState.UNTRACKED,
    ),
    SINGLE_SUCCESS(
        code = "SINGLE_SUCCESS",
        message = "Maintained because more than one successful session is needed " +
            "before progressing.",
        state = ProgressionState.MAINTAINING,
    ),
    SCHEDULE_MET(
        code = "SCHEDULE_MET",
        message = "Maintained because this week's planned sessions are already done.",
        state = ProgressionState.MAINTAINING,
    ),
    AT_LIMIT(
        code = "AT_LIMIT",
        message = "Maintained because the target is already at its safe limit.",
        state = ProgressionState.MAINTAINING,
    ),
    STABLE(
        code = "STABLE",
        message = "Maintained because recent performance matched the target.",
        state = ProgressionState.MAINTAINING,
    ),

    // --- Progression ---
    SETS_PROGRESSED(
        code = "SETS_PROGRESSED",
        message = "Progressed because you completed all target sets.",
        state = ProgressionState.PROGRESSING,
    ),
    REPS_PROGRESSED(
        code = "REPS_PROGRESSED",
        message = "Progressed because you matched the rep target on every set.",
        state = ProgressionState.PROGRESSING,
    ),
    TIME_PROGRESSED(
        code = "TIME_PROGRESSED",
        message = "Progressed because you held the target time on every set.",
        state = ProgressionState.PROGRESSING,
    ),

    // --- Regression ---
    SETS_REDUCED(
        code = "SETS_REDUCED",
        message = "Reduced because you completed fewer sets than the target.",
        state = ProgressionState.REGRESSING,
    ),
    REPS_REDUCED(
        code = "REPS_REDUCED",
        message = "Reduced because recent performance was below target.",
        state = ProgressionState.REGRESSING,
    ),
    TIME_REDUCED(
        code = "TIME_REDUCED",
        message = "Reduced because recent hold time was below target.",
        state = ProgressionState.REGRESSING,
    ),
    FATIGUE_REDUCED(
        code = "FATIGUE_REDUCED",
        message = "Reduced because reported fatigue was high.",
        state = ProgressionState.REGRESSING,
    ),
    SORENESS_REDUCED(
        code = "SORENESS_REDUCED",
        message = "Reduced because reported soreness was high.",
        state = ProgressionState.REGRESSING,
    ),
    MISSED_REDUCED(
        code = "MISSED_REDUCED",
        message = "Reduced conservatively after missed sessions.",
        state = ProgressionState.REGRESSING,
    ),

    // --- Safety clamping ---
    TARGET_CLAMPED(
        code = "TARGET_CLAMPED",
        message = "Clamped to the safe range before use.",
        state = ProgressionState.MAINTAINING,
    );

    companion object {
        /** Unknown persisted codes fall back instead of crashing a read. */
        fun fromCode(code: String?): AdaptationReason =
            entries.firstOrNull { it.code == code } ?: STABLE
    }
}
