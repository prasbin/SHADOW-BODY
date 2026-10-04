package com.shadowbody.app.domain.notification

import com.shadowbody.app.domain.coach.CoachProfile

object NotificationEngine {

    fun generate(profile: CoachProfile): NotificationState {
        val notifications = mutableListOf<NotificationContent>()

        if (profile.isSaturday()) {
            notifications.add(
                NotificationContent(
                    id = "saturday_recovery",
                    title = "RECOVERY DAY",
                    body = "Today is Saturday recovery. Focus on rest and hydration.",
                    category = "RECOVERY",
                    priority = 90,
                )
            )
        }

        if (profile.hasReadinessToday && profile.fatigue >= 4) {
            notifications.add(
                NotificationContent(
                    id = "fatigue_alert",
                    title = "Fatigue Alert",
                    body = "High fatigue reported. Consider a lighter session.",
                    category = "READINESS",
                    priority = 85,
                )
            )
        }

        if (profile.isTrainingDay() && !profile.isSaturday() && !profile.todayWorkoutCompleted) {
            notifications.add(
                NotificationContent(
                    id = "workout_reminder",
                    title = "TRAIN",
                    body = "Your scheduled training session is ready.",
                    category = "WORKOUT",
                    priority = 80,
                )
            )
        }

        if (profile.hydrationGoalMl > 0 && profile.hydrationMl < profile.hydrationGoalMl / 2) {
            notifications.add(
                NotificationContent(
                    id = "hydration_reminder",
                    title = "DRINK WATER",
                    body = "Hydration is below half of your daily goal.",
                    category = "NUTRITION",
                    priority = 70,
                )
            )
        }

        if (profile.morningStatus == "NOT_STARTED") {
            notifications.add(
                NotificationContent(
                    id = "morning_reminder",
                    title = "MORNING ROUTINE",
                    body = "Complete your morning activation routine.",
                    category = "MORNING",
                    priority = 60,
                )
            )
        }

        if (profile.groomingStatus == "NOT_STARTED") {
            notifications.add(
                NotificationContent(
                    id = "grooming_reminder",
                    title = "GROOMING",
                    body = "Complete your daily grooming routine.",
                    category = "GROOMING",
                    priority = 50,
                )
            )
        }

        val sorted = notifications.sortedByDescending { it.priority }

        return NotificationState(
            notifications = sorted,
            topNotification = sorted.firstOrNull(),
        )
    }

    private fun CoachProfile.isTrainingDay(): Boolean {
        if (trainingDays.isEmpty()) return true
        return dayOfWeek in trainingDays
    }

    private fun CoachProfile.isSaturday(): Boolean {
        return dayOfWeek == 7
    }
}
