package com.shadowbody.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSeedsTest {

    @Test
    fun `library holds 28 unique named exercises`() {
        val seeds = ExerciseSeeds.ALL
        assertEquals(28, seeds.size)
        assertEquals(28, seeds.map { it.name }.distinct().size)
    }

    @Test
    fun `every seed is complete and flagged`() {
        ExerciseSeeds.ALL.forEach { exercise ->
            assertTrue(exercise.name.isNotBlank())
            assertTrue(exercise.description.isNotBlank())
            assertTrue(exercise.instructions.isNotBlank())
            assertTrue(exercise.isSeeded)
        }
    }

    @Test
    fun `major movement categories are covered`() {
        val muscles = ExerciseSeeds.ALL.map { it.muscleGroup }.toSet()
        assertTrue(muscles.size >= 6)
        val categories = ExerciseSeeds.ALL.map { it.category }.toSet()
        assertTrue(categories.size >= 3)
    }
}
