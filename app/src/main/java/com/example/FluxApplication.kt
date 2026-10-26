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
}
