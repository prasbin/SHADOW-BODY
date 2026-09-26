package com.shadowbody.app.domain.model

/** Lifecycle of a workout session. Only COMPLETED counts as finished. */
enum class SessionStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED,
}
