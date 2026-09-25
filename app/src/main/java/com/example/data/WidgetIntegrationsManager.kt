package com.example.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WidgetIntegrationConfig(
    val weatherPackage: String = "",
    val calendarPackage: String = "",
    val musicPackage: String = "",
    val notesPackage: String = "",
    val clockPackage: String = ""
)

class WidgetIntegrationsManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("flux_widget_integrations", Context.MODE_PRIVATE)

    private val _integrations = MutableStateFlow(loadIntegrations())
    val integrations: StateFlow<WidgetIntegrationConfig> = _integrations.asStateFlow()

    private fun loadIntegrations(): WidgetIntegrationConfig {
        return WidgetIntegrationConfig(
            weatherPackage = prefs.getString("weather_pkg", "") ?: "",
            calendarPackage = prefs.getString("calendar_pkg", "") ?: "",
            musicPackage = prefs.getString("music_pkg", "") ?: "",
            notesPackage = prefs.getString("notes_pkg", "") ?: "",
            clockPackage = prefs.getString("clock_pkg", "") ?: ""
        )
    }

    fun setIntegration(type: String, packageName: String) {
        val current = _integrations.value
        val updated = when (type) {
            "weather" -> {
                prefs.edit().putString("weather_pkg", packageName).apply()
                current.copy(weatherPackage = packageName)
            }
            "calendar" -> {
                prefs.edit().putString("calendar_pkg", packageName).apply()
                current.copy(calendarPackage = packageName)
            }
            "music" -> {
                prefs.edit().putString("music_pkg", packageName).apply()
                current.copy(musicPackage = packageName)
            }
            "notes" -> {
                prefs.edit().putString("notes_pkg", packageName).apply()
                current.copy(notesPackage = packageName)
            }
            "clock" -> {
                prefs.edit().putString("clock_pkg", packageName).apply()
                current.copy(clockPackage = packageName)
            }
            else -> current
        }
        _integrations.value = updated
    }

    fun launchWeatherApp() {
        val customPkg = _integrations.value.weatherPackage
        if (customPkg.isNotBlank() && launchPackage(customPkg)) return

        // Known manufacturer weather packages
        val commonPkgs = listOf(
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
        for (pkg in commonPkgs) {
            if (launchPackage(pkg)) return
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
                    return
                }
            } catch (_: Exception) {}
        }

        Toast.makeText(context, "No weather app found", Toast.LENGTH_SHORT).show()
    }

    fun launchCalendarApp() {
        val customPkg = _integrations.value.calendarPackage
        if (customPkg.isNotBlank() && launchPackage(customPkg)) return

        // Fallback: System Calendar intent
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(context, "No calendar app found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchMusicApp() {
        val customPkg = _integrations.value.musicPackage
        if (customPkg.isNotBlank() && launchPackage(customPkg)) return

        // Check detected YouTube Music / ReVanced package
        val detectedYt = com.example.ui.components.getInstalledYouTubeMusicPackage(context)
        if (detectedYt != null && launchPackage(detectedYt)) return

        // Fallback: Common music players
        val musicPkgs = listOf(
            "app.revanced.android.apps.youtube.music",
            "com.google.android.apps.youtube.music",
            "app.rvx.android.apps.youtube.music",
            "com.vanced.android.apps.youtube.music",
            "com.spotify.music",
            "com.apple.android.music",
            "com.amazon.mp3",
            "deezer.android.app",
            "com.soundcloud.android"
        )
        for (pkg in musicPkgs) {
            if (launchPackage(pkg)) return
        }

        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MUSIC)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No music app found", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchNotesApp() {
        val customPkg = _integrations.value.notesPackage
        if (customPkg.isNotBlank() && launchPackage(customPkg)) return

        val notesPkgs = listOf(
            "com.google.android.keep",
            "com.microsoft.todos",
            "com.notion.id",
            "com.ticktick.task",
            "com.samsung.android.app.notes"
        )
        for (pkg in notesPkgs) {
            if (launchPackage(pkg)) return
        }
        Toast.makeText(context, "Configure a notes app in Flux Settings > Widget Integrations", Toast.LENGTH_SHORT).show()
    }

    fun launchClockApp() {
        val customPkg = _integrations.value.clockPackage
        if (customPkg.isNotBlank() && launchPackage(customPkg)) return

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
                    return
                }
            } catch (_: Exception) {}
        }

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
            if (launchPackage(pkg)) return
        }
        Toast.makeText(context, "No clock app found", Toast.LENGTH_SHORT).show()
    }

    private fun launchPackage(pkg: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
