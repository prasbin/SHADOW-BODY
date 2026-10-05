package com.shadowbody.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shadowbody.app.data.local.WorkoutPlan
import com.shadowbody.app.data.local.WorkoutSession
import com.shadowbody.app.domain.model.SessionStatus
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.StatCard
import com.shadowbody.app.ui.components.SystemCard
import com.shadowbody.app.ui.components.SystemDivider
import com.shadowbody.app.ui.components.SystemStatBlock
import com.shadowbody.app.ui.profile.formatRecordedAt
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/** Workout hub: plans + recent history. Empty states guide first use. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutListScreen(
    plans: List<WorkoutPlan>,
    sessions: List<WorkoutSession>,
    completedCount: Int,
    seededCount: Int?,
    onBack: () -> Unit,
    onOpenPlan: (Long) -> Unit,
    onNewPlan: () -> Unit,
    onOpenResult: (Long) -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TRAINING HALL", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                onClick = onNewPlan,
                modifier = Modifier.testTag("planNew"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New plan")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.md)
                .testTag("hallList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    StatCard(
                        label = "Plans",
                        value = plans.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Completed",
                        value = completedCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Exercises",
                        value = seededCount?.toString() ?: "…",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item { SectionHeader(title = "Plans", trailing = "${plans.size} ACTIVE") }
            if (plans.isEmpty()) {
                item {
                    SystemCard(accent = true) {
                        Text(
                            text = "[ TRAINING SCHEDULE ]",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = "Your training schedule will appear here once configured.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(plans, key = { "plan-${it.id}" }) { plan ->
                    PlanRow(plan = plan, onClick = { onOpenPlan(plan.id) })
                }
            }

            item { SectionHeader(title = "History", trailing = "${sessions.size} RECENT") }
            if (sessions.isEmpty()) {
                item {
                    SystemCard {
                        Text(
                            text = "No workouts recorded yet. Finished sessions appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(sessions, key = { "session-${it.id}" }) { session ->
                    SessionRow(session = session, onClick = { onOpenResult(session.id) })
                }
            }

            item {
                SystemCard {
                    Text(
                        text = "Train within your limits. Stop if you feel pain, dizziness, or " +
                            "unusual discomfort, and consult a qualified professional " +
                            "about medical concerns.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun PlanRow(plan: WorkoutPlan, onClick: () -> Unit) {
    SystemCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (plan.description.isNotBlank()) {
                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            androidx.compose.material3.Button(
                onClick = onClick,
                modifier = Modifier.testTag("plan:${plan.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("OPEN")
            }
        }
    }
}

@Composable
private fun SessionRow(session: WorkoutSession, onClick: () -> Unit) {
    SystemCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatRecordedAt(session.startedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val (label, color) = when (session.status) {
                    SessionStatus.COMPLETED -> "DONE" to MaterialTheme.colorScheme.tertiary
                    SessionStatus.IN_PROGRESS -> "ACTIVE" to MaterialTheme.colorScheme.primary
                    SessionStatus.ABANDONED -> "ENDED" to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    modifier = Modifier.testTag("session:${session.id}:$label"),
                )
                androidx.compose.material3.Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("VIEW")
                }
            }
        }
    }
}