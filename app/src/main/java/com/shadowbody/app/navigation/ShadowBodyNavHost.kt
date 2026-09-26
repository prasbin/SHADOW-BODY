package com.shadowbody.app.navigation

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shadowbody.app.ShadowBodyApp
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
import com.shadowbody.app.ui.theme.LocalShadowSpacing
import com.shadowbody.app.ui.workout.ActiveWorkoutScreen
import com.shadowbody.app.ui.workout.ActiveWorkoutViewModel
import com.shadowbody.app.ui.workout.PlanDetailScreen
import com.shadowbody.app.ui.workout.PlanDetailViewModel
import com.shadowbody.app.ui.workout.PlanEditorScreen
import com.shadowbody.app.ui.workout.PlanEditorViewModel
import com.shadowbody.app.ui.workout.WorkoutListScreen
import com.shadowbody.app.ui.workout.WorkoutListViewModel
import com.shadowbody.app.ui.workout.WorkoutResultScreen
import com.shadowbody.app.ui.workout.WorkoutResultViewModel

/**
 * Navigation graph: Dashboard + Settings (Phase 1), Profile flow (Phase 2),
 * Workout engine (Phase 3). Future phase routes land here as features do —
 * no placeholder destinations for unimplemented features.
 */
@Composable
fun ShadowBodyNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val shadow = app as ShadowBodyApp
    val profile by shadow.profileRepository.profile.collectAsState(initial = null)

    NavHost(navController = navController, startDestination = Routes.START) {
        composable(Routes.DASHBOARD) {
            val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory(app))
            DashboardScreen(
                viewModel = vm,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenWorkout = { navController.navigate(Routes.WORKOUT) },
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
        composable(Routes.WORKOUT) {
            val vm: WorkoutListViewModel = viewModel(factory = WorkoutListViewModel.Factory(app))
            val plans by vm.planList.collectAsState()
            val sessions by vm.recentSessions.collectAsState()
            val completed by vm.completedCount.collectAsState()
            val seeded by vm.seededCount.collectAsState()
            WorkoutListScreen(
                plans = plans,
                sessions = sessions,
                completedCount = completed,
                seededCount = seeded,
                onBack = { navController.popBackStack() },
                onOpenPlan = { navController.navigate(Routes.planDetail(it)) },
                onNewPlan = { navController.navigate(Routes.planEditor()) },
                onOpenResult = { navController.navigate(Routes.workoutResult(it)) },
            )
        }
        composable(
            Routes.PLAN_DETAIL,
            arguments = listOf(navArgument("planId") { type = NavType.LongType }),
        ) { entry ->
            val planId = entry.arguments?.getLong("planId") ?: 0L
            val vm: PlanDetailViewModel = viewModel(
                key = "plan-$planId",
                factory = PlanDetailViewModel.Factory(app, planId),
            )
            val detail by vm.detail.collectAsState()
            val started by vm.startedSession.collectAsState()
            LaunchedEffect(started) {
                started?.let { navController.navigate(Routes.activeWorkout(it)) }
            }
            PlanDetailScreen(
                detail = detail,
                ownedEquipment = profile?.equipment ?: emptySet(),
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.planEditor(planId)) },
                onDelete = {
                    vm.deletePlan {
                        navController.popBackStack()
                    }
                },
                onStart = vm::startSession,
            )
        }
        composable(
            Routes.PLAN_EDITOR,
            arguments = listOf(navArgument("planId") {
                type = NavType.LongType
                defaultValue = 0L
            }),
        ) { entry ->
            val planId = entry.arguments?.getLong("planId") ?: 0L
            val vm: PlanEditorViewModel = viewModel(
                key = "editor-$planId",
                factory = PlanEditorViewModel.Factory(app, planId),
            )
            val uiState by vm.uiState.collectAsState()
            val library by vm.library.collectAsState()
            val savedId = uiState.savedId
            LaunchedEffect(savedId) {
                savedId?.let {
                    navController.popBackStack()
                    navController.navigate(Routes.planDetail(it))
                }
            }
            PlanEditorScreen(
                state = uiState,
                library = library,
                onInputChange = vm::updateInput,
                onAddExercise = vm::addExercise,
                onUpdateSlot = vm::updateSlot,
                onRemoveSlot = vm::removeSlot,
                onMoveSlot = vm::moveSlot,
                onSave = vm::save,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.ACTIVE_WORKOUT,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
        ) { entry ->
            val sessionId = entry.arguments?.getLong("sessionId") ?: 0L
            val vm: ActiveWorkoutViewModel = viewModel(
                key = "active-$sessionId",
                factory = ActiveWorkoutViewModel.Factory(app, sessionId),
            )
            val detail by vm.detail.collectAsState()
            val rest by vm.timer.state.collectAsState()
            val finished by vm.finished.collectAsState()
            LaunchedEffect(finished) {
                if (finished) {
                    navController.popBackStack()
                    navController.navigate(Routes.workoutResult(sessionId))
                }
            }
            ActiveWorkoutScaffold(
                title = (detail?.session?.name ?: "WORKOUT").uppercase(),
                onFinish = vm::finish,
                onAbandon = { vm.abandon { navController.popBackStack() } },
            ) {
                ActiveWorkoutScreen(
                    detail = detail,
                    rest = rest,
                    onSaveSet = vm::saveSet,
                    onToggleExercise = vm::toggleExercise,
                    onStartRest = vm.timer::start,
                    onPauseRest = vm.timer::pause,
                    onResumeRest = vm.timer::resume,
                    onResetRest = vm.timer::reset,
                    onFinish = vm::finish,
                    onAbandon = { vm.abandon { navController.popBackStack() } },
                )
            }
        }
        composable(
            Routes.WORKOUT_RESULT,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
        ) { entry ->
            val sessionId = entry.arguments?.getLong("sessionId") ?: 0L
            val vm: WorkoutResultViewModel = viewModel(
                key = "result-$sessionId",
                factory = WorkoutResultViewModel.Factory(app, sessionId),
            )
            val detail by vm.detail.collectAsState()
            WorkoutResultScreen(
                detail = detail,
                onDone = {
                    navController.popBackStack(Routes.WORKOUT, inclusive = false)
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveWorkoutScaffold(
    title: String,
    onFinish: () -> Unit,
    onAbandon: () -> Unit,
    content: @Composable () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs)) {
                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth().testTag("sessionFinish"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("COMPLETE WORKOUT")
                }
                OutlinedButton(
                    onClick = onAbandon,
                    modifier = Modifier.fillMaxWidth().testTag("sessionAbandon"),
                ) {
                    Text("ABANDON")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            content()
        }
    }
}
