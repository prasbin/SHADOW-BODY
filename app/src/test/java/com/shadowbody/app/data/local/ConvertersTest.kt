package com.shadowbody.app.data.local

import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.FitnessLevel
import com.shadowbody.app.domain.model.Goal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `fitness level round-trips`() {
        assertEquals(
            FitnessLevel.ADVANCED,
            converters.stringToFitnessLevel(converters.fitnessLevelToString(FitnessLevel.ADVANCED)),
        )
    }

    @Test
    fun `unknown fitness level falls back to beginner`() {
        assertEquals(FitnessLevel.BEGINNER, converters.stringToFitnessLevel("ULTRA"))
    }

    @Test
    fun `equipment set round-trips`() {
        val original = setOf(Equipment.DUMBBELLS, Equipment.BENCH)
        assertEquals(original, converters.stringToEquipmentSet(converters.equipmentSetToString(original)))
    }

    @Test
    fun `unknown equipment tokens are dropped`() {
        assertEquals(
            setOf(Equipment.BODYWEIGHT),
            converters.stringToEquipmentSet("BODYWEIGHT,LASER_SWORD"),
        )
    }

    @Test
    fun `goal set round-trips`() {
        val original = setOf(Goal.LOSE_WEIGHT, Goal.IMPROVE_CONSISTENCY)
        assertEquals(original, converters.stringToGoalSet(converters.goalSetToString(original)))
    }

    @Test
    fun `training days round-trip`() {
        val original = setOf(1, 3, 5)
        assertEquals(original, converters.stringToIntSet(converters.intSetToString(original)))
        assertTrue(converters.stringToIntSet("1,nope,7").containsAll(listOf(1, 7)))
    }
}
