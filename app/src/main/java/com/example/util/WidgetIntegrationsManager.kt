package com.example.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

enum class IntegrationCategory(
    val id: String,
    val displayName: String,
    val defaultDescription: String
) {
    WEATHER("weather", "Weather", "Launch favorite weather radar or forecast app"),
    CALENDAR("calendar", "Calendar", "Launch agenda, schedule, or calendar app"),
    MUSIC("music", "Music Player", "Launch media streaming or audio player app"),
    NOTES("notes", "Notes & Tasks", "Launch note-taking or to-do list app"),
    CLOCK("clock", "Clock & Timer", "Launch alarm clock or world time app")
}

class WidgetIntegrationsManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("flux_widget_integrations", Context.MODE_PRIVATE)

    fun getPackage(category: IntegrationCategory): String? {
        val pkg = prefs.getString("pkg_${category.id}", null)
        return if (pkg.isNullOrBlank() || pkg == "auto") null else pkg
    }

    fun setPackage(category: IntegrationCategory, packageName: String?) {
        if (packageName == null || packageName == "auto") {
            prefs.edit().remove("pkg_${category.id}").apply()
        } else {
            prefs.edit().putString("pkg_${category.id}", packageName).apply()
        }
    }

    fun getAppLabel(category: IntegrationCategory, pm: PackageManager): String {
        val customPkg = getPackage(category)
        if (customPkg != null) {
            try {
                val appInfo = pm.getApplicationInfo(customPkg, 0)
                return pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                // fall through to default
            }
        }
        return "Auto-Detect System Default"
    }

    fun launchClockApp(): Boolean {
        if (launch(IntegrationCategory.CLOCK)) return true

        // Check AlarmClock intents directly
        val alarmIntents = listOf(
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            Intent(AlarmClock.ACTION_SET_ALARM),
            Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.DESK_CLOCK")
        )
        for (intent in alarmIntents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {}
        }

        // Check common manufacturer clock packages
        val clockPkgs = listOf(
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.android.deskclock",
            "com.xiaomi.deskclock",
            "com.miui.clockpackage",
            "com.coloros.alarmclock",
            "com.oppo.alarmclock",
            "com.oneplus.deskclock",
            "com.asus.deskclock"
        )
        for (pkg in clockPkgs) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                try {
                    context.startActivity(launchIntent)
                    return true
                } catch (_: Exception) {}
            }
        }
        Toast.makeText(context, "No default clock app found", Toast.LENGTH_SHORT).show()
        return false
    }

    fun launchWeatherApp(): Boolean {
        if (launch(IntegrationCategory.WEATHER)) return true

        // Try known system weather packages
        val weatherPkgs = listOf(
            "com.google.android.apps.weather",
            "com.sec.android.daemonapp",
            "com.miui.weather2",
            "com.coloros.weather2",
            "com.oneplus.weather",
            "com.realme.weather",
            "com.motorola.timeweatherwidget",
            "com.accuweather.android",
            "com.weather.Weather"
        )
        for (pkg in weatherPkgs) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                try {
                    context.startActivity(launchIntent)
                    return true
                } catch (_: Exception) {}
            }
        }

        // Try Google search weather intent / browser fallback
        val weatherIntents = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("dynact://velour/weather/ProxyActivity")),
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=weather")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather"))
        )
        for (intent in weatherIntents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {}
        }

        Toast.makeText(context, "No weather app found", Toast.LENGTH_SHORT).show()
        return false
    }

    fun launch(category: IntegrationCategory): Boolean {
        val customPkg = getPackage(category)
        if (customPkg != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(customPkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    // ignore and try fallback
                }
            }
        }

        // Fallback default intent resolution
        val fallbackIntent = when (category) {
            IntegrationCategory.CALENDAR -> {
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("content://com.android.calendar/time")
                }
            }
            IntegrationCategory.MUSIC -> {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MUSIC)
                }
            }
            IntegrationCategory.CLOCK -> {
                Intent(AlarmClock.ACTION_SHOW_ALARMS)
            }
            IntegrationCategory.WEATHER -> {
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("geo:0,0?q=weather")
                }
            }
            IntegrationCategory.NOTES -> {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory("android.intent.category.APP_NOTES")
                }
            }
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(fallbackIntent)
                return true
            }
        } catch (e: Exception) {
            // ignore
        }

        Toast.makeText(context, "No default app found for ${category.displayName}", Toast.LENGTH_SHORT).show()
        return false
    }
}
