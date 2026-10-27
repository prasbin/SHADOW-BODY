package com.shadowbody.app.ui.workout

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.shadowbody.app.domain.demo.DemoVideoResolver
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/**
 * Feature A: external demonstration action for one exercise.
 *
 * What it does: opens the deterministic YouTube SEARCH query for this
 * exercise ("…exercise proper form tutorial") in the external YouTube app or
 * browser. The label states the truth — external, online, search-based; no
 * individual video is verified by SHADOW BODY.
 *
 * What it shows instead when the name resolves to nothing or no external app
 * can handle the link: an honest "unavailable" state. It never plays a
 * random or unrelated video.
 */
@Composable
fun WatchDemonstrationAction(
    exerciseName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val spacing = LocalShadowSpacing.current
    val demo = remember(exerciseName) { DemoVideoResolver.forExercise(exerciseName) }
    var externallyUnavailable by remember(exerciseName) { mutableStateOf(false) }

    if (demo == null) {
        Text(
            text = "Demonstration unavailable for this exercise (no name on record).",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.testTag("demoUnavailable:$exerciseName"),
        )
        return
    }

    if (externallyUnavailable) {
        Text(
            text = "External video unavailable on this device (no app or browser). Connect or install one to watch.",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
            modifier = modifier.testTag("demoExternalFail:$exerciseName"),
        )
        return
    }

    TextButton(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(demo.url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
                .onFailure { externallyUnavailable = true }
        },
        modifier = modifier.testTag("watchDemo:$exerciseName"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "WATCH DEMO · EXTERNAL/YOUTUBE (online)",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            )
        }
    }
}
