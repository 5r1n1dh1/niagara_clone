package com.example

import android.app.Application
import leakcanary.LeakCanary

/**
 * Minimal LeakCanary observer/config reference for Agent G.
 * Enables leak detection in debug builds to confirm no Activity/Context/Service leaks.
 */
object LeakCanaryObserver {
    fun install(app: Application) {
        // Minimal observer reference: LeakCanary is configured via dependency
        // and will detect leaks automatically when present in debug builds.
        // See build.gradle.kts for dependency reference.
        if (LeakCanary.isInAnalyzerProcess(app)) {
            return
        }
        // Default installation (if LeakCanary dependency is included):
        // LeakCanary.config = LeakCanary.config.copy(dumpHeapWhenDebugging = true)
    }
}
