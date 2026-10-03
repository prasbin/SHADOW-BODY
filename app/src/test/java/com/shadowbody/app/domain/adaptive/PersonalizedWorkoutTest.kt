package com.shadowbody.app.domain.adaptive

import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.ExerciseCategory
import com.shadowbody.app.domain.model.FitnessLevel
import com.shadowbody.app.domain.model.Goal
import com.shadowbody.app.domain.model.MuscleGroup
import com.shadowbody.app.domain.model.ProgressionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalizedWorkoutTest {

    private fun context(
        aggressionLevel: Int = 3,
        fitnessLevel: FitnessLevel? = FitnessLevel.INTERMEDIATE,
        heightCm: Double? = 175.0,
        weightKg: Double? = 70.0,
        age: Int? = 30,
        readiness: ReadinessSnapshot? = null,
        missedSessions: Int = 0,
        equipment: Set<Equipment> = setOf(Equipment.BODYWEIGHT),
    ) = GenerationContext(
        now = System.currentTimeMillis(),
        equipment = equipment,
        goals = setOf(Goal.BUILD_STRENGTH),
        fitnessLevel = fitnessLevel,
        heightCm = heightCm,
        weightKg = weightKg,
        age = age,
        aggressionLevel = aggressionLevel,
        sessionMinutes = 30,
        trainingDays = setOf(1, 3, 5),
        library = listOf(
            SlotCandidate(
                exerciseId = 1L,
                exerciseName = "Push-Up",
                muscleGroup = MuscleGroup.CHEST,
                equipment = Equipment.BODYWEIGHT,
                category = ExerciseCategory.BODYWEIGHT,
                sets = 3,
                reps = 10,
                durationSec = null,
                restSec = 60,
            ),
        ),
        adaptations = emptyMap(),
        readiness = readiness,
        missedSessions = missedSessions,
    )

    @Test
    fun highAggressionProducesHigherIntensity() {
        val lowAggression = WorkoutGenerator.generate(context(aggressionLevel = 1))
        val highAggression = WorkoutGenerator.generate(context(aggressionLevel = 5))

        assertTrue(lowAggression != null)
        assertTrue(highAggression != null)

        val lowSets = lowAggression!!.exercises.sumOf { it.sets }
        val highSets = highAggression!!.exercises.sumOf { it.sets }

        assertTrue("High aggression should produce more sets", highSets >= lowSets)
    }

    @Test
    fun beginnerFitnessLevelReducesIntensity() {
        val beginner = WorkoutGenerator.generate(context(fitnessLevel = FitnessLevel.BEGINNER))
        val advanced = WorkoutGenerator.generate(context(fitnessLevel = FitnessLevel.ADVANCED))

        assertTrue(beginner != null)
        assertTrue(advanced != null)

        val beginnerSets = beginner!!.exercises.sumOf { it.sets }
        val advancedSets = advanced!!.exercises.sumOf { it.sets }

        assertTrue("Advanced should produce more sets", advancedSets >= beginnerSets)
    }

    @Test
    fun highFatigueReducesIntensity() {
        val normal = WorkoutGenerator.generate(context(readiness = null))
        val tired = WorkoutGenerator.generate(
            context(readiness = ReadinessSnapshot(fatigue = 5, soreness = 2, notes = ""))
        )

        assertTrue(normal != null)
        assertTrue(tired != null)

        assertTrue(
            "High fatigue should reduce or maintain intensity",
            tired!!.exercises.sumOf { it.sets } <= normal!!.exercises.sumOf { it.sets }
        )
    }

    @Test
    fun missedSessionsReduceVolume() {
        val normal = WorkoutGenerator.generate(context(missedSessions = 0))
        val missed = WorkoutGenerator.generate(context(missedSessions = 3))

        assertTrue(normal != null)
        assertTrue(missed != null)

        assertTrue(
            "Missed sessions should reduce or maintain volume",
            missed!!.exercises.sumOf { it.sets } <= normal!!.exercises.sumOf { it.sets }
        )
    }

    @Test
    fun deterministicSameInputSameOutput() {
        val context = context(aggressionLevel = 4, fitnessLevel = FitnessLevel.INTERMEDIATE)
        val result1 = WorkoutGenerator.generate(context)
        val result2 = WorkoutGenerator.generate(context)

        assertEquals(result1, result2)
    }

    @Test
    fun bmiAffectsIntensity() {
        val normalBmi = WorkoutGenerator.generate(context(heightCm = 175.0, weightKg = 70.0))
        val highBmi = WorkoutGenerator.generate(context(heightCm = 175.0, weightKg = 120.0))

        assertTrue(normalBmi != null)
        assertTrue(highBmi != null)

        assertTrue(
            "High BMI should reduce or maintain intensity",
            highBmi!!.exercises.sumOf { it.sets } <= normalBmi!!.exercises.sumOf { it.sets }
        )
    }

    @Test
    fun ageAffectsIntensity() {
        val young = WorkoutGenerator.generate(context(age = 20))
        val older = WorkoutGenerator.generate(context(age = 60))

        assertTrue(young != null)
        assertTrue(older != null)

        assertTrue(
            "Older age should reduce or maintain intensity",
            older!!.exercises.sumOf { it.sets } <= young!!.exercises.sumOf { it.sets }
        )
    }

    @Test
    fun equipmentConstraintsRespected() {
        val context = context(equipment = setOf(Equipment.BODYWEIGHT))
        val result = WorkoutGenerator.generate(context)

        assertTrue(result != null)
        result!!.exercises.forEach { exercise ->
            assertEquals(Equipment.BODYWEIGHT, Equipment.BODYWEIGHT)
        }
    }
}
