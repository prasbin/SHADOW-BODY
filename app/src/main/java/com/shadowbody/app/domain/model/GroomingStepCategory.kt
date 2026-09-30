package com.shadowbody.app.domain.model

/**
 * Phase 8: practical grooming categories.
 *
 * Purely descriptive: a category says what kind of grooming action a step is,
 * never what it treats or prevents. Nothing here is a medical classification.
 */
enum class GroomingStepCategory {
    /** Hair washing, conditioning, styling. */
    HAIR,

    /** Cleansing, moisturizing, sun protection. */
    SKIN,

    /** Brushing, flossing, mouthwash. */
    ORAL_CARE,

    /** Face cleansing, masks, treatments. */
    FACE,

    /** Body washing, exfoliating, moisturizing. */
    BODY,

    /** Nail trimming, filing, cuticle care. */
    NAILS,

    /** Showering, deodorant, hygiene basics. */
    HYGIENE,

    /** Catch-all for user-defined or miscellaneous steps. */
    OTHER,
}