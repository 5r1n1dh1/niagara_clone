package com.example.ui.theme

import com.example.data.LauncherThemePreset
import com.example.data.LauncherSettings

object GlassStyleFactory {
    fun fromSettings(settings: LauncherSettings): GlassStyle {
        return GlassStyle.create(
            translucency = settings.frostedTranslucency,
            blurDp = settings.frostedBlurRadius,
            specular = settings.frostedGlassEffect,
            preset = settings.themePreset
        )
    }

    fun default(): GlassStyle = GlassStyle()
}
