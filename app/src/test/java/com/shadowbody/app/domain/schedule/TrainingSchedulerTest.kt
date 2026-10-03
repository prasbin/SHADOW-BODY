package com.shadowbody.app.domain.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingSchedulerTest {

    @Test
    fun trainingDayWithActivePlanReturnsWorkout() {
        val result = TrainingScheduler.determineToday(
            isTrainingDay = true,
            activePlanId = 1L,
            activePlanName = "Strength Session",
            estimatedMinutes = 30,
            nextTrainingDay = null,
        )

        assertTrue(result.isTrainingDay)
        assertEquals(1L, result.scheduledWorkout?.planId)
        assertEquals("Strength Session", result.scheduledWorkout?.planName)
        assertEquals(30, result.scheduledWorkout?.estimatedMinutes)
    }

    @Test
    fun restDayReturnsRestInfo() {
        val result = TrainingScheduler.determineToday(
            isTrainingDay = false,
            activePlanId = null,
            activePlanName = null,
            estimatedMinutes = 0,
            nextTrainingDay = "Tomorrow",
        )

        assertFalse(result.isTrainingDay)
        assertNull(result.scheduledWorkout)
        assertTrue(result.restDayInfo?.contains("Rest day") == true)
        assertTrue(result.restDayInfo?.contains("Tomorrow") == true)
    }

    @Test
    fun trainingDayWithoutPlanReturnsNoWorkout() {
        val result = TrainingScheduler.determineToday(
            isTrainingDay = true,
            activePlanId = null,
            activePlanName = null,
            estimatedMinutes = 0,
            nextTrainingDay = null,
        )

        assertTrue(result.isTrainingDay)
        assertNull(result.scheduledWorkout)
    }

    @Test
    fun isTrainingDayReturnsTrueWhenDayInTrainingDays() {
        assertTrue(TrainingScheduler.isTrainingDay(1, setOf(1, 3, 5)))
        assertTrue(TrainingScheduler.isTrainingDay(3, setOf(1, 3, 5)))
        assertTrue(TrainingScheduler.isTrainingDay(5, setOf(1, 3, 5)))
    }

    @Test
    fun isTrainingDayReturnsFalseWhenDayNotInTrainingDays() {
        assertFalse(TrainingScheduler.isTrainingDay(2, setOf(1, 3, 5)))
        assertFalse(TrainingScheduler.isTrainingDay(4, setOf(1, 3, 5)))
        assertFalse(TrainingScheduler.isTrainingDay(6, setOf(1, 3, 5)))
    }

    @Test
    fun emptyTrainingDaysMeansEveryDayExceptSaturdayIsTrainingDay() {
        assertTrue(TrainingScheduler.isTrainingDay(1, emptySet()))
        assertTrue(TrainingScheduler.isTrainingDay(6, emptySet()))
        assertFalse(TrainingScheduler.isTrainingDay(7, emptySet()))
    }

    @Test
    fun saturdayIsAlwaysRestDay() {
        assertFalse(TrainingScheduler.isTrainingDay(7, setOf(7)))
        assertFalse(TrainingScheduler.isTrainingDay(7, setOf(1, 2, 3, 4, 5, 6, 7)))
        assertTrue(TrainingScheduler.isTrainingDay(1, setOf(1)))
    }
}
