package com.shadowbody.app.domain.validation

/** Raw plan-form input for create/edit. */
data class PlanInput(
    val name: String = "",
    val description: String = "",
    val targetDurationMin: String = "",
)

/** One editor row: which exercise with which targets. */
data class PlanSlotInput(
    val exerciseId: Long,
    val targetSets: String = "3",
    val targetReps: String = "10",
    /** Blank = rep-based; filled = time-based (seconds). */
    val targetDurationSec: String = "",
    val restSec: String = "60",
)

object WorkoutValidator {

    const val FIELD_NAME = "name"
    const val FIELD_DURATION = "duration"
    const val FIELD_SLOTS = "slots"

    const val MAX_NAME = 80
    const val MAX_DESCRIPTION = 500
    const val MIN_SETS = 1
    const val MAX_SETS = 20
    const val MIN_REPS = 1
    const val MAX_REPS = 500
    const val MIN_DURATION_SEC = 5
    const val MAX_DURATION_SEC = 3600
    const val MIN_REST_SEC = 0
    const val MAX_REST_SEC = 900
    const val MAX_WEIGHT_KG = 500.0

    fun validatePlan(input: PlanInput, slotCount: Int): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (input.name.isBlank()) {
            errors[FIELD_NAME] = "Give the plan a name."
        } else if (input.name.trim().length > MAX_NAME) {
            errors[FIELD_NAME] = "Name must be under $MAX_NAME characters."
        }
        if (input.description.length > MAX_DESCRIPTION) {
            errors[FIELD_DURATION] = "Description must be under $MAX_DESCRIPTION characters."
        }
        if (input.targetDurationMin.isNotBlank()) {
            val minutes = input.targetDurationMin.trim().toIntOrNull()
            if (minutes == null || minutes !in 5..300) {
                errors[FIELD_DURATION] = "Duration must be 5–300 minutes."
            }
        }
        if (slotCount == 0) {
            errors[FIELD_SLOTS] = "Add at least one exercise."
        }
        return errors
    }

    /** Index-keyed slot errors ("3:sets" style keys). Empty = valid. */
    fun validateSlots(slots: List<PlanSlotInput>): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        slots.forEachIndexed { index, slot ->
            val sets = slot.targetSets.trim().toIntOrNull()
            if (sets == null || sets !in MIN_SETS..MAX_SETS) {
                errors["$index:sets"] = "Sets must be $MIN_SETS–$MAX_SETS."
            }
            if (slot.targetDurationSec.isBlank()) {
                val reps = slot.targetReps.trim().toIntOrNull()
                if (reps == null || reps !in MIN_REPS..MAX_REPS) {
                    errors["$index:reps"] = "Reps must be $MIN_REPS–$MAX_REPS."
                }
            } else {
                val seconds = slot.targetDurationSec.trim().toIntOrNull()
                if (seconds == null || seconds !in MIN_DURATION_SEC..MAX_DURATION_SEC) {
                    errors["$index:duration"] = "Time must be $MIN_DURATION_SEC–$MAX_DURATION_SEC s."
                }
            }
            val rest = slot.restSec.trim().toIntOrNull()
            if (rest == null || rest !in MIN_REST_SEC..MAX_REST_SEC) {
                errors["$index:rest"] = "Rest must be $MIN_REST_SEC–$MAX_REST_SEC s."
            }
        }
        return errors
    }

    /** Validates one set log; blank fields mean "not recorded". */
    fun validateSetLog(reps: String, durationSec: String, weightKg: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (reps.isNotBlank()) {
            val value = reps.trim().toIntOrNull()
            if (value == null || value !in 0..MAX_REPS) {
                errors["reps"] = "Reps must be 0–$MAX_REPS."
            }
        }
        if (durationSec.isNotBlank()) {
            val value = durationSec.trim().toIntOrNull()
            if (value == null || value !in 0..MAX_DURATION_SEC) {
                errors["duration"] = "Time must be 0–$MAX_DURATION_SEC s."
            }
        }
        if (weightKg.isNotBlank()) {
            val value = weightKg.trim().toDoubleOrNull()
            if (value == null || value.isNaN() || value.isInfinite() ||
                value < 0 || value > MAX_WEIGHT_KG
            ) {
                errors["weight"] = "Weight must be 0–${MAX_WEIGHT_KG.toInt()} kg."
            }
        }
        return errors
    }

    /**
     * A set may only be marked complete once something was actually performed and
     * recorded: at least one rep or one second of work. Blank logs are for
     * in-progress rows only, so history never claims unearned work.
     */
    fun validateSetCompletion(reps: String, durationSec: String, weightKg: String): Map<String, String> {
        val errors = validateSetLog(reps, durationSec, weightKg).toMutableMap()
        if (errors.isEmpty()) {
            val recordedReps = reps.trim().toIntOrNull() ?: 0
            val recordedSec = durationSec.trim().toIntOrNull() ?: 0
            if (recordedReps <= 0 && recordedSec <= 0) {
                errors["reps"] = "Record reps or time before completing this set."
            }
        }
        return errors
    }
}
