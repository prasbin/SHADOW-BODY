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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shadowbody.app.data.local.WardrobePhotoCombination
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobePhotoInputScreen(
    viewModel: WardrobePhotoInputViewModel,
    onBack: () -> Unit,
    onAddPhoto: (Int) -> Unit,
) {
    val combinations by viewModel.combinations.collectAsState()
    val spacing = LocalShadowSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "WARDROBE PHOTOS",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("wardrobePhotoTitle"),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("wardrobePhotoBack")) {
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
                .testTag("wardrobePhotoList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                Text(
                    text = "Add photos of 3 clothing + shoe combinations. Photos stay on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items(combinations.sortedBy { it.position }, key = { it.id }) { combo ->
                CombinationCard(
                    combination = combo,
                    onAddPhoto = { onAddPhoto(combo.position) },
                    onReplacePhoto = { viewModel.replacePhoto(combo.position, "/new/path.jpg") },
                    onRemovePhoto = { viewModel.removePhoto(combo.position) },
                )
            }

            if (combinations.isEmpty()) {
                item {
                    SystemPanel {
                        Text(
                            text = "No combinations yet. Add your first photo.",
                            style = MaterialTheme.typography.bodyMedium,
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
private fun CombinationCard(
    combination: WardrobePhotoCombination,
    onAddPhoto: () -> Unit,
    onReplacePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    SystemPanel(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("combination:${combination.position}"),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = combination.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (combination.photoPath != null) "PHOTO ADDED" else "EMPTY",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (combination.photoPath != null) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            if (combination.photoPath != null) {
                Text(
                    text = "Photo: ${combination.photoPath}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Button(
                    onClick = onAddPhoto,
                    modifier = Modifier.weight(1f).testTag("addPhoto:${combination.position}"),
                ) {
                    Text("ADD PHOTO")
                }
                if (combination.photoPath != null) {
                    OutlinedButton(
                        onClick = onReplacePhoto,
                        modifier = Modifier.weight(1f).testTag("replacePhoto:${combination.position}"),
                    ) {
                        Text("REPLACE")
                    }
                    OutlinedButton(
                        onClick = onRemovePhoto,
                        modifier = Modifier.weight(1f).testTag("removePhoto:${combination.position}"),
                    ) {
                        Text("REMOVE")
                    }
                }
            }
        }
    }
}
