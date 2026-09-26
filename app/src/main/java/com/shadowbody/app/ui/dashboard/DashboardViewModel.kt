package com.shadowbody.app.ui.dashboard

import androidx.lifecycle.ViewModel
import com.shadowbody.app.domain.model.ModuleState
import com.shadowbody.app.domain.model.SystemModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Read-only dashboard state for Phase 1. Populated by later phases. */
data class DashboardUiState(
    val hunterName: String = "PLAYER",
    val level: Int = 1,
    val xpProgress: Float = 0f,
    val modules: List<SystemModule> = defaultModules(),
)

/** Phase roadmap as dashboard entries. Only Dashboard/Settings exist in Phase 1. */
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

class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
}
