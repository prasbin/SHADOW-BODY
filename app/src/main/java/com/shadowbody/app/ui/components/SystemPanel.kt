package com.shadowbody.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shadowbody.app.ui.theme.LocalShadowSpacing

/**
 * Dark futuristic "system panel": the base container for dashboard sections.
 * Subtle border glow via [accentBorder]; corner radius from [ShadowSpacing].
 */
@Composable
fun SystemPanel(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    val shape = RoundedCornerShape(spacing.panelCorner)
    val borderColor = if (accent) {
        androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    } else {
        androidx.compose.material3.MaterialTheme.colorScheme.outline
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = LocalShadowSpacing.current.panelBorder,
                color = if (accent) {
                    androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                } else {
                    androidx.compose.material3.MaterialTheme.colorScheme.outline
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(LocalShadowSpacing.current.panelCorner),
            ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(LocalShadowSpacing.current.panelCorner),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        tonalElevation = LocalShadowSpacing.current.xxs,
    ) {
        Column(modifier = Modifier.padding(LocalShadowSpacing.current.md)) {
            content()
        }
    }
}