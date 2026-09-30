package com.shadowbody.app.ui.grooming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.shadowbody.app.data.local.GroomingRoutineStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroomingRoutineEditorScreen(
    viewModel: GroomingRoutineEditorViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val spacing = com.shadowbody.app.ui.theme.LocalShadowSpacing.current
    val isEditing = state.isEditing

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "EDIT ROUTINE" else "NEW ROUTINE",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("groomingEditorTitle"),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("groomingEditorBack")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.saved) {
                        IconButton(onClick = viewModel::save, modifier = Modifier.testTag("groomingEditorSave")) {
                            Icon(Icons.Filled.Check, contentDescription = "Save")
                        }
                    }
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
                .testTag("groomingEditorList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                com.shadowbody.app.ui.components.SystemPanel {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = viewModel::updateName,
                            label = { Text("Routine Name") },
                            modifier = Modifier.fillMaxWidth().testTag("inputRoutineName"),
                            isError = state.name.trim().isEmpty() && state.saved,
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        OutlinedTextField(
                            value = state.description,
                            onValueChange = viewModel::updateDescription,
                            label = { Text("Description (optional)") },
                            modifier = Modifier.fillMaxWidth().testTag("inputRoutineDescription"),
                        )
                    }
                }
            }

            item {
                com.shadowbody.app.ui.components.SectionHeader(title = "Steps", trailing = "${state.steps.size} steps")
            }

            if (state.steps.isEmpty()) {
                item {
                    com.shadowbody.app.ui.components.SystemPanel {
                        Text(
                            text = "No steps yet. Add your first step.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("groomingEmptySteps"),
                        )
                    }
                }
            } else {
                items(state.steps, key = { "step-${it.position}" }) { step ->
                    com.shadowbody.app.ui.components.SystemPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editorStep:${step.position}"),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = "Drag to reorder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = spacing.xs),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "STEP ${step.position + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = step.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (step.instructions.isNotBlank()) {
                                    Text(
                                        text = step.instructions,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (step.targetDurationSec != null) {
                                    Text(
                                        text = "Duration: ${formatDuration(step.targetDurationSec!!)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                            ) {
                                IconButton(
                                    onClick = { },
                                    modifier = Modifier.testTag("editStep:${step.position}"),
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit step", tint = MaterialTheme.colorScheme.tertiary)
                                }
                                IconButton(
                                    onClick = { viewModel.removeStep(step.position) },
                                    modifier = Modifier.testTag("deleteStep:${step.position}"),
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete step", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = viewModel::addStep,
                    modifier = Modifier.fillMaxWidth().testTag("addStepButton"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add step")
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text("ADD STEP")
                }
            }

            if (state.message != null) {
                item {
                    Text(
                        text = state.message!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.md)
                            .testTag("editorMessage"),
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.md)) }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    return if (seconds >= 60) {
        "${seconds / 60}m ${seconds % 60}s"
    } else {
        "${seconds}s"
    }
}
