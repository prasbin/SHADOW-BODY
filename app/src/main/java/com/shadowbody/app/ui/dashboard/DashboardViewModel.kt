package com.shadowbody.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.repository.ProfileRepository
import com.shadowbody.app.domain.model.ModuleState
import com.shadowbody.app.domain.model.SystemModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Read-only dashboard state. Populated by later phases. */
data class DashboardUiState(
    val hunterName: String = "PLAYER",
    val level: Int = 1,
    val xpProgress: Float = 0f,
    val profileConfigured: Boolean = false,
    val modules: List<SystemModule> = defaultModules(),
)

/**
 * Phase roadmap as dashboard entries. Phase 2 unlocks the profile module
 * (handled by the screen via [DashboardUiState.profileConfigured]); every
 * Phase 3-10 module stays LOCKED.
 */
fun defaultModules(): List<SystemModule> = listOf(
    SystemModule("profile", "Profile", "Body baseline — Phase 2", 2, ModuleState.LOCKED),
    SystemModule("workout", "Workout Engine", "Plans & sessions — Phase 3", 3, ModuleState.LOCKED),
    SystemModule("adaptive", "Adaptive Training", "Progression logic — Phase 4", 4, ModuleState.LOCKED),
    SystemModule("activation", "Morning Activation", "Daily ignition — Phase 5", 5, ModuleState.LOCKED),
    SystemModule("nutrition", "Nutrition", "Fuel & hydration — Phase 6", 6, ModuleState.LOCKED),
    SystemModule("progression", "Progression", "XP · streaks · ranks — Phase 7", 7, ModuleState.LOCKED),
    SystemModule("grooming", "Grooming", "Routines — Phase 8", 8, ModuleState.LOCKED),
    SystemModule("wardrobe", "Wardrobe", "Outfits — Phase 9", 9, ModuleState.LOCKED),
    SystemModule("coach", "Body Coach", "Guidance — Phase 10", 10, ModuleState.LOCKED),
)

class DashboardViewModel(
    profileRepository: ProfileRepository? = null,
) : ViewModel() {

    private val unconfigured = MutableStateFlow(DashboardUiState())

    /** Null repository = static Phase 1 behavior (used by legacy tests). */
    val uiState: StateFlow<DashboardUiState> =
        if (profileRepository == null) {
            unconfigured.asStateFlow()
        } else {
            profileRepository.profile
                .map { profile -> DashboardUiState(profileConfigured = profile != null) }
                .stateIn(viewModelScope, SharingStarted.Eagerly, DashboardUiState())
        }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repo = (app as ShadowBodyApp).profileRepository
            return DashboardViewModel(repo) as T
        }
    }
}
