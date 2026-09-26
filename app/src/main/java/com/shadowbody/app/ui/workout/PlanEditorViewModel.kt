package com.shadowbody.app.ui.workout

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.Exercise
import com.shadowbody.app.data.local.PlanDetail
import com.shadowbody.app.data.local.WorkoutPlan
import com.shadowbody.app.data.local.WorkoutPlanExercise
import com.shadowbody.app.data.repository.ExerciseRepository
import com.shadowbody.app.data.repository.PlanRepository
import com.shadowbody.app.domain.validation.PlanInput
import com.shadowbody.app.domain.validation.PlanSlotInput
import com.shadowbody.app.domain.validation.WorkoutValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanEditorUiState(
    val planId: Long = 0,
    val input: PlanInput = PlanInput(),
    val slots: List<PlanSlotInput> = emptyList(),
    val errors: Map<String, String> = emptyMap(),
    val slotErrors: Map<String, String> = emptyMap(),
    val loaded: Boolean = false,
    val savedId: Long? = null,
    val saving: Boolean = false,
)

class PlanEditorViewModel(
    private val plans: PlanRepository,
    private val exercises: ExerciseRepository,
    private val planId: Long = 0,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanEditorUiState(planId = planId))
    val uiState: StateFlow<PlanEditorUiState> = _uiState.asStateFlow()

    val library: StateFlow<List<Exercise>> = exercises.library()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        if (planId != 0L) {
            viewModelScope.launch {
                plans.getDetailOnce(planId)?.let { detail ->
                    _uiState.update {
                        it.copy(
                            input = PlanInput(
                                name = detail.plan.name,
                                description = detail.plan.description,
                                targetDurationMin = detail.plan.targetDurationMin?.toString() ?: "",
                            ),
                            slots = detail.slots.map { slot ->
                                PlanSlotInput(
                                    exerciseId = slot.exercise.id,
                                    targetSets = slot.slot.targetSets.toString(),
                                    targetReps = slot.slot.targetReps?.toString() ?: "",
                                    targetDurationSec = slot.slot.targetDurationSec?.toString() ?: "",
                                    restSec = slot.slot.restSec.toString(),
                                )
                            },
                            loaded = true,
                        )
                    }
                } ?: _uiState.update { it.copy(loaded = true) }
            }
        } else {
            _uiState.update { it.copy(loaded = true) }
        }
    }

    fun updateInput(input: PlanInput) {
        _uiState.update { it.copy(input = input, errors = emptyMap(), savedId = null) }
    }

    fun addExercise(exerciseId: Long) {
        _uiState.update { state ->
            if (state.slots.any { it.exerciseId == exerciseId }) {
                state.copy(slotErrors = mapOf("general" to "That exercise is already in this plan."))
            } else {
                state.copy(slots = state.slots + PlanSlotInput(exerciseId = exerciseId), slotErrors = emptyMap())
            }
        }
    }

    fun updateSlot(index: Int, slot: PlanSlotInput) {
        _uiState.update { state ->
            state.copy(
                slots = state.slots.toMutableList().also { it[index] = slot },
                slotErrors = emptyMap(),
                savedId = null,
            )
        }
    }

    fun removeSlot(index: Int) {
        _uiState.update { state ->
            state.copy(slots = state.slots.filterIndexed { i, _ -> i != index })
        }
    }

    fun moveSlot(index: Int, delta: Int) {
        _uiState.update { state ->
            val list = state.slots.toMutableList()
            val target = index + delta
            if (target !in list.indices) return@update state
            val tmp = list[index]
            list[index] = list[target]
            list[target] = tmp
            state.copy(slots = list)
        }
    }

    fun save() {
        val current = _uiState.value
        val planErrors = WorkoutValidator.validatePlan(current.input, current.slots.size)
        val slotErrors = WorkoutValidator.validateSlots(current.slots)
        if (planErrors.isNotEmpty() || slotErrors.isNotEmpty()) {
            _uiState.update { it.copy(errors = planErrors, slotErrors = slotErrors) }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val plan = WorkoutPlan(
                id = current.planId,
                name = current.input.name.trim(),
                description = current.input.description.trim(),
                targetDurationMin = current.input.targetDurationMin.ifBlank { null }
                    ?.toInt(),
                updatedAt = now,
            )
            val entries = current.slots.map { slot ->
                WorkoutPlanExercise(
                    planId = current.planId,
                    exerciseId = slot.exerciseId,
                    position = 0, // Normalized inside savePlan.
                    targetSets = slot.targetSets.trim().toInt(),
                    targetReps = slot.targetReps.ifBlank { null }?.toInt(),
                    targetDurationSec = slot.targetDurationSec.ifBlank { null }?.toInt(),
                    restSec = slot.restSec.trim().toInt(),
                )
            }
            val savedId = plans.savePlan(plan, entries)
            _uiState.update { it.copy(saving = false, savedId = savedId) }
        }
    }

    class Factory(
        private val app: Application,
        private val planId: Long = 0,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val shadow = app as ShadowBodyApp
            return PlanEditorViewModel(shadow.planRepository, shadow.exerciseRepository, planId) as T
        }
    }
}
