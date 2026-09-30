package com.shadowbody.app.ui.grooming

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.shadowbody.app.data.repository.GroomingRunDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroomingRunScreen(
    viewModel: GroomingRunViewModel,
    onFinished: () -> Unit,
    onBack: () -> Unit,
) {
    val detail by viewModel.detail.collectAsState()
    val spacing = com.shadowbody.app.ui.theme.LocalShadowSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GROOMING RUN",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("groomingRunTitle"),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("groomingRunBack")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (detail != null) {
                        IconButton(onClick = viewModel::finish, modifier = Modifier.testTag("groomingRunFinish")) {
                            Icon(Icons.Filled.Check, contentDescription = "Finish")
                        }
                        IconButton(onClick = viewModel::abandon, modifier = Modifier.testTag("groomingRunAbandon")) {
                            Icon(Icons.Filled.Close, contentDescription = "Abandon")
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
        val steps = detail?.steps ?: emptyList()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.md)
                .testTag("groomingRunList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items(steps, key = { "step-${it.position}" }) { step ->
                val isDone = step.outcome != null
                com.shadowbody.app.ui.components.SystemPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isDone, onClick = { viewModel.onStepShown(step.position, step.targetDurationSec) })
                        .testTag("groomingStep:${step.position}"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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
                                when (step.outcome) {
                                    "COMPLETED" -> Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Completed",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    "SKIPPED" -> Text(
                                        text = "SKIPPED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    else -> Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Pending",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    )
                                }
                            }
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("groomingStepTitle:${step.position}"),
                            )
                            Text(
                                text = step.instructions,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (step.targetDurationSec != null) {
                                Text(
                                    text = "Target: ${formatDuration(step.targetDurationSec!!)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                        if (!isDone) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                            ) {
                                androidx.compose.material3.Button(
                                    onClick = { viewModel.record(step.position, "COMPLETED") },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    modifier = Modifier.testTag("completeStep:${step.position}"),
                                ) {
                                    Text("COMPLETE")
                                }
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { viewModel.record(step.position, "SKIPPED") },
                                    modifier = Modifier.testTag("skipStep:${step.position}"),
                                ) {
                                    Text("SKIP")
                                }
                            }
                        }
                    }
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
