package com.shadowbody.app.domain.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDeterministicCoachEngineTest {

    private val engine = LocalDeterministicCoachEngine()

    @Test
    fun emptyProfileRecommendsSetup() {
        val profile = CoachProfile()
        val summary = engine.generate(profile)

        assertNotNull(summary.topRecommendation)
        assertEquals("setup_profile", summary.topRecommendation?.id)
        assertEquals(100, summary.topRecommendation?.priority)
    }

    @Test
    fun highFatigueTriggersRecoveryRecommendation() {
        val profile = CoachProfile(
            hasProfile = true,
            hasReadinessToday = true,
            fatigue = 4,
            soreness = 2,
        )
        val summary = engine.generate(profile)

        val fatigueRec = summary.recommendations.find { it.id == "high_fatigue" }
        assertNotNull(fatigueRec)
        assertEquals(90, fatigueRec?.priority)
        assertEquals("READINESS", fatigueRec?.category)
    }

    @Test
    fun highSorenessTriggersRecoveryRecommendation() {
        val profile = CoachProfile(
            hasProfile = true,
            hasReadinessToday = true,
            fatigue = 2,
            soreness = 5,
        )
        val summary = engine.generate(profile)

        val sorenessRec = summary.recommendations.find { it.id == "high_soreness" }
        assertNotNull(sorenessRec)
        assertEquals(85, sorenessRec?.priority)
    }

    @Test
    fun trainingDayWithoutWorkoutRecommendsWorkout() {
        val profile = CoachProfile(
            hasProfile = true,
            trainingDays = setOf(1, 3, 5),
            dayOfWeek = 1,
            todayWorkoutCompleted = false,
        )
        val summary = engine.generate(profile)

        val workoutRec = summary.recommendations.find { it.id == "workout_today" }
        assertNotNull(workoutRec)
        assertEquals(80, workoutRec?.priority)
        assertEquals("workout", workoutRec?.actionRoute)
    }

    @Test
    fun completedWorkoutShowsPositiveFeedback() {
        val profile = CoachProfile(
            hasProfile = true,
            todayWorkoutCompleted = true,
        )
        val summary = engine.generate(profile)

        val doneRec = summary.recommendations.find { it.id == "workout_done" }
        assertNotNull(doneRec)
        assertEquals(10, doneRec?.priority)
    }

    @Test
    fun lowHydrationTriggersReminder() {
        val profile = CoachProfile(
            hasProfile = true,
            hydrationMl = 500,
            hydrationGoalMl = 3000,
        )
        val summary = engine.generate(profile)

        val hydrationRec = summary.recommendations.find { it.id == "hydration_low" }
        assertNotNull(hydrationRec)
        assertEquals(70, hydrationRec?.priority)
        assertEquals("nutrition", hydrationRec?.actionRoute)
    }

    @Test
    fun adequateHydrationDoesNotTriggerReminder() {
        val profile = CoachProfile(
            hasProfile = true,
            hydrationMl = 2000,
            hydrationGoalMl = 3000,
        )
        val summary = engine.generate(profile)

        val hydrationRec = summary.recommendations.find { it.id == "hydration_low" }
        assertNull(hydrationRec)
    }

    @Test
    fun morningNotStartedTriggersReminder() {
        val profile = CoachProfile(
            hasProfile = true,
            morningStatus = "NOT_STARTED",
        )
        val summary = engine.generate(profile)

        val morningRec = summary.recommendations.find { it.id == "morning_pending" }
        assertNotNull(morningRec)
        assertEquals(60, morningRec?.priority)
        assertEquals("morning", morningRec?.actionRoute)
    }

    @Test
    fun groomingNotStartedTriggersReminder() {
        val profile = CoachProfile(
            hasProfile = true,
            groomingStatus = "NOT_STARTED",
        )
        val summary = engine.generate(profile)

        val groomingRec = summary.recommendations.find { it.id == "grooming_pending" }
        assertNotNull(groomingRec)
        assertEquals(50, groomingRec?.priority)
        assertEquals("grooming", groomingRec?.actionRoute)
    }

    @Test
    fun wardrobeItemsTriggerOutfitSuggestion() {
        val profile = CoachProfile(
            hasProfile = true,
            wardrobeItemCount = 5,
        )
        val summary = engine.generate(profile)

        val outfitRec = summary.recommendations.find { it.id == "outfit_suggestion" }
        assertNotNull(outfitRec)
        assertEquals(30, outfitRec?.priority)
        assertEquals("outfit_generator", outfitRec?.actionRoute)
    }

    @Test
    fun missedWorkoutsTriggerConsistencyReminder() {
        val profile = CoachProfile(
            hasProfile = true,
            recentMissedWorkouts = 2,
        )
        val summary = engine.generate(profile)

        val consistencyRec = summary.recommendations.find { it.id == "consistency" }
        assertNotNull(consistencyRec)
        assertEquals(75, consistencyRec?.priority)
    }

    @Test
    fun streakTriggersEncouragement() {
        val profile = CoachProfile(
            hasProfile = true,
            currentStreak = 5,
        )
        val summary = engine.generate(profile)

        val streakRec = summary.recommendations.find { it.id == "streak" }
        assertNotNull(streakRec)
        assertEquals(20, streakRec?.priority)
        assertEquals("progression", streakRec?.actionRoute)
    }

    @Test
    fun deterministicSameInputSameOutput() {
        val profile = CoachProfile(
            hasProfile = true,
            trainingDays = setOf(1, 3),
            dayOfWeek = 3,
            fatigue = 4,
            hydrationMl = 500,
            hydrationGoalMl = 2000,
            morningStatus = "NOT_STARTED",
            groomingStatus = "NOT_STARTED",
            wardrobeItemCount = 3,
            currentStreak = 2,
        )

        val summary1 = engine.generate(profile)
        val summary2 = engine.generate(profile)

        assertEquals(summary1.recommendations.size, summary2.recommendations.size)
        assertEquals(summary1.topRecommendation?.id, summary2.topRecommendation?.id)
        summary1.recommendations.zip(summary2.recommendations).forEach { (r1, r2) ->
            assertEquals(r1.id, r2.id)
            assertEquals(r1.priority, r2.priority)
            assertEquals(r1.title, r2.title)
        }
    }

    @Test
    fun recommendationsSortedByPriorityDescending() {
        val profile = CoachProfile(
            hasProfile = true,
            trainingDays = setOf(1),
            dayOfWeek = 1,
            fatigue = 5,
            hydrationMl = 100,
            hydrationGoalMl = 3000,
            morningStatus = "NOT_STARTED",
            groomingStatus = "NOT_STARTED",
            wardrobeItemCount = 5,
            currentStreak = 3,
            recentMissedWorkouts = 1,
        )
        val summary = engine.generate(profile)

        val priorities = summary.recommendations.map { it.priority }
        assertEquals(priorities.sortedDescending(), priorities)
    }

    @Test
    fun allRecommendationsHaveReasons() {
        val profile = CoachProfile(
            hasProfile = true,
            trainingDays = setOf(1),
            dayOfWeek = 1,
            fatigue = 4,
            hydrationMl = 500,
            hydrationGoalMl = 2000,
            morningStatus = "NOT_STARTED",
            groomingStatus = "NOT_STARTED",
            wardrobeItemCount = 3,
            currentStreak = 2,
        )
        val summary = engine.generate(profile)

        summary.recommendations.forEach { rec ->
            assertTrue("${rec.id} should have a reason", rec.reason.isNotBlank())
            assertTrue("${rec.id} should have a category", rec.category.isNotBlank())
        }
    }
}
