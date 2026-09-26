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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shadowbody.app.domain.model.MorningStepCategory
import com.shadowbody.app.domain.validation.MorningRoutineValidator
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SingleChoiceChips
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/**
 * Phase 5 routine editor.
 *
 * Editing the built-in routine is allowed — it is a starting point, not a
 * contract — but the seeded flag is shown so the user knows they are editing
 * the default. Recorded history is never touched by an edit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningRoutineEditorScreen(
    onBack: () -> Unit,
    viewModel: MorningRoutineEditorViewModel,
) {
    val state by viewModel.state.collectAsState()
    val seeded by viewModel.isSeeded.collectAsState()
    val spacing = LocalShadowSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isNew) "NEW ROUTINE" else "EDIT ROUTINE",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("morningEditorTitle"),
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
                .testTag("morningEditorList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item { SectionHeader(title = "Routine") }

            item {
                SystemPanel {
                    if (seeded) {
                        Text(
                            text = "BUILT-IN ROUTINE · EDITS STAY LOCAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.testTag("morningEditorSeeded"),
                        )
                        Spacer(modifier = Modifier.height(spacing.xxs))
                    }
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::setName,
                        label = { Text("Name") },
                        singleLine = true,
                        isError = state.errors.containsKey(MorningRoutineValidator.FIELD_NAME),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("morningEditorName"),
                    )
                    val nameError = state.errors[MorningRoutineValidator.FIELD_NAME]
                    if (nameError != null) {
                        Text(
                            text = nameError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("morningEditorNameError"),
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.xs))
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = viewModel::setDescription,
                        label = { Text("Description (optional)") },
                        isError = state.errors.containsKey(
                            MorningRoutineValidator.FIELD_DESCRIPTION,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("morningEditorDescription"),
                    )
                    val descriptionError = state.errors[MorningRoutineValidator.FIELD_DESCRIPTION]
                    if (descriptionError != null) {
                        Text(
                            text = descriptionError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Steps",
                    trailing = "${state.steps.size} · ORDERED",
                )
            }

            if (state.errors.containsKey(MorningRoutineValidator.FIELD_STEP)) {
                item {
                    Text(
                        text = state.errors.getValue(MorningRoutineValidator.FIELD_STEP),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("morningEditorStepError"),
                    )
                }
            }

            itemsIndexed(state.steps, key = { index, step -> "${step.id}-$index" }) { index, step ->
                StepEditor(
                    index = index,
                    step = step,
                    lastIndex = state.steps.lastIndex,
                    errors = state.errors
                        .filterKeys { it.startsWith("${MorningRoutineEditorViewModel.STEP_ERROR_PREFIX}$index:") }
                        .mapKeys { (key, _) ->
                            key.substringAfter(
                                "${MorningRoutineEditorViewModel.STEP_ERROR_PREFIX}$index:",
                            )
                        },
                    categories = viewModel.categories,
                    onChange = { block -> viewModel.updateStep(index, block) },
                    onRemove = { viewModel.removeStep(index) },
                    onMoveUp = { viewModel.moveStep(index, index - 1) },
                    onMoveDown = { viewModel.moveStep(index, index + 1) },
                )
            }

            item {
                OutlinedButton(
                    onClick = viewModel::addStep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("morningEditorAddStep"),
                ) {
                    Text("ADD STEP")
                }
            }

            item {
                Button(
                    onClick = viewModel::save,
                    enabled = state.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("morningEditorSave"),
                ) {
                    Text(if (state.busy) "SAVING" else "SAVE ROUTINE")
                }
            }

            if (!state.isNew) {
                item {
                    OutlinedButton(
                        onClick = viewModel::deleteRoutine,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("morningEditorDelete"),
                    ) {
                        Text("DELETE ROUTINE")
                    }
                }
                item {
                    Text(
                        text = "Deleting a routine keeps its recorded runs: history stores " +
                            "its own copy of the name and steps.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                            modifier = Modifier.testTag("morningEditorMessage"),
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("morningEditorBack"),
                ) {
                    Text("BACK")
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.md)) }
        }
    }
}

@Composable
private fun StepEditor(
    index: Int,
    step: MorningStepInput,
    lastIndex: Int,
    errors: Map<String, String>,
    categories: List<MorningStepCategory>,
    onChange: ((MorningStepInput) -> MorningStepInput) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    SystemPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "STEP ${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                OutlinedButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.testTag("morningEditorUp:$index"),
                ) {
                    Text("UP")
                }
                OutlinedButton(
                    onClick = onMoveDown,
                    enabled = index < lastIndex,
                    modifier = Modifier.testTag("morningEditorDown:$index"),
                ) {
                    Text("DOWN")
                }
                OutlinedButton(
                    onClick = onRemove,
                    enabled = lastIndex > 0,
                    modifier = Modifier.testTag("morningEditorRemove:$index"),
                ) {
                    Text("REMOVE")
                }
            }
        }
        Spacer(modifier = Modifier.height(spacing.xxs))
        OutlinedTextField(
            value = step.title,
            onValueChange = { value -> onChange { it.copy(title = value) } },
            label = { Text("Step name") },
            singleLine = true,
            isError = errors.containsKey(MorningRoutineValidator.FIELD_TITLE),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("morningEditorStepTitle:$index"),
        )
        errors[MorningRoutineValidator.FIELD_TITLE]?.let { ErrorText(it, "morningEditorStepTitleError:$index") }
        Spacer(modifier = Modifier.height(spacing.xxs))
        OutlinedTextField(
            value = step.instructions,
            onValueChange = { value -> onChange { it.copy(instructions = value) } },
            label = { Text("How to do it") },
            isError = errors.containsKey(MorningRoutineValidator.FIELD_INSTRUCTIONS),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("morningEditorStepInstructions:$index"),
        )
        errors[MorningRoutineValidator.FIELD_INSTRUCTIONS]?.let {
            ErrorText(it, "morningEditorStepInstructionsError:$index")
        }
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = "CATEGORY",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(spacing.xxs))
        SingleChoiceChips(
            options = categories,
            selected = step.category,
            onSelect = { category -> onChange { it.copy(category = category) } },
            labelOf = { it.name },
            tagPrefix = "morningEditorCategory:$index",
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            OutlinedTextField(
                value = step.durationSec?.toString().orEmpty(),
                onValueChange = { raw ->
                    onChange { it.copy(durationSec = raw.toIntOrNull()?.takeIf { sec -> sec > 0 }) }
                },
                label = { Text("Seconds") },
                singleLine = true,
                isError = errors.containsKey(MorningRoutineValidator.FIELD_DURATION),
                modifier = Modifier
                    .weight(1f)
                    .testTag("morningEditorStepDuration:$index"),
            )
            OutlinedTextField(
                value = step.reps?.toString().orEmpty(),
                onValueChange = { raw ->
                    onChange { it.copy(reps = raw.toIntOrNull()?.takeIf { reps -> reps > 0 }) }
                },
                label = { Text("Reps") },
                singleLine = true,
                isError = errors.containsKey(MorningRoutineValidator.FIELD_REPS),
                modifier = Modifier
                    .weight(1f)
                    .testTag("morningEditorStepReps:$index"),
            )
        }
        Spacer(modifier = Modifier.height(spacing.xxs))
        Text(
            text = "Leave both empty for a plain reminder, like a glass of water.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(spacing.xxs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = step.isEnabled,
                onClick = { onChange { it.copy(isEnabled = !it.isEnabled) } },
                label = { Text(if (step.isEnabled) "ENABLED" else "DISABLED") },
                modifier = Modifier.testTag("morningEditorStepEnabled:$index"),
            )
        }
    }
}

@Composable
private fun ErrorText(message: String, tag: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag(tag),
    )
}
