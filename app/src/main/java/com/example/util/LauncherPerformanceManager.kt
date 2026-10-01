package com.example.util

import android.content.Context
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.*

/**
 * ============================================================================
 * UNIFIED LAUNCHER PERFORMANCE & MEMORY MANAGER
 * ============================================================================
 * Implements centralized LRU caching, memory trimming on onTrimMemory,
 * and robust logged error handling for background operations.
 * ============================================================================
 */
object LauncherPerformanceManager {

    private const val TAG = "LauncherPerf"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Unified bounded LRU cache for app icons and thumbnails (approx 8MB)
    private val appIconMemoryCache = object : LruCache<String, ImageBitmap>(80) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return (value.width * value.height * 4) / 1024
        }
    }

    fun getCachedIcon(key: String): ImageBitmap? {
        return appIconMemoryCache.get(key)
    }

    fun cacheIcon(key: String, bitmap: ImageBitmap) {
        if (key.isNotBlank()) {
            appIconMemoryCache.put(key, bitmap)
        }
    }

    /**
     * Aggressively trim memory when launcher goes into background or OS signals low memory.
     */
    fun performAggressiveMemoryCleanup(context: Context) {
        try {
            appIconMemoryCache.evictAll()
            Log.d(TAG, "Unified icon memory cache evicted successfully on memory pressure.")
        } catch (t: Throwable) {
            Log.e(TAG, "Error during memory cleanup: ${t.message}", t)
        }
    }

    /**
     * Executes background tasks on Dispatchers.IO with logged error handling instead of silent swallows.
     */
    fun runInBackground(taskName: String, task: suspend () -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                task()
            } catch (t: Throwable) {
                Log.e(TAG, "Background task '$taskName' failed: ${t.message}", t)
            }
        }
    }
}
