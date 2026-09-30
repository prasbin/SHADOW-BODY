package com.shadowbody.app.ui.grooming

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.GroomingRoutine
import com.shadowbody.app.data.local.GroomingRoutineStep
import com.shadowbody.app.data.repository.GroomingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroomingRoutineEditorUiState(
    val routineId: Long? = null,
    val name: String = "",
    val description: String = "",
    val steps: List<GroomingRoutineStep> = emptyList(),
    val isEditing: Boolean = false,
    val saved: Boolean = false,
    val message: String? = null,
)

class GroomingRoutineEditorViewModel(
    private val routines: GroomingRepository,
    private val routineId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(GroomingRoutineEditorUiState(
        routineId = routineId,
        isEditing = routineId != null,
    ))
    val state: StateFlow<GroomingRoutineEditorUiState> = _state.asStateFlow()

    init {
        if (routineId != null) {
            loadRoutine(routineId!!)
        }
    }

    private fun loadRoutine(id: Long) {
        viewModelScope.launch {
            val routine = routines.getRoutine(id)
            val steps = routines.getSteps(id)
            routine?.let {
                _state.value = _state.value.copy(
                    routineId = it.id,
                    name = it.name,
                    description = it.description,
                    steps = steps,
                )
            }
        }
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name)
    }

    fun updateDescription(description: String) {
        _state.value = _state.value.copy(description = description)
    }

    fun addStep() {
        val newStep = GroomingRoutineStep(
            routineId = _state.value.routineId ?: 0,
            title = "",
            instructions = "",
            category = "OTHER",
            position = _state.value.steps.size,
        )
        _state.value = _state.value.copy(steps = _state.value.steps + newStep)
    }

    fun updateStep(index: Int, step: GroomingRoutineStep) {
        val steps = _state.value.steps.toMutableList()
        steps[index] = step
        _state.value = _state.value.copy(steps = steps)
    }

    fun removeStep(index: Int) {
        val steps = _state.value.steps.toMutableList()
        val removed = steps.removeAt(index)
        // Reorder positions
        steps.forEachIndexed { idx, s -> steps[idx] = s.copy(position = idx) }
        _state.value = _state.value.copy(steps = steps)
        // If editing existing routine, also delete from DB
        if (removed.id != 0L) {
            viewModelScope.launch { routines.deleteStep(removed.id) }
        }
    }

    fun moveStep(fromIndex: Int, toIndex: Int) {
        val steps = _state.value.steps.toMutableList()
        val item = steps.removeAt(fromIndex)
        steps.add(toIndex, item)
        steps.forEachIndexed { idx, s -> steps[idx] = s.copy(position = idx) }
        _state.value = _state.value.copy(steps = steps)
    }

    fun save() {
        val st = _state.value
        if (st.name.trim().isEmpty()) {
            _state.value = st.copy(message = "Routine name is required.")
            return
        }
        if (st.steps.isEmpty()) {
            _state.value = st.copy(message = "At least one step is required.")
            return
        }
        // Validate steps
        for ((idx, step) in st.steps.withIndex()) {
            if (step.title.trim().isEmpty()) {
                _state.value = st.copy(message = "Step ${idx + 1} needs a title.")
                return
            }
        }
        viewModelScope.launch {
            val routineId = if (st.isEditing) {
                val routine = com.shadowbody.app.data.local.GroomingRoutine(
                    id = st.routineId!!,
                    seedKey = null,
                    name = st.name.trim(),
                    description = st.description.trim(),
                    isActive = true,
                    sortOrder = 0,
                    updatedAt = System.currentTimeMillis(),
                )
                routines.updateRoutine(routine)
                st.routineId!!
            } else {
                routines.createRoutine(com.shadowbody.app.data.local.GroomingRoutine(
                    name = st.name.trim(),
                    description = st.description.trim(),
                    isActive = true,
                ))
            }
            // Save steps
            for ((idx, step) in st.steps.withIndex()) {
                if (step.id != 0L) {
                    routines.updateStep(step.copy(routineId = routineId, position = idx))
                } else {
                    routines.createStep(step.copy(routineId = routineId, position = idx))
                }
            }
            _state.value = st.copy(saved = true)
        }
    }

    class Factory(
        private val app: Application,
        private val routineId: Long?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val shadow = app as ShadowBodyApp
            return GroomingRoutineEditorViewModel(
                shadow.groomingRepository,
                routineId,
            ) as T
        }
    }
}