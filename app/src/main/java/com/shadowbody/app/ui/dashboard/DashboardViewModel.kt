package com.shadowbody.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.repository.ProfileRepository
import com.shadowbody.app.data.repository.ProgressionRepository
import com.shadowbody.app.data.repository.GroomingRepository
import com.shadowbody.app.data.repository.MorningActivationRepository
import com.shadowbody.app.data.repository.NutritionRepository
import com.shadowbody.app.data.repository.WardrobeRepository
import com.shadowbody.app.data.repository.ReadinessRepository
import com.shadowbody.app.data.repository.AdaptationRepository
import com.shadowbody.app.data.repository.PlanRepository
import com.shadowbody.app.domain.grooming.GroomingDayKey
import com.shadowbody.app.domain.nutrition.NutritionDayKey
import com.shadowbody.app.domain.schedule.TrainingScheduler
import com.shadowbody.app.domain.schedule.TrainingOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardUiState(
    val hunterName: String = "PLAYER",
    val level: Int = 1,
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val profileConfigured: Boolean = false,
    val morningStatus: String = "NOT_STARTED",
    val groomingStatus: String = "NOT_STARTED",
    val hydrationMl: Int = 0,
    val hydrationGoalMl: Int = 0,
    val wardrobeItemCount: Int = 0,
    val hasReadinessToday: Boolean = false,
    val fatigue: Int = 0,
    val soreness: Int = 0,
    val recentMissedWorkouts: Int = 0,
    val isTrainingDay: Boolean = false,
    val todayPlanId: Long? = null,
    val todayPlanName: String? = null,
    val todayEstimatedMinutes: Int = 0,
    val restDayInfo: String? = null,
)

class DashboardViewModel(
    private val app: ShadowBodyApp,
) : ViewModel() {

    private val orchestrator: TrainingOrchestrator = app.trainingOrchestrator

    val uiState: StateFlow<DashboardUiState> = combine(
        app.profileRepository.profile,
        app.progressionRepository.observeTotalXp(),
        app.progressionRepository.observeStreak(),
        app.morningActivationRepository.observeDay(null, GroomingDayKey.today()),
        app.groomingRepository.observeDay(0, GroomingDayKey.today()),
        app.nutritionRepository.getDailySummary(NutritionDayKey.today()),
        app.wardrobeRepository.enabledItems,
        app.readinessRepository.latest(),
        app.adaptationRepository.observeMissed(7),
        app.planRepository.plans(),
    ) { values ->
        val profile = values[0] as? com.shadowbody.app.data.local.UserProfile
        val totalXp = values[1] as? Int ?: 0
        @Suppress("UNCHECKED_CAST")
        val streak = values[2] as? com.shadowbody.app.data.local.Streak
        @Suppress("UNCHECKED_CAST")
        val morningState = values[3] as? com.shadowbody.app.domain.morning.MorningDayState
        @Suppress("UNCHECKED_CAST")
        val groomingState = values[4] as? com.shadowbody.app.data.repository.GroomingDayState
        @Suppress("UNCHECKED_CAST")
        val nutrition = values[5] as? com.shadowbody.app.data.repository.DailyNutritionSummary
        @Suppress("UNCHECKED_CAST")
        val wardrobeItems = values[6] as? List<com.shadowbody.app.data.local.WardrobeItem>
        val readiness = values[7] as? com.shadowbody.app.data.local.ReadinessReport
        @Suppress("UNCHECKED_CAST")
        val missedWorkouts = values[8] as? List<com.shadowbody.app.data.local.MissedWorkout>
        @Suppress("UNCHECKED_CAST")
        val plans = values[9] as? List<com.shadowbody.app.data.local.WorkoutPlan>

        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK).let {
            if (it == Calendar.SUNDAY) 7 else it - 1
        }

        val trainingDays = profile?.trainingDays ?: emptySet()
        val isTrainingDay = TrainingScheduler.isTrainingDay(dayOfWeek, trainingDays)

        val activePlan = plans?.firstOrNull { it.isActive }
        val todaySchedule = TrainingScheduler.determineToday(
            isTrainingDay = isTrainingDay,
            activePlanId = activePlan?.id,
            activePlanName = activePlan?.name,
            estimatedMinutes = activePlan?.targetDurationMin ?: 0,
            nextTrainingDay = null,
        )

        val level = if (totalXp > 0) (totalXp / 100) + 1 else 1

        DashboardUiState(
            level = level,
            totalXp = totalXp,
            currentStreak = streak?.currentStreak ?: 0,
            profileConfigured = profile != null,
            morningStatus = morningState?.status?.toString() ?: "NOT_STARTED",
            groomingStatus = groomingState?.status ?: "NOT_STARTED",
            hydrationMl = nutrition?.totalHydrationMl ?: 0,
            hydrationGoalMl = nutrition?.goal?.hydrationMlTarget ?: 0,
            wardrobeItemCount = wardrobeItems?.size ?: 0,
            hasReadinessToday = readiness != null,
            fatigue = readiness?.fatigue ?: 0,
            soreness = readiness?.soreness ?: 0,
            recentMissedWorkouts = missedWorkouts?.size ?: 0,
            isTrainingDay = todaySchedule.isTrainingDay,
            todayPlanId = todaySchedule.scheduledWorkout?.planId,
            todayPlanName = todaySchedule.scheduledWorkout?.planName,
            todayEstimatedMinutes = todaySchedule.scheduledWorkout?.estimatedMinutes ?: 0,
            restDayInfo = todaySchedule.restDayInfo,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DashboardUiState())

    init {
        viewModelScope.launch {
            orchestrator.initializeIfNeeded()
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(app as ShadowBodyApp) as T
        }
    }
}
