package com.shadowbody.app.domain.model

/**
 * Phase 4: lifecycle of a generated recommendation.
 *
 * [MISSED] is a user action ("I skipped this"), never an automatic penalty
 * and never a change to an already completed session.
 */
enum class RecommendationStatus(val label: String) {
    ACTIVE("Active"),
    ADOPTED("Adopted"),
    MISSED("Missed"),
    DISMISSED("Dismissed"),
}
