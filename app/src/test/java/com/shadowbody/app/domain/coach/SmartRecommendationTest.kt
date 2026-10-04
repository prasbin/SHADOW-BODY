package com.shadowbody.app.domain.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartRecommendationTest {

    private val engine = LocalDeterministicCoachEngine()

    @Test
    fun saturdayShowsRecoveryAsTopPriority() {
        val profile = CoachProfile(
            hasProfile = true,
            dayOfWeek = 7,
            trainingDays = setOf(1, 2, 3, 4, 5, 6, 7),
        )
        val summary = engine.generate(profile)

        assertEquals("saturday_recovery", summary.topRecommendation?.id)
        assertEquals("RECOVER", summary.topRecommendation?.title)
        assertTrue(summary.topRecommendation?.reason?.contains("Saturday") == true)
    }

    @Test
    fun saturdayDoesNotRecommendWorkout() {
        val profile = CoachProfile(
            hasProfile = true,
            dayOfWeek = 7,
            trainingDays = setOf(1, 2, 3, 4, 5, 6, 7),
        )
        val summary = engine.generate(profile)

        val workoutRec = summary.recommendations.find { it.id == "workout_today" }
        assertEquals(null, workoutRec)
    }

    @Test
    fun trainingDayRecommendsWorkout() {
        val profile = CoachProfile(
            hasProfile = true,
            dayOfWeek = 1,
            trainingDays = setOf(1, 3, 5),
        )
        val summary = engine.generate(profile)

        val workoutRec = summary.recommendations.find { it.id == "workout_today" }
        assertNotNull(workoutRec)
        assertEquals("TRAIN", workoutRec?.title)
    }

    @Test
    fun completedWorkoutChangesRecommendation() {
        val profile = CoachProfile(
            hasProfile = true,
            dayOfWeek = 1,
            trainingDays = setOf(1, 3, 5),
            todayWorkoutCompleted = true,
        )
        val summary = engine.generate(profile)

        val workoutRec = summary.recommendations.find { it.id == "workout_today" }
        assertEquals(null, workoutRec)

        val doneRec = summary.recommendations.find { it.id == "workout_done" }
        assertNotNull(doneRec)
    }

    @Test
    fun hydrationLowRecommendsWater() {
        val profile = CoachProfile(
            hasProfile = true,
            hydrationMl = 500,
            hydrationGoalMl = 3000,
        )
        val summary = engine.generate(profile)

        val hydrationRec = summary.recommendations.find { it.id == "hydration_low" }
        assertNotNull(hydrationRec)
        assertEquals("DRINK WATER", hydrationRec?.title)
    }

    @Test
    fun morningNotStartedRecommendsCompletion() {
        val profile = CoachProfile(
            hasProfile = true,
            morningStatus = "NOT_STARTED",
        )
        val summary = engine.generate(profile)

        val morningRec = summary.recommendations.find { it.id == "morning_pending" }
        assertNotNull(morningRec)
        assertEquals("COMPLETE MORNING ROUTINE", morningRec?.title)
    }

    @Test
    fun groomingNotStartedRecommendsCheck() {
        val profile = CoachProfile(
            hasProfile = true,
            groomingStatus = "NOT_STARTED",
        )
        val summary = engine.generate(profile)

        val groomingRec = summary.recommendations.find { it.id == "grooming_pending" }
        assertNotNull(groomingRec)
        assertEquals("CHECK GROOMING", groomingRec?.title)
    }

    @Test
    fun wardrobeItemsRecommendOutfit() {
        val profile = CoachProfile(
            hasProfile = true,
            wardrobeItemCount = 5,
        )
        val summary = engine.generate(profile)

        val outfitRec = summary.recommendations.find { it.id == "outfit_suggestion" }
        assertNotNull(outfitRec)
        assertEquals("PREPARE OUTFIT", outfitRec?.title)
    }

    @Test
    fun deterministicSameInputSameOutput() {
        val profile = CoachProfile(
            hasProfile = true,
            dayOfWeek = 1,
            trainingDays = setOf(1, 3, 5),
            hydrationMl = 500,
            hydrationGoalMl = 3000,
        )

        val summary1 = engine.generate(profile)
        val summary2 = engine.generate(profile)

        assertEquals(summary1.recommendations.size, summary2.recommendations.size)
        assertEquals(summary1.topRecommendation?.id, summary2.topRecommendation?.id)
    }

    @Test
    fun recommendationsHaveReasons() {
        val profile = CoachProfile(
            hasProfile = true,
            dayOfWeek = 1,
            trainingDays = setOf(1, 3, 5),
        )
        val summary = engine.generate(profile)

        summary.recommendations.forEach { rec ->
            assertTrue("${rec.id} should have a reason", rec.reason.isNotBlank())
        }
    }

    @Test
    fun noDuplicateRecommendations() {
        val profile = CoachProfile(
            hasProfile = true,
            dayOfWeek = 1,
            trainingDays = setOf(1, 3, 5),
        )
        val summary = engine.generate(profile)

        val ids = summary.recommendations.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun noCloudDependency() {
        val profile = CoachProfile(hasProfile = true)
        val summary = engine.generate(profile)
        assertNotNull(summary)
    }

    @Test
    fun highFatigueRecommendsLighterSession() {
        val profile = CoachProfile(
            hasProfile = true,
            hasReadinessToday = true,
            fatigue = 5,
        )
        val summary = engine.generate(profile)

        val fatigueRec = summary.recommendations.find { it.id == "high_fatigue" }
        assertNotNull(fatigueRec)
    }

    @Test
    fun missedWorkoutsRecommendConsistency() {
        val profile = CoachProfile(
            hasProfile = true,
            recentMissedWorkouts = 2,
        )
        val summary = engine.generate(profile)

        val consistencyRec = summary.recommendations.find { it.id == "consistency" }
        assertNotNull(consistencyRec)
    }

    @Test
    fun streakRecommendsKeepingAlive() {
        val profile = CoachProfile(
            hasProfile = true,
            currentStreak = 5,
        )
        val summary = engine.generate(profile)

        val streakRec = summary.recommendations.find { it.id == "streak" }
        assertNotNull(streakRec)
    }

    @Test
    fun emptyProfileRecommendsSetup() {
        val profile = CoachProfile(hasProfile = false)
        val summary = engine.generate(profile)

        assertEquals("setup_profile", summary.topRecommendation?.id)
    }
}
