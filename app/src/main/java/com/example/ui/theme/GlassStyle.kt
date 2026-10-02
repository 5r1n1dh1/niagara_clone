package com.example.ui.theme

import androidx.compose.runtime.Immutable
import com.example.data.LauncherThemePreset

/**
 * Immutable value type representing the global Frosted Glass theme settings,
 * validated and clamped at construction.
 */
@Immutable
data class GlassStyle(
    val translucency: Float = 0.30f,
    val blurDp: Float = 15f,
    val specular: Float = 0.30f,
    val enabled: Boolean = true
) {
    init {
        // Enforce clamping rules per architecture guidelines
        require(translucency in 0.05f..0.95f) { "translucency must be between 0.05 and 0.95" }
        require(blurDp in 0f..30f) { "blurDp must be between 0 and 30" }
        require(specular in 0f..1f) { "specular must be between 0 and 1" }
    }

    companion object {
        fun create(
            translucency: Float,
            blurDp: Float,
            specular: Float,
            preset: LauncherThemePreset
        ): GlassStyle {
            return GlassStyle(
                translucency = translucency.coerceIn(0.05f, 0.95f),
                blurDp = blurDp.coerceIn(0f, 30f),
                specular = specular.coerceIn(0f, 1f),
                enabled = preset == LauncherThemePreset.FROSTED_GLASS
            )
        }
    }
}
