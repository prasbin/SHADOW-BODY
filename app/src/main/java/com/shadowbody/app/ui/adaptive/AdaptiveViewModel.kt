package com.shadowbody.app.ui.adaptive

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.RecommendationDetail
import com.shadowbody.app.data.local.ReadinessReport
import com.shadowbody.app.data.repository.AdaptiveWorkoutPlanner
import com.shadowbody.app.data.repository.ReadinessRepository
import com.shadowbody.app.domain.model.RecommendationStatus
import com.shadowbody.app.domain.validation.AdaptiveValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One exercise row of a recommendation, flattened for display. */
data class AdaptiveExerciseRow(
    val exerciseId: Long,
    val position: Int,
    val targetText: String,
    val restSec: Int,
    val reasonCode: String,
    val reasonText: String,
)

/** Everything the adaptive screen renders. No domain logic lives here. */
data class AdaptiveUiState(
    val recommendationId: Long? = null,
    val recommendationName: String = "",
    val summary: String = "",
    val estimatedMinutes: Int = 0,
    val status: RecommendationStatus? = null,
    val exercises: List<AdaptiveExerciseRow> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val adoptedPlanId: Long? = null,
) {
    val hasRecommendation: Boolean get() = exercises.isNotEmpty()
    val isOpen: Boolean get() = hasRecommendation && status == RecommendationStatus.ACTIVE
}

/** Readiness form state; ratings are only valid inside 1..5. */
data class ReadinessInput(
    val fatigue: Int? = null,
    val soreness: Int? = null,
    val notes: String = "",
    val errors: Map<String, String> = emptyMap(),
)

/**
 * Phase 4 adaptive screen state.
 *
 * Deliberately thin: generation, adoption and skip decisions belong to
 * [AdaptiveWorkoutPlanner], and the training rules belong to the pure engine.
 * This class only collects input and renders stored results.
 */
class AdaptiveViewModel(
    private val planner: AdaptiveWorkoutPlanner,
    private val readinessRepository: ReadinessRepository,
) : ViewModel() {

    private val _busy = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    private val _adoptedPlanId = MutableStateFlow<Long?>(null)
    private val _readinessInput = MutableStateFlow(ReadinessInput())
    private val _readinessSaved = MutableStateFlow(false)

    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    val message: StateFlow<String?> = _message.asStateFlow()
    val adoptedPlanId: StateFlow<Long?> = _adoptedPlanId.asStateFlow()
    val readinessInput: StateFlow<ReadinessInput> = _readinessInput.asStateFlow()
    val readinessSaved: StateFlow<Boolean> = _readinessSaved.asStateFlow()

    val latestReadiness: StateFlow<ReadinessReport?> = readinessRepository.latest()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<AdaptiveUiState> = combine(
        planner.observeLatest(),
        _busy,
        _message,
        _adoptedPlanId,
    ) { detail, busy, message, adoptedPlanId ->
        detail.toUiState(busy, message, adoptedPlanId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdaptiveUiState())

    init {
        // Fold in anything completed since the last visit, so the screen never
        // shows a recommendation that ignores today's evidence.
        viewModelScope.launch { planner.applyLatestEvidence() }
    }

    fun generate() = runAction {
        val detail = planner.generate()
        _message.value = if (detail == null) {
            "No exercise matches your recorded equipment."
        } else {
            "Recommendation updated."
        }
    }

    fun adopt() = runAction {
        val id = uiState.value.recommendationId
        if (id == null) {
            _message.value = "Nothing to adopt."
            return@runAction
        }
        val planId = planner.adopt(id)
        _message.value = if (planId == null) {
            "Could not create a plan from this recommendation."
        } else {
            _adoptedPlanId.value = planId
            "Saved as a plan. Start it from Workouts."
        }
    }

    fun markMissed() = runAction {
        val id = uiState.value.recommendationId
        if (id == null) {
            _message.value = "Nothing to skip."
            return@runAction
        }
        planner.markMissed(id)
        _message.value = "Skip recorded. Completed sessions are untouched."
    }

    fun dismiss() = runAction {
        val id = uiState.value.recommendationId
        if (id == null) {
            _message.value = "Nothing to dismiss."
            return@runAction
        }
        planner.dismiss(id)
        _message.value = "Recommendation dismissed."
    }

    fun clearMessage() {
        _message.value = null
    }

    // --- Readiness form ---

    fun setFatigue(value: Int) = updateReadiness {
        copy(fatigue = value, errors = errors - AdaptiveValidator.FIELD_FATIGUE)
    }

    fun setSoreness(value: Int) = updateReadiness {
        copy(soreness = value, errors = errors - AdaptiveValidator.FIELD_SORENESS)
    }

    fun setNotes(value: String) = updateReadiness { copy(notes = value) }

    fun saveReadiness() {
        val input = _readinessInput.value
        val errors = AdaptiveValidator.validateReadiness(
            input.fatigue,
            input.soreness,
            input.notes.trim(),
        )
        if (errors.isNotEmpty()) {
            _readinessInput.value = input.copy(errors = errors)
            return
        }
        viewModelScope.launch {
            val id = readinessRepository.record(
                fatigue = input.fatigue,
                soreness = input.soreness,
                notes = input.notes,
            )
            if (id == null) {
                _message.value = "Readiness check-in was rejected."
            } else {
                _readinessInput.value = ReadinessInput()
                _readinessSaved.value = true
            }
        }
    }

    private fun updateReadiness(block: ReadinessInput.() -> ReadinessInput) {
        _readinessSaved.value = false
        _readinessInput.value = _readinessInput.value.block()
    }

    /** Runs [block] with the busy flag set, then releases it. */
    private fun runAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _message.value = null
            block()
            _busy.value = false
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val shadow = app as ShadowBodyApp
            return AdaptiveViewModel(
                shadow.adaptivePlanner,
                shadow.readinessRepository,
            ) as T
        }
    }
}

/** No stored recommendation yet is a normal empty state, not an error. */
private fun RecommendationDetail?.toUiState(
    busy: Boolean,
    message: String?,
    adoptedPlanId: Long?,
): AdaptiveUiState {
    if (this == null) {
        return AdaptiveUiState(busy = busy, message = message, adoptedPlanId = adoptedPlanId)
    }
    val recommendation = this.recommendation
    return AdaptiveUiState(
        recommendationId = recommendation.id,
        recommendationName = recommendation.name,
        summary = recommendation.summary,
        estimatedMinutes = recommendation.estimatedMinutes,
        status = recommendation.status,
        exercises = exercises.sortedBy { it.position }.map { row ->
            AdaptiveExerciseRow(
                exerciseId = row.exerciseId,
                position = row.position,
                targetText = if (row.reps != null) {
                    "${row.sets} x ${row.reps}"
                } else {
                    "${row.sets} x ${row.durationSec ?: 0}s"
                },
                restSec = row.restSec,
                reasonCode = row.reasonCode,
                reasonText = row.reasonText,
            )
        },
        busy = busy,
        message = message,
        adoptedPlanId = adoptedPlanId,
    )
}
