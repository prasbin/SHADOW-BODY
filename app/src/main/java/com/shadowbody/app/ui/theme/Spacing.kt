package com.shadowbody.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared spacing/dimension scale so panels stay visually consistent. */
@Immutable
data class ShadowSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val panelCorner: Dp = 10.dp,
    val panelBorder: Dp = 1.dp,
)

val LocalShadowSpacing = staticCompositionLocalOf { ShadowSpacing() }
