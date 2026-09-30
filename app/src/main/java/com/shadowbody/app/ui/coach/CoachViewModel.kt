package com.shadowbody.app.ui.coach

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.domain.coach.CoachProfile
import com.shadowbody.app.domain.coach.CoachSummary
import com.shadowbody.app.domain.coach.LocalDeterministicCoachEngine
import com.shadowbody.app.domain.grooming.GroomingDayKey
import com.shadowbody.app.domain.nutrition.NutritionDayKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class CoachViewModel(
    private val app: ShadowBodyApp,
) : ViewModel() {

    private val engine = LocalDeterministicCoachEngine()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val summary: StateFlow<CoachSummary?> = combine(
        app.profileRepository.profile,
        app.readinessRepository.latest(),
        app.morningActivationRepository.observeDay(null, GroomingDayKey.today()),
        app.groomingRepository.observeDay(0, GroomingDayKey.today()),
        app.nutritionRepository.getDailySummary(NutritionDayKey.today()),
        app.progressionRepository.observeTotalXp(),
        app.progressionRepository.observeStreak(),
        app.wardrobeRepository.enabledItems,
        app.adaptationRepository.observeMissed(7),
    ) { values ->
        val profile = values[0] as? com.shadowbody.app.data.local.UserProfile
        val readiness = values[1] as? com.shadowbody.app.data.local.ReadinessReport
        @Suppress("UNCHECKED_CAST")
        val morningState = values[2] as? com.shadowbody.app.domain.morning.MorningDayState
        @Suppress("UNCHECKED_CAST")
        val groomingState = values[3] as? com.shadowbody.app.data.repository.GroomingDayState
        @Suppress("UNCHECKED_CAST")
        val nutrition = values[4] as? com.shadowbody.app.data.repository.DailyNutritionSummary
        val totalXp = values[5] as? Int ?: 0
        @Suppress("UNCHECKED_CAST")
        val streak = values[6] as? com.shadowbody.app.data.local.Streak
        @Suppress("UNCHECKED_CAST")
        val wardrobeItems = values[7] as? List<com.shadowbody.app.data.local.WardrobeItem>
        @Suppress("UNCHECKED_CAST")
        val missedWorkouts = values[8] as? List<com.shadowbody.app.data.local.MissedWorkout>

        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK).let {
            if (it == Calendar.SUNDAY) 7 else it - 1
        }

        val coachProfile = CoachProfile(
            hasProfile = profile != null,
            fitnessLevel = profile?.fitnessLevel?.name ?: "",
            trainingDays = profile?.trainingDays ?: emptySet(),
            todayWorkoutCompleted = false,
            todayWorkoutInProgress = false,
            hasReadinessToday = readiness != null,
            fatigue = readiness?.fatigue ?: 0,
            soreness = readiness?.soreness ?: 0,
            morningStatus = morningState?.status?.toString() ?: "NOT_STARTED",
            groomingStatus = groomingState?.status ?: "NOT_STARTED",
            hydrationMl = nutrition?.totalHydrationMl ?: 0,
            hydrationGoalMl = nutrition?.goal?.hydrationMlTarget ?: 0,
            totalXp = totalXp,
            currentStreak = streak?.currentStreak ?: 0,
            wardrobeItemCount = wardrobeItems?.size ?: 0,
            recentMissedWorkouts = missedWorkouts?.size ?: 0,
            dayOfWeek = dayOfWeek,
        )

        engine.generate(coachProfile)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun refresh() {
        viewModelScope.launch {
            _message.value = "Recommendations refreshed."
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val shadow = app as ShadowBodyApp
            return CoachViewModel(shadow) as T
        }
    }
}
