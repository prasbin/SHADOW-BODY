package com.shadowbody.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadowbody.app.ui.components.SystemActionButton
import com.shadowbody.app.ui.components.SystemCard
import com.shadowbody.app.ui.components.SystemDivider
import com.shadowbody.app.ui.components.SystemStatBlock
import com.shadowbody.app.ui.components.SystemStatusChip
import com.shadowbody.app.ui.theme.LocalShadowSpacing
import com.shadowbody.app.ui.theme.ShadowCyan
import com.shadowbody.app.ui.theme.ShadowSuccess
import com.shadowbody.app.ui.theme.ShadowTextMuted
import com.shadowbody.app.ui.theme.ShadowTextPrimary
import com.shadowbody.app.ui.theme.ShadowTextSecondary
import com.shadowbody.app.ui.theme.ShadowViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenTrain: () -> Unit,
    onOpenTrack: () -> Unit,
    onOpenCoach: () -> Unit,
    onOpenActivation: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenGrooming: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onStartTodayWorkout: (Long) -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = LocalShadowSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SHADOW BODY",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.testTag("appTitle"),
                        )
                        Text(
                            text = "SYSTEM ONLINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShadowCyan,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = ShadowCyan,
                    actionIconContentColor = ShadowTextSecondary,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.md)
                .testTag("dashboardList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                SystemCard(accent = true) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SystemStatBlock(
                                label = "LEVEL",
                                value = state.level.toString().padStart(2, '0'),
                                accent = true,
                            )
                            SystemStatBlock(
                                label = "XP",
                                value = state.totalXp.toString(),
                            )
                            SystemStatBlock(
                                label = "STREAK",
                                value = state.currentStreak.toString(),
                                accent = true,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "TODAY",
                    style = MaterialTheme.typography.labelMedium,
                    color = ShadowCyan,
                    modifier = Modifier.testTag("todayHeader"),
                )
            }

            if (state.isTrainingDay && state.todayPlanId != null) {
                item {
                    SystemCard(accent = true) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "TODAY'S WORKOUT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ShadowTextMuted,
                                    )
                                    Text(
                                        text = state.todayPlanName ?: "WORKOUT",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ShadowTextPrimary,
                                    )
                                    Text(
                                        text = "${state.todayEstimatedMinutes} MIN",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ShadowTextSecondary,
                                    )
                                }
                                SystemStatusChip(
                                    label = "READY",
                                    isActive = true,
                                )
                            }
                            Spacer(modifier = Modifier.height(spacing.sm))
                            SystemActionButton(
                                text = "START WORKOUT",
                                onClick = { state.todayPlanId?.let { onStartTodayWorkout(it) } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("startWorkoutButton"),
                            )
                        }
                    }
                }
            } else if (!state.isTrainingDay) {
                item {
                    SystemCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "REST DAY",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ShadowTextPrimary,
                                    )
                                    state.restDayInfo?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ShadowTextSecondary,
                                        )
                                    }
                                }
                                SystemStatusChip(
                                    label = "RECOVERY",
                                    isActive = true,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "TODAY'S OBJECTIVES",
                    style = MaterialTheme.typography.labelMedium,
                    color = ShadowCyan,
                    modifier = Modifier.testTag("todayObjectivesHeader"),
                )
            }

            val objectives = buildList {
                if (state.morningStatus == "NOT_STARTED") {
                    add(Objective("morning", "Morning Activation", "Start your daily routine", onOpenActivation))
                }
                if (state.groomingStatus == "NOT_STARTED") {
                    add(Objective("grooming", "Grooming", "Complete your grooming routine", onOpenGrooming))
                }
                if (state.hydrationGoalMl > 0 && state.hydrationMl < state.hydrationGoalMl / 2) {
                    add(Objective("hydration", "Hydration", "${state.hydrationMl}/${state.hydrationGoalMl}ml", onOpenNutrition))
                }
                if (state.wardrobeItemCount > 0) {
                    add(Objective("outfit", "Outfit", "Generate today's outfit", onOpenWardrobe))
                }
            }

            items(objectives, key = { it.id }) { obj ->
                SystemCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { obj.action() }
                        .testTag("objective:${obj.id}"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = obj.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = ShadowTextPrimary,
                            )
                            Text(
                                text = obj.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = ShadowTextSecondary,
                            )
                        }
                        SystemStatusChip(
                            label = "GO",
                            isActive = true,
                        )
                    }
                }
            }

            item {
                SystemDivider()
                Text(
                    text = "SYSTEM RECOMMENDATION",
                    style = MaterialTheme.typography.labelMedium,
                    color = ShadowCyan,
                )
            }

            item {
                SystemCard(
                    accent = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCoach() }
                        .testTag("systemRecommendation"),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val recommendation = when {
                            state.isTrainingDay && state.todayPlanId != null -> "Today's workout is ready. Start when you're ready."
                            !state.isTrainingDay -> "Rest day. Recovery is training."
                            state.morningStatus == "NOT_STARTED" -> "Your next action is Morning Activation."
                            state.groomingStatus == "NOT_STARTED" -> "Complete your grooming routine."
                            state.hydrationGoalMl > 0 && state.hydrationMl < state.hydrationGoalMl / 2 -> "Increase your hydration today."
                            state.recentMissedWorkouts > 0 -> "Maintain consistency with your training."
                            state.currentStreak > 0 -> "Keep your ${state.currentStreak}-day streak alive."
                            else -> "All systems nominal. Review your progress."
                        }
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ShadowTextPrimary,
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = "VIEW COACH",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShadowCyan,
                        )
                    }
                }
            }

            item {
                SystemDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    SystemCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenTrain() }
                            .testTag("navTrain"),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "TRAIN",
                                style = MaterialTheme.typography.labelMedium,
                                color = ShadowCyan,
                            )
                            Text(
                                text = "Workouts",
                                style = MaterialTheme.typography.bodySmall,
                                color = ShadowTextSecondary,
                            )
                        }
                    }
                    SystemCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenTrack() }
                            .testTag("navTrack"),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "TRACK",
                                style = MaterialTheme.typography.labelMedium,
                                color = ShadowCyan,
                            )
                            Text(
                                text = "Progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = ShadowTextSecondary,
                            )
                        }
                    }
                    SystemCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenProfile() }
                            .testTag("navProfile"),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "PROFILE",
                                style = MaterialTheme.typography.labelMedium,
                                color = ShadowCyan,
                            )
                            Text(
                                text = "Player",
                                style = MaterialTheme.typography.bodySmall,
                                color = ShadowTextSecondary,
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.md)) }
        }
    }
}

private data class Objective(
    val id: String,
    val title: String,
    val subtitle: String,
    val action: () -> Unit,
)