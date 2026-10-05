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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shadowbody.app.data.local.SessionDetail
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.StatCard
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.profile.formatRecordedAt
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/** Post-workout debrief from actual stored rows. Nothing is estimated. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutResultScreen(
    detail: SessionDetail?,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QUEST COMPLETE", style = MaterialTheme.typography.headlineSmall) },
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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.sm)
                    .testTag("resultDone"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("RETURN TO HALL")
            }
        },
    ) { padding ->
        if (detail == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(spacing.md)) {
                Text("Loading result…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                SystemPanel(accent = true) {
                    Text(
                        text = "[ ${detail.session.name.uppercase()} — ${detail.session.status.name} ]",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("resultTitle"),
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = formatRecordedAt(detail.session.startedAt) +
                            (detail.durationMin?.let { " · ${it} MIN" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    StatCard(
                        label = "Exercises",
                        value = "${detail.completedExercises}/${detail.exercises.size}",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Sets",
                        value = "${detail.completedSets}/${detail.totalSets}",
                        modifier = Modifier.weight(1f).testTag("resultSets"),
                    )
                }
            }
            item {
                LinearProgressIndicator(
                    progress = { detail.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.outline,
                )
            }
            item { SectionHeader(title = "Breakdown") }
            items(detail.exercises, key = { it.sessionExercise.id }) { row ->
                SystemPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.exercise.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${row.sets.count { it.isCompleted }}/${row.sets.size} SETS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(spacing.md))
            }
        }
    }
}

