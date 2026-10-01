package com.example.domain

import com.example.data.ClockStyle
import com.example.data.IconPackType
import com.example.data.WallpaperStyle

/**
 * Centralized Defaults provider adhering to Phase 8 architectural hardening.
 */
object Defaults {
    const val CLOCK_TINT_HEX = "#FFFFFF"
    const val CLOCK_SCALE = 1.0f
    const val FROSTED_BLUR_RADIUS = 15f
    const val FROSTED_TRANSLUCENCY = 0.30f
    const val FROSTED_GLASS_EFFECT = 0.30f
    const val DOCK_SLOT_COUNT = 4

    val DEFAULT_CLOCK_STYLE = ClockStyle.MINIMAL_DIGITAL
    val DEFAULT_ICON_PACK = IconPackType.ANYCONS_MINIMAL
    val DEFAULT_WALLPAPER_STYLE = WallpaperStyle.SYSTEM_DEVICE
}
