package com.example.data

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow

/**
 * WallpaperRepository implementation — Agent C split.
 */
class WallpaperRepositoryImpl(context: Context) : WallpaperRepository {
    private val gitSource = GitHubWallpaperSource(context)
    private val localSource = LocalWallpaperSource(context)
    private val googleSource = GooglePhotosWallpaperSource(context)

    override val autoWallpaperBitmap: StateFlow<Bitmap?> = kotlinx.coroutines.flow.MutableStateFlow(null)
    override val isWallpaperSwitching: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)
    override fun refreshAutoWallpaperNow(overrideConfig: AutoWallpaperConfig?) {}
}
