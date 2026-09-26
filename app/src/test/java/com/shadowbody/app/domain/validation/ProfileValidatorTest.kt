package com.shadowbody.app.domain.validation

import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.FitnessLevel
import com.shadowbody.app.domain.model.Goal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidatorTest {

    private fun valid() = ProfileInput(
        age = "28",
        heightCm = "178",
        weightKg = "75.5",
        fitnessLevel = FitnessLevel.INTERMEDIATE,
        equipment = setOf(Equipment.BODYWEIGHT, Equipment.DUMBBELLS),
        goals = setOf(Goal.BUILD_STRENGTH),
        trainingDays = setOf(1, 3, 5),
        sessionMinutes = 45,
    )

    @Test
    fun `valid input has no errors`() {
        assertTrue(ProfileValidator.validate(valid()).isEmpty())
    }

    @Test
    fun `blank age is rejected`() {
        val errors = ProfileValidator.validate(valid().copy(age = ""))
        assertEquals("Enter your age in years.", errors[ProfileValidator.FIELD_AGE])
    }

    @Test
    fun `non-numeric age is rejected`() {
        assertTrue(ProfileValidator.validate(valid().copy(age = "old")).containsKey(ProfileValidator.FIELD_AGE))
    }

    @Test
    fun `age outside human range is rejected`() {
        assertTrue(ProfileValidator.validate(valid().copy(age = "5")).containsKey(ProfileValidator.FIELD_AGE))
        assertTrue(ProfileValidator.validate(valid().copy(age = "150")).containsKey(ProfileValidator.FIELD_AGE))
    }

    @Test
    fun `height bounds are enforced`() {
        assertTrue(ProfileValidator.validate(valid().copy(heightCm = "50")).containsKey(ProfileValidator.FIELD_HEIGHT))
        assertTrue(ProfileValidator.validate(valid().copy(heightCm = "300")).containsKey(ProfileValidator.FIELD_HEIGHT))
    }

    @Test
    fun `weight bounds are enforced`() {
        assertTrue(ProfileValidator.validate(valid().copy(weightKg = "10")).containsKey(ProfileValidator.FIELD_WEIGHT))
        assertTrue(ProfileValidator.validate(valid().copy(weightKg = "500")).containsKey(ProfileValidator.FIELD_WEIGHT))
    }

    @Test
    fun `none combined with equipment is rejected`() {
        val errors = ProfileValidator.validate(
            valid().copy(equipment = setOf(Equipment.NONE, Equipment.BENCH)),
        )
        assertTrue(errors.containsKey(ProfileValidator.FIELD_EQUIPMENT))
    }

    @Test
    fun `lone none is accepted`() {
        val errors = ProfileValidator.validate(valid().copy(equipment = setOf(Equipment.NONE)))
        assertTrue(!errors.containsKey(ProfileValidator.FIELD_EQUIPMENT))
    }

    @Test
    fun `empty selections are rejected`() {
        val errors = ProfileValidator.validate(
            valid().copy(equipment = emptySet(), goals = emptySet(), trainingDays = emptySet()),
        )
        assertTrue(errors.containsKey(ProfileValidator.FIELD_EQUIPMENT))
        assertTrue(errors.containsKey(ProfileValidator.FIELD_GOALS))
        assertTrue(errors.containsKey(ProfileValidator.FIELD_DAYS))
    }

    @Test
    fun `invalid training days and session length are rejected`() {
        val errors = ProfileValidator.validate(valid().copy(trainingDays = setOf(9), sessionMinutes = 90))
        assertTrue(errors.containsKey(ProfileValidator.FIELD_DAYS))
        assertTrue(errors.containsKey(ProfileValidator.FIELD_SESSION))
    }

    @Test
    fun `toProfile maps a valid input`() {
        val profile = ProfileValidator.toProfile(valid())
        assertEquals(28, profile.age)
        assertEquals(178.0, profile.heightCm, 0.0)
        assertEquals(75.5, profile.weightKg, 0.0)
        assertEquals(FitnessLevel.INTERMEDIATE, profile.fitnessLevel)
        assertEquals(45, profile.sessionMinutes)
    }
}
