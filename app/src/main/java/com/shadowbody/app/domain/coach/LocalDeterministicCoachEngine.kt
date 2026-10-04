package com.shadowbody.app.domain.coach

class LocalDeterministicCoachEngine : CoachEngine {

    override fun generate(profile: CoachProfile): CoachSummary {
        val recommendations = mutableListOf<CoachRecommendation>()

        if (!profile.hasProfile) {
            recommendations.add(
                CoachRecommendation(
                    id = "setup_profile",
                    title = "Complete your profile",
                    reason = "Your fitness level, goals, and schedule are needed for personalized recommendations.",
                    category = "PROFILE",
                    priority = 100,
                    actionRoute = "profile",
                )
            )
        }

        if (profile.isSaturday()) {
            recommendations.add(
                CoachRecommendation(
                    id = "saturday_recovery",
                    title = "RECOVER",
                    reason = "Today is Saturday recovery. Keep training paused and focus on recovery.",
                    category = "RECOVERY",
                    priority = 95,
                )
            )
        }

        if (profile.hasReadinessToday && profile.fatigue >= 4) {
            recommendations.add(
                CoachRecommendation(
                    id = "high_fatigue",
                    title = "Consider a lighter session today",
                    reason = "Your reported fatigue is elevated (${profile.fatigue}/5). A conservative approach is recommended.",
                    category = "READINESS",
                    priority = 90,
                    actionRoute = "adaptive",
                )
            )
        }

        if (profile.hasReadinessToday && profile.soreness >= 4) {
            recommendations.add(
                CoachRecommendation(
                    id = "high_soreness",
                    title = "Consider recovery-focused activity",
                    reason = "Your reported soreness is elevated (${profile.soreness}/5). Avoid intense training.",
                    category = "READINESS",
                    priority = 85,
                    actionRoute = "adaptive",
                )
            )
        }

        if (profile.isTrainingDay() && !profile.isSaturday() && !profile.todayWorkoutCompleted && !profile.todayWorkoutInProgress) {
            recommendations.add(
                CoachRecommendation(
                    id = "workout_today",
                    title = "TRAIN",
                    reason = "Your scheduled training session is ready.",
                    category = "WORKOUT",
                    priority = 80,
                    actionRoute = "workout",
                )
            )
        }

        if (profile.todayWorkoutCompleted) {
            recommendations.add(
                CoachRecommendation(
                    id = "workout_done",
                    title = "Workout completed today",
                    reason = "You have already completed a workout today. Recovery is the priority.",
                    category = "WORKOUT",
                    priority = 10,
                )
            )
        }

        if (profile.hydrationGoalMl > 0 && profile.hydrationMl < profile.hydrationGoalMl / 2) {
            recommendations.add(
                CoachRecommendation(
                    id = "hydration_low",
                    title = "DRINK WATER",
                    reason = "Current hydration (${profile.hydrationMl}ml) is below half of your daily goal (${profile.hydrationGoalMl}ml).",
                    category = "NUTRITION",
                    priority = 70,
                    actionRoute = "nutrition",
                )
            )
        }

        if (profile.morningStatus == "NOT_STARTED") {
            recommendations.add(
                CoachRecommendation(
                    id = "morning_pending",
                    title = "COMPLETE MORNING ROUTINE",
                    reason = "Your morning activation routine has not been started today.",
                    category = "MORNING",
                    priority = 60,
                    actionRoute = "morning",
                )
            )
        }

        if (profile.groomingStatus == "NOT_STARTED") {
            recommendations.add(
                CoachRecommendation(
                    id = "grooming_pending",
                    title = "CHECK GROOMING",
                    reason = "Your daily grooming routine has not been started today.",
                    category = "GROOMING",
                    priority = 50,
                    actionRoute = "grooming",
                )
            )
        }

        if (profile.wardrobeItemCount > 0) {
            recommendations.add(
                CoachRecommendation(
                    id = "outfit_suggestion",
                    title = "PREPARE OUTFIT",
                    reason = "You have ${profile.wardrobeItemCount} wardrobe items available for outfit suggestions.",
                    category = "WARDROBE",
                    priority = 30,
                    actionRoute = "outfit_generator",
                )
            )
        }

        if (profile.recentMissedWorkouts > 0) {
            recommendations.add(
                CoachRecommendation(
                    id = "consistency",
                    title = "Maintain consistency",
                    reason = "You have ${profile.recentMissedWorkouts} recent missed workout(s). Consistency drives progress.",
                    category = "WORKOUT",
                    priority = 75,
                    actionRoute = "workout",
                )
            )
        }

        if (profile.currentStreak > 0) {
            recommendations.add(
                CoachRecommendation(
                    id = "streak",
                    title = "Keep your streak alive",
                    reason = "Current streak: ${profile.currentStreak} day(s). Today's activities count toward it.",
                    category = "PROGRESSION",
                    priority = 20,
                    actionRoute = "progression",
                )
            )
        }

        val sorted = recommendations.sortedByDescending { it.priority }

        return CoachSummary(
            recommendations = sorted,
            topRecommendation = sorted.firstOrNull(),
            generatedAt = System.currentTimeMillis(),
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
