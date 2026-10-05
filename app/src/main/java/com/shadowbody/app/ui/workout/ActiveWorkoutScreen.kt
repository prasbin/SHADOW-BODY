package com.shadowbody.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shadowbody.app.data.local.SessionExerciseDetail
import com.shadowbody.app.data.local.SessionSet
import com.shadowbody.app.domain.validation.WorkoutValidator
import com.shadowbody.app.domain.workout.RestPhase
import com.shadowbody.app.domain.workout.RestState
import com.shadowbody.app.ui.components.MeasureField
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/**
 * Active workout: per-set actual logging, completion toggles, rest timer,
 * progress, finish/abandon. All writes persist through the ViewModel.
 */
@Composable
fun ActiveWorkoutScreen(
    detail: com.shadowbody.app.data.local.SessionDetail?,
    rest: RestState,
    onSaveSet: (SessionSet, Int?, Int?, Double?, Boolean) -> Unit,
    onToggleExercise: (Long, Boolean) -> Unit,
    onStartRest: (Int) -> Unit,
    onPauseRest: () -> Unit,
    onResumeRest: () -> Unit,
    onResetRest: () -> Unit,
    onFinish: () -> Unit,
    onAbandon: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    if (detail == null) {
        Column(modifier = Modifier.fillMaxSize().padding(spacing.md)) {
            Text("Loading session…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = spacing.md).testTag("activeList"),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item {
            Spacer(modifier = Modifier.height(spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${detail.completedSets}/${detail.totalSets} SETS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("sessionProgress"),
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                LinearProgressIndicator(
                    progress = { detail.progress },
                    modifier = Modifier.weight(1f).testTag("sessionProgressBar"),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline,
                )
            }
        }

        items(detail.exercises, key = { it.sessionExercise.id }) { exerciseDetail ->
            ExerciseBlock(
                detail = exerciseDetail,
                onSaveSet = onSaveSet,
                onToggleExercise = onToggleExercise,
            )
        }

        item {
            RestTimerPanel(
                rest = rest,
                onStart = onStartRest,
                onPause = onPauseRest,
                onResume = onResumeRest,
                onReset = onResetRest,
            )
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun ExerciseBlock(
    detail: SessionExerciseDetail,
    onSaveSet: (SessionSet, Int?, Int?, Double?, Boolean) -> Unit,
    onToggleExercise: (Long, Boolean) -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    SystemPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${detail.sessionExercise.position + 1}. ${detail.exercise.name.uppercase()}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Checkbox(
                checked = detail.sessionExercise.isCompleted,
                onCheckedChange = { onToggleExercise(detail.sessionExercise.id, it) },
                modifier = Modifier.testTag("exerciseDone:${detail.sessionExercise.position}"),
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.tertiary),
            )
        }
        Spacer(modifier = Modifier.height(spacing.xs))
        detail.sets.forEach { set ->
            SetRow(
                position = detail.sessionExercise.position,
                set = set,
                onSave = { reps, dur, weight, done -> onSaveSet(set, reps, dur, weight, done) },
            )
        }
    }
}

@Composable
private fun SetRow(
    position: Int,
    set: SessionSet,
    onSave: (Int?, Int?, Double?, Boolean) -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    var reps by remember(set.id) { mutableStateOf(set.actualReps?.toString() ?: "") }
    var duration by remember(set.id) { mutableStateOf(set.actualDurationSec?.toString() ?: "") }
    var weight by remember(set.id) { mutableStateOf(set.weightKg?.toString() ?: "") }
    var errors by remember(set.id) { mutableStateOf(emptyMap<String, String>()) }

    val target = if (set.targetDurationSec != null) {
        "TARGET ${set.targetDurationSec}s"
    } else {
        "TARGET ${set.targetReps ?: 0} REPS"
    }
    Column(modifier = Modifier.padding(vertical = spacing.xxs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "SET ${set.setNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(56.dp),
            )
            Text(
                text = target,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Checkbox(
                checked = set.isCompleted,
                onCheckedChange = { done ->
                    val validation = if (done) {
                        WorkoutValidator.validateSetCompletion(reps, duration, weight)
                    } else {
                        WorkoutValidator.validateSetLog(reps, duration, weight)
                    }
                    if (done && validation.isNotEmpty()) {
                        errors = validation
                    } else {
                        errors = emptyMap()
                        onSave(
                            reps.toIntOrNull(), duration.toIntOrNull(),
                            weight.toDoubleOrNull(), done,
                        )
                    }
                },
                modifier = Modifier.testTag("setDone:$position:${set.setNumber}"),
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.tertiary),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            MeasureField(
                label = "Reps", value = reps, onValueChange = { reps = it },
                error = errors["reps"], testTag = "setReps:$position:${set.setNumber}",
                modifier = Modifier.weight(1f),
            )
            MeasureField(
                label = "Time", value = duration, onValueChange = { duration = it },
                unit = "s", error = errors["duration"], testTag = "setTime:$position:${set.setNumber}",
                modifier = Modifier.weight(1f),
            )
            MeasureField(
                label = "Kg", value = weight, onValueChange = { weight = it },
                error = errors["weight"], testTag = "setWeight:$position:${set.setNumber}",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Rest timer panel: start presets + live countdown with pause/resume. */
@Composable
fun RestTimerPanel(
    rest: RestState,
    defaultSec: Int = 60,
    onStart: ((Int) -> Unit)? = null,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    SystemPanel(accent = rest.phase == RestPhase.RUNNING) {
        SectionHeader(title = "Rest", trailing = rest.phase.name)
        when (rest.phase) {
            RestPhase.IDLE -> {
                if (onStart != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        listOf(30, 60, 90).forEach { preset ->
                            OutlinedButton(
                                onClick = { onStart(preset) },
                                modifier = Modifier.weight(1f).testTag("restStart:$preset"),
                            ) {
                                Text("${preset}s")
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Start a rest from any completed set row.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            RestPhase.RUNNING, RestPhase.PAUSED -> {
                Text(
                    text = formatClock(rest.remainingSec),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("restRemaining"),
                )
                LinearProgressIndicator(
                    progress = {
                        if (rest.totalSec == 0) 0f
                        else rest.remainingSec.toFloat() / rest.totalSec.toFloat()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    if (rest.phase == RestPhase.RUNNING) {
                        OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f).testTag("restPause")) {
                            Text("PAUSE")
                        }
                    } else {
                        OutlinedButton(onClick = onResume, modifier = Modifier.weight(1f).testTag("restResume")) {
                            Text("RESUME")
                        }
                    }
                    OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f).testTag("restReset")) {
                        Text("RESET")
                    }
                }
            }
            RestPhase.FINISHED -> {
                Text(
                    text = "REST COMPLETE — NEXT SET",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.testTag("restFinished"),
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                OutlinedButton(onClick = onReset, modifier = Modifier.testTag("restReset")) {
                    Text("DISMISS")
                }
            }
        }
        if (defaultSec != 60 && rest.phase == RestPhase.IDLE && onStart != null) {
            OutlinedButton(
                onClick = { onStart(defaultSec) },
                modifier = Modifier.fillMaxWidth().testTag("restStart:default"),
            ) {
                Text("REST ${defaultSec}s")
            }
        }
    }
}

fun formatClock(totalSec: Int): String {
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%d:%02d".format(minutes, seconds)
}
