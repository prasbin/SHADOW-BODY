package com.shadowbody.app.ui.wardrobe

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.shadowbody.app.data.local.WardrobePhotoCombination
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/**
 * Feature C option capture screen: real camera/gallery photos of the user's
 * actual outfit combinations, held in app-private storage. Everything on
 * screen is functional; no placeholder buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobePhotoInputScreen(
    viewModel: WardrobePhotoInputViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val combinations by viewModel.combinations.collectAsState()
    val message by viewModel.message.collectAsState()
    val spacing = LocalShadowSpacing.current

    var pendingPosition by remember { mutableStateOf<Int?>(null) }
    var galleryPosition by remember { mutableStateOf<Int?>(null) }
    // Camera target files must be created BEFORE launching; remember per slot.
    val cameraFiles = remember { androidx.compose.runtime.mutableStateMapOf<Int, java.io.File>() }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { captured ->
        val position = pendingPosition
        pendingPosition = null
        position?.let { pos ->
            cameraFiles[pos]?.let { file ->
                viewModel.onCameraComplete(pos, file, captured)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val position = galleryPosition
        galleryPosition = null
        position?.let { viewModel.onGalleryResult(it, uri) }
    }

    fun startCamera(position: Int) {
        val file = viewModel.newCameraFile(position)
        cameraFiles[position] = file
        pendingPosition = position
        val uri = FileProvider.getUriForFile(context, "com.shadowbody.app.fileprovider", file)
        runCatching { cameraLauncher.launch(uri) }
            .onFailure { pendingPosition = null }
    }

    fun startGallery(position: Int) {
        galleryPosition = position
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        runCatching { galleryLauncher.launch(request) }
            .onFailure { galleryPosition = null }
    }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3500)
            viewModel.clearMessage()
        }
    }

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
                Spacer(modifier = Modifier.height(spacing.xs))
                SystemPanel(accent = true) {
                    Text(
                        text = "Your outfit options — the photos feed today's outfit picks.",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Photograph each full combination (clothes + shoes) once. Photos stay in the app's private storage on this device. Write what the set is (e.g. 'casual jeans + hoodie') — your labels feed the honest daily selector.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(combinations.sortedBy { it.position }, key = { it.id }) { combo ->
                CombinationCard(
                    combination = combo,
                    onCamera = {
                        startCamera(combo.position)
                    },
                    onGallery = { startGallery(combo.position) },
                    onRemovePhoto = { viewModel.removePhoto(combo.position) },
                )
            }

            if (combinations.size < MAX_COMBINATIONS) {
                item {
                    Button(
                        onClick = { startCamera(combinations.size) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("addCombination"),
                    ) {
                        Text("ADD COMBINATION ${combinations.size + 1} (CAMERA)")
                    }
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    OutlinedButton(
                        onClick = { startGallery(combinations.size) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("addCombinationGallery"),
                    ) {
                        Text("ADD COMBINATION ${combinations.size + 1} (GALLERY)")
                    }
                }
            } else {
                item {
                    SystemPanel {
                        Text(
                            text = "All $MAX_COMBINATIONS options are in use. Remove one to change it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (combinations.isEmpty()) {
                item {
                    SystemPanel {
                        Text(
                            text = "No combinations yet — add your first photo above.",
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

private const val MAX_COMBINATIONS = 6

@Composable
private fun CombinationCard(
    combination: WardrobePhotoCombination,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
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
                    text = if (combination.photoPath != null) "PHOTO SAVED" else "EMPTY",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (combination.photoPath != null) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            combination.photoPath?.let { path ->
                val bitmap = remember(path) {
                    runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
                }
                if (bitmap != null) {
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Photo of ${combination.label}",
                        modifier = Modifier
                            .size(width = 160.dp, height = 160.dp),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = "Photo file missing on disk — re-capture to replace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (combination.notes.isNotBlank()) {
                Text(
                    text = combination.notes,
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
                    onClick = onCamera,
                    modifier = Modifier.weight(1f).testTag("addPhoto:${combination.position}"),
                ) {
                    Text("CAMERA")
                }
                OutlinedButton(
                    onClick = onGallery,
                    modifier = Modifier.weight(1f).testTag("galleryPhoto:${combination.position}"),
                ) {
                    Text("GALLERY")
                }
                if (combination.photoPath != null) {
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
