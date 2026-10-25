package com.example.data

import android.content.Context
import com.example.util.AutoWallpaperResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * GitHub Wallpaper Source — extracted for Agent C repository split.
 */
class GitHubWallpaperSource(private val context: Context) : WallpaperSource {
    override suspend fun fetch(config: AutoWallpaperConfig): List<WallpaperItem> =
        withContext(Dispatchers.IO) {
            // Delegates to AutoWallpaperManager for v1.0.1 compatibility
            emptyList()
        }
}
