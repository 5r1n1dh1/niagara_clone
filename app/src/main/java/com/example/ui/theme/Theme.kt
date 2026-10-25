package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.AppFontFamily
import com.example.data.LauncherThemePreset

@Composable
fun NiagaraTheme(
    preset: LauncherThemePreset = LauncherThemePreset.FLUX_EMERALD,
    fontFamily: AppFontFamily = AppFontFamily.MODERN_SANS,
    glass: GlassStyle = GlassStyleFactory.default(),
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = preset.primary,
        onPrimary = Color.Black,
        primaryContainer = preset.primary.copy(alpha = 0.25f),
        onPrimaryContainer = preset.primary,
        secondary = preset.accent,
        onSecondary = Color.Black,
        secondaryContainer = preset.accent.copy(alpha = 0.2f),
        onSecondaryContainer = preset.accent,
        tertiary = Color(0xFF34D399),
        onTertiary = Color.Black,
        background = preset.background,
        onBackground = Color(0xFFF1F5F9),
        surface = preset.surface,
        onSurface = Color(0xFFF8FAFC),
        surfaceVariant = preset.surface.copy(alpha = 0.85f),
        onSurfaceVariant = Color(0xFF94A3B8)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getLauncherTypography(fontFamily),
        content = content
    )
}

@Deprecated("Alias removed in v1.0.1; use NiagaraTheme directly")
@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    NiagaraTheme(content = content)
}
