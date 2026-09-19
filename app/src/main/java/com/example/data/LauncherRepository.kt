package com.example.data

import android.Manifest
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.os.UserManager
import android.provider.CalendarContract
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import com.example.util.SystemWidgetProviderItem
import com.example.util.IntegrationCategory
import com.example.util.WidgetIntegrationsManager

class LauncherRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("niagara_launcher_prefs", Context.MODE_PRIVATE)
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _isAppsLoading = MutableStateFlow(true)
    val isAppsLoading: StateFlow<Boolean> = _isAppsLoading.asStateFlow()

    private val _availableSystemWidgets = MutableStateFlow<List<AppWidgetProviderInfo>>(emptyList())
    val availableSystemWidgets: StateFlow<List<AppWidgetProviderInfo>> = _availableSystemWidgets.asStateFlow()

    private val _allApps = MutableStateFlow<List<AppItem>>(emptyList())
    val allApps: StateFlow<List<AppItem>> = _allApps.asStateFlow()

    private val _favorites = MutableStateFlow<List<AppItem>>(emptyList())
    val favorites: StateFlow<List<AppItem>> = _favorites.asStateFlow()

    private val _folders = MutableStateFlow<List<FolderItem>>(emptyList())
    val folders: StateFlow<List<FolderItem>> = _folders.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _weather = MutableStateFlow(WeatherInfo())
    val weather: StateFlow<WeatherInfo> = _weather.asStateFlow()

    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    private val _mediaPlayer = MutableStateFlow(MediaPlaybackState())
    val mediaPlayer: StateFlow<MediaPlaybackState> = _mediaPlayer.asStateFlow()

    private val _activeNotifications = MutableStateFlow<Map<String, NotificationItem>>(emptyMap())
    val activeNotifications: StateFlow<Map<String, NotificationItem>> = _activeNotifications.asStateFlow()

    private val _allActiveNotifications = MutableStateFlow<Map<String, List<NotificationItem>>>(emptyMap())
    val allActiveNotifications: StateFlow<Map<String, List<NotificationItem>>> = _allActiveNotifications.asStateFlow()

    private val _leftWidgets = MutableStateFlow<List<CustomWidgetConfig>>(emptyList())
    val leftWidgets: StateFlow<List<CustomWidgetConfig>> = _leftWidgets.asStateFlow()

    private val _carouselWidgets = MutableStateFlow<List<WidgetType>>(emptyList())
    val carouselWidgets: StateFlow<List<WidgetType>> = _carouselWidgets.asStateFlow()

    private val _dockPackageNames = MutableStateFlow<List<String>>(emptyList())
    val dockPackageNames: StateFlow<List<String>> = _dockPackageNames.asStateFlow()

    val widgetIntegrations = WidgetIntegrationsManager(context)

    init {
        com.example.util.SystemWidgetManager.init(context)
        observeLiveNotifications()
        loadAvailableSystemWidgets()
        loadLeftWidgets()
        loadCarouselWidgets()
        loadDockPackages()
        loadFolders()
        refreshCalendarEvents()
        reloadApps()
    }

    private fun observeLiveNotifications() {
        repositoryScope.launch {
            com.example.service.FluxNotificationListenerService.liveNotifications.collect { liveMap ->
                _activeNotifications.value = liveMap
                val currentApps = _allApps.value
                if (currentApps.isNotEmpty()) {
                    val updated = currentApps.map { app ->
                        val notif = liveMap[app.packageName]
                        if (app.notification != notif) app.copy(notification = notif) else app
                    }
                    _allApps.value = updated
                }
            }
        }
        repositoryScope.launch {
            com.example.service.FluxNotificationListenerService.allLiveNotifications.collect { multiMap ->
                _allActiveNotifications.value = multiMap
            }
        }
    }

    private fun loadSettings(): LauncherSettings {
        val customPreset = try {
            CustomPackPreset.valueOf(prefs.getString("custom_icon_preset", CustomPackPreset.WHICONS_WHITE.name) ?: CustomPackPreset.WHICONS_WHITE.name)
        } catch (e: Exception) {
            CustomPackPreset.WHICONS_WHITE
        }
        val customShape = try {
            IconShape.valueOf(prefs.getString("custom_icon_shape", IconShape.ROUNDED_SQUIRCLE.name) ?: IconShape.ROUNDED_SQUIRCLE.name)
        } catch (e: Exception) {
            IconShape.ROUNDED_SQUIRCLE
        }
        val customColor = prefs.getString("custom_icon_color", "#FFFFFF") ?: "#FFFFFF"
        val customOpacity = prefs.getFloat("custom_icon_opacity", 0.22f)
        val installedPackPkg = prefs.getString("custom_installed_pack_pkg", null)
        val installedPackName = prefs.getString("custom_installed_pack_name", null)

        val customIconSettings = CustomIconPackSettings(
            packPreset = customPreset,
            iconShape = customShape,
            customColorHex = customColor,
            backgroundOpacity = customOpacity,
            installedPackPackageName = installedPackPkg,
            installedPackName = installedPackName
        )

        return LauncherSettings(
            leftHandedMode = prefs.getBoolean("left_handed_mode", false),
            hideStatusBar = prefs.getBoolean("hide_status_bar", false),
            clockStyle = ClockStyle.entries.getOrElse(prefs.getInt("clock_style", ClockStyle.FLUX_INLINE.ordinal)) { ClockStyle.FLUX_INLINE },
            showSeconds = prefs.getBoolean("show_seconds", false),
            showDate = prefs.getBoolean("show_date", true),
            showClockHeader = prefs.getBoolean("show_clock_header", true),
            iconPackType = IconPackType.entries.getOrElse(prefs.getInt("icon_pack", 0)) { IconPackType.MATERIAL },
            themePreset = LauncherThemePreset.entries.getOrElse(prefs.getInt("theme_preset", LauncherThemePreset.FLUX_EMERALD.ordinal)) { LauncherThemePreset.FLUX_EMERALD },
            wallpaperStyle = WallpaperStyle.entries.getOrElse(prefs.getInt("wallpaper_style", WallpaperStyle.FLUX_EMBLEM.ordinal)) { WallpaperStyle.FLUX_EMBLEM },
            customFont = AppFontFamily.entries.getOrElse(prefs.getInt("custom_font", 0)) { AppFontFamily.MODERN_SANS },
            fontScale = prefs.getFloat("font_scale", 1.0f),
            hapticFeedback = prefs.getBoolean("haptic_feedback", true),
            doubleTapToSleep = prefs.getBoolean("double_tap_sleep", true),
            swipeDownForNotificationCenter = prefs.getBoolean("swipe_down_notif", true),
            klwpCompatMode = prefs.getBoolean("klwp_compat", true),
            audioAppAutoSurfacing = prefs.getBoolean("audio_surfacing", true),
            usageBreakerDurationSeconds = prefs.getInt("usage_breaker_sec", 5),
            isHeadphonesConnected = prefs.getBoolean("headphones_connected", false),
            isGoogleDiscoverEnabled = prefs.getBoolean("google_discover", true),
            appIconScale = prefs.getFloat("app_icon_scale", 1.0f),
            showAppNotificationBadges = prefs.getBoolean("app_notif_badges", true),
            compactAppRowSpacing = prefs.getBoolean("compact_row_spacing", false),
            showDock = prefs.getBoolean("show_dock", true),
            dockSlotCount = prefs.getInt("dock_slot_count", 3).coerceIn(3, 5),
            showDockLabels = prefs.getBoolean("show_dock_labels", false),
            dockBlurBackground = prefs.getBoolean("dock_blur_bg", true),
            dockIconScale = prefs.getFloat("dock_icon_scale", 1.0f),
            waveAlphabetHapticIntensity = prefs.getInt("wave_haptic_intensity", 2),
            customIconSettings = customIconSettings,
            hasCompletedOnboarding = prefs.getBoolean("has_completed_onboarding", false)
        )
    }

    fun updateSettings(newSettings: LauncherSettings) {
        _settings.value = newSettings
        prefs.edit()
            .putBoolean("left_handed_mode", newSettings.leftHandedMode)
            .putBoolean("hide_status_bar", newSettings.hideStatusBar)
            .putInt("clock_style", newSettings.clockStyle.ordinal)
            .putBoolean("show_seconds", newSettings.showSeconds)
            .putBoolean("show_date", newSettings.showDate)
            .putBoolean("show_clock_header", newSettings.showClockHeader)
            .putInt("icon_pack", newSettings.iconPackType.ordinal)
            .putInt("theme_preset", newSettings.themePreset.ordinal)
            .putInt("wallpaper_style", newSettings.wallpaperStyle.ordinal)
            .putInt("custom_font", newSettings.customFont.ordinal)
            .putFloat("font_scale", newSettings.fontScale)
            .putBoolean("haptic_feedback", newSettings.hapticFeedback)
            .putBoolean("double_tap_sleep", newSettings.doubleTapToSleep)
            .putBoolean("swipe_down_notif", newSettings.swipeDownForNotificationCenter)
            .putBoolean("klwp_compat", newSettings.klwpCompatMode)
            .putBoolean("audio_surfacing", newSettings.audioAppAutoSurfacing)
            .putInt("usage_breaker_sec", newSettings.usageBreakerDurationSeconds)
            .putBoolean("headphones_connected", newSettings.isHeadphonesConnected)
            .putBoolean("google_discover", newSettings.isGoogleDiscoverEnabled)
            .putFloat("app_icon_scale", newSettings.appIconScale)
            .putBoolean("app_notif_badges", newSettings.showAppNotificationBadges)
            .putBoolean("compact_row_spacing", newSettings.compactAppRowSpacing)
            .putBoolean("show_dock", newSettings.showDock)
            .putInt("dock_slot_count", newSettings.dockSlotCount)
            .putBoolean("show_dock_labels", newSettings.showDockLabels)
            .putBoolean("dock_blur_bg", newSettings.dockBlurBackground)
            .putFloat("dock_icon_scale", newSettings.dockIconScale)
            .putInt("wave_haptic_intensity", newSettings.waveAlphabetHapticIntensity)
            .putString("custom_icon_preset", newSettings.customIconSettings.packPreset.name)
            .putString("custom_icon_shape", newSettings.customIconSettings.iconShape.name)
            .putString("custom_icon_color", newSettings.customIconSettings.customColorHex)
            .putFloat("custom_icon_opacity", newSettings.customIconSettings.backgroundOpacity)
            .putString("custom_installed_pack_pkg", newSettings.customIconSettings.installedPackPackageName)
            .putString("custom_installed_pack_name", newSettings.customIconSettings.installedPackName)
            .putBoolean("has_completed_onboarding", newSettings.hasCompletedOnboarding)
            .apply()
    }

    fun completeOnboarding() {
        updateSettings(_settings.value.copy(hasCompletedOnboarding = true))
    }

    fun resetOnboarding() {
        updateSettings(_settings.value.copy(hasCompletedOnboarding = false))
    }

    fun setFavoritePackages(packages: Collection<String>) {
        saveFavoritePackages(packages.toSet())
        reloadApps()
    }

    fun setCarouselWidgets(widgets: List<WidgetType>) {
        val sanitized = widgets.distinct().take(5)
        _carouselWidgets.value = if (sanitized.isEmpty()) listOf(WidgetType.WEATHER_FORECAST) else sanitized
        saveCarouselWidgets(_carouselWidgets.value)
    }

    fun setLeftWidgetsList(widgets: List<CustomWidgetConfig>) {
        _leftWidgets.value = widgets
        saveLeftWidgets(widgets)
    }

    fun getInstalledIconPacks(): List<InstalledIconPackInfo> {
        val pm = context.packageManager
        val list = mutableListOf<InstalledIconPackInfo>()
        val actions = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.fede.launcher.THEME_ICONPACK",
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME"
        )
        val seenPkgs = mutableSetOf<String>()
        for (action in actions) {
            try {
                val intent = Intent(action)
                val resolveList = pm.queryIntentActivities(intent, 0)
                for (info in resolveList) {
                    val pkg = info.activityInfo.packageName
                    if (seenPkgs.add(pkg)) {
                        val label = info.loadLabel(pm).toString()
                        val icon = try { info.loadIcon(pm) } catch (e: Exception) { null }
                        list.add(InstalledIconPackInfo(packageName = pkg, label = label, icon = icon))
                    }
                }
            } catch (e: Exception) {
                // Ignore query failures
            }
        }
        return list
    }

    fun loadAvailableSystemWidgets() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val providers = appWidgetManager.installedProviders ?: emptyList()
            _availableSystemWidgets.value = providers
        } catch (t: Throwable) {
            _availableSystemWidgets.value = emptyList()
        }
    }

    fun isWidgetAvailable(type: WidgetType): Boolean {
        return when (type) {
            WidgetType.CALENDAR_AGENDA -> {
                try {
                    val calendarIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
                    val resolve = context.packageManager.queryIntentActivities(calendarIntent, 0)
                    resolve.isNotEmpty() || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                } catch (t: Throwable) {
                    true
                }
            }
            WidgetType.MEDIA_PLAYER -> true
            WidgetType.WEATHER_FORECAST -> true
            WidgetType.SYSTEM_TOGGLES -> true
            WidgetType.QUICK_NOTE -> true
            WidgetType.CLOCK -> true
            WidgetType.DAILY_HABITS -> true
            WidgetType.MINDFULNESS_QUOTE -> true
            WidgetType.DUAL_CLOCK -> true
            WidgetType.SYSTEM_APPWIDGET -> true
        }
    }

    fun refreshCalendarEvents() {
        repositoryScope.launch(Dispatchers.IO) {
            val events = readDeviceCalendarEvents()
            _calendarEvents.value = events
        }
    }

    private fun readDeviceCalendarEvents(): List<CalendarEvent> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        val events = mutableListOf<CalendarEvent>()
        try {
            val now = System.currentTimeMillis()
            val endOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, now)
            ContentUris.appendId(builder, endOfDay)

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.EVENT_LOCATION
            )

            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val locIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

                while (cursor.moveToNext() && events.size < 5) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx).toString() else UUID.randomUUID().toString()
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "Event" else "Event"
                    val begin = if (beginIdx >= 0) cursor.getLong(beginIdx) else now
                    val loc = if (locIdx >= 0) cursor.getString(locIdx) ?: "" else ""

                    events.add(
                        CalendarEvent(
                            id = id,
                            title = title,
                            time = timeFormat.format(Date(begin)),
                            location = loc.ifBlank { "Calendar" },
                            isSoon = begin - now in 1..3600000
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            // Handle safely without crash
        }
        return events
    }

    fun getBatteryLevel(): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        } catch (t: Throwable) {
            85
        }
    }

    private fun loadDefaultNotifications() {
        // Active notifications are collected in real-time from FluxNotificationListenerService
    }

    fun reloadApps() {
        _isAppsLoading.value = true
        repositoryScope.launch(Dispatchers.IO) {
            try {
                loadInstalledAppsInternal()
            } catch (t: Throwable) {
                // Safeguard against any unexpected errors
            } finally {
                _isAppsLoading.value = false
            }
        }
    }

    private fun loadInstalledAppsInternal() {
        val pm = context.packageManager
        val installedAppItems = mutableListOf<AppItem>()
        val seenPackages = mutableSetOf<String>()

        // 1. Primary: query via LauncherApps service (the official Android API for Home Screen apps)
        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            val profiles = userManager?.userProfiles ?: listOf(Process.myUserHandle())

            for (profile in profiles) {
                val activityList = launcherApps?.getActivityList(null, profile) ?: emptyList()
                for (activityInfo in activityList) {
                    val pkg = activityInfo.applicationInfo.packageName
                    if (pkg == context.packageName || seenPackages.contains(pkg)) continue
                    seenPackages.add(pkg)

                    val label = try {
                        activityInfo.label?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                            ?: pm.getApplicationLabel(activityInfo.applicationInfo).toString()
                    } catch (t: Throwable) {
                        pkg
                    }

                    val iconDrawable = try {
                        activityInfo.getBadgedIcon(context.resources.displayMetrics.densityDpi)
                    } catch (t: Throwable) {
                        try {
                            activityInfo.applicationInfo.loadIcon(pm)
                        } catch (t2: Throwable) {
                            null
                        }
                    }

                    installedAppItems.add(
                        AppItem(
                            packageName = pkg,
                            activityName = activityInfo.componentName.className,
                            label = label,
                            shortcuts = listOf("App info", "Share", "Open"),
                            iconDrawable = iconDrawable,
                            notification = _activeNotifications.value[pkg]
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            // Fall through to queryIntentActivities
        }

        // 2. Secondary fallback/supplement: queryIntentActivities
        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            for (info in resolveInfos) {
                val pkg = try { info.activityInfo?.packageName } catch (t: Throwable) { null } ?: continue
                if (pkg == context.packageName || seenPackages.contains(pkg)) continue
                seenPackages.add(pkg)

                val actName = try { info.activityInfo?.name ?: "" } catch (t: Throwable) { "" }
                val label = try {
                    info.loadLabel(pm)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                        ?: info.activityInfo?.name
                        ?: pkg
                } catch (t: Throwable) {
                    pkg
                }

                val iconDrawable = try {
                    info.loadIcon(pm)
                } catch (t: Throwable) {
                    null
                }

                installedAppItems.add(
                    AppItem(
                        packageName = pkg,
                        activityName = actName,
                        label = label,
                        shortcuts = listOf("App info", "Share", "Open"),
                        iconDrawable = iconDrawable,
                        notification = _activeNotifications.value[pkg]
                    )
                )
            }
        } catch (t: Throwable) {
            // Safe
        }

        // Sort alphabetically by localized label
        installedAppItems.sortBy { it.label.lowercase(Locale.getDefault()) }

        val favoritePackages = getSavedFavoritePackages()
        val hiddenPackages = getSavedHiddenPackages()
        val usageBreakerPackages = getSavedUsageBreakerPackages()

        val finalAppItems = installedAppItems.map { app ->
            app.copy(
                isFavorite = favoritePackages.contains(app.packageName),
                isHidden = hiddenPackages.contains(app.packageName),
                isUsageBreakerActive = usageBreakerPackages.contains(app.packageName)
            )
        }

        _allApps.value = finalAppItems

        // Set favorites strictly from REAL INSTALLED APPS
        val favList = finalAppItems.filter { it.isFavorite && !it.isHidden }.toMutableList()
        if (favList.isEmpty() && finalAppItems.isNotEmpty()) {
            val preferredMatchers = listOf(
                { app: AppItem -> app.packageName.contains("dialer", true) || app.packageName.contains("phone", true) || app.label.equals("Phone", true) },
                { app: AppItem -> app.packageName.contains("messaging", true) || app.packageName.contains("message", true) || app.label.equals("Messages", true) },
                { app: AppItem -> app.packageName.contains("chrome", true) || app.packageName.contains("browser", true) || app.packageName.contains("sbrowser", true) || app.label.contains("Browser", true) },
                { app: AppItem -> app.packageName.contains("camera", true) || app.label.equals("Camera", true) },
                { app: AppItem -> app.packageName.contains("spotify", true) || app.packageName.contains("music", true) || app.label.contains("Music", true) },
                { app: AppItem -> app.packageName.contains("settings", true) || app.label.equals("Settings", true) }
            )

            for (matcher in preferredMatchers) {
                val match = finalAppItems.find { matcher(it) && !favList.contains(it) && !it.isHidden }
                if (match != null) {
                    favList.add(match.copy(isFavorite = true))
                }
            }

            if (favList.size < 5) {
                for (app in finalAppItems) {
                    if (favList.size >= 5) break
                    if (!favList.any { it.packageName == app.packageName } && !app.isHidden) {
                        favList.add(app.copy(isFavorite = true))
                    }
                }
            }
        }

        if (_settings.value.isHeadphonesConnected && _settings.value.audioAppAutoSurfacing) {
            val musicApp = finalAppItems.find { it.packageName.contains("spotify", true) || it.label.contains("music", true) }
            if (musicApp != null) {
                favList.removeAll { it.packageName == musicApp.packageName }
                favList.add(0, musicApp)
            }
        }

        _favorites.value = favList

        // Synchronize dock icons with real installed packages
        val savedDock = _dockPackageNames.value
        val validDock = savedDock.filter { pkg -> finalAppItems.any { it.packageName == pkg } }.toMutableList()
        if (validDock.size < 3 && finalAppItems.isNotEmpty()) {
            for (fav in favList) {
                if (validDock.size >= 3) break
                if (!validDock.contains(fav.packageName)) {
                    validDock.add(fav.packageName)
                }
            }
            if (validDock.size < 3) {
                for (app in finalAppItems) {
                    if (validDock.size >= 3) break
                    if (!validDock.contains(app.packageName)) {
                        validDock.add(app.packageName)
                    }
                }
            }
            _dockPackageNames.value = validDock.take(3)
        }
    }

    private fun loadFolders() {
        val savedFoldersJson = prefs.getString("folders_json", null)
        if (savedFoldersJson != null) {
            try {
                val array = JSONArray(savedFoldersJson)
                val list = mutableListOf<FolderItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pkgs = mutableListOf<String>()
                    val pkgArr = obj.optJSONArray("packages")
                    if (pkgArr != null) {
                        for (j in 0 until pkgArr.length()) {
                            pkgs.add(pkgArr.getString(j))
                        }
                    }
                    list.add(
                        FolderItem(
                            id = obj.optString("id", "folder_$i"),
                            name = obj.optString("name", "Folder"),
                            iconEmoji = obj.optString("icon", "📁"),
                            appPackageNames = pkgs
                        )
                    )
                }
                _folders.value = list
                return
            } catch (e: Exception) {
                // fall through to defaults
            }
        }

        // Default folders
        _folders.value = listOf(
            FolderItem(
                id = "f_social",
                name = "Social & Chat",
                iconEmoji = "💬",
                appPackageNames = listOf("com.whatsapp", "com.instagram.android", "com.twitter.android", "com.discord")
            ),
            FolderItem(
                id = "f_work",
                name = "Work & Tools",
                iconEmoji = "💼",
                appPackageNames = listOf("com.slack", "com.github.android", "com.google.android.gm", "com.google.android.keep")
            )
        )
    }

    fun toggleFolderExpanded(folderId: String) {
        _folders.value = _folders.value.map {
            if (it.id == folderId) it.copy(isExpanded = !it.isExpanded) else it
        }
    }

    fun createFolder(name: String, iconEmoji: String, appPackageNames: List<String>) {
        val newFolder = FolderItem(
            id = "f_${System.currentTimeMillis()}",
            name = name,
            iconEmoji = iconEmoji,
            appPackageNames = appPackageNames
        )
        val updated = _folders.value + newFolder
        _folders.value = updated
        saveFolders(updated)
    }

    private fun saveFolders(folders: List<FolderItem>) {
        val arr = JSONArray()
        for (f in folders) {
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("name", f.name)
            obj.put("icon", f.iconEmoji)
            val pkgArr = JSONArray()
            for (p in f.appPackageNames) pkgArr.put(p)
            obj.put("packages", pkgArr)
            arr.put(obj)
        }
        prefs.edit().putString("folders_json", arr.toString()).apply()
    }

    fun toggleFavorite(packageName: String) {
        val currentFavs = getSavedFavoritePackages().toMutableSet()
        val isNowFav = if (currentFavs.contains(packageName)) {
            currentFavs.remove(packageName)
            false
        } else {
            currentFavs.add(packageName)
            true
        }
        saveFavoritePackages(currentFavs)

        // Instant in-memory state update without full reloading spinner
        val updatedApps = _allApps.value.map {
            if (it.packageName == packageName) it.copy(isFavorite = isNowFav) else it
        }
        _allApps.value = updatedApps
        val targetApp = updatedApps.find { it.packageName == packageName }
        if (isNowFav && targetApp != null) {
            if (!_favorites.value.any { it.packageName == packageName }) {
                _favorites.value = _favorites.value + targetApp
            }
        } else {
            _favorites.value = _favorites.value.filter { it.packageName != packageName }
        }
    }

    fun reorderFavorites(fromIndex: Int, toIndex: Int) {
        val current = _favorites.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _favorites.value = current
            saveFavoritePackages(current.map { it.packageName }.toSet())
        }
    }

    fun toggleHideApp(packageName: String) {
        val hidden = getSavedHiddenPackages().toMutableSet()
        val isNowHidden = if (hidden.contains(packageName)) {
            hidden.remove(packageName)
            false
        } else {
            hidden.add(packageName)
            true
        }
        saveHiddenPackages(hidden)
        _allApps.value = _allApps.value.map {
            if (it.packageName == packageName) it.copy(isHidden = isNowHidden) else it
        }
        if (isNowHidden) {
            _favorites.value = _favorites.value.filter { it.packageName != packageName }
        }
    }

    fun toggleUsageBreaker(packageName: String) {
        val ub = getSavedUsageBreakerPackages().toMutableSet()
        val isNowActive = if (ub.contains(packageName)) {
            ub.remove(packageName)
            false
        } else {
            ub.add(packageName)
            true
        }
        saveUsageBreakerPackages(ub)
        _allApps.value = _allApps.value.map {
            if (it.packageName == packageName) it.copy(isUsageBreakerActive = isNowActive) else it
        }
        _favorites.value = _favorites.value.map {
            if (it.packageName == packageName) it.copy(isUsageBreakerActive = isNowActive) else it
        }
    }

    fun disguiseApp(packageName: String, newName: String?) {
        val map = getDisguisedMap().toMutableMap()
        if (newName.isNullOrBlank()) {
            map.remove(packageName)
        } else {
            map[packageName] = newName
        }
        saveDisguisedMap(map)
        _allApps.value = _allApps.value.map {
            if (it.packageName == packageName) it.copy(disguisedName = newName) else it
        }
        _favorites.value = _favorites.value.map {
            if (it.packageName == packageName) it.copy(disguisedName = newName) else it
        }
    }

    fun replyToNotification(packageName: String, replyText: String, key: String? = null) {
        if (key != null) {
            com.example.service.FluxNotificationListenerService.instance?.replyNotificationByKey(key, replyText)
        } else {
            com.example.service.FluxNotificationListenerService.replyToPackage(packageName, replyText)
        }
        val current = _activeNotifications.value.toMutableMap()
        current.remove(packageName)
        _activeNotifications.value = current
        val currentApps = _allApps.value
        _allApps.value = currentApps.map {
            if (it.packageName == packageName) it.copy(notification = null) else it
        }
        _favorites.value = _favorites.value.map {
            if (it.packageName == packageName) it.copy(notification = null) else it
        }
    }

    fun dismissNotification(packageName: String, key: String? = null) {
        if (key != null) {
            com.example.service.FluxNotificationListenerService.dismissNotification(key)
        }
        com.example.service.FluxNotificationListenerService.dismissAllForPackage(packageName)
        val current = _activeNotifications.value.toMutableMap()
        current.remove(packageName)
        _activeNotifications.value = current
        val currentApps = _allApps.value
        _allApps.value = currentApps.map {
            if (it.packageName == packageName) it.copy(notification = null) else it
        }
        _favorites.value = _favorites.value.map {
            if (it.packageName == packageName) it.copy(notification = null) else it
        }
    }

    fun simulateIncomingNotification(packageName: String, sender: String, message: String) {
        val notif = NotificationItem(
            id = "sim_${System.currentTimeMillis()}",
            packageName = packageName,
            sender = sender,
            message = message,
            timeAgo = "just now",
            canReply = true
        )
        val current = _activeNotifications.value.toMutableMap()
        current[packageName] = notif
        _activeNotifications.value = current
        reloadApps()
    }

    fun togglePlayPauseMedia() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
        try {
            val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            audioManager?.dispatchMediaKeyEvent(downEvent)
            audioManager?.dispatchMediaKeyEvent(upEvent)
        } catch (_: Exception) {}

        val newPlayingState = !_mediaPlayer.value.isPlaying
        val musicPkg = widgetIntegrations.getPackage(IntegrationCategory.MUSIC)
        val appLabel = if (musicPkg != null) {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(musicPkg, PackageManager.GET_META_DATA)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                _mediaPlayer.value.appName
            }
        } else {
            _mediaPlayer.value.appName
        }

        _mediaPlayer.value = _mediaPlayer.value.copy(
            isPlaying = newPlayingState,
            appName = appLabel
        )
    }

    fun skipNextMedia() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
        try {
            val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            audioManager?.dispatchMediaKeyEvent(downEvent)
            audioManager?.dispatchMediaKeyEvent(upEvent)
        } catch (_: Exception) {}

        val musicPkg = widgetIntegrations.getPackage(IntegrationCategory.MUSIC)
        val currentApp = if (musicPkg != null) widgetIntegrations.getAppLabel(IntegrationCategory.MUSIC, context.packageManager) else "Spotify"

        val tracks = listOf(
            MediaPlaybackState(true, "Midnight City", "M83", currentApp, 0.35f, "4:03", "1:25"),
            MediaPlaybackState(true, "Starboy", "The Weeknd, Daft Punk", currentApp, 0.12f, "3:50", "0:27"),
            MediaPlaybackState(true, "Solar Power", "Lorde", currentApp, 0.65f, "3:12", "2:04"),
            MediaPlaybackState(true, "Resonance", "HOME", currentApp, 0.80f, "3:32", "2:49")
        )
        val next = tracks.random()
        _mediaPlayer.value = next
    }

    fun syncMusicIntegration() {
        val musicPkg = widgetIntegrations.getPackage(IntegrationCategory.MUSIC)
        if (musicPkg != null) {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(musicPkg, PackageManager.GET_META_DATA)
                val label = pm.getApplicationLabel(appInfo).toString()
                _mediaPlayer.value = _mediaPlayer.value.copy(
                    appName = label
                )
            } catch (_: Exception) {}
        }
    }

    fun skipPreviousMedia() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
        try {
            val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            audioManager?.dispatchMediaKeyEvent(downEvent)
            audioManager?.dispatchMediaKeyEvent(upEvent)
        } catch (_: Exception) {}

        val musicPkg = widgetIntegrations.getPackage(IntegrationCategory.MUSIC)
        val currentApp = if (musicPkg != null) widgetIntegrations.getAppLabel(IntegrationCategory.MUSIC, context.packageManager) else "Spotify"
        val tracks = listOf(
            MediaPlaybackState(true, "Resonance", "HOME", currentApp, 0.10f, "3:32", "0:21"),
            MediaPlaybackState(true, "Solar Power", "Lorde", currentApp, 0.05f, "3:12", "0:10"),
            MediaPlaybackState(true, "Starboy", "The Weeknd, Daft Punk", currentApp, 0.08f, "3:50", "0:18"),
            MediaPlaybackState(true, "Midnight City", "M83", currentApp, 0.15f, "4:03", "0:36")
        )
        _mediaPlayer.value = tracks.random()
    }

    fun seekMedia(progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        val totalSec = 240
        val currentSec = (p * totalSec).toInt()
        val min = currentSec / 60
        val sec = currentSec % 60
        val timeStr = String.format(Locale.getDefault(), "%d:%02d", min, sec)
        _mediaPlayer.value = _mediaPlayer.value.copy(
            progressPercent = p,
            currentTimeText = timeStr
        )
    }

    fun setHeadphonesConnected(connected: Boolean) {
        updateSettings(_settings.value.copy(isHeadphonesConnected = connected))
        reloadApps()
    }

    // Backup & Restore
    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val favArr = JSONArray()
        for (f in getSavedFavoritePackages()) favArr.put(f)
        root.put("favorites", favArr)

        val hidArr = JSONArray()
        for (h in getSavedHiddenPackages()) hidArr.put(h)
        root.put("hidden", hidArr)

        val ubArr = JSONArray()
        for (u in getSavedUsageBreakerPackages()) ubArr.put(u)
        root.put("usageBreaker", ubArr)

        val s = _settings.value
        val settObj = JSONObject()
        settObj.put("leftHanded", s.leftHandedMode)
        settObj.put("hideStatusBar", s.hideStatusBar)
        settObj.put("clockStyle", s.clockStyle.name)
        settObj.put("iconPack", s.iconPackType.name)
        settObj.put("themePreset", s.themePreset.name)
        settObj.put("customFont", s.customFont.name)
        root.put("settings", settObj)

        return root.toString(2)
    }

    fun importBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val favArr = root.optJSONArray("favorites")
            if (favArr != null) {
                val favs = mutableSetOf<String>()
                for (i in 0 until favArr.length()) favs.add(favArr.getString(i))
                saveFavoritePackages(favs)
            }
            val hidArr = root.optJSONArray("hidden")
            if (hidArr != null) {
                val hids = mutableSetOf<String>()
                for (i in 0 until hidArr.length()) hids.add(hidArr.getString(i))
                saveHiddenPackages(hids)
            }
            val settObj = root.optJSONObject("settings")
            if (settObj != null) {
                val s = _settings.value.copy(
                    leftHandedMode = settObj.optBoolean("leftHanded", _settings.value.leftHandedMode),
                    hideStatusBar = settObj.optBoolean("hideStatusBar", _settings.value.hideStatusBar),
                    clockStyle = ClockStyle.entries.find { it.name == settObj.optString("clockStyle") } ?: _settings.value.clockStyle,
                    iconPackType = IconPackType.entries.find { it.name == settObj.optString("iconPack") } ?: _settings.value.iconPackType,
                    themePreset = LauncherThemePreset.entries.find { it.name == settObj.optString("themePreset") } ?: _settings.value.themePreset
                )
                updateSettings(s)
            }
            reloadApps()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _settings.value = LauncherSettings()
        reloadApps()
        loadFolders()
    }

    private fun getSavedFavoritePackages(): Set<String> {
        return prefs.getStringSet("favorite_packages", emptySet()) ?: emptySet()
    }

    private fun saveFavoritePackages(set: Set<String>) {
        prefs.edit().putStringSet("favorite_packages", set).apply()
    }

    private fun getSavedHiddenPackages(): Set<String> {
        return prefs.getStringSet("hidden_packages", emptySet()) ?: emptySet()
    }

    private fun saveHiddenPackages(set: Set<String>) {
        prefs.edit().putStringSet("hidden_packages", set).apply()
    }

    private fun getSavedUsageBreakerPackages(): Set<String> {
        return prefs.getStringSet("usage_breaker_packages", emptySet()) ?: emptySet()
    }

    private fun saveUsageBreakerPackages(set: Set<String>) {
        prefs.edit().putStringSet("usage_breaker_packages", set).apply()
    }

    private fun getDisguisedMap(): Map<String, String> {
        val str = prefs.getString("disguised_map", null) ?: return emptyMap()
        val obj = JSONObject(str)
        val map = mutableMapOf<String, String>()
        for (key in obj.keys()) {
            map[key] = obj.getString(key)
        }
        return map
    }

    private fun saveDisguisedMap(map: Map<String, String>) {
        val obj = JSONObject()
        for ((k, v) in map) obj.put(k, v)
        prefs.edit().putString("disguised_map", obj.toString()).apply()
    }

    // ==========================================
    // 1. LEFT SCREEN WIDGETS (CRUD)
    // ==========================================
    private fun loadLeftWidgets() {
        val jsonStr = prefs.getString("left_widgets_json", null)
        if (jsonStr != null) {
            try {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<CustomWidgetConfig>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val typeStr = obj.optString("type", WidgetType.WEATHER_FORECAST.name)
                    val type = WidgetType.entries.find { it.name == typeStr } ?: WidgetType.WEATHER_FORECAST
                    list.add(
                        CustomWidgetConfig(
                            id = obj.optString("id", "w_$i"),
                            type = type,
                            title = obj.optString("title", type.displayName),
                            content = obj.optString("content", ""),
                            isEnabled = obj.optBoolean("isEnabled", true),
                            appWidgetId = obj.optInt("appWidgetId", -1),
                            providerPackage = if (obj.has("providerPackage")) obj.optString("providerPackage") else null,
                            providerClass = if (obj.has("providerClass")) obj.optString("providerClass") else null,
                            linkedPackageName = if (obj.has("linkedPackageName")) obj.optString("linkedPackageName") else null
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    _leftWidgets.value = list
                    return
                }
            } catch (e: Exception) {
                // fallback to default widgets
            }
        }

        // Default Left Page widgets
        _leftWidgets.value = listOf(
            CustomWidgetConfig("w_weather", WidgetType.WEATHER_FORECAST, "Weather & Forecast"),
            CustomWidgetConfig("w_media", WidgetType.MEDIA_PLAYER, "Media Player"),
            CustomWidgetConfig("w_agenda", WidgetType.CALENDAR_AGENDA, "Upcoming Agenda"),
            CustomWidgetConfig(
                id = "w_note",
                type = WidgetType.QUICK_NOTE,
                title = "Quick Scratchpad",
                content = "Swipe right to All Apps • Long press dock to swap icons • Swipe left to customize widgets."
            ),
            CustomWidgetConfig("w_toggles", WidgetType.SYSTEM_TOGGLES, "Quick Device Controls"),
            CustomWidgetConfig(
                id = "w_habits",
                type = WidgetType.DAILY_HABITS,
                title = "Daily Habits",
                content = "Morning walk & stretch\nReview calendar\nRead 20 pages"
            )
        )
    }

    private fun saveLeftWidgets(widgets: List<CustomWidgetConfig>) {
        val arr = JSONArray()
        for (w in widgets) {
            val obj = JSONObject()
            obj.put("id", w.id)
            obj.put("type", w.type.name)
            obj.put("title", w.title)
            obj.put("content", w.content)
            obj.put("isEnabled", w.isEnabled)
            obj.put("appWidgetId", w.appWidgetId)
            w.providerPackage?.let { obj.put("providerPackage", it) }
            w.providerClass?.let { obj.put("providerClass", it) }
            w.linkedPackageName?.let { obj.put("linkedPackageName", it) }
            arr.put(obj)
        }
        prefs.edit().putString("left_widgets_json", arr.toString()).apply()
    }

    fun addLeftWidget(type: WidgetType, title: String = type.displayName, content: String = "") {
        val newWidget = CustomWidgetConfig(
            id = "w_${System.currentTimeMillis()}",
            type = type,
            title = title,
            content = content
        )
        val updated = _leftWidgets.value + newWidget
        _leftWidgets.value = updated
        saveLeftWidgets(updated)
    }

    fun addLeftSystemWidget(providerItem: SystemWidgetProviderItem, appWidgetId: Int) {
        val newWidget = CustomWidgetConfig(
            id = "sys_w_${System.currentTimeMillis()}",
            type = WidgetType.SYSTEM_APPWIDGET,
            title = providerItem.widgetLabel,
            appWidgetId = appWidgetId,
            providerPackage = providerItem.providerInfo.provider.packageName,
            providerClass = providerItem.providerInfo.provider.className
        )
        val updated = _leftWidgets.value + newWidget
        _leftWidgets.value = updated
        saveLeftWidgets(updated)
    }

    fun updateLeftWidget(id: String, newTitle: String, newContent: String) {
        val updated = _leftWidgets.value.map {
            if (it.id == id) it.copy(title = newTitle, content = newContent) else it
        }
        _leftWidgets.value = updated
        saveLeftWidgets(updated)
    }

    fun deleteLeftWidget(id: String) {
        val target = _leftWidgets.value.find { it.id == id }
        if (target != null && target.appWidgetId > 0) {
            com.example.util.SystemWidgetManager.deleteAppWidgetId(target.appWidgetId)
        }
        val updated = _leftWidgets.value.filter { it.id != id }
        _leftWidgets.value = updated
        saveLeftWidgets(updated)
    }

    fun reorderLeftWidgets(fromIndex: Int, toIndex: Int) {
        val current = _leftWidgets.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _leftWidgets.value = current
            saveLeftWidgets(current)
        }
    }

    // ==========================================
    // 2. CENTER CAROUSEL WIDGETS (MAX 5)
    // ==========================================
    private fun loadCarouselWidgets() {
        val jsonStr = prefs.getString("carousel_widgets_json", null)
        if (jsonStr != null) {
            try {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<WidgetType>()
                for (i in 0 until arr.length()) {
                    val name = arr.getString(i)
                    WidgetType.entries.find { it.name == name }?.let { list.add(it) }
                }
                if (list.isNotEmpty()) {
                    _carouselWidgets.value = list.take(5)
                    return
                }
            } catch (e: Exception) {
                // fallback
            }
        }

        // Default 3 widgets in carousel (max 5)
        _carouselWidgets.value = listOf(
            WidgetType.WEATHER_FORECAST,
            WidgetType.MEDIA_PLAYER,
            WidgetType.CALENDAR_AGENDA
        )
    }

    private fun saveCarouselWidgets(widgets: List<WidgetType>) {
        val arr = JSONArray()
        for (w in widgets.take(5)) {
            arr.put(w.name)
        }
        prefs.edit().putString("carousel_widgets_json", arr.toString()).apply()
    }

    fun addCarouselWidget(type: WidgetType) {
        val current = _carouselWidgets.value
        if (current.size < 5 && !current.contains(type)) {
            val updated = current + type
            _carouselWidgets.value = updated
            saveCarouselWidgets(updated)
        }
    }

    fun removeCarouselWidget(type: WidgetType) {
        val current = _carouselWidgets.value
        if (current.size > 1) {
            val updated = current.filter { it != type }
            _carouselWidgets.value = updated
            saveCarouselWidgets(updated)
        }
    }

    fun reorderCarouselWidgets(fromIndex: Int, toIndex: Int) {
        val current = _carouselWidgets.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _carouselWidgets.value = current
            saveCarouselWidgets(current)
        }
    }

    // ==========================================
    // 3. FIXED DOCK (CUSTOMIZABLE MAX 5 APPS)
    // ==========================================
    private fun loadDockPackages() {
        val jsonStr = prefs.getString("dock_apps_json", null)
        if (jsonStr != null) {
            try {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                if (list.isNotEmpty()) {
                    _dockPackageNames.value = list.take(5)
                    return
                }
            } catch (e: Exception) {
                // fallback
            }
        }

        // Default dock icons
        _dockPackageNames.value = listOf(
            "com.google.android.dialer",
            "com.google.android.apps.messaging",
            "com.android.chrome"
        )
    }

    private fun saveDockPackages(pkgs: List<String>) {
        val arr = JSONArray()
        for (p in pkgs.take(5)) {
            arr.put(p)
        }
        prefs.edit().putString("dock_apps_json", arr.toString()).apply()
    }

    fun setDockSlot(slotIndex: Int, packageName: String) {
        val current = _dockPackageNames.value.toMutableList()
        while (current.size <= slotIndex) {
            current.add("")
        }
        if (slotIndex in 0..4) {
            current[slotIndex] = packageName
            val finalized = current.take(5)
            _dockPackageNames.value = finalized
            saveDockPackages(finalized)
        }
    }

    fun clearDockSlot(slotIndex: Int) {
        val current = _dockPackageNames.value.toMutableList()
        if (slotIndex in current.indices) {
            current.removeAt(slotIndex)
            _dockPackageNames.value = current
            saveDockPackages(current)
        }
    }

    fun resetDockToDefaults() {
        val defaults = listOf(
            "com.google.android.dialer",
            "com.google.android.apps.messaging",
            "com.android.chrome"
        )
        _dockPackageNames.value = defaults
        saveDockPackages(defaults)
    }
}

