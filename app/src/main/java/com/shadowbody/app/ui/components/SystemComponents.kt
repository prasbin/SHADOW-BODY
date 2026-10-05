package com.shadowbody.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shadowbody.app.ui.theme.LocalShadowSpacing
import com.shadowbody.app.ui.theme.ShadowBorder
import com.shadowbody.app.ui.theme.ShadowCyan
import com.shadowbody.app.ui.theme.ShadowCyanDim
import com.shadowbody.app.ui.theme.ShadowSuccess
import com.shadowbody.app.ui.theme.ShadowSurface
import com.shadowbody.app.ui.theme.ShadowSurfaceVariant
import com.shadowbody.app.ui.theme.ShadowTextMuted
import com.shadowbody.app.ui.theme.ShadowTextPrimary
import com.shadowbody.app.ui.theme.ShadowTextSecondary
import com.shadowbody.app.ui.theme.ShadowViolet

@Composable
fun SystemCard(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    content: @Composable () -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    val borderBrush = if (accent) {
        Brush.linearGradient(listOf(ShadowCyan.copy(alpha = 0.6f), ShadowCyanDim.copy(alpha = 0.2f)))
    } else {
        Brush.linearGradient(listOf(ShadowBorder, ShadowBorder.copy(alpha = 0.5f)))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(spacing.panelCorner))
            .background(ShadowSurface)
            .border(
                width = spacing.panelBorder,
                brush = borderBrush,
                shape = RoundedCornerShape(spacing.panelCorner),
            )
            .padding(spacing.md),
    ) {
        content()
    }
}

@Composable
fun SystemStatusChip(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalShadowSpacing.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isActive) ShadowCyan.copy(alpha = 0.15f) else ShadowSurfaceVariant
            )
            .border(
                width = 1.dp,
                color = if (isActive) ShadowCyan else ShadowBorder,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = spacing.sm, vertical = spacing.xxs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) ShadowCyan else ShadowTextSecondary,
        )
    }
}

@Composable
fun SystemProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = ShadowCyan,
) {
    val spacing = LocalShadowSpacing.current
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = color,
        trackColor = ShadowSurfaceVariant,
    )
}

@Composable
fun SystemStatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val spacing = LocalShadowSpacing.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = if (accent) ShadowCyan else ShadowTextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(spacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ShadowTextMuted,
        )
    }
}

@Composable
fun SystemActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
) {
    val spacing = LocalShadowSpacing.current
    val bgColor = if (primary) {
        if (enabled) ShadowCyan else ShadowCyanDim
    } else {
        ShadowSurfaceVariant
    }
    val textColor = if (primary) {
        Color(0xFF060A12)
    } else {
        ShadowTextPrimary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(spacing.panelCorner))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (primary) ShadowCyan else ShadowBorder,
                shape = RoundedCornerShape(spacing.panelCorner),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SystemDivider(modifier: Modifier = Modifier) {
    val spacing = LocalShadowSpacing.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ShadowBorder.copy(alpha = 0.3f)),
    )
}