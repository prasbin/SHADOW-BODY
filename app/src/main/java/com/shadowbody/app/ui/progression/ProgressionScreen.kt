package com.shadowbody.app.ui.progression

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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.shadowbody.app.data.local.Achievement
import com.shadowbody.app.data.local.Attribute
import com.shadowbody.app.data.local.Streak
import com.shadowbody.app.data.local.XpTransaction
import com.shadowbody.app.domain.progression.ProgressionEngine.ProgressionSummary
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.StatCard
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressionScreen(
    viewModel: ProgressionViewModel,
    onBack: () -> Unit,
) {
    val summary by viewModel.summary.collectAsState()
    val spacing = LocalShadowSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PROGRESSION SYSTEM",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("progressionTitle"),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("progressionBack")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::forceProcessProgression, modifier = Modifier.testTag("progressionRefresh")) {
                        Icon(Icons.Filled.Star, contentDescription = "Refresh Progression")
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
                .testTag("progressionList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            // Level & XP Header
            item {
                SystemPanel(accentBorder = true) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "LEVEL ${summary.level}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("progressionLevel"),
                                )
                                Text(
                                    text = "Total XP: ${summary.totalXp}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(spacing.xs))
                        // XP Progress Bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("progressionXpBar"),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${summary.xpInCurrentLevel} / ${summary.xpInCurrentLevel + summary.xpToNextLevel} XP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "${(summary.levelProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = summary.levelProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }

            // Attributes
            item {
                SectionHeader(title = "Attributes")
            }

            item {
                val attrs = summary.attribute
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    AttributeCard("Strength", attrs.strength, Modifier.weight(1f))
                    AttributeCard("Endurance", attrs.endurance, Modifier.weight(1f))
                    AttributeCard("Discipline", attrs.discipline, Modifier.weight(1f))
                    AttributeCard("Recovery", attrs.recovery, Modifier.weight(1f))
                    AttributeCard("Nutrition", attrs.nutrition, Modifier.weight(1f))
                }
            }

            // Streaks
            item {
                SectionHeader(title = "Streaks")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    StatCard(
                        label = "Current Streak",
                        value = "${summary.streak.currentStreak} days",
                        footnote = "Consecutive active days",
                        modifier = Modifier.weight(1f).testTag("currentStreakCard"),
                    )
                    StatCard(
                        label = "Longest Streak",
                        value = "${summary.streak.longestStreak} days",
                        footnote = "Personal best",
                        modifier = Modifier.weight(1f).testTag("longestStreakCard"),
                    )
                }
            }

            // Achievements
            item {
                SectionHeader(title = "Achievements", trailing = "${summary.achievements.count { it.unlocked }} / ${summary.achievements.size}")
            }

            item {
                val achievements = summary.achievements
                if (achievements.isEmpty()) {
                    SystemPanel {
                        Text(
                            text = "No achievements defined.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("progressionEmptyAchievements"),
                        )
                    }
                } else {
                    achievements.forEach { ach ->
                        SystemPanel {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ach.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (ach.unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("achievementName:${ach.id}"),
                                    )
                                    Text(
                                        text = ach.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (ach.progressTarget > 1 && !ach.unlocked) {
                                        Text(
                                            text = "Progress: ${ach.progressCurrent} / ${ach.progressTarget}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.testTag("achievementProgress:${ach.id}"),
                                        )
                                    }
                                }
                                if (ach.unlocked) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = "Unlocked",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("achievementUnlocked:${ach.id}"),
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = "Locked",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.testTag("achievementLocked:${ach.id}"),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent XP Transactions
            item {
                SectionHeader(title = "Recent XP", trailing = "${summary.recentTransactions.size} entries")
            }

            item {
                val transactions = summary.recentTransactions
                if (transactions.isEmpty()) {
                    SystemPanel {
                        Text(
                            text = "No XP transactions yet. Complete activities to earn XP.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("progressionEmptyXp"),
                        )
                    }
                } else {
                    transactions.forEach { tx ->
                        SystemPanel {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "+${tx.xpAmount} XP — ${tx.reason}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("xpTransaction:${tx.id}"),
                                    )
                                    Text(
                                        text = "${tx.dayKey} • ${formatTime(tx.loggedAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
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

@Composable
private fun AttributeCard(label: String, value: Int, modifier: Modifier) {
    val spacing = LocalShadowSpacing.current
    SystemPanel(modifier = modifier.padding(vertical = spacing.xs)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val date = java.time.Instant.ofEpochMilli(timestamp)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
    return date.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
}