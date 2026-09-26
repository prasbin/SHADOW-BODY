package com.shadowbody.app.domain.model

/**
 * Phase 5 state of one performed morning routine.
 *
 * [ABANDONED] means the user finished a partial run: some steps were done and
 * some were not. It is recorded honestly rather than being relabelled as a
 * completion.
 */
enum class MorningLogStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED,
}
