package com.example.domain

import android.graphics.Bitmap
import com.example.data.AutoWallpaperConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * Wallpaper Repository Interface - Phase 2 Decomposed Domain Layer
 */
interface WallpaperRepository {
    val autoWallpaperBitmap: StateFlow<Bitmap?>
    val isWallpaperSwitching: StateFlow<Boolean>
    fun refreshAutoWallpaperNow(overrideConfig: AutoWallpaperConfig? = null)
}
