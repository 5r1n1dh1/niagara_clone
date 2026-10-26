package com.example

import android.app.Application

/**
 * Minimal LeakCanary observer/config reference for Agent G.
 * LeakCanary is configured via dependency and detects leaks automatically
 * when present in debug builds. This file avoids a hard dependency so the
 * release build compiles even when the LeakCanary artifact is absent.
 */
object LeakCanaryObserver {
    fun install(app: Application) {
        // Minimal observer reference: LeakCanary is configured via dependency
        // and will detect leaks automatically when present in debug builds.
        // See build.gradle.kts for dependency reference.
        try {
            val isInAnalyzerProcess = Class.forName("leakcanary.LeakCanary")
                .getDeclaredMethod("isInAnalyzerProcess", Application::class.java)
            val result = isInAnalyzerProcess.invoke(null, app) as? Boolean
            if (result == true) {
                return
            }
        } catch (_: Throwable) {
            // LeakCanary dependency not present; skip observer installation.
        }
    }
}