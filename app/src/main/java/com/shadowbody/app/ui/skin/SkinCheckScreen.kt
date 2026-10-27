package com.shadowbody.app.ui.skin

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.shadowbody.app.data.local.SkinCheckRecord
import com.shadowbody.app.data.local.isComplete
import com.shadowbody.app.data.repository.SkinCheckRepository
import com.shadowbody.app.ui.components.SectionHeader
import com.shadowbody.app.ui.components.SystemPanel
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/**
 * Feature B: daily two-cheek skin photo check.
 *
 * - One entry point drives camera capture or gallery pick for each cheek.
 * - Camera photos go straight into app-private storage via a FileProvider
 *   URI; gallery picks are copied into private storage by the repository.
 * - Nothing here claims analysis. The status panel says plainly what SHADOW
 *   BODY does (store + compare) and does not do (diagnose).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinCheckScreen(
    viewModel: SkinCheckViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val today by viewModel.today.collectAsState()
    val history by viewModel.history.collectAsState()
    val message by viewModel.message.collectAsState()
    val spacing = LocalShadowSpacing.current

    var pendingLeftFile by remember { mutableStateOf<java.io.File?>(null) }
    var pendingRightFile by remember { mutableStateOf<java.io.File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { captured ->
        val cheek = when {
            pendingLeftFile != null -> SkinCheckRepository.Cheek.LEFT
            pendingRightFile != null -> SkinCheckRepository.Cheek.RIGHT
            else -> return@rememberLauncherForActivityResult
        }
        val file = pendingLeftFile ?: pendingRightFile
        pendingLeftFile = null
        pendingRightFile = null
        viewModel.onCameraComplete(cheek, file ?: return@rememberLauncherForActivityResult, captured)
    }

    val galleryLeft = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onGalleryResult(SkinCheckRepository.Cheek.LEFT, uri) }

    val galleryRight = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onGalleryResult(SkinCheckRepository.Cheek.RIGHT, uri) }

    fun startCameraCapture(cheek: SkinCheckRepository.Cheek) {
        val file = viewModel.newCameraFile(cheek)
        val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
        if (cheek == SkinCheckRepository.Cheek.LEFT) pendingLeftFile = file else pendingRightFile = file
        runCatching { cameraLauncher.launch(uri) }
            .onFailure {
                pendingLeftFile = null
                pendingRightFile = null
            }
    }

    fun startGalleryPick(cheek: SkinCheckRepository.Cheek) {
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        runCatching {
            if (cheek == SkinCheckRepository.Cheek.LEFT) galleryLeft.launch(request)
            else galleryRight.launch(request)
        }
    }

    var confirmDeleteDayKey by remember { mutableStateOf<String?>(null) }

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
                        text = "DAILY SKIN CHECK",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("skinCheckTitle"),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("skinCheckBack")) {
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
                .testTag("skinCheckList"),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                Spacer(modifier = Modifier.height(spacing.xs))
                val todayRecord = today
                SystemPanel(accent = true) {
                    Text(
                        text = "TODAY · ${todayRecord?.dayKey ?: "not started"}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("skinCheckTodayHeader"),
                    )
                    val status = when {
                        todayRecord == null -> "NOT STARTED"
                        !todayRecord.isComplete -> "MISSING ${missingCheekLabel(todayRecord)}"
                        else -> "COMPLETE"
                    }
                    Text(
                        text = "Status: $status",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (todayRecord != null && todayRecord.isComplete) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.testTag("skinCheckTodayStatus"),
                    )
                    Text(
                        text = "A day counts as complete only when BOTH cheeks are photographed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                CheekCapturePanel(
                    label = "LEFT CHEEK",
                    record = today,
                    cheek = SkinCheckRepository.Cheek.LEFT,
                    onCamera = { startCameraCapture(SkinCheckRepository.Cheek.LEFT) },
                    onGallery = { startGalleryPick(SkinCheckRepository.Cheek.LEFT) },
                    onRemove = { viewModel.removeCheek(SkinCheckRepository.Cheek.LEFT) },
                )
            }
            item {
                CheekCapturePanel(
                    label = "RIGHT CHEEK",
                    record = today,
                    cheek = SkinCheckRepository.Cheek.RIGHT,
                    onCamera = { startCameraCapture(SkinCheckRepository.Cheek.RIGHT) },
                    onGallery = { startGalleryPick(SkinCheckRepository.Cheek.RIGHT) },
                    onRemove = { viewModel.removeCheek(SkinCheckRepository.Cheek.RIGHT) },
                )
            }

            item {
                SystemPanel {
                    Text(
                        text = "Capture guidance",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = "• Same place, same approximate time of day daily.\n" +
                            "• Bright, indirect light facing you — avoid strong single-side shadows.\n" +
                            "• Similar distance and straight-on angle each day.\n" +
                            "• No beauty filters, no smoothing modes, no makeup for this photo.\n" +
                            "• Clean, dry skin; keep lenses and hands clean.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SystemPanel {
                    Text(
                        text = "What SHADOW BODY does — and does not do",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = "Your photos stay on this device, in the app's private storage. They are never uploaded. " +
                            "SHADOW BODY does not analyse, score, or diagnose your skin — no on-device or cloud image " +
                            "analysis is performed. Comparisons below let YOU track visible change over time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("skinCheckPrivacyNote"),
                    )
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = "General care, not medical advice: be conservative with new products — patch test on a " +
                            "small area first. Do not stack several strong actives at once. Daily sun protection matters " +
                            "more than any single product. For anything persistent, painful, spreading or concerning, " +
                            "see a qualified clinician or dermatologist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (today != null) {
                item {
                    SpinnerNotesEditor(
                        initial = today?.notes.orEmpty(),
                        placeholderText = "Optional observation — how does your skin look or feel in your own words?",
                        inputTag = "skinNotesInput",
                        onCommit = viewModel::updateNotes,
                    )
                }
            }

            item {
                SectionHeader(title = "History", trailing = "${history.size} days recorded")
            }

            if (history.isEmpty()) {
                item {
                    SystemPanel {
                        Text(
                            text = "No days recorded yet. Capture both cheeks to start your history.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("skinCheckEmpty"),
                        )
                    }
                }
            } else {
                items(history, key = { it.id }) { record ->
                    HistoryDayCard(
                        record = record,
                        onDelete = { confirmDeleteDayKey = record.dayKey },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.md)) }
        }
    }

    confirmDeleteDayKey?.let { dayKey ->
        AlertDialog(
            onDismissRequest = { confirmDeleteDayKey = null },
            title = { Text("Delete day?") },
            text = {
                Text("This permanently deletes that day's record and BOTH photo files from this device. It cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDay(dayKey)
                    confirmDeleteDayKey = null
                }) { Text("Delete day and photos") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteDayKey = null }) { Text("Keep") }
            },
        )
    }
}

private fun missingCheekLabel(record: SkinCheckRecord?): String = when {
    record == null -> "both photos"
    record.leftCheekPhotoPath == null &&
        record.rightCheekPhotoPath == null -> "both photos"
    record.leftCheekPhotoPath == null -> "left photo"
    else -> "right photo"
}

private const val FILE_PROVIDER_AUTHORITY = "com.shadowbody.app.fileprovider"

/** A debounced free-text notes editor: commits when the Save button is used. */
@Composable
private fun SpinnerNotesEditor(
    initial: String,
    placeholderText: String,
    inputTag: String,
    onCommit: (String) -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    var value by remember(initial) { mutableStateOf(initial) }
    SystemPanel {
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(inputTag),
            placeholder = { Text(placeholderText, style = MaterialTheme.typography.bodySmall) },
            minLines = 2,
            maxLines = 5,
        )
        Spacer(modifier = Modifier.height(spacing.xxs))
        TextButton(onClick = { onCommit(value) }, modifier = Modifier.testTag("skinNotesSave")) {
            Text("SAVE NOTE")
        }
    }
}

@Composable
private fun CheekCapturePanel(
    label: String,
    record: SkinCheckRecord?,
    cheek: SkinCheckRepository.Cheek,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemove: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    val path = when (cheek) {
        SkinCheckRepository.Cheek.LEFT -> record?.leftCheekPhotoPath
        SkinCheckRepository.Cheek.RIGHT -> record?.rightCheekPhotoPath
    }
    val method = when (cheek) {
        SkinCheckRepository.Cheek.LEFT -> record?.leftCheekCaptureMethod
        SkinCheckRepository.Cheek.RIGHT -> record?.rightCheekCaptureMethod
    }
    SystemPanel(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cheekPanel:${cheek.name}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (path != null) "PHOTO SAVED (${method.orEmpty()})" else "MISSING",
                style = MaterialTheme.typography.labelSmall,
                color = if (path != null) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.testTag("cheekStatus:${cheek.name}"),
            )
        }
        Spacer(modifier = Modifier.height(spacing.xs))
        if (path != null) {
            val bitmap = remember(path) {
                runCatching {
                    BitmapFactory.decodeFile(path)?.let { bmp ->
                        // Downsample for thumbnail display; the original file is untouched.
                        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                        BitmapFactory.decodeFile(path, opts) ?: bmp
                    }
                }.getOrNull()
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "$label saved photo",
                    modifier = Modifier
                        .size(width = 140.dp, height = 140.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = "Saved photo file is missing on disk. Re-capture to replace.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(spacing.xs))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Button(onClick = onCamera, modifier = Modifier.weight(1f).testTag("cheekCamera:${cheek.name}")) {
                Text("CAMERA")
            }
            OutlinedButton(onClick = onGallery, modifier = Modifier.weight(1f).testTag("cheekGallery:${cheek.name}")) {
                Text("GALLERY")
            }
            if (path != null) {
                OutlinedButton(onClick = onRemove, modifier = Modifier.weight(1f).testTag("cheekRemove:${cheek.name}")) {
                    Text("REMOVE")
                }
            }
        }
    }
}

@Composable
private fun HistoryDayCard(
    record: SkinCheckRecord,
    onDelete: () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    SystemPanel(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("skinHistoryDay:${record.dayKey}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = record.dayKey,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            TextButton(onClick = onDelete) { Text("DELETE") }
        }
        Spacer(modifier = Modifier.height(spacing.xxs))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            CheekThumb(label = "LEFT", path = record.leftCheekPhotoPath)
            CheekThumb(label = "RIGHT", path = record.rightCheekPhotoPath)
        }
        if (record.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = "Note: ${record.notes}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CheekThumb(label: String, path: String?) {
    val spacing = LocalShadowSpacing.current
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (path == null) {
            Text(
                text = "missing",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(72.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline),
            )
        } else {
            val bitmap = remember(path) {
                runCatching {
                    BitmapFactory.decodeFile(path)?.let { bmp ->
                        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                        BitmapFactory.decodeFile(path, opts) ?: bmp
                    }
                }.getOrNull()
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "$label cheek photo",
                    modifier = Modifier
                        .size(72.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = "file missing",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(72.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline),
                )
            }
        }
    }
}
