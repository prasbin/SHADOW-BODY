package com.shadowbody.app.ui.workout

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.WorkoutPlan
import com.shadowbody.app.data.local.WorkoutSession
import com.shadowbody.app.data.repository.ExerciseRepository
import com.shadowbody.app.data.repository.PlanRepository
import com.shadowbody.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutListViewModel(
    plans: PlanRepository,
    sessions: SessionRepository,
    private val exercises: ExerciseRepository,
) : ViewModel() {

    private val _seeded = MutableStateFlow<Int?>(null)
    val seededCount: StateFlow<Int?> = _seeded.asStateFlow()

    val planList: StateFlow<List<WorkoutPlan>> = plans.plans()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentSessions: StateFlow<List<WorkoutSession>> = sessions.recent()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val completedCount: StateFlow<Int> = sessions.completedCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        // Fresh installs seed here (duplicate-safe); upgrades seed via migration.
        viewModelScope.launch { _seeded.value = exercises.ensureSeeded() }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val shadow = app as ShadowBodyApp
            return WorkoutListViewModel(
                shadow.planRepository,
                shadow.sessionRepository,
                shadow.exerciseRepository,
            ) as T
        }
    }
}
