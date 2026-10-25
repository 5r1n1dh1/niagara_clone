package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Google Photos Wallpaper Source — extracted for Agent C repository split.
 */
class GooglePhotosWallpaperSource(private val context: Context) : WallpaperSource {
    override suspend fun fetch(config: AutoWallpaperConfig): List<WallpaperItem> =
        withContext(Dispatchers.IO) {
            emptyList()
        }
}
