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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.shadowbody.app.data.local.PlanDetail
import com.shadowbody.app.domain.model.Equipment
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/** Plan detail: slots with targets, equipment flags, start/edit/delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    detail: PlanDetail?,
    ownedEquipment: Set<Equipment>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        (detail?.plan?.name ?: "PLAN").uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("planEdit")) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit plan")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("planDelete")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete plan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (detail == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(spacing.md),
            ) {
                Text("Plan not found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            if (detail.plan.description.isNotBlank()) {
                item {
                    SystemPanel {
                        Text(
                            text = detail.plan.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            item {
                SectionHeader(
                    title = "Exercises",
                    trailing = "${detail.slots.size} SLOTS" +
                        (detail.plan.targetDurationMin?.let { " · ~$it MIN" } ?: ""),
                )
            }
            items(detail.slots, key = { it.slot.id }) { slot ->
                val missing = slot.exercise.equipment != Equipment.BODYWEIGHT &&
                    slot.exercise.equipment != Equipment.NONE &&
                    !ownedEquipment.contains(slot.exercise.equipment)
                SystemPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${slot.slot.position + 1}. ${slot.exercise.name.uppercase()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = targetSummary(slot.slot.targetSets, slot.slot.targetReps, slot.slot.targetDurationSec) +
                                    " · REST ${slot.slot.restSec}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (missing) {
                                Text(
                                    text = "⚠ NEEDS ${slot.exercise.equipment.label.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("missing:${slot.slot.id}"),
                                )
                            }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = onStart,
                    enabled = detail.slots.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().testTag("planStart"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("BEGIN WORKOUT")
                }
                Spacer(modifier = Modifier.height(spacing.md))
            }
        }
    }
}

fun targetSummary(sets: Int, reps: Int?, durationSec: Int?): String {
    val target = if (durationSec != null) "${durationSec}s" else "${reps ?: 0} REPS"
    return "$sets × $target"
}
