package com.shadowbody.app.ui.coach

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
import androidx.compose.material.icons.filled.Refresh
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
import com.shadowbody.app.domain.coach.CoachRecommendation
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachScreen(
    viewModel: CoachViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val summary by viewModel.summary.collectAsState()
    val spacing = LocalShadowSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "BODY COACH",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("coachTitle"),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("coachBack")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }, modifier = Modifier.testTag("coachRefresh")) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
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
                .testTag("coachList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                SystemPanel(accentBorder = true) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "SYSTEM COACH ONLINE",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("coachStatus"),
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = "Deterministic local recommendations. No cloud. No AI.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            val topRec = summary?.topRecommendation
            if (topRec != null) {
                item {
                    SectionHeader(title = "TOP PRIORITY", trailing = "P${topRec.priority}")
                }

                item {
                    SystemPanel(accentBorder = true) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (topRec.actionRoute != null) {
                                        Modifier.clickable { onNavigate(topRec.actionRoute) }
                                    } else {
                                        Modifier
                                    }
                                )
                                .testTag("topRecommendation"),
                        ) {
                            Text(
                                text = topRec.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(spacing.xs))
                            Text(
                                text = topRec.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(spacing.xs))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = topRec.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                                if (topRec.actionRoute != null) {
                                    Text(
                                        text = "TAP TO OPEN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val allRecs = summary?.recommendations ?: emptyList()
            if (allRecs.size > 1) {
                item {
                    SectionHeader(title = "ALL RECOMMENDATIONS", trailing = "${allRecs.size} total")
                }

                items(allRecs.drop(1), key = { it.id }) { rec ->
                    SystemPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (rec.actionRoute != null) {
                                    Modifier.clickable { onNavigate(rec.actionRoute) }
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("recommendation:${rec.id}"),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = rec.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "P${rec.priority}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                            Spacer(modifier = Modifier.height(spacing.xs))
                            Text(
                                text = rec.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (allRecs.isEmpty()) {
                item {
                    SystemPanel {
                        Text(
                            text = "No recommendations at this time. Complete your profile to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("coachEmpty"),
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.md)) }
        }
    }
}
