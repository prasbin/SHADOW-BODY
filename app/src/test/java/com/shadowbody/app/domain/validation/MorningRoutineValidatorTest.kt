package com.shadowbody.app.domain.validation

import com.shadowbody.app.domain.model.MorningStepCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5 routine validation.
 *
 * A morning routine is an everyday habit, not a prescription, so the bounds are
 * generous. What is rejected is empty text, absurd targets and a step with
 * nothing to follow.
 */
class MorningRoutineValidatorTest {

    @Test
    fun `a normal routine is accepted`() {
        assertTrue(
            MorningRoutineValidator.validateRoutine("MORNING ACTIVATION", "A short start").isEmpty(),
        )
    }

    @Test
    fun `a blank name is rejected`() {
        val errors = MorningRoutineValidator.validateRoutine("   ", "")
        assertTrue(errors.containsKey(MorningRoutineValidator.FIELD_NAME))
    }

    @Test
    fun `an over-long name is rejected`() {
        val errors = MorningRoutineValidator.validateRoutine(
            "x".repeat(MorningRoutineValidator.MAX_NAME + 1),
            "",
        )
        assertTrue(errors.containsKey(MorningRoutineValidator.FIELD_NAME))
    }

    @Test
    fun `an over-long description is rejected`() {
        val errors = MorningRoutineValidator.validateRoutine(
            "Fine",
            "x".repeat(MorningRoutineValidator.MAX_DESCRIPTION + 1),
        )
        assertTrue(errors.containsKey(MorningRoutineValidator.FIELD_DESCRIPTION))
    }

    @Test
    fun `a well formed step is accepted`() {
        assertTrue(
            MorningRoutineValidator.validateStep(
                title = "Leg swings",
                instructions = "Hold a wall and swing.",
                category = MorningStepCategory.JOINT_MOBILITY,
                durationSec = null,
                reps = 20,
            ).isEmpty(),
        )
    }

    @Test
    fun `a step needs a name and an instruction`() {
        val errors = MorningRoutineValidator.validateStep(
            title = "  ",
            instructions = " ",
            category = MorningStepCategory.MOBILITY,
            durationSec = null,
            reps = null,
        )
        assertTrue(errors.containsKey(MorningRoutineValidator.FIELD_TITLE))
        assertTrue(errors.containsKey(MorningRoutineValidator.FIELD_INSTRUCTIONS))
    }

    @Test
    fun `a step needs a category`() {
        val errors = MorningRoutineValidator.validateStep("Title", "Do it", null, null, null)
        assertTrue(errors.containsKey(MorningRoutineValidator.FIELD_STEP))
    }

    @Test
    fun `a reminder step with no target is valid`() {
        assertTrue(
            MorningRoutineValidator.validateStep(
                title = "Drink a glass of water",
                instructions = "Fill a glass and drink it slowly.",
                category = MorningStepCategory.HYDRATION,
                durationSec = null,
                reps = null,
            ).isEmpty(),
        )
    }

    @Test
    fun `an out of range duration is rejected`() {
        val tooLong = MorningRoutineValidator.validateStep(
            "Stretch", "Hold it", MorningStepCategory.COOLDOWN,
            MorningRoutineValidator.MAX_DURATION_SEC + 1, null,
        )
        assertTrue(tooLong.containsKey(MorningRoutineValidator.FIELD_DURATION))

        val tooShort = MorningRoutineValidator.validateStep(
            "Stretch", "Hold it", MorningStepCategory.COOLDOWN,
            MorningRoutineValidator.MIN_DURATION_SEC - 1, null,
        )
        assertTrue(tooShort.containsKey(MorningRoutineValidator.FIELD_DURATION))
    }

    @Test
    fun `an out of range repetition count is rejected`() {
        val errors = MorningRoutineValidator.validateStep(
            "Squats", "Sit and stand", MorningStepCategory.ACTIVATION,
            null, MorningRoutineValidator.MAX_REPS + 1,
        )
        assertTrue(errors.containsKey(MorningRoutineValidator.FIELD_REPS))
    }

    @Test
    fun `duration and repetition helpers read the same rule`() {
        assertTrue(MorningRoutineValidator.isTimed(60))
        assertFalse(MorningRoutineValidator.isTimed(null))
        assertFalse(MorningRoutineValidator.isTimed(0))
        assertTrue(MorningRoutineValidator.isCounted(10))
        assertFalse(MorningRoutineValidator.isCounted(null))
    }
}
