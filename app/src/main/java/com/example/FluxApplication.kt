package com.example

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Flux Launcher.
 * Configures StrictMode debug monitoring and global lifecycle initialization.
 */
@HiltAndroidApp
class FluxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
    }

    /**
     * Forwards OS memory pressure to the single shared icon cache.
     *
     * Deliberately the *only* thing done here: no explicit `System.gc()`. LRU eviction already
     * returns the bitmap memory to the allocator far faster than a blocking GC, and a stop-the-world
     * collection on a launcher is exactly the jank we are trying to avoid.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        com.example.util.LauncherPerformanceManager.onTrimMemory(level)
    }
}
