package com.example.util

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * SystemServiceWrapper encapsulating system service actions adhering to Phase 3 guidelines.
 */
class SystemServiceWrapper(private val context: Context) {
    fun expandNotificationShade() {
        try {
            val service = context.getSystemService("statusbar")
            val statusbarManager = Class.forName("android.app.StatusBarManager")
            val method = statusbarManager.getMethod("expandNotificationsPanel")
            method.invoke(service)
        } catch (_: Throwable) {
            try {
                // Fallback action
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (_: Throwable) {}
        }
    }
}
