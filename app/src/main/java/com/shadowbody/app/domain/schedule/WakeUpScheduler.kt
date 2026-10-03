package com.shadowbody.app.domain.schedule

import java.util.Calendar

data class WakeUpSchedule(
    val nextWakeUpTime: Long?,
    val isSaturday: Boolean,
    val nextDayName: String,
)

object WakeUpScheduler {

    private const val WAKE_UP_HOUR = 6
    private const val WAKE_UP_MINUTE = 0

    fun calculateNextWakeUp(now: Long = System.currentTimeMillis()): WakeUpSchedule {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        if (dayOfWeek == Calendar.SATURDAY) {
            return WakeUpSchedule(
                nextWakeUpTime = null,
                isSaturday = true,
                nextDayName = "Sunday",
            )
        }

        val nextCal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, WAKE_UP_HOUR)
            set(Calendar.MINUTE, WAKE_UP_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (nextCal.timeInMillis <= now) {
            nextCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        if (nextCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            nextCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val dayName = when (nextCal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            else -> "Saturday"
        }

        return WakeUpSchedule(
            nextWakeUpTime = nextCal.timeInMillis,
            isSaturday = false,
            nextDayName = dayName,
        )
    }

    fun isWakeUpDay(dayOfWeek: Int): Boolean {
        return dayOfWeek != Calendar.SATURDAY
    }
}
