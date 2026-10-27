package com.shadowbody.app.ui.outfit

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shadowbody.app.data.local.WardrobePhotoCombination
import com.shadowbody.app.domain.outfit.OutfitRecommenderEngine
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemActionButton
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/**
 * Feature C: daily outfit recommendation from real photographed options.
 * Every explanation shown derives from the honest, rule-based engine — the
 * screen never words it as visual AI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitDailyScreen(
    viewModel: OutfitDailyViewModel,
    onBack: () -> Unit,
    onOpenPhotoInput: () -> Unit,
) {
    val options by viewModel.options.collectAsState()
    val decisions by viewModel.decisions.collectAsState()
    val todayDecision by viewModel.todayDecision.collectAsState()
    val selectedOccasion by viewModel.selectedOccasion.collectAsState()
    val message by viewModel.message.collectAsState()
    val busy by viewModel.recommendLock.collectAsState()
    val spacing = LocalShadowSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TODAY'S OUTFIT",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("outfitDailyTitle"),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("outfitDailyBack")) {
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
                .testTag("outfitDailyList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                Spacer(modifier = Modifier.height(spacing.xs))
                SystemPanel(accent = true) {
                    Text(
                        text = "THE DECISION PANEL",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Pick what today is for, then press the recommend action. " +
                            "SHADOW BODY chooses between your photographed options using the " +
                            "words you wrote on them — it does not analyse the photos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        OutfitRecommenderEngine.Occasions.ALL.forEach { occasion ->
                            val selected = selectedOccasion == occasion
                            SystemActionButton(
                                text = shortOccasionLabel(occasion),
                                onClick = { viewModel.selectOccasion(occasion) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("occasion:$occasion"),
                                primary = selected,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.xs))
                    SystemActionButton(
                        text = if (busy) "THINKING…" else "RECOMMEND TODAY'S OUTFIT",
                        onClick = viewModel::recommendToday,
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recommendOutfit"),
                    )
                    message?.let {
                        Spacer(modifier = Modifier.height(spacing.xxs))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.testTag("outfitDailyMessage"),
                        )
                    }
                }
            }

            if (todayDecision != null) {
                item {
                    val decision = todayDecision
                    SystemPanel {
                        Text(
                            text = "TODAY · ${decision?.dayKey}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("outfitDecisionHeader"),
                        )
                        Text(
                            text = "Recommended: ${viewModel.labelFor(decision?.selectedCombinationId ?: -1L, options)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("outfitDecisionPick"),
                        )
                        Text(
                            text = "Why: ${decision?.explanation.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("outfitDecisionExplanation"),
                        )
                    }
                }
            }

            item {
                SectionHeader(title = "Your photographed options", trailing = "${options.size} with photos")
            }

            item {
                SystemPanel {
                    Text(
                        text = "Options come from your wardrobe photos. Add or edit photos on the 'Wardrobe photos' screen. Labels/notes you write there feed the honest rule-based selection above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    SystemActionButton(
                        text = "ADD / EDIT OUTFIT PHOTOS",
                        onClick = onOpenPhotoInput,
                        primary = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("openPhotoInput"),
                    )
                }
            }

            if (options.isEmpty()) {
                item {
                    SystemPanel {
                        Text(
                            text = "No photographed outfit options yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("outfitDailyEmpty"),
                        )
                    }
                }
            } else {
                items(options, key = { it.id }) { option ->
                    OptionCard(option, viewModel, selectedOccasion)
                }
            }

            item {
                SectionHeader(title = "Decision history", trailing = "${decisions.size} saved")
            }

            if (decisions.isEmpty()) {
                item {
                    SystemPanel {
                        Text(
                            text = "No decisions recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("decisionHistoryEmpty"),
                        )
                    }
                }
            } else {
                items(decisions, key = { it.id }) { decision ->
                    SystemPanel(modifier = Modifier.testTag("decisionDay:${decision.dayKey}")) {
                        Text(
                            text = "${decision.dayKey} · ${OutfitRecommenderEngine.Occasions.label(decision.occasion)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Chose: ${viewModel.labelFor(decision.selectedCombinationId, options)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = decision.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.md)) }
        }
    }
}

@Composable
private fun OptionCard(
    option: WardrobePhotoCombination,
    viewModel: OutfitDailyViewModel,
    occasion: String,
) {
    val spacing = LocalShadowSpacing.current
    SystemPanel(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("outfitOption:${option.id}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            option.photoPath?.let { path ->
                val bitmap = remember(path) {
                    runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Photo of ${option.label}",
                        modifier = Modifier
                            .size(72.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (option.notes.isNotBlank()) {
                    Text(
                        text = option.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val matches = OutfitRecommenderEngine.keywordMatches(option, occasion)
                Text(
                    text = if (matches > 0) {
                        "$matches label/notes keyword match(es) for today"
                    } else {
                        "No label/notes keywords match today"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

private fun shortOccasionLabel(occasion: String): String = when (occasion) {
    OutfitRecommenderEngine.Occasions.TRAINING -> "TRAINING"
    OutfitRecommenderEngine.Occasions.WORK_FORMAL -> "WORK"
    OutfitRecommenderEngine.Occasions.SOCIAL_CASUAL -> "SOCIAL"
    else -> "HOME"
}
