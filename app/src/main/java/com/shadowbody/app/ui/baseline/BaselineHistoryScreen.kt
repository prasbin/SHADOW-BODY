package com.shadowbody.app.ui.baseline

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shadowbody.app.data.local.BaselineRecord
import com.shadowbody.app.domain.validation.BaselineInput
import com.shadowbody.app.domain.validation.BaselineValidator
import com.shadowbody.app.ui.components.MeasureField
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.profile.formatCm
import com.shadowbody.app.ui.profile.formatKg
import com.shadowbody.app.ui.profile.formatRecordedAt
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/** Baseline history + input dialog. Empty state explains the first record. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaselineHistoryScreen(
    history: List<BaselineRecord>,
    inputState: BaselineInputUiState,
    onOpenDialog: () -> Unit,
    onCloseDialog: () -> Unit,
    onInputChange: (BaselineInput) -> Unit,
    onSave: () -> Unit,
    onDelete: (BaselineRecord) -> Unit,
    onBack: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BASELINE ARCHIVE", style = MaterialTheme.typography.headlineSmall) },
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
            FloatingActionButton(
                onClick = onOpenDialog,
                modifier = Modifier.testTag("baselineAdd"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add baseline")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (history.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                SystemPanel(accent = true) {
                    Text(
                        text = "[ ARCHIVE EMPTY ]",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = "Record your first baseline to begin transformation history.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                item { SectionHeader(title = "Records", trailing = "${history.size} TOTAL") }
                items(history, key = { it.id }) { record ->
                    BaselineCard(record = record, onDelete = { onDelete(record) })
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (inputState.showDialog) {
        BaselineInputDialog(
            state = inputState,
            onChange = onInputChange,
            onDismiss = onCloseDialog,
            onSave = onSave,
        )
    }
}

@Composable
private fun BaselineCard(record: BaselineRecord, onDelete: () -> Unit) {
    SystemPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatRecordedAt(record.recordedAt),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = onDelete, modifier = Modifier.testTag("baselineDelete:${record.id}")) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete baseline",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        RecordLine("Weight", formatKg(record.weightKg))
        RecordLine("Chest", formatCm(record.chestCm))
        RecordLine("Waist", formatCm(record.waistCm))
        RecordLine("Hips", formatCm(record.hipsCm))
        RecordLine("Biceps", formatCm(record.bicepsCm))
        RecordLine("Thigh", formatCm(record.thighCm))
        RecordLine("Body fat", record.bodyFatPct?.let { "$it %" } ?: "—")
        if (!record.notes.isNullOrBlank()) {
            Text(
                text = record.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecordLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BaselineInputDialog(
    state: BaselineInputUiState,
    onChange: (BaselineInput) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    val input = state.input
    val errors = state.errors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NEW BASELINE", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                errors[BaselineValidator.FIELD_GENERAL]?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    MeasureField(
                        label = "Weight", value = input.weightKg,
                        onValueChange = { onChange(input.copy(weightKg = it)) },
                        unit = "kg", error = errors[BaselineValidator.FIELD_WEIGHT],
                        testTag = "baselineWeight", modifier = Modifier.weight(1f),
                    )
                    MeasureField(
                        label = "Body fat", value = input.bodyFatPct,
                        onValueChange = { onChange(input.copy(bodyFatPct = it)) },
                        unit = "%", error = errors[BaselineValidator.FIELD_BODY_FAT],
                        testTag = "baselineBodyFat", modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    MeasureField(
                        label = "Chest", value = input.chestCm,
                        onValueChange = { onChange(input.copy(chestCm = it)) },
                        unit = "cm", error = errors[BaselineValidator.FIELD_CHEST],
                        testTag = "baselineChest", modifier = Modifier.weight(1f),
                    )
                    MeasureField(
                        label = "Waist", value = input.waistCm,
                        onValueChange = { onChange(input.copy(waistCm = it)) },
                        unit = "cm", error = errors[BaselineValidator.FIELD_WAIST],
                        testTag = "baselineWaist", modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    MeasureField(
                        label = "Hips", value = input.hipsCm,
                        onValueChange = { onChange(input.copy(hipsCm = it)) },
                        unit = "cm", error = errors[BaselineValidator.FIELD_HIPS],
                        testTag = "baselineHips", modifier = Modifier.weight(1f),
                    )
                    MeasureField(
                        label = "Biceps", value = input.bicepsCm,
                        onValueChange = { onChange(input.copy(bicepsCm = it)) },
                        unit = "cm", error = errors[BaselineValidator.FIELD_BICEPS],
                        testTag = "baselineBiceps", modifier = Modifier.weight(1f),
                    )
                }
                MeasureField(
                    label = "Thigh", value = input.thighCm,
                    onValueChange = { onChange(input.copy(thighCm = it)) },
                    unit = "cm", error = errors[BaselineValidator.FIELD_THIGH],
                    testTag = "baselineThigh",
                )
                OutlinedTextField(
                    value = input.notes,
                    onValueChange = { onChange(input.copy(notes = it)) },
                    label = { Text("Notes (optional)") },
                    isError = errors[BaselineValidator.FIELD_NOTES] != null,
                    supportingText = {
                        errors[BaselineValidator.FIELD_NOTES]?.let { Text(it) }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("baselineNotes"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !state.saving,
                modifier = Modifier.testTag("baselineSave"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("SEAL RECORD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
    )
}

