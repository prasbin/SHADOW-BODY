package com.shadowbody.app.domain.grooming

/**
 * Phase 8 completion rule for grooming runs.
 *
 * A grooming run is COMPLETED only when every step has been explicitly
 * marked COMPLETED or SKIPPED. Otherwise it is ABANDONED.
 */
object GroomingCompletionRule {

    enum class FinishDecision {
        NOT_READY,   // some steps still untouched
        COMPLETED,   // every step has an outcome
    }

    fun evaluate(
        totalSteps: Int,
        completedSteps: Int,
        skippedSteps: Int,
    ): FinishDecision {
        val dealtWith = completedSteps + skippedSteps
        return if (dealtWith < totalSteps) FinishDecision.NOT_READY else FinishDecision.COMPLETED
    }

    fun statusFor(decision: FinishDecision): String =
        when (decision) {
            FinishDecision.COMPLETED -> "COMPLETED"
            else -> "ABANDONED" // partial run
        }
}