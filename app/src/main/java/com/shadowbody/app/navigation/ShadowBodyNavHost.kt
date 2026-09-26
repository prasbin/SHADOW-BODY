package com.shadowbody.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shadowbody.app.ui.dashboard.DashboardScreen
import com.shadowbody.app.ui.settings.SettingsScreen
import com.shadowbody.app.ui.settings.SettingsViewModel

/**
 * Phase 1 navigation graph: Dashboard + Settings.
 * Future phase routes are added here as their features land —
 * no placeholder destinations for unimplemented features.
 */
@Composable
fun ShadowBodyNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.START) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            val context = LocalContext.current
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(context.applicationContext as android.app.Application),
            )
            val uiState by vm.uiState.collectAsState()
            SettingsScreen(
                uiState = uiState,
                onThemeModeChange = vm::setThemeMode,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
