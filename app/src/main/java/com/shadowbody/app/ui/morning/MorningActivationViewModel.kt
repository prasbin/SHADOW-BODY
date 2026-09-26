package com.shadowbody.app.ui.morning

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.MorningRoutine
import com.shadowbody.app.data.local.MorningRoutineLog
import com.shadowbody.app.data.repository.MorningActivationRepository
import com.shadowbody.app.data.repository.MorningRoutineRepository
import com.shadowbody.app.data.repository.MorningFinishResult
import com.shadowbody.app.data.repository.MorningStartResult
import com.shadowbody.app.data.repository.MorningStepResult
import com.shadowbody.app.domain.model.MorningStepOutcome
import com.shadowbody.app.domain.morning.MorningDayKey
import com.shadowbody.app.domain.morning.MorningDayState
import com.shadowbody.app.domain.morning.MorningRunDetail
import com.shadowbody.app.domain.morning.MorningTimer
import com.shadowbody.app.domain.morning.MorningTimerPhase
import com.shadowbody.app.domain.morning.MorningTimerSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Home screen state. */
data class MorningActivationUiState(
    val routines: List<MorningRoutine> = emptyList(),
    val history: List<MorningRoutineLog> = emptyList(),
    val dayKey: String = "",
    val message: String? = null,
) {
    val hasRoutines: Boolean get() = routines.isNotEmpty()
}

/**
 * Phase 5 home: the routine list, today's state and the way in to a run.
 *
 * All rules live in the repositories; this only seeds the built-in routine,
 * surfaces today's resolved state and forwards intent.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MorningActivationViewModel(
    private val routines: MorningRoutineRepository,
    private val activation: MorningActivationRepository,
) : ViewModel() {
    private val dayKey = MorningDayKey.today()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _startedLogId = MutableStateFlow<Long?>(null)

    /**
     * The run to open. One-shot: it is cleared by [consumeStartedLogId] as soon
     * as it is acted on, so neither rotation nor coming back from the run can
     * navigate into it a second time.
     */
    val startedLogId: StateFlow<Long?> = _startedLogId.asStateFlow()

    val routineList: StateFlow<List<MorningRoutine>> = routines.observeRoutines()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val history: StateFlow<List<MorningRoutineLog>> = activation.observeHistory(HISTORY_LIMIT)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Today's state for the routine the home card acts on: the first active
     * routine, or the first routine of any kind so a user who deactivated
     * everything still sees honest state instead of an empty card.
     */
    val dayState: StateFlow<MorningDayState> = routineList
        .flatMapLatest { list ->
            val target = list.firstOrNull { it.isActive } ?: list.firstOrNull()
            if (target == null) {
                flowOf(MorningDayState(dayKey = dayKey))
            } else {
                activation.observeDay(target.id, dayKey)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            MorningDayState(dayKey = dayKey),
        )

    val uiState: StateFlow<MorningActivationUiState> =
        combine(routineList, history, _message) { list, logs, message ->
            MorningActivationUiState(
                routines = list,
                history = logs,
                dayKey = dayKey,
                message = message,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, MorningActivationUiState(dayKey = dayKey))

    init {
        // Fresh installs and upgrades converge here: the repository seeds
        // duplicate-safely, so this is safe on every launch.
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
            when (val result = activation.startRun(routineId, dayKey)) {
                is MorningStartResult.Run -> _startedLogId.value = result.logId
                is MorningStartResult.AlreadyCompleted ->
                    _message.value = "Today's routine is already complete."

                MorningStartResult.NoEnabledSteps ->
                    _message.value = "That routine has no enabled steps."

                MorningStartResult.RoutineMissing ->
                    _message.value = "That routine no longer exists."
            }
        }
    }

    /**
     * Marks [startedLogId] as handled. Called by the host the moment it
     * navigates, so the event fires exactly once and returning from the run
     * cannot push the user back into it.
     */
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
            return MorningActivationViewModel(
                shadow.morningRoutineRepository,
                shadow.morningActivationRepository,
            ) as T
        }
    }

    companion object {
        const val HISTORY_LIMIT = 20
    }
}

/**
 * Phase 5 run screen state holder.
 *
 * Owns the [MorningTimer] for the current step and records outcomes. The timer
 * snapshot is kept in [SavedStateHandle] — deadline included — so rotation or
 * process death can neither hand out free time nor lose a running countdown.
 */
class MorningRunViewModel(
    private val activation: MorningActivationRepository,
    private val savedState: SavedStateHandle,
    val logId: Long,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    val detail: StateFlow<MorningRunDetail?> = activation.observeRun(logId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val timer = MorningTimer(viewModelScope)

    /** Position the timer is currently armed for; -1 means unarmed. */
    private var activePosition: Int
        get() = savedState.get<Int>(KEY_POSITION) ?: -1
        set(value) {
            savedState[KEY_POSITION] = value
        }

    init {
        restoreTimer()
    }

    /**
     * Arms the countdown when a timed step becomes current. Idempotent per
     * position so recomposition cannot restart a running timer.
     */
    fun onStepShown(position: Int, durationSec: Int?) {
        if (position == activePosition) {
            persistTimer()
            return
        }
        activePosition = position
        if (durationSec != null && durationSec > 0) {
            timer.start(durationSec)
        } else {
            timer.reset()
        }
        persistTimer()
    }

    /** Records an explicit outcome for the step at [position]. */
    fun record(position: Int, outcome: MorningStepOutcome) {
        val step = detail.value?.steps?.firstOrNull { it.position == position }
        if (step == null) {
            _message.value = "That step is no longer available."
            return
        }
        val elapsedSec = step.targetDurationSec?.let { target ->
            (target - timer.state.value.remainingSec).coerceAtLeast(0)
        }
        viewModelScope.launch {
            when (val result = activation.recordStep(
                logId = logId,
                step = step,
                outcome = outcome,
                elapsedSec = elapsedSec,
            )) {
                is MorningStepResult.Recorded -> {
                    timer.reset()
                    activePosition = -1
                    persistTimer()
                    _message.value = if (outcome == MorningStepOutcome.COMPLETED) {
                        "Recorded."
                    } else {
                        "Skipped."
                    }
                }

                MorningStepResult.RunClosed ->
                    _message.value = "This run is already closed."

                MorningStepResult.NotFound ->
                    _message.value = "That run no longer exists."
            }
        }
    }

    fun finish() {
        viewModelScope.launch {
            when (val result = activation.finish(logId)) {
                is MorningFinishResult.Closed -> _finished.value = true
                MorningFinishResult.NotReady ->
                    _message.value = "Deal with every step first."

                MorningFinishResult.AlreadyCompleted ->
                    _message.value = "Today's routine is already complete."

                MorningFinishResult.NotFound ->
                    _message.value = "That run no longer exists."
            }
        }
    }

    fun abandon() {
        viewModelScope.launch {
            activation.abandon(logId)
            _finished.value = true
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    /** Rebuilds a running countdown after recreation. */
    private fun restoreTimer() {
        val phaseName = savedState.get<String>(KEY_PHASE) ?: return
        val phase = runCatching { MorningTimerPhase.valueOf(phaseName) }
            .getOrDefault(MorningTimerPhase.IDLE)
        if (phase == MorningTimerPhase.IDLE) return
        timer.restore(
            MorningTimerSnapshot(
                totalSec = savedState.get<Int>(KEY_TOTAL) ?: 0,
                remainingSec = savedState.get<Int>(KEY_REMAINING) ?: 0,
                phase = phase,
                endsAtElapsedMs = savedState.get<Long>(KEY_ENDS_AT) ?: 0L,
            ),
        )
    }

    /** Mirrors the live timer into saved state on every state change. */
    fun persistTimer() {
        val snapshot = timer.snapshot()
        savedState[KEY_TOTAL] = snapshot.totalSec
        savedState[KEY_REMAINING] = snapshot.remainingSec
        savedState[KEY_PHASE] = snapshot.phase.name
        savedState[KEY_ENDS_AT] = snapshot.endsAtElapsedMs
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
            return MorningRunViewModel(
                shadow.morningActivationRepository,
                extras.createSavedStateHandle(),
                logId,
            ) as T
        }
    }

    private companion object {
        const val KEY_TOTAL = "morningTimerTotal"
        const val KEY_REMAINING = "morningTimerRemaining"
        const val KEY_PHASE = "morningTimerPhase"
        const val KEY_ENDS_AT = "morningTimerEndsAt"
        const val KEY_POSITION = "morningTimerPosition"
    }
}
