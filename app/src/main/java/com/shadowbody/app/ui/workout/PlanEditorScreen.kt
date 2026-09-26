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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shadowbody.app.data.local.Exercise
import com.shadowbody.app.domain.validation.PlanInput
import com.shadowbody.app.domain.validation.PlanSlotInput
import com.shadowbody.app.domain.validation.WorkoutValidator
import com.shadowbody.app.ui.components.MeasureField
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/** Plan create/edit: header form + slot list with simple up/down reorder. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    state: PlanEditorUiState,
    library: List<Exercise>,
    onInputChange: (PlanInput) -> Unit,
    onAddExercise: (Long) -> Unit,
    onUpdateSlot: (Int, PlanSlotInput) -> Unit,
    onRemoveSlot: (Int) -> Unit,
    onMoveSlot: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.planId == 0L) "FORGE PLAN" else "REFORGE PLAN",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
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
        bottomBar = {
            Button(
                onClick = onSave,
                enabled = !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.sm)
                    .testTag("planSave"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(if (state.saving) "SEALING…" else "SEAL PLAN")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.md)
                .testTag("editorList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                SystemPanel {
                    OutlinedTextField(
                        value = state.input.name,
                        onValueChange = { onInputChange(state.input.copy(name = it)) },
                        label = { Text("Plan name") },
                        singleLine = true,
                        isError = state.errors[WorkoutValidator.FIELD_NAME] != null,
                        supportingText = {
                            state.errors[WorkoutValidator.FIELD_NAME]?.let { Text(it) }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("planName"),
                        colors = fieldColors(),
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    OutlinedTextField(
                        value = state.input.description,
                        onValueChange = { onInputChange(state.input.copy(description = it)) },
                        label = { Text("Description (optional)") },
                        isError = state.errors[WorkoutValidator.FIELD_DURATION] != null,
                        supportingText = {
                            state.errors[WorkoutValidator.FIELD_DURATION]?.let { Text(it) }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("planDesc"),
                        colors = fieldColors(),
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    OutlinedTextField(
                        value = state.input.targetDurationMin,
                        onValueChange = { onInputChange(state.input.copy(targetDurationMin = it)) },
                        label = { Text("Target minutes (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("planDuration"),
                        colors = fieldColors(),
                    )
                }
            }

            item {
                SectionHeader(title = "Exercises", trailing = "${state.slots.size} SLOTS")
                state.slotErrors["general"]?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                state.errors[WorkoutValidator.FIELD_SLOTS]?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth().testTag("slotAdd"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("ADD EXERCISE")
                }
            }

            itemsIndexed(state.slots, key = { index, slot -> "slot-$index-${slot.exerciseId}" }) { index, slot ->
                val name = library.firstOrNull { it.id == slot.exerciseId }?.name ?: "…"
                SlotEditor(
                    index = index,
                    name = name,
                    slot = slot,
                    errors = state.slotErrors,
                    onChange = { onUpdateSlot(index, it) },
                    onRemove = { onRemoveSlot(index) },
                    onMove = { delta -> onMoveSlot(index, delta) },
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showPicker) {
        ExercisePicker(
            library = library,
            onPick = { onAddExercise(it); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun SlotEditor(
    index: Int,
    name: String,
    slot: PlanSlotInput,
    errors: Map<String, String>,
    onChange: (PlanSlotInput) -> Unit,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    SystemPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${index + 1}. ${name.uppercase()}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onMove(-1) }) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
            }
            IconButton(onClick = { onMove(1) }) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
            }
            IconButton(onClick = onRemove, modifier = Modifier.testTag("slotRemove:$index")) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            MeasureField(
                label = "Sets", value = slot.targetSets,
                onValueChange = { onChange(slot.copy(targetSets = it)) },
                error = errors["$index:sets"],
                testTag = "slotSets:$index", modifier = Modifier.weight(1f),
            )
            MeasureField(
                label = "Reps", value = slot.targetReps,
                onValueChange = { onChange(slot.copy(targetReps = it)) },
                error = errors["$index:reps"],
                testTag = "slotReps:$index", modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            MeasureField(
                label = "Time", value = slot.targetDurationSec,
                onValueChange = { onChange(slot.copy(targetDurationSec = it)) },
                unit = "s, blank=reps", error = errors["$index:duration"],
                testTag = "slotTime:$index", modifier = Modifier.weight(1f),
            )
            MeasureField(
                label = "Rest", value = slot.restSec,
                onValueChange = { onChange(slot.copy(restSec = it)) },
                unit = "s", error = errors["$index:rest"],
                testTag = "slotRest:$index", modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ExercisePicker(
    library: List<Exercise>,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, library) {
        val q = query.trim()
        if (q.isEmpty()) library else library.filter { it.name.contains(q, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SELECT EXERCISE", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("pickSearch"),
                    colors = fieldColors(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    itemsIndexed(filtered, key = { _, e -> e.id }) { _, exercise ->
                        TextButton(
                            onClick = { onPick(exercise.id) },
                            modifier = Modifier.fillMaxWidth().testTag("pick:${exercise.name}"),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = exercise.name.uppercase(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${exercise.muscleGroup.label} · ${exercise.equipment.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (exercise.description.isNotBlank()) {
                                    Text(
                                        text = exercise.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
)
