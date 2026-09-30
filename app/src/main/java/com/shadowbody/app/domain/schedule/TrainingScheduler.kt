package com.shadowbody.app.domain.schedule

data class ScheduledWorkout(
    val planId: Long,
    val planName: String,
    val estimatedMinutes: Int,
    val isRestDay: Boolean,
    val nextTrainingDay: String? = null,
)

data class TodaySchedule(
    val isTrainingDay: Boolean,
    val scheduledWorkout: ScheduledWorkout?,
    val restDayInfo: String? = null,
)

object TrainingScheduler {

    fun determineToday(
        isTrainingDay: Boolean,
        activePlanId: Long?,
        activePlanName: String?,
        estimatedMinutes: Int,
        nextTrainingDay: String?,
    ): TodaySchedule {
        if (!isTrainingDay) {
            return TodaySchedule(
                isTrainingDay = false,
                scheduledWorkout = null,
                restDayInfo = "Rest day. Next training: ${nextTrainingDay ?: "soon"}",
            )
        }

        if (activePlanId == null || activePlanName == null) {
            return TodaySchedule(
                isTrainingDay = true,
                scheduledWorkout = null,
                restDayInfo = null,
            )
        }

        return TodaySchedule(
            isTrainingDay = true,
            scheduledWorkout = ScheduledWorkout(
                planId = activePlanId,
                planName = activePlanName,
                estimatedMinutes = estimatedMinutes,
                isRestDay = false,
            ),
        )
    }

    fun isTrainingDay(todayDayOfWeek: Int, trainingDays: Set<Int>): Boolean {
        if (trainingDays.isEmpty()) return true
        return todayDayOfWeek in trainingDays
    }
}
