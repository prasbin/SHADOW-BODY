package com.shadowbody.app.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shadowbody.app.ui.baseline.BaselineHistoryScreen
import com.shadowbody.app.ui.baseline.BaselineViewModel
import com.shadowbody.app.ui.dashboard.DashboardScreen
import com.shadowbody.app.ui.dashboard.DashboardViewModel
import com.shadowbody.app.ui.profile.ProfileEditScreen
import com.shadowbody.app.ui.profile.ProfileEditViewModel
import com.shadowbody.app.ui.profile.ProfileScreen
import com.shadowbody.app.ui.profile.ProfileViewModel
import com.shadowbody.app.ui.settings.SettingsScreen
import com.shadowbody.app.ui.settings.SettingsViewModel

/**
 * Navigation graph: Dashboard + Settings (Phase 1) + Profile flow (Phase 2).
 * Future phase routes are added here as their features land —
 * no placeholder destinations for unimplemented features.
 */
@Composable
fun ShadowBodyNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as Application

    NavHost(navController = navController, startDestination = Routes.START) {
        composable(Routes.DASHBOARD) {
            val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory(app))
            DashboardScreen(
                viewModel = vm,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
            )
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app),
            )
            val uiState by vm.uiState.collectAsState()
            SettingsScreen(
                uiState = uiState,
                onThemeModeChange = vm::setThemeMode,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PROFILE) {
            val vm: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory(app))
            val uiState by vm.uiState.collectAsState()
            ProfileScreen(
                state = uiState,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.PROFILE_EDIT) },
                onOpenBaselineHistory = { navController.navigate(Routes.BASELINE_HISTORY) },
            )
        }
        composable(Routes.PROFILE_EDIT) {
            val vm: ProfileEditViewModel = viewModel(factory = ProfileEditViewModel.Factory(app))
            val uiState by vm.uiState.collectAsState()
            ProfileEditScreen(
                state = uiState,
                onChange = { vm.update(it.input) },
                onSave = vm::save,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.BASELINE_HISTORY) {
            val vm: BaselineViewModel = viewModel(factory = BaselineViewModel.Factory(app))
            val history by vm.history.collectAsState()
            val inputState by vm.inputState.collectAsState()
            BaselineHistoryScreen(
                history = history,
                inputState = inputState,
                onOpenDialog = vm::openDialog,
                onCloseDialog = vm::closeDialog,
                onInputChange = vm::updateInput,
                onSave = vm::save,
                onDelete = vm::delete,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
