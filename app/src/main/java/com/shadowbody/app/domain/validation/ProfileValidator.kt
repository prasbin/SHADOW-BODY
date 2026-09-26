package com.shadowbody.app.domain.validation

import com.shadowbody.app.data.local.UserProfile
import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.FitnessLevel
import com.shadowbody.app.domain.model.Goal

/** Raw profile form input: everything arrives as user-typed text + picks. */
data class ProfileInput(
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val fitnessLevel: FitnessLevel = FitnessLevel.BEGINNER,
    val equipment: Set<Equipment> = emptySet(),
    val goals: Set<Goal> = emptySet(),
    val trainingDays: Set<Int> = emptySet(),
    val sessionMinutes: Int = 30,
)

/** Field-keyed validation errors; empty map means the input is valid. */
object ProfileValidator {

    const val FIELD_AGE = "age"
    const val FIELD_HEIGHT = "height"
    const val FIELD_WEIGHT = "weight"
    const val FIELD_EQUIPMENT = "equipment"
    const val FIELD_GOALS = "goals"
    const val FIELD_DAYS = "days"
    const val FIELD_SESSION = "session"

    const val MIN_AGE = 10
    const val MAX_AGE = 100
    const val MIN_HEIGHT_CM = 100.0
    const val MAX_HEIGHT_CM = 250.0
    const val MIN_WEIGHT_KG = 25.0
    const val MAX_WEIGHT_KG = 350.0

    fun validate(input: ProfileInput): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        val age = input.age.trim().toIntOrNull()
        if (age == null) {
            errors[FIELD_AGE] = "Enter your age in years."
        } else if (age < MIN_AGE || age > MAX_AGE) {
            errors[FIELD_AGE] = "Age must be between $MIN_AGE and $MAX_AGE."
        }

        val height = input.heightCm.trim().toDoubleOrNull()
        if (height == null) {
            errors[FIELD_HEIGHT] = "Enter your height in cm."
        } else if (height < MIN_HEIGHT_CM || height > MAX_HEIGHT_CM) {
            errors[FIELD_HEIGHT] =
                "Height must be between ${MIN_HEIGHT_CM.toInt()} and ${MAX_HEIGHT_CM.toInt()} cm."
        }

        val weight = input.weightKg.trim().toDoubleOrNull()
        if (weight == null) {
            errors[FIELD_WEIGHT] = "Enter your weight in kg."
        } else if (weight < MIN_WEIGHT_KG || weight > MAX_WEIGHT_KG) {
            errors[FIELD_WEIGHT] =
                "Weight must be between ${MIN_WEIGHT_KG.toInt()} and ${MAX_WEIGHT_KG.toInt()} kg."
        }

        if (input.equipment.isEmpty()) {
            errors[FIELD_EQUIPMENT] = "Select at least one option."
        } else if (input.equipment.contains(Equipment.NONE) && input.equipment.size > 1) {
            errors[FIELD_EQUIPMENT] = "“None” cannot be combined with other equipment."
        }

        if (input.goals.isEmpty()) {
            errors[FIELD_GOALS] = "Select at least one goal."
        }

        if (input.trainingDays.isEmpty()) {
            errors[FIELD_DAYS] = "Select at least one training day."
        } else if (!input.trainingDays.all { it in 1..7 }) {
            errors[FIELD_DAYS] = "Training days are invalid."
        }

        if (input.sessionMinutes !in UserProfile.ALLOWED_SESSION_MINUTES) {
            errors[FIELD_SESSION] = "Pick one of the offered session lengths."
        }

        return errors
    }

    /** Converts validated input into the entity. Call only when valid. */
    fun toProfile(input: ProfileInput): com.shadowbody.app.data.local.UserProfile =
        com.shadowbody.app.data.local.UserProfile(
            age = input.age.trim().toInt(),
            heightCm = input.heightCm.trim().toDouble(),
            weightKg = input.weightKg.trim().toDouble(),
            fitnessLevel = input.fitnessLevel,
            equipment = input.equipment.toSet(),
            goals = input.goals.toSet(),
            trainingDays = input.trainingDays.toSet(),
            sessionMinutes = input.sessionMinutes,
        )
}
