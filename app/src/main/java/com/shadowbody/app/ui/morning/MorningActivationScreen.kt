package com.shadowbody.app.ui.morning

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shadowbody.app.data.local.MorningRoutine
import com.shadowbody.app.data.local.MorningRoutineLog
import com.shadowbody.app.domain.model.MorningLogStatus
import com.shadowbody.app.domain.morning.MorningDayState
import com.shadowbody.app.domain.morning.MorningDayStatus
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/**
 * Phase 5 morning activation home.
 *
 * Three sections, all reading stored state: today's card, the routine list and
 * recent history. Nothing is inferred and nothing is claimed before it is
 * recorded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningActivationScreen(
    onBack: () -> Unit,
    onOpenRun: (Long) -> Unit,
    onEdit: (Long?) -> Unit,
    viewModel: MorningActivationViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val day by viewModel.dayState.collectAsState()
    val spacing = LocalShadowSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MORNING ACTIVATION",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("morningTitle"),
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
                .testTag("morningList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item { SectionHeader(title = "Today", trailing = state.dayKey) }

            item {
                TodayPanel(
                    day = day,
                    onResume = onOpenRun,
                )
            }

            item {
                Button(
                    onClick = viewModel::startToday,
                    enabled = !day.isFinished,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("morningStart"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        when {
                            day.status == MorningDayStatus.COMPLETED -> "COMPLETE TODAY"
                            day.status == MorningDayStatus.IN_PROGRESS -> "RESUME ROUTINE"
                            day.status == MorningDayStatus.ABANDONED -> "START AGAIN"
                            else -> "START ROUTINE"
                        },
                    )
                }
            }

            item { SectionHeader(title = "Routines", trailing = "${state.routines.size}") }

            item {
                Button(
                    onClick = { onEdit(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("morningNewRoutine"),
                ) {
                    Text("NEW ROUTINE")
                }
            }

            items(state.routines, key = { it.id }) { routine ->
                RoutineRow(
                    routine = routine,
                    onOpen = { viewModel.startRoutine(routine.id) },
                    onEdit = { onEdit(routine.id) },
                )
            }

            item { SectionHeader(title = "History", trailing = "LAST ${state.history.size}") }

            if (state.history.isEmpty()) {
                item {
                    SystemPanel(accentBorder = true) {
                        Text(
                            text = "[ NO RUNS YET ]",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("morningHistoryEmpty"),
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = "Completed and abandoned runs appear here with the " +
                                "step outcomes you recorded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(state.history, key = { it.id }) { log ->
                    HistoryRow(log = log, onOpen = { onOpenRun(log.id) })
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
                            modifier = Modifier.testTag("morningMessage"),
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("morningBack"),
                ) {
                    Text("BACK")
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.md)) }
        }
    }
}

@Composable
private fun TodayPanel(
    day: MorningDayState,
    onResume: (Long) -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    SystemPanel(accentBorder = true) {
        Text(
            text = "[ ${day.status.name} ]",
            style = MaterialTheme.typography.titleSmall,
            color = when (day.status) {
                MorningDayStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.testTag("morningDayStatus"),
        )
        if (day.routineName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = day.routineName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("morningDayRoutine"),
            )
        }
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = when (day.status) {
                MorningDayStatus.NOT_STARTED ->
                    "Nothing recorded yet today. One tap starts the routine."

                MorningDayStatus.IN_PROGRESS ->
                    "Run in progress: ${day.completedSteps} completed, " +
                        "${day.skippedSteps} skipped of ${day.totalSteps}."

                MorningDayStatus.COMPLETED ->
                    "Today's routine is complete. Recorded steps are immutable."

                MorningDayStatus.ABANDONED ->
                    "Last attempt ended early. You can run it again as a new attempt."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("morningDayDetail"),
        )
        if (day.totalSteps > 0) {
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = "${day.completedSteps + day.skippedSteps}/${day.totalSteps} STEPS " +
                    "DEALT WITH",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("morningDayProgress"),
            )
        }
        val activeLogId = day.activeLogId
        if (day.status == MorningDayStatus.IN_PROGRESS && activeLogId != null) {
            Spacer(modifier = Modifier.height(spacing.xs))
            OutlinedButton(
                onClick = { onResume(activeLogId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("morningResume"),
            ) {
                Text("RESUME")
            }
        }
        if (day.status != MorningDayStatus.IN_PROGRESS) {
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = "Movement only. Stop if anything feels wrong.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RoutineRow(
    routine: MorningRoutine,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    SystemPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .testTag("morningRoutine:${routine.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (routine.isActive) "ACTIVE" else "INACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (routine.isActive) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (routine.description.isNotBlank()) {
                    Text(
                        text = routine.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(spacing.xxs))
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.testTag("morningRoutineStart:${routine.id}"),
                ) {
                    Text("RUN")
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(log: MorningRoutineLog, onOpen: () -> Unit) {
    val spacing = LocalShadowSpacing.current
    SystemPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .testTag("morningHistory:${log.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.routineName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${log.dayKey} · ATTEMPT ${log.attempt} · " +
                        "${log.completedSteps} DONE / ${log.skippedSteps} SKIPPED",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = when (log.status) {
                    MorningLogStatus.COMPLETED -> "DONE"
                    MorningLogStatus.ABANDONED -> "PARTIAL"
                    MorningLogStatus.IN_PROGRESS -> "OPEN"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (log.status == MorningLogStatus.COMPLETED) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.testTag("morningHistoryStatus:${log.id}"),
            )
        }
        Spacer(modifier = Modifier.height(spacing.xxs))
    }
}
