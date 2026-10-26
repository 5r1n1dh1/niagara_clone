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

    /** Agent G: bitmap downsample reference to avoid full-resolution decode. */
    fun downsampleBitmap(source: Bitmap, maxDimension: Int = 1024): Bitmap {
        if (source.width <= maxDimension && source.height <= maxDimension) return source
        val ratio = source.width.toFloat() / source.height.toFloat()
        val newW: Int
        val newH: Int
        if (source.width > source.height) {
            newW = maxDimension
            newH = (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            newH = maxDimension
            newW = (maxDimension * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }
}
