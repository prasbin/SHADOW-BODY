package com.shadowbody.app.ui.wardrobe

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.shadowbody.app.data.local.WardrobeItem
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeScreen(
    viewModel: WardrobeViewModel,
    onBack: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    onOpenOutfitGenerator: () -> Unit,
    onOpenPhotoInput: () -> Unit = {},
) {
    val uiItems by viewModel.filteredItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val spacing = LocalShadowSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "WARDROBE",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("wardrobeTitle"),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("wardrobeBack")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenOutfitGenerator, modifier = Modifier.testTag("outfitGeneratorButton")) {
                        Icon(Icons.Filled.Check, contentDescription = "Outfit Generator")
                    }
                    IconButton(onClick = onAddItem, modifier = Modifier.testTag("addItemButton")) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Item")
                    }
                    IconButton(
                        onClick = onOpenPhotoInput,
                        modifier = Modifier.testTag("photoInputButton"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Photo Input")
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
                .testTag("wardrobeList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    label = { Text("Search") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    modifier = Modifier.fillMaxWidth().testTag("searchInput"),
                )
            }

            item {
                SectionHeader(title = "Categories", trailing = "${uiItems.size} items")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    listOf("ALL", "TOP", "BOTTOM", "FOOTWEAR", "ACCESSORY").forEach { cat ->
                        val isSelected = if (cat == "ALL") selectedCategory == null else selectedCategory == cat
                        androidx.compose.material3.FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.setCategory(if (cat == "ALL") null else cat)
                            },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("categoryFilter:$cat"),
                        )
                    }
                }
            }

            if (uiItems.isEmpty()) {
                item {
                    SystemPanel {
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategory != null)
                                "No items match your filters."
                            else
                                "No wardrobe items yet. Add your first item.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("wardrobeEmpty"),
                        )
                    }
                }
            } else {
                items(uiItems, key = { "item-${it.id}" }) { item ->
                    SystemPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditItem(item.id) }
                            .testTag("wardrobeItem:${item.id}"),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${item.category} - ${item.clothingType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = item.color,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                            if (!item.isEnabled) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Disabled",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
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
