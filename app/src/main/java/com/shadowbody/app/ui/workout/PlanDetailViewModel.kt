package com.shadowbody.app.ui.workout

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.PlanDetail
import com.shadowbody.app.data.repository.PlanRepository
import com.shadowbody.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanDetailViewModel(
    private val plans: PlanRepository,
    private val sessions: SessionRepository,
    val planId: Long,
) : ViewModel() {

    val detail: StateFlow<PlanDetail?> = plans.detail(planId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _startedSession = MutableStateFlow<Long?>(null)
    val startedSession: StateFlow<Long?> = _startedSession

    fun startSession() {
        viewModelScope.launch {
            _startedSession.value = sessions.start(planId)
        }
    }

    fun deletePlan(onGone: () -> Unit) {
        viewModelScope.launch {
            plans.deletePlan(planId)
            onGone()
        }
    }

    class Factory(
        private val app: Application,
        private val planId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val shadow = app as ShadowBodyApp
            return PlanDetailViewModel(shadow.planRepository, shadow.sessionRepository, planId) as T
        }
    }
}
