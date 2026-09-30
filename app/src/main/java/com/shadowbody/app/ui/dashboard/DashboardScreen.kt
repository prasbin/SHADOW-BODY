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
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

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
                            color = MaterialTheme.colorScheme.primary,
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
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                SystemPanel(accentBorder = true) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "LEVEL ${state.level.toString().padStart(2, '0')}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${state.totalXp} XP",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${state.currentStreak}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "STREAK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.primary,
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
                add(Objective("training", "Training", "View workout plans", onOpenTrain))
            }

            items(objectives, key = { it.id }) { obj ->
                SystemPanel(
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
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = obj.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "GO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = "SYSTEM RECOMMENDATION",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            item {
                SystemPanel(
                    accentBorder = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCoach() }
                        .testTag("systemRecommendation"),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val recommendation = when {
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
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = "VIEW COACH",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    SystemPanel(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenTrain() }
                            .testTag("navTrain"),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "TRAIN",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Workouts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    SystemPanel(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenTrack() }
                            .testTag("navTrack"),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "TRACK",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    SystemPanel(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenProfile() }
                            .testTag("navProfile"),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "PROFILE",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Player",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
