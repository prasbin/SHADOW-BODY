package com.shadowbody.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shadowbody.app.data.local.UserProfile
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.StatCard
import com.shadowbody.app.ui.components.SystemCard
import com.shadowbody.app.ui.components.SystemDivider
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/**
 * Profile view. Empty state is honest: PROFILE NOT CONFIGURED + create
 * action. Nothing is fabricated when data is missing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenBaselineHistory: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PLAYER PROFILE", style = MaterialTheme.typography.headlineSmall) },
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
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val profile = state.profile
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            if (profile == null) {
                SystemCard(accent = true) {
                    Text(
                        text = "[ PROFILE NOT CONFIGURED ]",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("profileEmpty"),
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = "Initialize your record to begin your transformation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                androidx.compose.material3.Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().testTag("profileCreate"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("CONFIGURE PROFILE")
                }
            } else {
                ProfileSummary(profile = profile, baselineCount = state.baselineCount)
                androidx.compose.material3.Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().testTag("profileEdit"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("EDIT PROFILE")
                }
            }

            SectionHeader(title = "Baseline", trailing = "${state.baselineCount} RECORDS")
            val latest = state.latestBaseline
            if (latest == null) {
                SystemCard {
                    Text(
                        text = "No baseline recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = "Add your first measurements to start transformation history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                SystemCard {
                    BaselineLine("Recorded", formatRecordedAt(latest.recordedAt))
                    BaselineLine("Weight", formatKg(latest.weightKg))
                    BaselineLine("Waist", formatCm(latest.waistCm))
                    BaselineLine("Body fat", latest.bodyFatPct?.let { "$it %" } ?: "—")
                }
            }
            androidx.compose.material3.OutlinedButton(
                onClick = onOpenBaselineHistory,
                modifier = Modifier.fillMaxWidth().testTag("baselineHistory"),
            ) {
                Text("BASELINE HISTORY")
            }
            Spacer(modifier = Modifier.height(spacing.md))
        }
    }
}

@Composable
private fun ProfileSummary(profile: UserProfile, baselineCount: Int) {
    val spacing = LocalShadowSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        StatCard(
            label = "Age",
            value = profile.age.toString(),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Height",
            value = "${trimShort(profile.heightCm)} cm",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Weight",
            value = "${trimShort(profile.weightKg)} kg",
            modifier = Modifier.weight(1f),
        )
    }
    SystemCard {
        SummaryLine("RANK", profile.fitnessLevel.name.uppercase())
        SummaryLine("EQUIPMENT", profile.equipment.joinToString(" · ") { it.label })
        SummaryLine("GOALS", profile.goals.joinToString(" · ") { it.label })
        SummaryLine("SCHEDULE", "${formatDays(profile.trainingDays)} · ${profile.sessionMinutes} MIN")
        SummaryLine("BASELINES", baselineCount.toString())
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    val spacing = LocalShadowSpacing.current
    Column(modifier = Modifier.padding(vertical = spacing.xxs)) {
        Text(
            text = label,
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
private fun BaselineLine(label: String, value: String) {
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
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun trimShort(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()