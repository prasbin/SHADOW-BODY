package com.shadowbody.app.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionValidatorTest {

    @Test
    fun validFoodLogPasses() {
        val result = NutritionValidator.validateFoodLog("Rice", "200", "4", "45", "1", "1 cup")
        assertTrue(result.isValid)
    }

    @Test
    fun negativeCaloriesFail() {
        val result = NutritionValidator.validateFoodLog("Rice", "-10", "4", "45", "1", "1 cup")
        assertFalse(result.isValid)
        assertEquals("Calories cannot be negative.", result.caloriesError)
    }

    @Test
    fun emptyNameFails() {
        val result = NutritionValidator.validateFoodLog("", "200", "4", "45", "1", "1 cup")
        assertFalse(result.isValid)
        assertEquals("Food name is required.", result.nameError)
    }

    @Test
    fun validGoalPasses() {
        val result = NutritionValidator.validateGoal("2000", "150", "200", "70", "2500")
        assertTrue(result.isValid)
    }

    @Test
    fun outOfRangeGoalFails() {
        val result = NutritionValidator.validateGoal("200", "150", "200", "70", "2500")
        assertFalse(result.isValid)
        assertEquals("Calories must be between 500 and 10,000.", result.caloriesError)
    }
}
