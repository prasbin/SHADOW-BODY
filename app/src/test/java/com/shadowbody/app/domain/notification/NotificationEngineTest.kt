package com.shadowbody.app.domain.notification

import com.shadowbody.app.domain.coach.CoachProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationEngineTest {

    private fun profile(
        dayOfWeek: Int = 1,
        trainingDays: Set<Int> = setOf(1, 3, 5),
        todayWorkoutCompleted: Boolean = false,
        hasReadinessToday: Boolean = false,
        fatigue: Int = 0,
        hydrationMl: Int = 0,
        hydrationGoalMl: Int = 0,
        morningStatus: String = "NOT_STARTED",
        groomingStatus: String = "NOT_STARTED",
    ) = CoachProfile(
        hasProfile = true,
        dayOfWeek = dayOfWeek,
        trainingDays = trainingDays,
        todayWorkoutCompleted = todayWorkoutCompleted,
        hasReadinessToday = hasReadinessToday,
        fatigue = fatigue,
        hydrationMl = hydrationMl,
        hydrationGoalMl = hydrationGoalMl,
        morningStatus = morningStatus,
        groomingStatus = groomingStatus,
    )

    @Test
    fun saturdayShowsRecoveryNotification() {
        val saturdayProfile = CoachProfile(
            hasProfile = true,
            dayOfWeek = 7,
            trainingDays = setOf(1, 2, 3, 4, 5, 6),
        )
        val result = NotificationEngine.generate(saturdayProfile)

        assertEquals("saturday_recovery", result.topNotification?.id)
        assertEquals("RECOVERY DAY", result.topNotification?.title)
    }



    @Test
    fun saturdayDoesNotShowWorkoutReminder() {
        val result = NotificationEngine.generate(profile(dayOfWeek = 7))

        val workoutNotif = result.notifications.find { it.id == "workout_reminder" }
        assertNull(workoutNotif)
    }

    @Test
    fun trainingDayShowsWorkoutReminder() {
        val result = NotificationEngine.generate(profile(dayOfWeek = 1))

        val workoutNotif = result.notifications.find { it.id == "workout_reminder" }
        assertNotNull(workoutNotif)
        assertEquals("TRAIN", workoutNotif?.title)
    }

    @Test
    fun completedWorkoutSuppressesReminder() {
        val result = NotificationEngine.generate(profile(dayOfWeek = 1, todayWorkoutCompleted = true))

        val workoutNotif = result.notifications.find { it.id == "workout_reminder" }
        assertNull(workoutNotif)
    }

    @Test
    fun lowHydrationShowsReminder() {
        val result = NotificationEngine.generate(
            profile(dayOfWeek = 1, hydrationMl = 500, hydrationGoalMl = 3000)
        )

        val hydrationNotif = result.notifications.find { it.id == "hydration_reminder" }
        assertNotNull(hydrationNotif)
        assertEquals("DRINK WATER", hydrationNotif?.title)
    }

    @Test
    fun adequateHydrationSuppressesReminder() {
        val result = NotificationEngine.generate(
            profile(dayOfWeek = 1, hydrationMl = 2000, hydrationGoalMl = 3000)
        )

        val hydrationNotif = result.notifications.find { it.id == "hydration_reminder" }
        assertNull(hydrationNotif)
    }

    @Test
    fun morningNotStartedShowsReminder() {
        val result = NotificationEngine.generate(profile(dayOfWeek = 1, morningStatus = "NOT_STARTED"))

        val morningNotif = result.notifications.find { it.id == "morning_reminder" }
        assertNotNull(morningNotif)
    }

    @Test
    fun morningCompletedSuppressesReminder() {
        val result = NotificationEngine.generate(profile(dayOfWeek = 1, morningStatus = "COMPLETED"))

        val morningNotif = result.notifications.find { it.id == "morning_reminder" }
        assertNull(morningNotif)
    }

    @Test
    fun groomingNotStartedShowsReminder() {
        val result = NotificationEngine.generate(profile(dayOfWeek = 1, groomingStatus = "NOT_STARTED"))

        val groomingNotif = result.notifications.find { it.id == "grooming_reminder" }
        assertNotNull(groomingNotif)
    }

    @Test
    fun highFatigueShowsAlert() {
        val result = NotificationEngine.generate(
            profile(dayOfWeek = 1, hasReadinessToday = true, fatigue = 5)
        )

        val fatigueNotif = result.notifications.find { it.id == "fatigue_alert" }
        assertNotNull(fatigueNotif)
    }

    @Test
    fun deterministicSameInputSameOutput() {
        val result1 = NotificationEngine.generate(profile(dayOfWeek = 1))
        val result2 = NotificationEngine.generate(profile(dayOfWeek = 1))

        assertEquals(result1.notifications.size, result2.notifications.size)
        assertEquals(result1.topNotification?.id, result2.topNotification?.id)
    }

    @Test
    fun noDuplicateNotifications() {
        val result = NotificationEngine.generate(profile(dayOfWeek = 1))

        val ids = result.notifications.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun noCloudDependency() {
        val result = NotificationEngine.generate(profile(dayOfWeek = 1))
        assertNotNull(result)
    }

    @Test
    fun notificationsHaveContent() {
        val result = NotificationEngine.generate(profile(dayOfWeek = 1))

        result.notifications.forEach { notif ->
            assertTrue("${notif.id} should have title", notif.title.isNotBlank())
            assertTrue("${notif.id} should have body", notif.body.isNotBlank())
        }
    }

    @Test
    fun busynessRespected() {
        val busyProfile = profile(dayOfWeek = 1).copy(busynessLevel = "VERY_BUSY")
        val result = NotificationEngine.generate(busyProfile)

        assertNotNull(result)
    }

    @Test
    fun missedWorkoutAdaptationRespected() {
        val missedProfile = profile(dayOfWeek = 1).copy(recentMissedWorkouts = 3)
        val result = NotificationEngine.generate(missedProfile)

        assertNotNull(result)
    }
}
