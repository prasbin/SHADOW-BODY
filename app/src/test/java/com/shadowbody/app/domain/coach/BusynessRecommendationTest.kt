package com.shadowbody.app.domain.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusynessRecommendationTest {

    private val engine = LocalDeterministicCoachEngine()

    private fun profile(busyness: String) = CoachProfile(
        hasProfile = true,
        dayOfWeek = 1,
        trainingDays = setOf(1, 3, 5),
        wardrobeItemCount = 5,
        busynessLevel = busyness,
    )

    @Test
    fun lightBusynessAllowsMoreOptionalActions() {
        val result = engine.generate(profile("LIGHT"))
        val outfitRec = result.recommendations.find { it.id == "outfit_suggestion" }
        assertNotNull(outfitRec)
        assertTrue(outfitRec!!.priority >= 35)
    }

    @Test
    fun normalBusynessHasStandardPriority() {
        val result = engine.generate(profile("NORMAL"))
        val outfitRec = result.recommendations.find { it.id == "outfit_suggestion" }
        assertNotNull(outfitRec)
        assertEquals(30, outfitRec!!.priority)
    }

    @Test
    fun busyReducesOptionalPriority() {
        val result = engine.generate(profile("BUSY"))
        val outfitRec = result.recommendations.find { it.id == "outfit_suggestion" }
        assertNotNull(outfitRec)
        assertEquals(20, outfitRec!!.priority)
    }

    @Test
    fun veryBusyMinimizesOptionalActions() {
        val result = engine.generate(profile("VERY_BUSY"))
        val outfitRec = result.recommendations.find { it.id == "outfit_suggestion" }
        assertNotNull(outfitRec)
        assertEquals(10, outfitRec!!.priority)
    }

    @Test
    fun busyDoesNotOverrideSaturdayRecovery() {
        val saturdayProfile = CoachProfile(
            hasProfile = true,
            dayOfWeek = 7,
            trainingDays = setOf(1, 2, 3, 4, 5, 6, 7),
            busynessLevel = "VERY_BUSY",
        )
        val result = engine.generate(saturdayProfile)

        assertEquals("saturday_recovery", result.topRecommendation?.id)
    }

    @Test
    fun busyDoesNotOverrideFatigue() {
        val tiredProfile = CoachProfile(
            hasProfile = true,
            dayOfWeek = 1,
            trainingDays = setOf(1, 3, 5),
            hasReadinessToday = true,
            fatigue = 5,
            busynessLevel = "VERY_BUSY",
        )
        val result = engine.generate(tiredProfile)

        val fatigueRec = result.recommendations.find { it.id == "high_fatigue" }
        assertNotNull(fatigueRec)
        assertTrue(fatigueRec!!.priority > 80)
    }

    @Test
    fun busyDoesNotOverrideMissedWorkoutAdaptation() {
        val missedProfile = CoachProfile(
            hasProfile = true,
            dayOfWeek = 1,
            trainingDays = setOf(1, 3, 5),
            recentMissedWorkouts = 3,
            busynessLevel = "VERY_BUSY",
        )
        val result = engine.generate(missedProfile)

        val consistencyRec = result.recommendations.find { it.id == "consistency" }
        assertNotNull(consistencyRec)
    }

    @Test
    fun deterministicSameBusynessSameOutput() {
        val result1 = engine.generate(profile("BUSY"))
        val result2 = engine.generate(profile("BUSY"))

        assertEquals(result1.recommendations.size, result2.recommendations.size)
        assertEquals(result1.topRecommendation?.id, result2.topRecommendation?.id)
    }

    @Test
    fun defaultBusynessIsNormal() {
        val profile = CoachProfile(hasProfile = true)
        assertEquals("NORMAL", profile.busynessLevel)
    }

    @Test
    fun noCloudDependency() {
        val result = engine.generate(profile("BUSY"))
        assertNotNull(result)
    }

    @Test
    fun allBusynessLevelsSupported() {
        listOf("LIGHT", "NORMAL", "BUSY", "VERY_BUSY").forEach { level ->
            val result = engine.generate(profile(level))
            assertNotNull(result)
            assertTrue(result.recommendations.isNotEmpty())
        }
    }

    @Test
    fun busyReasonsExplainEffect() {
        val result = engine.generate(profile("BUSY"))
        val outfitRec = result.recommendations.find { it.id == "outfit_suggestion" }
        assertNotNull(outfitRec)
        assertTrue(outfitRec!!.reason.isNotBlank())
    }
}
