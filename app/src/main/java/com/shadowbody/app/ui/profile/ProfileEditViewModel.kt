package com.shadowbody.app.ui.profile

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.UserProfile
import com.shadowbody.app.data.repository.ProfileRepository
import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.domain.model.FitnessLevel
import com.shadowbody.app.domain.model.Goal
import com.shadowbody.app.domain.validation.ProfileInput
import com.shadowbody.app.domain.validation.ProfileValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileEditUiState(
    val input: ProfileInput = ProfileInput(),
    val errors: Map<String, String> = emptyMap(),
    val loaded: Boolean = false,
    val isEdit: Boolean = false,
    val saved: Boolean = false,
    val saving: Boolean = false,
)

class ProfileEditViewModel(
    private val repository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.get()?.let { existing ->
                _uiState.update {
                    it.copy(
                        isEdit = true,
                        input = ProfileInput(
                            age = existing.age.toString(),
                            heightCm = trimNum(existing.heightCm),
                            weightKg = trimNum(existing.weightKg),
                            fitnessLevel = existing.fitnessLevel,
                            equipment = existing.equipment,
                            goals = existing.goals,
                            trainingDays = existing.trainingDays,
                            sessionMinutes = existing.sessionMinutes,
                        ),
                        loaded = true,
                    )
                }
            } ?: _uiState.update { it.copy(loaded = true) }
        }
    }

    fun update(input: ProfileInput) {
        _uiState.update { it.copy(input = input, errors = emptyMap(), saved = false) }
    }

    fun save() {
        val current = _uiState.value
        val errors = ProfileValidator.validate(current.input)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.save(ProfileValidator.toProfile(current.input))
            _uiState.update { it.copy(saving = false, saved = true) }
        }
    }

    private fun trimNum(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repo = (app as ShadowBodyApp).profileRepository
            return ProfileEditViewModel(repo) as T
        }
    }
}

/** Convenience accessors used by the edit screen. */
fun ProfileEditUiState.updateAge(v: String) = copy(input = input.copy(age = v))
fun ProfileEditUiState.updateHeight(v: String) = copy(input = input.copy(heightCm = v))
fun ProfileEditUiState.updateWeight(v: String) = copy(input = input.copy(weightKg = v))
fun ProfileEditUiState.updateLevel(v: FitnessLevel) = copy(input = input.copy(fitnessLevel = v))
fun ProfileEditUiState.updateSession(v: Int) = copy(input = input.copy(sessionMinutes = v))
fun ProfileEditUiState.toggleEquipment(v: Equipment) =
    copy(input = input.copy(equipment = input.equipment.toggle(v)))
fun ProfileEditUiState.toggleGoal(v: Goal) =
    copy(input = input.copy(goals = input.goals.toggle(v)))
fun ProfileEditUiState.toggleDay(v: Int) =
    copy(input = input.copy(trainingDays = input.trainingDays.toggle(v)))

private fun <T> Set<T>.toggle(v: T): Set<T> =
    if (contains(v)) this - v else this + v

/** Labels for the edit form (kept next to the form that uses them). */
fun formatDays(days: Set<Int>): String =
    days.sorted().mapNotNull { UserProfile.DAY_LABELS[it] }.joinToString(" · ")
