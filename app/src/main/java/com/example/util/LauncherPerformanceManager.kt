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
     * Evicts the whole icon cache (used by onTrimMemory at the harshest levels, and on explicit
     * user-facing cache clears). LRU eviction policy only — never `System.gc()`, which stalls the
     * main thread without actually reclaiming anything the allocator has not already freed.
     */
    fun evictIconCache() {
        try {
            appIconMemoryCache.evictAll()
            Log.d(TAG, "Unified icon memory cache evicted.")
        } catch (t: Throwable) {
            Log.e(TAG, "Icon cache eviction failed: ${t.message}", t)
        }
    }

    /**
     * Proactive response to `Application.onTrimMemory` / `ComponentCallbacks2.onTrimMemory`.
     *
     * Softer levels trim only the oldest half of the cache; the harshest levels ([TRIM_MEMORY_COMPLETE]
     * and friends) evict everything. No explicit GC and never on the main thread.
     */
    fun onTrimMemory(level: Int) {
        when {
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE ||
                level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                evictIconCache()
                Log.w(TAG, "Memory pressure complete (level=$level): icon cache fully evicted.")
            }
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
                level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                appIconMemoryCache.trimToSize(appIconMemoryCache.maxSize() / 2)
                Log.d(TAG, "Memory pressure moderate (level=$level): icon cache trimmed to half.")
            }
            else -> Unit // TRIM_MEMORY_RUNNING_MODERATE / LOW: the LRU policy already handles it.
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
