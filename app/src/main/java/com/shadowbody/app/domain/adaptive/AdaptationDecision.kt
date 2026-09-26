package com.shadowbody.app.domain.adaptive

import com.shadowbody.app.domain.model.ProgressionState

/**
 * Phase 4: the engine's output for one exercise. Fully self-describing —
 * the UI renders [reason] and the before/after targets without re-deriving
 * anything, which is what makes the behaviour explainable.
 */
data class AdaptationDecision(
    val exerciseId: Long,
    val sets: Int,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val restSec: Int,
    val state: ProgressionState,
    val reason: AdaptationReason,
    /** True when the incoming target had to be clamped before any rule ran. */
    val clamped: Boolean = false,
) {
    val isProgression: Boolean get() = state == ProgressionState.PROGRESSING
    val isRegression: Boolean get() = state == ProgressionState.REGRESSING

    /** "3x10" or "3x45s" — the compact target shown in the UI. */
    fun targetText(): String = if (reps != null) {
        "$sets x $reps"
    } else {
        "$sets x ${durationSec ?: 0}s"
    }
}
