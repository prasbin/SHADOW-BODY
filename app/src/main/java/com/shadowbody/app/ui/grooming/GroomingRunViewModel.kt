package com.shadowbody.app.ui.grooming

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.GroomingLog
import com.shadowbody.app.data.repository.GroomingRepository
import com.shadowbody.app.data.repository.GroomingRunDetail
import com.shadowbody.app.data.repository.GroomingStepResult
import com.shadowbody.app.data.repository.GroomingFinishResult
import androidx.lifecycle.createSavedStateHandle
import com.shadowbody.app.domain.grooming.GroomingDayKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroomingRunViewModel(
    private val routines: GroomingRepository,
    private val savedState: SavedStateHandle,
    val logId: Long,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    val detail: StateFlow<GroomingRunDetail?> = routines.observeRun(logId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        restoreTimer()
    }

    fun onStepShown(position: Int, durationSec: Int?) {
    }

    fun record(position: Int, outcome: String) {
        val step = detail.value?.steps?.firstOrNull { it.position == position }
        if (step == null) {
            _message.value = "That step is no longer available."
            return
        }
        viewModelScope.launch {
            when (val result = routines.recordStep(
                logId = logId,
                step = com.shadowbody.app.data.local.GroomingRoutineStep(
                    id = step.stepId ?: 0,
                    routineId = 0,
                    title = step.title,
                    instructions = step.instructions,
                    category = step.category,
                    targetDurationSec = step.targetDurationSec,
                    position = step.position,
                    isEnabled = true,
                    isSeeded = false,
                ),
                outcome = outcome,
                elapsedSec = step.elapsedSec,
            )) {
                is GroomingStepResult.Recorded ->
                    _message.value = if (outcome == "COMPLETED") "Recorded." else "Skipped."
                is GroomingStepResult.RunClosed ->
                    _message.value = "This run is already closed."
                is GroomingStepResult.NotFound ->
                    _message.value = "That run no longer exists."
                else -> {
                    _message.value = "Unexpected result: $result"
                }
            }
        }
    }

    fun finish() {
        viewModelScope.launch {
            when (val result = routines.finish(logId)) {
                is GroomingFinishResult.Closed -> _finished.value = true
                is GroomingFinishResult.NotReady ->
                    _message.value = "Deal with every step first."
                is GroomingFinishResult.AlreadyCompleted ->
                    _message.value = "Today's grooming is already complete."
                is GroomingFinishResult.NotFound ->
                    _message.value = "That run no longer exists."
                else -> {
                    _message.value = "Unexpected result: $result"
                }
            }
        }
    }

    fun abandon() {
        viewModelScope.launch {
            routines.abandon(logId)
            _finished.value = true
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun restoreTimer() {
    }

    class Factory(
        private val app: Application,
        private val logId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras,
        ): T {
            val shadow = app as ShadowBodyApp
            return GroomingRunViewModel(
                shadow.groomingRepository,
                extras.createSavedStateHandle(),
                logId,
            ) as T
        }
    }
}
