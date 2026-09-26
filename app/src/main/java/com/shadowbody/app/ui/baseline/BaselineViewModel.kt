package com.shadowbody.app.ui.baseline

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.BaselineRecord
import com.shadowbody.app.data.repository.BaselineRepository
import com.shadowbody.app.domain.validation.BaselineInput
import com.shadowbody.app.domain.validation.BaselineValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BaselineInputUiState(
    val input: BaselineInput = BaselineInput(),
    val errors: Map<String, String> = emptyMap(),
    val showDialog: Boolean = false,
    val saving: Boolean = false,
)

class BaselineViewModel(
    private val repository: BaselineRepository,
) : ViewModel() {

    val history: StateFlow<List<BaselineRecord>> = repository.history()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _input = MutableStateFlow(BaselineInputUiState())
    val inputState: StateFlow<BaselineInputUiState> = _input.asStateFlow()

    val latest: StateFlow<BaselineRecord?> = repository.latest()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun openDialog() {
        _input.update { BaselineInputUiState(showDialog = true) }
    }

    fun closeDialog() {
        _input.update { it.copy(showDialog = false, errors = emptyMap()) }
    }

    fun updateInput(input: BaselineInput) {
        _input.update { it.copy(input = input, errors = emptyMap()) }
    }

    fun save() {
        val current = _input.value
        val errors = BaselineValidator.validate(current.input)
        if (errors.isNotEmpty()) {
            _input.update { it.copy(errors = errors) }
            return
        }
        _input.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.add(BaselineValidator.toRecord(current.input))
            _input.update { BaselineInputUiState(showDialog = false) }
        }
    }

    fun delete(record: BaselineRecord) {
        viewModelScope.launch { repository.delete(record) }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repo = (app as ShadowBodyApp).baselineRepository
            return BaselineViewModel(repo) as T
        }
    }
}
