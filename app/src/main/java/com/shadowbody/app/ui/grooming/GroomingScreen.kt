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
import androidx.compose.foundation.layout.size
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
import com.shadowbody.app.data.repository.GroomingDayState
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroomingScreen(
    viewModel: GroomingViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = LocalShadowSpacing.current
    val state = uiState.dayState

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GROOMING",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("groomingTitle"),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("groomingBack")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                .testTag("groomingList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                Spacer(modifier = Modifier.height(spacing.xs))
                SystemPanel(accentBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "ACTIVE DAY: ${uiState.dayKey}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("groomingDayHeader"),
                            )
                            when (state.status) {
                                "NOT_STARTED" -> Text(
                                    text = "No grooming run started today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                "IN_PROGRESS" -> Text(
                                    text = "Run in progress - ${state.completedSteps}/${state.totalSteps} steps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                                "COMPLETED" -> Text(
                                    text = "Today's grooming complete",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                else -> Text(
                                    text = "Previous run abandoned",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Routines", trailing = "${uiState.routines.size} available")
            }

            val routines = uiState.routines
            if (routines.isEmpty()) {
                item {
                    SystemPanel {
                        Text(
                            text = "No grooming routines yet. Create one to begin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("groomingEmptyRoutines"),
                        )
                    }
                }
            } else {
                items(routines, key = { "routine-${it.id}" }) { routine ->
                    SystemPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = true, onClick = { viewModel.startRoutine(routine.id) })
                            .testTag("routineItem:${routine.id}"),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = routine.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = routine.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (routine.isActive) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Active routine",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Recent History", trailing = "${uiState.history.size} entries")
            }

            val history = uiState.history
            if (history.isEmpty()) {
                item {
                    SystemPanel {
                        Text(
                            text = "No grooming history yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("groomingEmptyHistory"),
                        )
                    }
                }
            } else {
                items(history, key = { "log-${it.id}" }) { log ->
                    SystemPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = true, onClick = { })
                            .testTag("groomingLog:${log.id}"),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${log.routineName} - ${log.dayKey}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Status: ${log.status} - ${log.completedSteps}/${log.totalSteps} completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            when (log.status) {
                                "COMPLETED" -> Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Completed",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                "IN_PROGRESS" -> Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "In progress",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                )
                                else -> Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Abandoned",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.md)) }
        }
    }
}
