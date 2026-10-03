package com.shadowbody.app.domain.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SaturdayRecoveryTest {

    private fun timeAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun saturdayIsNeverATrainingDay() {
        assertFalse(TrainingScheduler.isTrainingDay(Calendar.SATURDAY, setOf(1, 2, 3, 4, 5, 6, 7)))
        assertFalse(TrainingScheduler.isTrainingDay(Calendar.SATURDAY, setOf(Calendar.SATURDAY)))
    }

    @Test
    fun sundayThroughFridayAreTrainingDays() {
        assertTrue(TrainingScheduler.isTrainingDay(Calendar.SUNDAY, setOf(1, 2, 3, 4, 5, 6, 7)))
        assertTrue(TrainingScheduler.isTrainingDay(Calendar.MONDAY, setOf(1, 2, 3, 4, 5, 6, 7)))
        assertTrue(TrainingScheduler.isTrainingDay(Calendar.TUESDAY, setOf(1, 2, 3, 4, 5, 6, 7)))
        assertTrue(TrainingScheduler.isTrainingDay(Calendar.WEDNESDAY, setOf(1, 2, 3, 4, 5, 6, 7)))
        assertTrue(TrainingScheduler.isTrainingDay(Calendar.THURSDAY, setOf(1, 2, 3, 4, 5, 6, 7)))
        assertTrue(TrainingScheduler.isTrainingDay(Calendar.FRIDAY, setOf(1, 2, 3, 4, 5, 6, 7)))
    }

    @Test
    fun saturdayDoesNotGetWakeUp() {
        val saturday = timeAt(2026, Calendar.OCTOBER, 10, 5, 0)
        val result = WakeUpScheduler.calculateNextWakeUp(saturday)

        assertTrue(result.isSaturday)
        assertNull(result.nextWakeUpTime)
    }

    @Test
    fun fridayAfter6AMSkipsSaturdayForWakeUp() {
        val fridayAfter6 = timeAt(2026, Calendar.OCTOBER, 9, 7, 0)
        val result = WakeUpScheduler.calculateNextWakeUp(fridayAfter6)

        assertFalse(result.isSaturday)
        assertEquals("Sunday", result.nextDayName)
    }

    @Test
    fun saturdayDetermineTodayReturnsRestDay() {
        val result = TrainingScheduler.determineToday(
            isTrainingDay = TrainingScheduler.isTrainingDay(Calendar.SATURDAY, setOf(1, 2, 3, 4, 5, 6, 7)),
            activePlanId = 1L,
            activePlanName = "Workout",
            estimatedMinutes = 30,
            nextTrainingDay = "Sunday",
        )

        assertFalse(result.isTrainingDay)
        assertNull(result.scheduledWorkout)
        assertTrue(result.restDayInfo?.contains("Rest day") == true)
    }

    @Test
    fun aggressionLevelDoesNotOverrideSaturdayRecovery() {
        val isTrainingDay = TrainingScheduler.isTrainingDay(Calendar.SATURDAY, setOf(1, 2, 3, 4, 5, 6, 7))

        assertFalse("Saturday must never be a training day regardless of aggression", isTrainingDay)
    }

    @Test
    fun saturdayToSundayTransitionWorks() {
        val saturday = timeAt(2026, Calendar.OCTOBER, 10, 5, 0)
        val saturdayResult = WakeUpScheduler.calculateNextWakeUp(saturday)

        assertTrue(saturdayResult.isSaturday)
        assertEquals("Sunday", saturdayResult.nextDayName)

        val sunday = timeAt(2026, Calendar.OCTOBER, 11, 5, 0)
        val sundayResult = WakeUpScheduler.calculateNextWakeUp(sunday)

        assertFalse(sundayResult.isSaturday)
        assertEquals("Sunday", sundayResult.nextDayName)
        assertTrue(sundayResult.nextWakeUpTime != null)
    }

    @Test
    fun fridayToSaturdayTransitionWorks() {
        val friday = timeAt(2026, Calendar.OCTOBER, 9, 5, 0)
        val fridayResult = WakeUpScheduler.calculateNextWakeUp(friday)

        assertFalse(fridayResult.isSaturday)
        assertEquals("Friday", fridayResult.nextDayName)

        val saturday = timeAt(2026, Calendar.OCTOBER, 10, 5, 0)
        val saturdayResult = WakeUpScheduler.calculateNextWakeUp(saturday)

        assertTrue(saturdayResult.isSaturday)
        assertNull(saturdayResult.nextWakeUpTime)
    }

    @Test
    fun saturdayWithEmptyTrainingDaysStillNotTrainingDay() {
        assertFalse(TrainingScheduler.isTrainingDay(Calendar.SATURDAY, emptySet()))
    }

    @Test
    fun saturdayIsDay7InCalendar() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 12, 0, 0)
        }
        assertEquals(Calendar.SATURDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertFalse(TrainingScheduler.isTrainingDay(cal.get(Calendar.DAY_OF_WEEK), setOf(1, 2, 3, 4, 5, 6, 7)))
    }
}
