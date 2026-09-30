package com.shadowbody.app.domain.coach

data class CoachRecommendation(
    val id: String,
    val title: String,
    val reason: String,
    val category: String,
    val priority: Int,
    val actionRoute: String? = null,
)

data class CoachSummary(
    val recommendations: List<CoachRecommendation>,
    val topRecommendation: CoachRecommendation?,
    val generatedAt: Long,
)

interface CoachEngine {
    fun generate(profile: CoachProfile): CoachSummary
}

data class CoachProfile(
    val hasProfile: Boolean = false,
    val fitnessLevel: String = "",
    val trainingDays: Set<Int> = emptySet(),
    val todayWorkoutCompleted: Boolean = false,
    val todayWorkoutInProgress: Boolean = false,
    val hasReadinessToday: Boolean = false,
    val fatigue: Int = 0,
    val soreness: Int = 0,
    val morningStatus: String = "NOT_STARTED",
    val groomingStatus: String = "NOT_STARTED",
    val hydrationMl: Int = 0,
    val hydrationGoalMl: Int = 0,
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val wardrobeItemCount: Int = 0,
    val recentMissedWorkouts: Int = 0,
    val dayOfWeek: Int = 1,
)
