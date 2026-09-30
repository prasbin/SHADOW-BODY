package com.shadowbody.app.ui.wardrobe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitGeneratorScreen(
    viewModel: OutfitViewModel,
    onBack: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    val selectedOccasion by viewModel.selectedOccasion.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val suggestion by viewModel.currentSuggestion.collectAsState()
    val enabledItems by viewModel.enabledItems.collectAsState()

    var outfitName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OUTFIT GENERATOR",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("outfitGenTitle"),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("outfitGenBack")) {
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
                .testTag("outfitGenList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                SectionHeader(title = "Parameters", trailing = "${enabledItems.size} items available")
            }

            item {
                SystemPanel {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Occasion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        ) {
                            listOf("CASUAL", "WORK", "SPORT", "FORMAL").forEach { occ ->
                                androidx.compose.material3.FilterChip(
                                    selected = selectedOccasion == occ,
                                    onClick = { viewModel.setOccasion(occ) },
                                    label = { Text(occ) },
                                    modifier = Modifier.testTag("occasionFilter:$occ"),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = "Season",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        ) {
                            listOf("ALL_SEASON", "SPRING", "SUMMER", "AUTUMN", "WINTER").forEach { sea ->
                                androidx.compose.material3.FilterChip(
                                    selected = selectedSeason == sea,
                                    onClick = { viewModel.setSeason(sea) },
                                    label = { Text(sea) },
                                    modifier = Modifier.testTag("seasonFilter:$sea"),
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.generateOutfit() },
                    modifier = Modifier.fillMaxWidth().testTag("generateOutfitButton"),
                ) {
                    Text("GENERATE OUTFIT")
                }
            }

            if (suggestion != null) {
                item {
                    SectionHeader(title = "Suggestion", trailing = "")
                }

                item {
                    SystemPanel(accentBorder = true) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            suggestion?.top?.let {
                                Text(
                                    text = "TOP: ${it.name} (${it.color})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("outfitTop"),
                                )
                            }
                            suggestion?.bottom?.let {
                                Text(
                                    text = "BOTTOM: ${it.name} (${it.color})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("outfitBottom"),
                                )
                            }
                            suggestion?.footwear?.let {
                                Text(
                                    text = "FOOTWEAR: ${it.name} (${it.color})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("outfitFootwear"),
                                )
                            }
                            suggestion?.accessory?.let {
                                Text(
                                    text = "ACCESSORY: ${it.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("outfitAccessory"),
                                )
                            }
                        }
                    }
                }

                item {
                    SystemPanel {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "EXPLANATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = suggestion?.explanation ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("outfitExplanation"),
                            )
                            if (suggestion?.missingCategories?.isNotEmpty() == true) {
                                Spacer(modifier = Modifier.height(spacing.xs))
                                Text(
                                    text = "Missing: ${suggestion?.missingCategories?.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("outfitMissing"),
                                )
                            }
                        }
                    }
                }

                item {
                    SystemPanel {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = outfitName,
                                onValueChange = { outfitName = it },
                                label = { Text("Outfit Name") },
                                modifier = Modifier.fillMaxWidth().testTag("outfitNameInput"),
                            )
                            Spacer(modifier = Modifier.height(spacing.xs))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                            ) {
                                Button(
                                    onClick = { viewModel.saveOutfit(outfitName) },
                                    modifier = Modifier.weight(1f).testTag("saveOutfitButton"),
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = "Save")
                                    Spacer(modifier = Modifier.padding(horizontal = spacing.xs))
                                    Text("SAVE")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.clearMessage() },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("CLEAR")
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
