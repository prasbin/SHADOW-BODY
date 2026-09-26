package com.shadowbody.app.domain.validation

import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutValidatorTest {

    @Test
    fun `valid plan passes`() {
        assertTrue(WorkoutValidator.validatePlan(PlanInput(name = "Morning Push"), 2).isEmpty())
    }

    @Test
    fun `blank and oversized names fail`() {
        assertTrue(
            WorkoutValidator.validatePlan(PlanInput(name = "  "), 1)
                .containsKey(WorkoutValidator.FIELD_NAME),
        )
        assertTrue(
            WorkoutValidator.validatePlan(PlanInput(name = "x".repeat(81)), 1)
                .containsKey(WorkoutValidator.FIELD_NAME),
        )
    }

    @Test
    fun `empty plan and bad duration fail`() {
        val errors = WorkoutValidator.validatePlan(
            PlanInput(name = "P", targetDurationMin = "9999"), 0,
        )
        assertTrue(errors.containsKey(WorkoutValidator.FIELD_SLOTS))
        assertTrue(errors.containsKey(WorkoutValidator.FIELD_DURATION))
    }

    @Test
    fun `valid slots pass`() {
        val slots = listOf(
            PlanSlotInput(exerciseId = 1, targetSets = "3", targetReps = "10", restSec = "60"),
            PlanSlotInput(exerciseId = 2, targetSets = "2", targetDurationSec = "30", restSec = "45"),
        )
        assertTrue(WorkoutValidator.validateSlots(slots).isEmpty())
    }

    @Test
    fun `bad slot numbers fail per slot`() {
        val errors = WorkoutValidator.validateSlots(
            listOf(PlanSlotInput(exerciseId = 1, targetSets = "0", targetReps = "9999", restSec = "-5")),
        )
        assertTrue(errors.containsKey("0:sets"))
        assertTrue(errors.containsKey("0:reps"))
        assertTrue(errors.containsKey("0:rest"))
    }

    @Test
    fun `time-based slot ignores reps`() {
        val errors = WorkoutValidator.validateSlots(
            listOf(PlanSlotInput(exerciseId = 1, targetSets = "3", targetDurationSec = "3")),
        )
        assertTrue(errors.containsKey("0:duration"))
    }

    @Test
    fun `set log blanks are fine, garbage is not`() {
        assertTrue(WorkoutValidator.validateSetLog("", "", "").isEmpty())
        assertTrue(WorkoutValidator.validateSetLog("8", "", "20").isEmpty())
        val errors = WorkoutValidator.validateSetLog("abc", "-1", "NaN")
        assertTrue(errors.containsKey("reps"))
        assertTrue(errors.containsKey("duration"))
        assertTrue(errors.containsKey("weight"))
    }

    @Test
    fun `completing a set requires recorded work`() {
        assertTrue(WorkoutValidator.validateSetCompletion("", "", "").containsKey("reps"))
        assertTrue(WorkoutValidator.validateSetCompletion("0", "", "20").containsKey("reps"))
        assertTrue(WorkoutValidator.validateSetCompletion("0", "0", "").containsKey("reps"))
        assertTrue(WorkoutValidator.validateSetCompletion("8", "", "20").isEmpty())
        assertTrue(WorkoutValidator.validateSetCompletion("", "45", "").isEmpty())
        assertTrue(WorkoutValidator.validateSetCompletion("abc", "", "").containsKey("reps"))
    }
}
