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
    accentBorder: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalShadowSpacing.current
    val shape = RoundedCornerShape(spacing.panelCorner)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (accentBorder) {
                    Modifier.border(
                        spacing.panelBorder,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        shape,
                    )
                } else {
                    Modifier.border(spacing.panelBorder, MaterialTheme.colorScheme.outline, shape)
                }
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = spacing.xxs,
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            content()
        }
    }
}
