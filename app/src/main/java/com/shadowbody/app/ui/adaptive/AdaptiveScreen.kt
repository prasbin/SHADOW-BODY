package com.shadowbody.app.ui.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shadowbody.app.data.local.ReadinessReport
import com.shadowbody.app.domain.adaptive.AdaptationLimits
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/**
 * Phase 4 adaptive training screen.
 *
 * Shows three things and nothing else: the readiness check-in the engine
 * actually reads, the recommended session, and the reason behind every target.
 * The screen never invents a number — everything shown is stored data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveScreen(
    onBack: () -> Unit,
    viewModel: AdaptiveViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val readiness by viewModel.latestReadiness.collectAsState(initial = null)
    val input by viewModel.readinessInput.collectAsState()
    val saved by viewModel.readinessSaved.collectAsState()
    val spacing = LocalShadowSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ADAPTIVE",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("adaptiveTitle"),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
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
                .testTag("adaptiveList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item { SectionHeader(title = "Readiness check-in") }

            item {
                ReadinessPanel(
                    input = input,
                    latest = readiness,
                    saved = saved,
                    onFatigue = viewModel::setFatigue,
                    onSoreness = viewModel::setSoreness,
                    onNotes = viewModel::setNotes,
                    onSave = viewModel::saveReadiness,
                )
            }

            item { SectionHeader(title = "Recommended session") }

            item {
                if (!state.hasRecommendation) {
                    SystemPanel(accent = true) {
                        Text(
                            text = "[ NO RECOMMENDATION ]",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("adaptiveEmpty"),
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = "Generate from your plans, equipment and " +
                                "recorded history. Nothing is sent anywhere.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    RecommendationPanel(
                        state = state,
                        onAdopt = viewModel::adopt,
                        onSkip = viewModel::markMissed,
                        onDismiss = viewModel::dismiss,
                    )
                }
            }

            item {
                Button(
                    onClick = viewModel::generate,
                    enabled = !state.busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adaptiveGenerate"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(if (state.busy) "WORKING" else "GENERATE RECOMMENDATION")
                }
            }

            val notice = state.message
            if (notice != null) {
                item {
                    SystemPanel {
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.testTag("adaptiveMessage"),
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adaptiveBack"),
                ) {
                    Text("BACK")
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.md)) }
        }
    }
}

@Composable
private fun ReadinessPanel(
    input: ReadinessInput,
    latest: ReadinessReport?,
    saved: Boolean,
    onFatigue: (Int) -> Unit,
    onSoreness: (Int) -> Unit,
    onNotes: (String) -> Unit,
    onSave: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    SystemPanel {
        Text(
            text = "HOW ARE YOU TODAY?",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        RatingRow(
            label = "Fatigue",
            selected = input.fatigue,
            tag = "readinessFatigue",
            error = input.errors["fatigue"],
            onSelect = onFatigue,
        )
        Spacer(modifier = Modifier.height(spacing.xxs))
        RatingRow(
            label = "Soreness",
            selected = input.soreness,
            tag = "readinessSoreness",
            error = input.errors["soreness"],
            onSelect = onSoreness,
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        OutlinedTextField(
            value = input.notes,
            onValueChange = onNotes,
            label = { Text("Note (optional)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("readinessNotes"),
        )
        val noteError = input.errors["notes"]
        if (noteError != null) {
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = noteError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(spacing.xs))
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("readinessSave"),
            enabled = !saved,
        ) {
            Text(if (saved) "RECORDED" else "RECORD CHECK-IN")
        }
        if (latest != null) {
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = "Last: fatigue ${latest.fatigue}/5 · soreness ${latest.soreness}/5",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("readinessLast"),
            )
        }
    }
}

@Composable
private fun RatingRow(
    label: String,
    selected: Int?,
    tag: String,
    error: String? = null,
    onSelect: (Int) -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(spacing.xxs))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            for (value in AdaptationLimits.READINESS_MIN..AdaptationLimits.READINESS_MAX) {
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(value.toString()) },
                    modifier = Modifier.testTag("$tag:$value"),
                )
            }
        }
        if (error != null) {
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("$tag:error"),
            )
        }
    }
}

@Composable
private fun RecommendationPanel(
    state: AdaptiveUiState,
    onAdopt: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    SystemPanel(accent = true) {
        Text(
            text = state.recommendationName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("adaptiveName"),
        )
        Spacer(modifier = Modifier.height(spacing.xxs))
        Text(
            text = "~${state.estimatedMinutes} MIN · ${state.status?.name ?: "—"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = state.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(spacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            state.exercises.forEach { row ->
                Column {
                    Text(
                        text = "${row.position + 1}. ${row.targetText} · REST ${row.restSec}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("adaptiveTarget:${row.exerciseId}"),
                    )
                    Text(
                        text = row.reasonText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("adaptiveReason:${row.exerciseId}"),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Button(
                onClick = onAdopt,
                enabled = state.isOpen && !state.busy,
                modifier = Modifier.testTag("adaptiveAdopt"),
            ) {
                Text("ADOPT")
            }
            OutlinedButton(
                onClick = onSkip,
                enabled = state.isOpen && !state.busy,
                modifier = Modifier.testTag("adaptiveSkip"),
            ) {
                Text("SKIP")
            }
            OutlinedButton(
                onClick = onDismiss,
                enabled = state.isOpen && !state.busy,
                modifier = Modifier.testTag("adaptiveDismiss"),
            ) {
                Text("DISMISS")
            }
        }
    }
}

