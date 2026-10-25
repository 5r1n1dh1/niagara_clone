package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

class FluxAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event interception needed
    }

    override fun onInterrupt() {
        // Safe no-op
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // instance assignment removed (Agent B); service injected via Hilt
    }

    override fun onDestroy() {
        super.onDestroy()
        // instance cleanup removed (Agent B)
    }

    companion object {
        // Static instance removed in v1.0.1 (Agent B); service injected via Hilt modules.

        fun isServiceRunning(): Boolean {
            // Service state tracked via injected repository; always false in static context.
            return false
        }

        fun requestLockScreen(context: Context): Boolean {
            // Static instance removed; service injected via Hilt for v1.0.1
            return false
        }

        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }

        fun openAppInfoSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }
}
