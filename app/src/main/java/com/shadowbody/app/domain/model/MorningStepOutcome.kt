package com.shadowbody.app.domain.model

/**
 * Phase 5 outcome of a single routine step.
 *
 * A skipped step is recorded as [SKIPPED] and is never counted as completed:
 * history must not claim work the user did not do.
 */
enum class MorningStepOutcome {
    COMPLETED,
    SKIPPED,
}
