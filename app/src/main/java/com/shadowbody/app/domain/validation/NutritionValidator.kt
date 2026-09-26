package com.shadowbody.app.domain.validation

data class NutritionValidationResult(
    val isValid: Boolean,
    val nameError: String? = null,
    val caloriesError: String? = null,
    val proteinError: String? = null,
    val carbError: String? = null,
    val fatError: String? = null,
    val servingError: String? = null,
)

data class NutritionGoalValidationResult(
    val isValid: Boolean,
    val caloriesError: String? = null,
    val proteinError: String? = null,
    val carbError: String? = null,
    val fatError: String? = null,
    val hydrationError: String? = null,
)

object NutritionValidator {
    fun validateFoodLog(
        name: String,
        caloriesStr: String,
        proteinStr: String,
        carbStr: String,
        fatStr: String,
        servingText: String,
    ): NutritionValidationResult {
        val trimmedName = name.trim()
        val nameErr = if (trimmedName.isEmpty()) "Food name is required." else null

        val calories = caloriesStr.trim().toIntOrNull()
        val calErr = when {
            calories == null -> "Calories must be a valid number."
            calories < 0 -> "Calories cannot be negative."
            calories > 10000 -> "Calories exceed realistic maximum."
            else -> null
        }

        val protein = proteinStr.trim().toDoubleOrNull()
        val proErr = when {
            protein == null -> "Protein must be a valid number."
            protein < 0.0 -> "Protein cannot be negative."
            protein > 1000.0 -> "Protein exceeds realistic maximum."
            else -> null
        }

        val carbs = carbStr.trim().toDoubleOrNull()
        val carbErr = when {
            carbs == null -> "Carbs must be a valid number."
            carbs < 0.0 -> "Carbs cannot be negative."
            carbs > 1500.0 -> "Carbs exceed realistic maximum."
            else -> null
        }

        val fat = fatStr.trim().toDoubleOrNull()
        val fatErr = when {
            fat == null -> "Fat must be a valid number."
            fat < 0.0 -> "Fat cannot be negative."
            fat > 1000.0 -> "Fat exceeds realistic maximum."
            else -> null
        }

        val trimmedServing = servingText.trim()
        val servingErr = if (trimmedServing.isEmpty()) "Serving size is required." else null

        val isValid = nameErr == null && calErr == null && proErr == null && carbErr == null && fatErr == null && servingErr == null
        return NutritionValidationResult(isValid, nameErr, calErr, proErr, carbErr, fatErr, servingErr)
    }

    fun validateGoal(
        caloriesStr: String,
        proteinStr: String,
        carbStr: String,
        fatStr: String,
        hydrationStr: String,
    ): NutritionGoalValidationResult {
        val calories = caloriesStr.trim().toIntOrNull()
        val calErr = when {
            calories == null -> "Calories must be a valid number."
            calories !in 500..10000 -> "Calories must be between 500 and 10,000."
            else -> null
        }

        val protein = proteinStr.trim().toIntOrNull()
        val proErr = when {
            protein == null -> "Protein must be a valid number."
            protein !in 0..1000 -> "Protein must be between 0 and 1,000g."
            else -> null
        }

        val carbs = carbStr.trim().toIntOrNull()
        val carbErr = when {
            carbs == null -> "Carbs must be a valid number."
            carbs !in 0..1500 -> "Carbs must be between 0 and 1,500g."
            else -> null
        }

        val fat = fatStr.trim().toIntOrNull()
        val fatErr = when {
            fat == null -> "Fat must be a valid number."
            fat !in 0..1000 -> "Fat must be between 0 and 1,000g."
            else -> null
        }

        val hydration = hydrationStr.trim().toIntOrNull()
        val hydErr = when {
            hydration == null -> "Hydration must be a valid number."
            hydration !in 0..10000 -> "Hydration must be between 0 and 10,000ml."
            else -> null
        }

        val isValid = calErr == null && proErr == null && carbErr == null && fatErr == null && hydErr == null
        return NutritionGoalValidationResult(isValid, calErr, proErr, carbErr, fatErr, hydErr)
    }
}
