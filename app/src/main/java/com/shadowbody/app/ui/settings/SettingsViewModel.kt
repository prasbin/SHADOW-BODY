package com.shadowbody.app.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.preferences.AppPreferences
import com.shadowbody.app.data.preferences.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val versionName: String = "1.0.0",
    val offlineReady: Boolean = true,
)

class SettingsViewModel(
    private val preferences: AppPreferences,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        preferences.themeMode
            .map { SettingsUiState(themeMode = it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    /** Wires the ViewModel to the app's DataStore-backed preferences. */
    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val prefs = (app as ShadowBodyApp).preferences
            return SettingsViewModel(prefs) as T
        }
    }
}
