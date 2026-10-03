package com.shadowbody.app.domain.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class WakeUpSchedulerTest {

    private fun timeAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun sundayBefore6AMSchedulesSunday6AM() {
        val sunday = timeAt(2026, Calendar.OCTOBER, 4, 5, 0)
        val result = WakeUpScheduler.calculateNextWakeUp(sunday)

        assertFalse(result.isSaturday)
        assertNotNull(result.nextWakeUpTime)
        assertEquals("Sunday", result.nextDayName)

        val cal = Calendar.getInstance().apply { timeInMillis = result.nextWakeUpTime!! }
        assertEquals(6, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun mondaySchedulesMonday() {
        val monday = timeAt(2026, Calendar.OCTOBER, 5, 5, 0)
        val result = WakeUpScheduler.calculateNextWakeUp(monday)

        assertFalse(result.isSaturday)
        assertEquals("Monday", result.nextDayName)
    }

    @Test
    fun tuesdaySchedulesTuesday() {
        val tuesday = timeAt(2026, Calendar.OCTOBER, 6, 5, 0)
        val result = WakeUpScheduler.calculateNextWakeUp(tuesday)

        assertFalse(result.isSaturday)
        assertEquals("Tuesday", result.nextDayName)
    }

    @Test
    fun wednesdaySchedulesWednesday() {
        val wednesday = timeAt(2026, Calendar.OCTOBER, 7, 5, 0)
        val result = WakeUpScheduler.calculateNextWakeUp(wednesday)

        assertFalse(result.isSaturday)
        assertEquals("Wednesday", result.nextDayName)
    }

    @Test
    fun thursdaySchedulesThursday() {
        val thursday = timeAt(2026, Calendar.OCTOBER, 8, 5, 0)
        val result = WakeUpScheduler.calculateNextWakeUp(thursday)

        assertFalse(result.isSaturday)
        assertEquals("Thursday", result.nextDayName)
    }

    @Test
    fun fridaySchedulesFriday() {
        val friday = timeAt(2026, Calendar.OCTOBER, 9, 5, 0)
        val result = WakeUpScheduler.calculateNextWakeUp(friday)

        assertFalse(result.isSaturday)
        assertEquals("Friday", result.nextDayName)
    }

    @Test
    fun saturdayDoesNotSchedule() {
        val saturday = timeAt(2026, Calendar.OCTOBER, 10, 5, 0)
        val result = WakeUpScheduler.calculateNextWakeUp(saturday)

        assertTrue(result.isSaturday)
        assertNull(result.nextWakeUpTime)
        assertEquals("Sunday", result.nextDayName)
    }

    @Test
    fun after6AMSchedulesNextDay() {
        val mondayAfter6 = timeAt(2026, Calendar.OCTOBER, 5, 7, 0)
        val result = WakeUpScheduler.calculateNextWakeUp(mondayAfter6)

        assertFalse(result.isSaturday)
        assertEquals("Tuesday", result.nextDayName)
    }

    @Test
    fun exactly6AMSchedulesNextDay() {
        val mondayAt6 = timeAt(2026, Calendar.OCTOBER, 5, 6, 0)
        val result = WakeUpScheduler.calculateNextWakeUp(mondayAt6)

        assertFalse(result.isSaturday)
        assertEquals("Tuesday", result.nextDayName)
    }

    @Test
    fun fridayAfter6AMSchedulesSundaySkippingSaturday() {
        val fridayAfter6 = timeAt(2026, Calendar.OCTOBER, 9, 7, 0)
        val result = WakeUpScheduler.calculateNextWakeUp(fridayAfter6)

        assertFalse(result.isSaturday)
        assertEquals("Sunday", result.nextDayName)
    }

    @Test
    fun isWakeUpDayReturnsFalseForSaturday() {
        assertFalse(WakeUpScheduler.isWakeUpDay(Calendar.SATURDAY))
    }

    @Test
    fun isWakeUpDayReturnsTrueForAllOtherDays() {
        assertTrue(WakeUpScheduler.isWakeUpDay(Calendar.SUNDAY))
        assertTrue(WakeUpScheduler.isWakeUpDay(Calendar.MONDAY))
        assertTrue(WakeUpScheduler.isWakeUpDay(Calendar.TUESDAY))
        assertTrue(WakeUpScheduler.isWakeUpDay(Calendar.WEDNESDAY))
        assertTrue(WakeUpScheduler.isWakeUpDay(Calendar.THURSDAY))
        assertTrue(WakeUpScheduler.isWakeUpDay(Calendar.FRIDAY))
    }
}
