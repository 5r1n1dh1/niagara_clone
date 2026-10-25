package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local Wallpaper Source — extracted for Agent C repository split.
 */
class LocalWallpaperSource(private val context: Context) : WallpaperSource {
    override suspend fun fetch(config: AutoWallpaperConfig): List<WallpaperItem> =
        withContext(Dispatchers.IO) {
            emptyList()
        }
}
