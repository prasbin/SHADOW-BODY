package com.shadowbody.app.domain.validation

import com.shadowbody.app.domain.model.MorningStepCategory

/**
 * Phase 5 validation for morning routines and their steps.
 *
 * Pure and offline, like every other validator in the project: it returns
 * per-field messages and never touches storage or the clock. Bounds are
 * deliberately wide — a morning routine is a light, everyday action, not a
 * prescription.
 */
object MorningRoutineValidator {

    const val FIELD_NAME = "name"
    const val FIELD_DESCRIPTION = "description"
    const val FIELD_TITLE = "title"
    const val FIELD_INSTRUCTIONS = "instructions"
    const val FIELD_DURATION = "durationSec"
    const val FIELD_REPS = "reps"
    const val FIELD_STEP = "step"

    const val MAX_NAME = 40
    const val MAX_DESCRIPTION = 140
    const val MAX_TITLE = 40
    const val MAX_INSTRUCTIONS = 200

    /** Shortest and longest timed step, in seconds. */
    const val MIN_DURATION_SEC = 10
    const val MAX_DURATION_SEC = 600

    const val MIN_REPS = 1
    const val MAX_REPS = 200

    /** Validates the routine header. */
    fun validateRoutine(name: String, description: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            errors[FIELD_NAME] = "Name the routine."
        } else if (trimmedName.length > MAX_NAME) {
            errors[FIELD_NAME] = "Keep the name under $MAX_NAME characters."
        }
        if (description.trim().length > MAX_DESCRIPTION) {
            errors[FIELD_DESCRIPTION] = "Keep the description under $MAX_DESCRIPTION characters."
        }
        return errors
    }

    /**
     * Validates one step.
     *
     * A step may be a timed step ([durationSec]), a counted step ([reps]), or a
     * plain reminder with neither — a hydration cue is a legitimate step. What
     * is rejected is a value that is present but out of range, or a step with no
     * instruction to follow.
     */
    fun validateStep(
        title: String,
        instructions: String,
        category: MorningStepCategory?,
        durationSec: Int?,
        reps: Int?,
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            errors[FIELD_TITLE] = "Name the step."
        } else if (trimmedTitle.length > MAX_TITLE) {
            errors[FIELD_TITLE] = "Keep the step name under $MAX_TITLE characters."
        }
        if (instructions.trim().isEmpty()) {
            errors[FIELD_INSTRUCTIONS] = "Describe how to do it."
        } else if (instructions.trim().length > MAX_INSTRUCTIONS) {
            errors[FIELD_INSTRUCTIONS] = "Keep instructions under $MAX_INSTRUCTIONS characters."
        }
        if (category == null) {
            errors[FIELD_STEP] = "Pick a category."
        }
        if (durationSec != null && durationSec > 0 &&
            (durationSec < MIN_DURATION_SEC || durationSec > MAX_DURATION_SEC)
        ) {
            errors[FIELD_DURATION] =
                "Use ${MIN_DURATION_SEC / 60}–${MAX_DURATION_SEC / 60} minutes."
        }
        if (reps != null && reps > 0 && (reps < MIN_REPS || reps > MAX_REPS)) {
            errors[FIELD_REPS] = "Use $MIN_REPS–$MAX_REPS reps."
        }
        return errors
    }

    /** True when a step carries at least one measurable target. */
    fun isTimed(durationSec: Int?): Boolean = durationSec != null && durationSec > 0

    /** True when a step carries a repetition target. */
    fun isCounted(reps: Int?): Boolean = reps != null && reps > 0
}
