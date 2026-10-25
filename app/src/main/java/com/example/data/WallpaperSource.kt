package com.example.data

/**
 * Source interface for wallpaper resolution (Agent C split).
 */
interface WallpaperSource {
    suspend fun fetch(config: AutoWallpaperConfig): List<WallpaperItem>
}
