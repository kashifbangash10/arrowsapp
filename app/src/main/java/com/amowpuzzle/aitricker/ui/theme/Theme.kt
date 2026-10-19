package com.amowpuzzle.aitricker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = AppColors.Accent,
    onPrimary = AppColors.Background,
    primaryContainer = AppColors.AccentSoft,
    onPrimaryContainer = AppColors.Navy,
    secondary = AppColors.Slate,
    onSecondary = AppColors.Background,
    background = AppColors.Background,
    onBackground = AppColors.Navy,
    surface = AppColors.Background,
    onSurface = AppColors.Navy,
    surfaceVariant = AppColors.Surface,
    onSurfaceVariant = AppColors.Slate,
    outline = AppColors.SurfaceStrong,
    error = AppColors.Blocked,
    onError = AppColors.Background
)

@Composable
fun ArrowPuzzleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = AppTypography,
        content = content
    )
}
