package com.shadowbody.app.domain.validation

import com.shadowbody.app.data.local.BaselineRecord

/** Raw baseline form input: every measurement is optional text. */
data class BaselineInput(
    val weightKg: String = "",
    val chestCm: String = "",
    val waistCm: String = "",
    val hipsCm: String = "",
    val bicepsCm: String = "",
    val thighCm: String = "",
    val bodyFatPct: String = "",
    val notes: String = "",
)

/** Field-keyed validation errors; empty map means the input is valid. */
object BaselineValidator {

    const val FIELD_WEIGHT = "weight"
    const val FIELD_CHEST = "chest"
    const val FIELD_WAIST = "waist"
    const val FIELD_HIPS = "hips"
    const val FIELD_BICEPS = "biceps"
    const val FIELD_THIGH = "thigh"
    const val FIELD_BODY_FAT = "bodyFat"
    const val FIELD_NOTES = "notes"
    const val FIELD_GENERAL = "general"

    const val MIN_WEIGHT_KG = 25.0
    const val MAX_WEIGHT_KG = 350.0
    const val MIN_GIRTH_CM = 20.0
    const val MAX_GIRTH_CM = 250.0
    const val MIN_BODY_FAT = 1.0
    const val MAX_BODY_FAT = 70.0
    const val MAX_NOTES = 500

    fun validate(input: BaselineInput): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        parseOptional(input.weightKg, FIELD_WEIGHT, errors, MIN_WEIGHT_KG, MAX_WEIGHT_KG, "kg")
        parseOptional(input.chestCm, FIELD_CHEST, errors, MIN_GIRTH_CM, MAX_GIRTH_CM, "cm")
        parseOptional(input.waistCm, FIELD_WAIST, errors, MIN_GIRTH_CM, MAX_GIRTH_CM, "cm")
        parseOptional(input.hipsCm, FIELD_HIPS, errors, MIN_GIRTH_CM, MAX_GIRTH_CM, "cm")
        parseOptional(input.bicepsCm, FIELD_BICEPS, errors, MIN_GIRTH_CM, MAX_GIRTH_CM, "cm")
        parseOptional(input.thighCm, FIELD_THIGH, errors, MIN_GIRTH_CM, MAX_GIRTH_CM, "cm")
        parseOptional(
            input.bodyFatPct, FIELD_BODY_FAT, errors,
            MIN_BODY_FAT, MAX_BODY_FAT, "%",
            label = "Body fat",
        )

        if (input.notes.length > MAX_NOTES) {
            errors[FIELD_NOTES] = "Notes must be under $MAX_NOTES characters."
        }

        val anyMeasurement = listOf(
            input.weightKg, input.chestCm, input.waistCm, input.hipsCm,
            input.bicepsCm, input.thighCm, input.bodyFatPct,
        ).any { it.isNotBlank() }
        if (!anyMeasurement && input.notes.isBlank()) {
            errors[FIELD_GENERAL] = "Enter at least one measurement to save a baseline."
        }

        return errors
    }

    private fun parseOptional(
        raw: String,
        field: String,
        errors: MutableMap<String, String>,
        min: Double,
        max: Double,
        unit: String,
        label: String = field.replaceFirstChar { it.uppercase() },
    ): Double? {
        if (raw.isBlank()) return null
        val value = raw.trim().toDoubleOrNull()
        if (value == null || value.isNaN() || value.isInfinite()) {
            errors[field] = "$label must be a number."
            return null
        }
        if (value < min || value > max) {
            errors[field] = "$label must be between $min and $max $unit."
            return null
        }
        return value
    }

    /** Converts validated input into the entity. Call only when valid. */
    fun toRecord(input: BaselineInput): BaselineRecord = BaselineRecord(
        weightKg = input.weightKg.toDoubleOrNull(),
        chestCm = input.chestCm.toDoubleOrNull(),
        waistCm = input.waistCm.toDoubleOrNull(),
        hipsCm = input.hipsCm.toDoubleOrNull(),
        bicepsCm = input.bicepsCm.toDoubleOrNull(),
        thighCm = input.thighCm.toDoubleOrNull(),
        bodyFatPct = input.bodyFatPct.toDoubleOrNull(),
        notes = input.notes.trim().ifBlank { null },
    )
}
