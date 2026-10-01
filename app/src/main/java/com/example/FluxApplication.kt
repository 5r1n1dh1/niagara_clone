package com.example

import android.app.Application

/**
 * Application class for Flux Launcher.
 * Annotated for Dependency Injection and global state lifecycle management.
 */
class FluxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
