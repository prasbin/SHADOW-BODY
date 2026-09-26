package com.shadowbody.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.shadowbody.app.data.preferences.ThemeMode

private val ShadowColorScheme = darkColorScheme(
    primary = ShadowCyan,
    onPrimary = ShadowBackground,
    secondary = ShadowViolet,
    onSecondary = ShadowTextPrimary,
    background = ShadowBackground,
    onBackground = ShadowTextPrimary,
    surface = ShadowSurface,
    onSurface = ShadowTextPrimary,
    surfaceVariant = ShadowSurfaceVariant,
    onSurfaceVariant = ShadowTextSecondary,
    outline = ShadowBorder,
    tertiary = ShadowSuccess,
)

/**
 * Application theme. SHADOW BODY is dark-first: every [ThemeMode] resolves
 * to the dark System palette in Phase 1. A light scheme may be added later
 * without changing call sites — [themeMode] is already threaded through.
 */
@Composable
fun ShadowBodyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalShadowSpacing provides ShadowSpacing()) {
        MaterialTheme(
            colorScheme = ShadowColorScheme,
            typography = ShadowTypography,
            content = content,
        )
    }
}
