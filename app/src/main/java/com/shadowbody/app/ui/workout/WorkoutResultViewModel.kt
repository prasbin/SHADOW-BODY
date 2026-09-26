package com.shadowbody.app.ui.workout

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.SessionDetail
import com.shadowbody.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Loads a finished session for the result screen. */
class WorkoutResultViewModel(
    private val sessions: SessionRepository,
    val sessionId: Long,
) : ViewModel() {

    private val _detail = MutableStateFlow<SessionDetail?>(null)
    val detail: StateFlow<SessionDetail?> = _detail.asStateFlow()

    init {
        viewModelScope.launch { _detail.value = sessions.detail(sessionId) }
    }

    class Factory(
        private val app: Application,
        private val sessionId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repo = (app as ShadowBodyApp).sessionRepository
            return WorkoutResultViewModel(repo, sessionId) as T
        }
    }
}
