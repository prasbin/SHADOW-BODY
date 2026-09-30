package com.shadowbody.app.ui.dashboard

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadowbody.app.domain.model.ModuleState
import com.shadowbody.app.domain.model.SystemModule
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.StatCard
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/**
 * Phase 1 dashboard: status window + honest empty-state stats + locked
 * module list. Real values arrive with Phases 2-10; nothing is fabricated —
 * placeholders are labelled as such.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenWorkout: () -> Unit,
    onOpenAdaptive: () -> Unit = {},
    onOpenActivation: () -> Unit = {},
    onOpenNutrition: () -> Unit = {},
    onOpenProgression: () -> Unit = {},
    onOpenGrooming: () -> Unit = {},
    onOpenWardrobe: () -> Unit = {},
    onOpenCoach: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = LocalShadowSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SHADOW BODY",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("appTitle"),
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                .testTag("dashboardList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                // Status window: the signature "system" greeting.
                SystemPanel(accentBorder = true) {
                    Text(
                        text = "[ SYSTEM AWAKENING ]",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = "Welcome, ${state.hunterName}. Foundation online.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Complete future phases to unlock your modules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SectionHeader(title = "Status", trailing = "LV ${state.level}")
            }

            item {
                // Profile status: honest empty state vs configured summary.
                if (state.profileConfigured) {
                    SystemPanel {
                        Text(
                            text = "[ PROFILE ONLINE ]",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.testTag("profileStatusOnline"),
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = "Record sealed. Training phases will build on it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    SystemPanel(accentBorder = true) {
                        Text(
                            text = "[ PROFILE NOT CONFIGURED ]",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("profileStatusMissing"),
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = "Initialize your record to begin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    StatCard(
                        label = "Level",
                        value = state.level.toString().padStart(3, '0'),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Streak",
                        value = "—",
                        footnote = "Starts in Phase 7",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                StatCard(
                    label = "Experience",
                    value = "0 XP",
                    progress = state.xpProgress,
                    footnote = "Progression engine arrives in Phase 7",
                )
            }

            item {
                SectionHeader(title = "Missions", trailing = "${state.modules.size} SEALED")
            }

            items(state.modules, key = { it.id }) { module ->
                // Phase 2: the profile module is live; Phase 3: workout too.
                // Everything else stays sealed.
                if (module.id == "profile") {
                    ModuleRow(
                        module = module,
                        statusOverride = if (state.profileConfigured) "OPEN" else "CREATE",
                        statusAvailable = true,
                        onClick = onOpenProfile,
                    )
                } else if (module.id == "workout") {
                    ModuleRow(
                        module = module,
                        statusOverride = "OPEN",
                        statusAvailable = true,
                        onClick = onOpenWorkout,
                    )
                } else if (module.id == "adaptive") {
                    // Phase 4: adaptive training is live and reachable.
                    ModuleRow(
                        module = module,
                        statusOverride = "OPEN",
                        statusAvailable = true,
                        onClick = onOpenAdaptive,
                    )
                } else if (module.id == "activation") {
                    // Phase 5: morning activation is live and reachable.
                    ModuleRow(
                        module = module,
                        statusOverride = "OPEN",
                        statusAvailable = true,
                        onClick = onOpenActivation,
                    )
                } else if (module.id == "nutrition") {
                    // Phase 6: nutrition is live and reachable.
                    ModuleRow(
                        module = module,
                        statusOverride = "OPEN",
                        statusAvailable = true,
                        onClick = onOpenNutrition,
                    )
                } else if (module.id == "progression") {
                    // Phase 7: progression is live and reachable.
                    ModuleRow(
                        module = module,
                        statusOverride = "OPEN",
                        statusAvailable = true,
                        onClick = onOpenProgression,
                    )
                } else if (module.id == "grooming") {
                    // Phase 8: grooming is live and reachable.
                    ModuleRow(
                        module = module,
                        statusOverride = "OPEN",
                        statusAvailable = true,
                        onClick = onOpenGrooming,
                    )
                } else if (module.id == "wardrobe") {
                    ModuleRow(
                        module = module,
                        statusOverride = "OPEN",
                        statusAvailable = true,
                        onClick = onOpenWardrobe,
                    )
                } else {
                    ModuleRow(module = module)
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.md)) }
        }
    }
}

@Composable
private fun ModuleRow(
    module: SystemModule,
    statusOverride: String? = null,
    statusAvailable: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val spacing = LocalShadowSpacing.current
    SystemPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
                .testTag("module:${module.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = module.title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = module.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val isOpen = statusAvailable || module.state == ModuleState.AVAILABLE
                Text(
                    text = statusOverride
                        ?: if (module.state == ModuleState.AVAILABLE) "OPEN" else "SEALED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOpen) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (module.state == ModuleState.LOCKED) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
