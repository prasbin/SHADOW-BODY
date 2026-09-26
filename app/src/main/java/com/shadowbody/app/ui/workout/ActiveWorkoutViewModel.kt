package com.shadowbody.app.ui.workout

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.SessionDetail
import com.shadowbody.app.data.local.SessionSet
import com.shadowbody.app.data.repository.SessionRepository
import com.shadowbody.app.domain.workout.RestTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActiveWorkoutViewModel(
    private val sessions: SessionRepository,
    val sessionId: Long,
) : ViewModel() {

    private val _detail = MutableStateFlow<SessionDetail?>(null)
    val detail: StateFlow<SessionDetail?> = _detail.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    val timer = RestTimer(viewModelScope)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { _detail.value = sessions.detail(sessionId) }
    }

    /** Persists one set log; caller validates first via WorkoutValidator. */
    fun saveSet(set: SessionSet, reps: Int?, durationSec: Int?, weightKg: Double?, completed: Boolean) {
        viewModelScope.launch {
            sessions.recordSet(set, reps, durationSec, weightKg, completed)
            refresh()
        }
    }

    fun toggleExercise(sessionExerciseId: Long, completed: Boolean) {
        viewModelScope.launch {
            sessions.setExerciseCompleted(sessionExerciseId, completed)
            refresh()
        }
    }

    fun finish() {
        viewModelScope.launch {
            sessions.finish(sessionId)
            _finished.value = true
        }
    }

    fun abandon(onGone: () -> Unit) {
        viewModelScope.launch {
            sessions.abandon(sessionId)
            onGone()
        }
    }

    class Factory(
        private val app: Application,
        private val sessionId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repo = (app as ShadowBodyApp).sessionRepository
            return ActiveWorkoutViewModel(repo, sessionId) as T
        }
    }
}
