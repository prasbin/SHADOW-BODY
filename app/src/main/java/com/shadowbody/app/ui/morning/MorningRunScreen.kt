package com.shadowbody.app.ui.morning

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shadowbody.app.domain.model.MorningLogStatus
import com.shadowbody.app.domain.model.MorningStepOutcome
import com.shadowbody.app.domain.morning.MorningRunDetail
import com.shadowbody.app.domain.morning.MorningRunStep
import com.shadowbody.app.domain.morning.MorningTimerPhase
import com.shadowbody.app.domain.morning.MorningTimerState
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/**
 * Phase 5 live run screen, and the result view of a finished run.
 *
 * One step at a time with an explicit decision — complete or skip — because the
 * completion rule depends on every step being *dealt with*. The countdown is
 * advisory: a timed step can be completed early or skipped, and a counted or
 * reminder step has no countdown at all.
 *
 * A run that is no longer open is read-only. History is immutable, so a closed
 * run is shown as a result: the outcome, the attempt, and the outcome of every
 * step, with no control that could change any of it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningRunScreen(
    onFinished: () -> Unit,
    onBack: () -> Unit,
    viewModel: MorningRunViewModel,
) {
    val detail by viewModel.detail.collectAsState()
    val timer by viewModel.timer.state.collectAsState()
    val message by viewModel.message.collectAsState()
    val finished by viewModel.finished.collectAsState()
    val spacing = LocalShadowSpacing.current

    LaunchedEffect(finished) {
        if (finished) onFinished()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (detail?.log?.routineName ?: "MORNING").uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("morningRunTitle"),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        bottomBar = {
            val run = detail
            val editable = run == null || run.log.status == MorningLogStatus.IN_PROGRESS
            if (editable) {
                Column(modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs)) {
                    Button(
                        onClick = viewModel::finish,
                        enabled = detail?.canFinish == true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("morningFinish"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text("FINISH ROUTINE")
                    }
                    OutlinedButton(
                        onClick = viewModel::abandon,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("morningAbandon"),
                    ) {
                        Text("END EARLY")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.md)
                .testTag("morningRunList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            val run = detail
            val isOpen = run == null || run.log.status == MorningLogStatus.IN_PROGRESS
            item {
                SectionHeader(
                    title = if (isOpen) "Progress" else "Result",
                    trailing = run?.let {
                        "${it.dealtCount}/${it.steps.size} · ${it.log.completedSteps} DONE"
                    } ?: "—",
                )
            }

            if (run == null) {
                item {
                    SystemPanel(accentBorder = true) {
                        Text(
                            text = "Loading run…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("morningRunLoading"),
                        )
                    }
                }
            } else if (run.log.status != MorningLogStatus.IN_PROGRESS) {
                item { ResultPanel(run) }
            } else {
                val current = run.currentStep
                if (current == null) {
                    item {
                        SystemPanel {
                            Text(
                                text = "[ EVERY STEP DEALT WITH ]",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.testTag("morningRunAllDone"),
                            )
                            Spacer(modifier = Modifier.height(spacing.xs))
                            Text(
                                text = if (run.log.completedSteps > 0) {
                                    "Finish to record it as a completion."
                                } else {
                                    "Nothing was completed, so this is recorded as " +
                                        "abandoned, not as a success."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    item {
                        CurrentStepPanel(
                            step = current,
                            timer = timer,
                            onComplete = {
                                viewModel.record(current.position, MorningStepOutcome.COMPLETED)
                            },
                            onSkip = {
                                viewModel.record(current.position, MorningStepOutcome.SKIPPED)
                            },
                            onAddTime = { viewModel.timer.addSeconds(EXTRA_SECONDS) },
                            onPause = viewModel.timer::pause,
                            onResume = viewModel.timer::resume,
                            onStepShown = viewModel::onStepShown,
                            onTimerChanged = viewModel::persistTimer,
                        )
                    }
                }

                item { SectionHeader(title = "All steps") }

                items(run.steps, key = { it.position }) { step ->
                    StepRow(
                        step = step,
                        isCurrent = step.position == current?.position,
                        onComplete = {
                            viewModel.record(step.position, MorningStepOutcome.COMPLETED)
                        },
                        onSkip = {
                            viewModel.record(step.position, MorningStepOutcome.SKIPPED)
                        },
                    )
                }
            }

            // The recorded step outcomes are shown for a closed run too: a
            // result has to show what was actually done, step by step.
            if (run != null && !isOpen) {
                item { SectionHeader(title = "Recorded steps") }

                items(run.steps, key = { it.position }) { step ->
                    StepRow(step = step, isCurrent = false, onComplete = {}, onSkip = {})
                }
            }

            val notice = message
            if (notice != null) {
                item {
                    SystemPanel {
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.testTag("morningRunMessage"),
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("morningRunBack"),
                ) {
                    Text(if (isOpen) "LEAVE RUN OPEN" else "BACK TO ACTIVATION")
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.md)) }
        }
    }
}

/**
 * A finished run, read-only.
 *
 * States what was recorded and nothing more: the outcome, the attempt, and
 * whether a step was completed or skipped. A closed run cannot be edited, so
 * no control that would change it is offered.
 */
@Composable
private fun ResultPanel(run: MorningRunDetail) {
    val spacing = LocalShadowSpacing.current
    val log = run.log
    SystemPanel(accentBorder = true) {
        Text(
            text = "[ ${log.status.name} ]",
            style = MaterialTheme.typography.titleSmall,
            color = if (log.status == MorningLogStatus.COMPLETED) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.testTag("morningResultStatus"),
        )
        Spacer(modifier = Modifier.height(spacing.xxs))
        Text(
            text = "${log.dayKey} · ATTEMPT ${log.attempt}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = when (log.status) {
                MorningLogStatus.COMPLETED ->
                    "Recorded as completed: ${log.completedSteps} completed, " +
                        "${log.skippedSteps} skipped. This is stored history and " +
                        "cannot be changed."

                MorningLogStatus.ABANDONED ->
                    if (log.completedSteps > 0) {
                        "Ended early with ${log.completedSteps} completed and " +
                            "${log.skippedSteps} skipped, so it is recorded as " +
                            "abandoned rather than as a success."
                    } else {
                        "Nothing was completed, so this is recorded as abandoned, " +
                            "not as a success."
                    }

                MorningLogStatus.IN_PROGRESS ->
                    "This run is still open."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("morningResultDetail"),
        )
    }
}

@Composable
private fun CurrentStepPanel(
    step: MorningRunStep,
    timer: MorningTimerState,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onAddTime: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStepShown: (Int, Int?) -> Unit,
    onTimerChanged: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    LaunchedEffect(step.position) {
        onStepShown(step.position, step.targetDurationSec)
    }
    LaunchedEffect(timer) {
        onTimerChanged()
    }
    SystemPanel(accentBorder = true) {
        Text(
            text = "STEP ${step.position + 1} · ${step.category.name}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(spacing.xxs))
        Text(
            text = step.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("morningCurrentTitle"),
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = step.instructions,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("morningCurrentInstructions"),
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = "TARGET: ${step.targetText}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.testTag("morningCurrentTarget"),
        )
        if (step.isTimed) {
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = formatSeconds(timer.remainingSec),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("morningTimerValue"),
            )
            if (timer.phase == MorningTimerPhase.FINISHED) {
                Text(
                    text = "TIME'S UP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.testTag("morningTimerDone"),
                )
            }
            Spacer(modifier = Modifier.height(spacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                OutlinedButton(
                    onClick = onAddTime,
                    modifier = Modifier.testTag("morningTimerAdd"),
                ) {
                    Text("+${EXTRA_SECONDS}s")
                }
                if (timer.phase == MorningTimerPhase.RUNNING) {
                    OutlinedButton(
                        onClick = onPause,
                        modifier = Modifier.testTag("morningTimerPause"),
                    ) {
                        Text("PAUSE")
                    }
                } else if (timer.phase == MorningTimerPhase.PAUSED) {
                    OutlinedButton(
                        onClick = onResume,
                        modifier = Modifier.testTag("morningTimerResume"),
                    ) {
                        Text("RESUME")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Button(
                onClick = onComplete,
                modifier = Modifier.testTag("morningComplete"),
            ) {
                Text("COMPLETE")
            }
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.testTag("morningSkip"),
            ) {
                Text("SKIP")
            }
        }
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = "Skip is honest, not a failure. Stop if anything feels wrong.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepRow(
    step: MorningRunStep,
    isCurrent: Boolean,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    SystemPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("morningStep:${step.position}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${step.position + 1}. ${step.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when (step.outcome) {
                        MorningStepOutcome.COMPLETED -> "COMPLETED"
                        MorningStepOutcome.SKIPPED -> "SKIPPED"
                        null -> if (isCurrent) "CURRENT" else "PENDING"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (step.outcome) {
                        MorningStepOutcome.COMPLETED -> MaterialTheme.colorScheme.tertiary
                        MorningStepOutcome.SKIPPED -> MaterialTheme.colorScheme.error
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.testTag("morningStepStatus:${step.position}"),
                )
            }
            if (step.isPending) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                    OutlinedButton(
                        onClick = onComplete,
                        modifier = Modifier.testTag("morningStepComplete:${step.position}"),
                    ) {
                        Text("OK")
                    }
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.testTag("morningStepSkip:${step.position}"),
                    ) {
                        Text("SKIP")
                    }
                }
            }
        }
    }
}

/** `m:ss` for long steps, plain seconds for short ones. */
internal fun formatSeconds(total: Int): String {
    val safe = total.coerceAtLeast(0)
    return if (safe >= 60) "${safe / 60}:${(safe % 60).toString().padStart(2, '0')}" else "$safe"
}

internal const val EXTRA_SECONDS = 30
