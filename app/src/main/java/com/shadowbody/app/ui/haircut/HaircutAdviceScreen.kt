package com.shadowbody.app.ui.haircut

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shadowbody.app.domain.haircut.HaircutAdvisor
import com.shadowbody.app.domain.haircut.HaircutCatalog
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemActionButton
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/**
 * Feature D: haircut advice with preferences and honest visual-example
 * states.Ranked list of fixed catalog styles; no imagery is faked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaircutAdviceScreen(
    onBack: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    var length by remember { mutableStateOf<String?>(null) }
    var maintenance by remember { mutableStateOf<String?>(null) }
    var look by remember { mutableStateOf<String?>(null) }
    var maxStylingMinutes by remember { mutableStateOf<Int?>(null) }

    var advice by remember {
        mutableStateOf(HaircutAdvisor.recommend(HaircutAdvisor.Input()))
    }

    fun recompute() {
        advice = HaircutAdvisor.recommend(
            HaircutAdvisor.Input(
                length = length,
                maintenance = maintenance,
                look = look,
                maxStylingMinutes = maxStylingMinutes,
            ),
        )
    }

    fun toggleFilter(current: String?, new: String): String? = if (current == new) null else new

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HAIRCUT ADVICE",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("haircutTitle"),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("haircutBack")) {
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
                .testTag("haircutList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                Spacer(modifier = Modifier.height(spacing.xs))
                SystemPanel(accent = true) {
                    Text(
                        text = "HONEST METHOD",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("haircutBannerTitle"),
                    )
                    Text(
                        text = "These are fixed barber styles ranked by your stated preferences only. " +
                            "SHADOW BODY performs no face-shape, hairline or photo analysis. " +
                            "No example images are bundled — the written barber instructions " +
                            "are the reliable reference to use in the chair.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))

                    Text(
                        text = "LENGTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                        listOf("SHORT", "MEDIUM", "LONG").forEach { option ->
                            SystemActionButton(
                                text = option,
                                onClick = {
                                    length = toggleFilter(length, option)
                                    recompute()
                                },
                                modifier = Modifier.weight(1f).testTag("length:$option"),
                                primary = length == option,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = "MAINTENANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { option ->
                            SystemActionButton(
                                text = option,
                                onClick = {
                                    maintenance = toggleFilter(maintenance, option)
                                    recompute()
                                },
                                modifier = Modifier.weight(1f).testTag("maintenance:$option"),
                                primary = maintenance == option,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = "LOOK",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                        listOf("PROFESSIONAL", "CASUAL", "VERSATILE").forEach { option ->
                            SystemActionButton(
                                text = option.take(9),
                                onClick = {
                                    look = toggleFilter(look, option)
                                    recompute()
                                },
                                modifier = Modifier.weight(1f).testTag("look:$option"),
                                primary = look == option,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = "MAX STYLING TIME",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
                        listOf(5, 10, 15).forEach { option ->
                            val label = "≤${option}m"
                            SystemActionButton(
                                text = label,
                                onClick = {
                                    maxStylingMinutes = if (maxStylingMinutes == option) null else option
                                    recompute()
                                },
                                modifier = Modifier.weight(1f).testTag("styling:$option"),
                                primary = maxStylingMinutes == option,
                            )
                        }
                        SystemActionButton(
                            text = "ANY",
                            onClick = {
                                maxStylingMinutes = null
                                recompute()
                            },
                            modifier = Modifier.weight(1f).testTag("styling:any"),
                            primary = maxStylingMinutes == null,
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.xs))
                    SystemActionButton(
                        text = "RESET PREFERENCES",
                        primary = false,
                        onClick = {
                            length = null
                            maintenance = null
                            look = null
                            maxStylingMinutes = null
                            recompute()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("haircutReset"),
                    )
                }
            }

            item {
                SectionHeader(title = "Recommendations", trailing = "${advice.size} styles")
            }

            if (advice.isEmpty()) {
                item {
                    SystemPanel {
                        Text(
                            text = "No styles match — every option is deselected or filters are too tight.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("haircutEmpty"),
                        )
                    }
                }
            } else {
                items(advice, key = { it.haircut.id }) { entry ->
                    HaircutCard(entry)
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.md)) }
        }
    }
}

@Composable
private fun HaircutCard(entry: HaircutAdvisor.Advice) {
    val spacing = LocalShadowSpacing.current
    val haircut = entry.haircut
    SystemPanel(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("haircutCard:${haircut.id}"),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${entry.rank}. ${haircut.name.uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "VISUAL EXAMPLE: " + if (haircut.hasVisualExample) "AVAILABLE" else "NOT AVAILABLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (haircut.hasVisualExample) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.testTag("haircutVisualState:${haircut.id}"),
                )
            }
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = haircut.visualExampleNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = "${haircut.description} (length ${haircut.length.lowercase()}, maintenance ${haircut.maintenance.lowercase()}, ~${haircut.stylingMinutes} min styling, fits ${haircut.looks.joinToString("/").lowercase()} looks.)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (entry.matchedPreferences.isNotEmpty()) {
                Text(
                    text = "Matched your preferences: ${entry.matchedPreferences.joinToString()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = "MAINTENANCE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = haircut.maintenanceGuidance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = "SHOW THIS TO YOUR BARBER",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = haircut.barberInstructions,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("haircutBarber:${haircut.id}"),
            )
        }
    }
}
