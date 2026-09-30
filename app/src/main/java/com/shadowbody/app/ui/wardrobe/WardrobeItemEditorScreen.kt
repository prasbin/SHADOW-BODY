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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.shadowbody.app.data.local.WardrobeItem
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeItemEditorScreen(
    viewModel: WardrobeViewModel,
    itemId: Long?,
    onBack: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    val isEditing = itemId != null

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("TOP") }
    var clothingType by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var secondaryColor by remember { mutableStateOf("") }
    var style by remember { mutableStateOf("") }
    var season by remember { mutableStateOf("ALL_SEASON") }
    var occasion by remember { mutableStateOf("CASUAL") }
    var fit by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf("") }
    var isEnabled by remember { mutableStateOf(true) }
    var loaded by remember { mutableStateOf(!isEditing) }

    if (isEditing && !loaded) {
        androidx.compose.runtime.LaunchedEffect(itemId) {
            // In a real app, load from repository
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "EDIT ITEM" else "NEW ITEM",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("itemEditorTitle"),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("itemEditorBack")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val item = WardrobeItem(
                                id = itemId ?: 0,
                                name = name,
                                category = category,
                                clothingType = clothingType,
                                color = color,
                                secondaryColor = secondaryColor.ifBlank { null },
                                style = style,
                                season = season,
                                occasion = occasion,
                                fit = fit,
                                isEnabled = isEnabled,
                                notes = notes,
                                photoPath = photoPath.ifBlank { null },
                            )
                            if (isEditing) viewModel.updateItem(item) else viewModel.addItem(item)
                            onBack()
                        },
                        modifier = Modifier.testTag("itemEditorSave"),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Save")
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
                .testTag("itemEditorList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                SystemPanel {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth().testTag("inputItemName"),
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category (TOP/BOTTOM/FOOTWEAR/ACCESSORY)") },
                            modifier = Modifier.fillMaxWidth().testTag("inputItemCategory"),
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        OutlinedTextField(
                            value = clothingType,
                            onValueChange = { clothingType = it },
                            label = { Text("Clothing Type") },
                            modifier = Modifier.fillMaxWidth().testTag("inputItemClothingType"),
                        )
                    }
                }
            }

            item {
                SystemPanel {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = color,
                            onValueChange = { color = it },
                            label = { Text("Color") },
                            modifier = Modifier.fillMaxWidth().testTag("inputItemColor"),
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        OutlinedTextField(
                            value = secondaryColor,
                            onValueChange = { secondaryColor = it },
                            label = { Text("Secondary Color (optional)") },
                            modifier = Modifier.fillMaxWidth().testTag("inputItemSecondaryColor"),
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        OutlinedTextField(
                            value = style,
                            onValueChange = { style = it },
                            label = { Text("Style") },
                            modifier = Modifier.fillMaxWidth().testTag("inputItemStyle"),
                        )
                    }
                }
            }

            item {
                SystemPanel {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = season,
                            onValueChange = { season = it },
                            label = { Text("Season (SPRING/SUMMER/AUTUMN/WINTER/ALL_SEASON)") },
                            modifier = Modifier.fillMaxWidth().testTag("inputItemSeason"),
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        OutlinedTextField(
                            value = occasion,
                            onValueChange = { occasion = it },
                            label = { Text("Occasion (CASUAL/WORK/SPORT/FORMAL/PARTY/OUTDOOR)") },
                            modifier = Modifier.fillMaxWidth().testTag("inputItemOccasion"),
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        OutlinedTextField(
                            value = fit,
                            onValueChange = { fit = it },
                            label = { Text("Fit") },
                            modifier = Modifier.fillMaxWidth().testTag("inputItemFit"),
                        )
                    }
                }
            }

            item {
                SystemPanel {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth().testTag("inputItemNotes"),
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        OutlinedTextField(
                            value = photoPath,
                            onValueChange = { photoPath = it },
                            label = { Text("Photo Path (optional)") },
                            modifier = Modifier.fillMaxWidth().testTag("inputItemPhotoPath"),
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.md)) }
        }
    }
}
