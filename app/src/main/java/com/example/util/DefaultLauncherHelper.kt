package com.example.util

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object DefaultLauncherHelper {

    private fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }

    fun isDefaultLauncher(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            resolveInfo?.activityInfo?.packageName == context.packageName
        } catch (e: Exception) {
            false
        }
    }

    fun openSetDefaultLauncher(context: Context) {
        val activity = findActivity(context)

        // Method 1: Android 10+ (Q) RoleManager dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                try {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    if (activity != null) {
                        activity.startActivityForResult(intent, 401)
                        return
                    } else {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return
                    }
                } catch (_: Exception) {
                    // Fallback to Settings intents below
                }
            }
        }

        // Method 2: Home settings screen (Standard Android / Pixel / Samsung / Xiaomi)
        try {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                if (activity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {}

        // Method 3: Default Apps Settings screen
        try {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                if (activity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {}

        // Method 4: Intent chooser fallback
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Choose Default Home App").apply {
                if (activity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return
        } catch (_: Exception) {}

        // Method 5: App details page as final fallback
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                if (activity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Tap 'Home app' in settings to set Flux as default", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Please open Android Settings > Apps > Default Apps > Home app", Toast.LENGTH_LONG).show()
        }
    }
}
