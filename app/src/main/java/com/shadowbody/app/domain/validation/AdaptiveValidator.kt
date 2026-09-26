package com.shadowbody.app.domain.validation

import com.shadowbody.app.domain.adaptive.AdaptationLimits

/**
 * Phase 4: validates the readiness/fatigue the user reports before it is
 * allowed to influence a target. Deliberately minimal and local — two 1..5
 * scales and an optional note. No health data, no diagnosis, nothing more
 * sensitive than what the user chose to type.
 */
object AdaptiveValidator {

    const val FIELD_FATIGUE = "fatigue"
    const val FIELD_SORENESS = "soreness"
    const val FIELD_NOTES = "notes"

    const val MAX_NOTES = 240

    fun isHighFatigue(fatigue: Int): Boolean =
        fatigue >= AdaptationLimits.HIGH_FATIGUE_THRESHOLD

    fun validateReadiness(fatigue: Int?, soreness: Int?, notes: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (fatigue == null || fatigue !in AdaptationLimits.READINESS_MIN..AdaptationLimits.READINESS_MAX) {
            errors[FIELD_FATIGUE] =
                "Rate fatigue ${AdaptationLimits.READINESS_MIN}-${AdaptationLimits.READINESS_MAX}."
        }
        if (soreness == null ||
            soreness !in AdaptationLimits.READINESS_MIN..AdaptationLimits.READINESS_MAX
        ) {
            errors[FIELD_SORENESS] =
                "Rate soreness ${AdaptationLimits.READINESS_MIN}-${AdaptationLimits.READINESS_MAX}."
        }
        if (notes.length > MAX_NOTES) {
            errors[FIELD_NOTES] = "Note must be under $MAX_NOTES characters."
        }
        return errors
    }
}
