package com.shadowbody.app.ui.morning

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.MorningRoutine
import com.shadowbody.app.data.local.MorningRoutineStep
import com.shadowbody.app.data.repository.MorningRoutineRepository
import com.shadowbody.app.domain.model.MorningStepCategory
import com.shadowbody.app.domain.validation.MorningRoutineValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One editable step row. Ids and positions are assigned on save. */
data class MorningStepInput(
    val id: Long = 0L,
    val title: String = "",
    val instructions: String = "",
    val category: MorningStepCategory = MorningStepCategory.MOBILITY,
    val durationSec: Int? = null,
    val reps: Int? = null,
    val isEnabled: Boolean = true,
)

/** Editor state; errors are per field, exactly like the other Phase 2-4 forms. */
data class MorningEditorState(
    val routineId: Long? = null,
    val name: String = "",
    val description: String = "",
    val steps: List<MorningStepInput> = emptyList(),
    val errors: Map<String, String> = emptyMap(),
    val busy: Boolean = false,
    val saved: Boolean = false,
    val message: String? = null,
) {
    val isNew: Boolean get() = routineId == null
    val canSave: Boolean get() = !busy && steps.isNotEmpty()
}

/**
 * Phase 5 routine editor: create, rename, reorder, enable/disable and delete
 * steps, plus create and delete whole routines.
 *
 * Validation is delegated to [MorningRoutineValidator] and the seeded routine
 * is protected: it can be edited, but a routine that is referenced by recorded
 * history is never silently removed.
 */
class MorningRoutineEditorViewModel(
    private val routines: MorningRoutineRepository,
    private val routineId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(MorningEditorState())
    val state: StateFlow<MorningEditorState> = _state.asStateFlow()

    private val _isSeeded = MutableStateFlow(false)

    /** True when the routine being edited is the built-in one. */
    val isSeeded: StateFlow<Boolean> = _isSeeded.asStateFlow()

    val categories: List<MorningStepCategory> = MorningStepCategory.entries

    init {
        viewModelScope.launch {
            val existing = routineId?.let { routines.getRoutine(it) }
            _isSeeded.value = existing?.seedKey != null
            if (existing == null) {
                _state.value = MorningEditorState(steps = listOf(MorningStepInput()))
            } else {
                val stored = routines.getSteps(existing.id)
                _state.value = MorningEditorState(
                    routineId = existing.id,
                    name = existing.name,
                    description = existing.description,
                    steps = stored.map { it.toInput() },
                )
            }
        }
    }

    fun setName(value: String) = update {
        copy(name = value, errors = errors - MorningRoutineValidator.FIELD_NAME)
    }

    fun setDescription(value: String) = update {
        copy(description = value, errors = errors - MorningRoutineValidator.FIELD_DESCRIPTION)
    }

    fun addStep() = update { copy(steps = steps + MorningStepInput()) }

    fun updateStep(index: Int, block: (MorningStepInput) -> MorningStepInput) {
        val current = _state.value
        if (index !in current.steps.indices) return
        val updated = current.steps.toMutableList()
        updated[index] = block(updated[index])
        // Editing any row clears the step-level errors, but keeps header errors.
        val cleared = current.errors.filterKeys { !it.startsWith(STEP_ERROR_PREFIX) }
        _state.value = current.copy(steps = updated, errors = cleared, saved = false)
    }

    fun removeStep(index: Int) {
        val current = _state.value
        if (index !in current.steps.indices) return
        _state.value = current.copy(
            steps = current.steps.filterIndexed { i, _ -> i != index },
            saved = false,
        )
    }

    fun moveStep(from: Int, to: Int) {
        val current = _state.value
        if (from !in current.steps.indices || to !in current.steps.indices || from == to) return
        val reordered = current.steps.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        _state.value = current.copy(steps = reordered, saved = false)
    }

    fun toggleStepEnabled(index: Int) = updateStep(index) { it.copy(isEnabled = !it.isEnabled) }

    fun save() {
        val current = _state.value
        val errors = buildMap {
            putAll(
                MorningRoutineValidator.validateRoutine(
                    current.name,
                    current.description,
                ),
            )
            val enabled = current.steps.filter { it.isEnabled }
            if (enabled.isEmpty()) {
                put(
                    MorningRoutineValidator.FIELD_STEP,
                    "Keep at least one step enabled.",
                )
            }
            current.steps.forEachIndexed { index, step ->
                val stepErrors = MorningRoutineValidator.validateStep(
                    title = step.title,
                    instructions = step.instructions,
                    category = step.category,
                    durationSec = step.durationSec,
                    reps = step.reps,
                )
                stepErrors.forEach { (field, message) ->
                    put("$STEP_ERROR_PREFIX$index:$field", message)
                }
            }
        }
        if (errors.isNotEmpty()) {
            _state.value = current.copy(errors = errors, saved = false)
            return
        }
        val enabled = current.steps.filter { it.isEnabled }
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true)
            val entries = enabled.map { it.toStep() }
            val trimmedName = current.name.trim()
            val trimmedDescription = current.description.trim()
            if (current.routineId == null) {
                val id = routines.createRoutine(trimmedName, trimmedDescription, entries)
                _state.value = _state.value.copy(
                    routineId = id,
                    busy = false,
                    saved = true,
                    message = "Routine created.",
                )
            } else {
                val routine = routines.getRoutine(current.routineId) ?: MorningRoutine(
                    id = current.routineId,
                    name = trimmedName,
                )
                routines.saveRoutine(
                    routine.copy(
                        name = trimmedName,
                        description = trimmedDescription,
                    ),
                    entries,
                )
                _state.value = _state.value.copy(busy = false, saved = true, message = "Saved.")
            }
        }
    }

    fun deleteRoutine() {
        val id = _state.value.routineId ?: return
        viewModelScope.launch {
            routines.delete(id)
            _state.value = _state.value.copy(saved = false, message = "Routine deleted.")
        }
    }

    fun reset() {
        viewModelScope.launch {
            val existing = routineId?.let { routines.getRoutine(it) }
            _state.value = if (existing == null) {
                MorningEditorState(steps = listOf(MorningStepInput()))
            } else {
                MorningEditorState(
                    routineId = existing.id,
                    name = existing.name,
                    description = existing.description,
                    steps = routines.getSteps(existing.id).map { it.toInput() },
                )
            }
        }
    }

    private fun update(block: MorningEditorState.() -> MorningEditorState) {
        _state.value = _state.value.block().copy(saved = false)
    }

    class Factory(
        private val app: Application,
        private val routineId: Long?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val shadow = app as ShadowBodyApp
            return MorningRoutineEditorViewModel(
                shadow.morningRoutineRepository,
                routineId,
            ) as T
        }
    }

    companion object {
        const val STEP_ERROR_PREFIX = "step:"

        fun MorningRoutineStep.toInput(): MorningStepInput = MorningStepInput(
            id = id,
            title = title,
            instructions = instructions,
            category = category,
            durationSec = targetDurationSec,
            reps = targetReps,
            isEnabled = isEnabled,
        )

        /** Maps an input row to a step entity; id/position are assigned on save. */
        fun MorningStepInput.toStep(): MorningRoutineStep = MorningRoutineStep(
            routineId = 0L,
            title = title.trim(),
            instructions = instructions.trim(),
            category = category,
            targetDurationSec = durationSec?.takeIf { it > 0 },
            targetReps = reps?.takeIf { it > 0 },
            position = 0,
            isEnabled = true,
        )
    }
}
