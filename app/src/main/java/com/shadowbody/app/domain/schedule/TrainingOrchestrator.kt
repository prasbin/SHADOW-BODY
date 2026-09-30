package com.shadowbody.app.domain.schedule

import com.shadowbody.app.data.local.UserProfile
import com.shadowbody.app.data.local.WorkoutPlan
import com.shadowbody.app.data.repository.AdaptiveWorkoutPlanner
import com.shadowbody.app.data.repository.PlanRepository
import com.shadowbody.app.data.repository.ProfileRepository
import com.shadowbody.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar

class TrainingOrchestrator(
    private val profileRepository: ProfileRepository,
    private val planRepository: PlanRepository,
    private val sessionRepository: SessionRepository,
    private val adaptivePlanner: AdaptiveWorkoutPlanner,
) {

    suspend fun initializeIfNeeded(): Boolean {
        val profile = profileRepository.get() ?: return false
        if (!isProfileSufficient(profile)) return false

        val plans = planRepository.plans().first()
        val hasActivePlan = plans.any { it.isActive }
        if (hasActivePlan) return false

        val recommendation = adaptivePlanner.generate() ?: return false
        val planId = adaptivePlanner.adopt(recommendation.recommendation.id) ?: return false

        return planId > 0
    }

    suspend fun getTodayWorkout(): TodayWorkout {
        val profile = profileRepository.get()
        if (profile == null || !isProfileSufficient(profile)) {
            return TodayWorkout(
                isTrainingDay = false,
                planId = null,
                planName = null,
                estimatedMinutes = 0,
                isRestDay = false,
            )
        }

        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK).let {
            if (it == Calendar.SUNDAY) 7 else it - 1
        }

        if (!TrainingScheduler.isTrainingDay(dayOfWeek, profile.trainingDays)) {
            return TodayWorkout(
                isTrainingDay = false,
                planId = null,
                planName = null,
                estimatedMinutes = 0,
                isRestDay = true,
                restDayInfo = "Rest day. Next training: ${getNextTrainingDayName(profile.trainingDays, dayOfWeek)}",
            )
        }

        val plans = planRepository.plans().first()
        var activePlan = plans.firstOrNull { it.isActive }

        if (activePlan == null) {
            initializeIfNeeded()
            activePlan = planRepository.plans().first().firstOrNull { it.isActive }
        }

        if (activePlan == null) {
            return TodayWorkout(
                isTrainingDay = true,
                planId = null,
                planName = null,
                estimatedMinutes = 0,
                isRestDay = false,
            )
        }

        return TodayWorkout(
            isTrainingDay = true,
            planId = activePlan.id,
            planName = activePlan.name,
            estimatedMinutes = activePlan.targetDurationMin ?: 0,
            isRestDay = false,
        )
    }

    suspend fun onWorkoutCompleted(sessionId: Long) {
        sessionRepository.finish(sessionId)
        adaptivePlanner.applyLatestEvidence()

        val profile = profileRepository.get() ?: return
        if (!isProfileSufficient(profile)) return

        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK).let {
            if (it == Calendar.SUNDAY) 7 else it - 1
        }

        if (!TrainingScheduler.isTrainingDay(dayOfWeek, profile.trainingDays)) return

        val plans = planRepository.plans().first()
        val hasActivePlan = plans.any { it.isActive }

        if (!hasActivePlan) {
            adaptivePlanner.generate()
        }
    }

    private fun isProfileSufficient(profile: UserProfile): Boolean {
        return profile.trainingDays.isNotEmpty() &&
            profile.sessionMinutes > 0 &&
            profile.goals.isNotEmpty()
    }

    private fun getNextTrainingDayName(trainingDays: Set<Int>, currentDay: Int): String {
        if (trainingDays.isEmpty()) return "soon"
        val sortedDays = trainingDays.sorted()
        val nextDay = sortedDays.firstOrNull { it > currentDay } ?: sortedDays.first()
        return when (nextDay) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "soon"
        }
    }
}

data class TodayWorkout(
    val isTrainingDay: Boolean,
    val planId: Long?,
    val planName: String?,
    val estimatedMinutes: Int,
    val isRestDay: Boolean,
    val restDayInfo: String? = null,
)
