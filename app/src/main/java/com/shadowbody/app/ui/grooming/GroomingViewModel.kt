package com.shadowbody.app.ui.grooming

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.repository.GroomingRepository
import com.shadowbody.app.data.repository.GroomingDayState
import com.shadowbody.app.data.repository.GroomingStartResult
import com.shadowbody.app.domain.grooming.GroomingDayKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GroomingViewModel(
    private val routines: GroomingRepository,
) : ViewModel() {
    private val dayKey = GroomingDayKey.today()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _startedLogId = MutableStateFlow<Long?>(null)

    val startedLogId: StateFlow<Long?> = _startedLogId.asStateFlow()

    val routineList: StateFlow<List<com.shadowbody.app.data.local.GroomingRoutine>> = routines.activeRoutines
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val history: StateFlow<List<com.shadowbody.app.data.local.GroomingLog>> = routines.observeHistory(HISTORY_LIMIT)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dayState: StateFlow<GroomingDayState> = routineList
        .flatMapLatest { list ->
            val target = list.firstOrNull { it.isActive } ?: list.firstOrNull()
            if (target == null) {
                kotlinx.coroutines.flow.flowOf(GroomingDayState(dayKey = dayKey, routineId = null, status = "NOT_STARTED"))
            } else {
                routines.observeDay(target.id, dayKey)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GroomingDayState(dayKey = dayKey, routineId = null, status = "NOT_STARTED"))

    val uiState: StateFlow<GroomingUiState> =
        combine(routineList, history, dayState, _message) { list, logs, state, message ->
            GroomingUiState(
                routines = list,
                history = logs,
                dayKey = dayKey,
                dayState = state,
                message = message,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, GroomingUiState(dayKey = dayKey))

    init {
        viewModelScope.launch { routines.ensureSeeded() }
    }

    fun startToday() {
        val target = routineList.value.firstOrNull { it.isActive }
            ?: routineList.value.firstOrNull()
        if (target == null) {
            _message.value = "No routine available."
            return
        }
        startRoutine(target.id)
    }

    fun startRoutine(routineId: Long) {
        viewModelScope.launch {
            when (val result = routines.startRun(routineId, dayKey)) {
                is GroomingStartResult.Run -> _startedLogId.value = result.logId
                is GroomingStartResult.AlreadyCompleted ->
                    _message.value = "Today's grooming is already complete."
                is GroomingStartResult.NoEnabledSteps ->
                    _message.value = "That routine has no enabled steps."
                is GroomingStartResult.RoutineMissing ->
                    _message.value = "That routine no longer exists."
            }
        }
    }

    fun consumeStartedLogId() {
        _startedLogId.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val shadow = app as ShadowBodyApp
            return GroomingViewModel(shadow.groomingRepository) as T
        }
    }

    companion object {
        const val HISTORY_LIMIT = 20
    }
}

data class GroomingUiState(
    val routines: List<com.shadowbody.app.data.local.GroomingRoutine> = emptyList(),
    val history: List<com.shadowbody.app.data.local.GroomingLog> = emptyList(),
    val dayKey: String = "",
    val dayState: GroomingDayState = GroomingDayState(dayKey = "", routineId = null, status = "NOT_STARTED"),
    val message: String? = null,
) {
    val hasRoutines: Boolean get() = routines.isNotEmpty()
}
