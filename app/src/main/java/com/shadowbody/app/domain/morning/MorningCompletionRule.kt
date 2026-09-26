package com.shadowbody.app.domain.morning

import com.shadowbody.app.domain.model.MorningLogStatus

/** What [MorningCompletionRule] decided about a finished run. */
enum class MorningFinishDecision {
    /** Steps are still untouched: nothing may be written yet. */
    NOT_READY,

    /** Every enabled step was dealt with and at least one was completed. */
    COMPLETED,

    /** Partial run: stored honestly as [MorningLogStatus.ABANDONED]. */
    PARTIAL,
}

/**
 * Phase 5 completion criteria — pure, so the rule is testable without storage.
 *
 * The product rule is deliberately strict:
 * - every enabled step must have an explicit outcome, and
 * - at least one step must actually have been *completed*.
 *
 * A run where the user only skipped everything, or walked away half way, is
 * never promoted to a completion. Skipped steps never count as completed.
 */
object MorningCompletionRule {

    const val MIN_COMPLETED_STEPS = 1

    fun evaluate(
        totalSteps: Int,
        completedSteps: Int,
        skippedSteps: Int,
    ): MorningFinishDecision {
        if (totalSteps <= 0) return MorningFinishDecision.NOT_READY
        val dealtWith = completedSteps + skippedSteps
        if (dealtWith < totalSteps) return MorningFinishDecision.NOT_READY
        if (completedSteps < MIN_COMPLETED_STEPS) return MorningFinishDecision.PARTIAL
        return MorningFinishDecision.COMPLETED
    }

    /** Maps a decision to the status that gets stored. */
    fun statusFor(decision: MorningFinishDecision): MorningLogStatus = when (decision) {
        MorningFinishDecision.COMPLETED -> MorningLogStatus.COMPLETED
        else -> MorningLogStatus.ABANDONED
    }
}
