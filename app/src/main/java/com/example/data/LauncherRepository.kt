package com.example.data

import android.Manifest
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
import com.example.util.IntegrationCategory
import com.example.util.WidgetIntegrationsManager
import com.example.domain.*

import javax.inject.Inject

class LauncherRepository @Inject constructor(private val context: Context) : 
    SettingsRepository, 
    AppRepository, 
    NotificationRepository, 
    MediaPlaybackRepository, 
    WidgetRepository, 
    WallpaperRepository, 
    FolderRepository, 
    CategoryRepository, 
    DockRepository, 
    CalendarRepository {

    private val prefs = context.getSharedPreferences("niagara_launcher_prefs", Context.MODE_PRIVATE)
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var reloadDebounceJob: kotlinx.coroutines.Job? = null

    private val _isAppsLoading = MutableStateFlow(true)
    override val isAppsLoading: StateFlow<Boolean> = _isAppsLoading.asStateFlow()

    private val _allApps = MutableStateFlow<List<AppItem>>(emptyList())
    override val allApps: StateFlow<List<AppItem>> = _allApps.asStateFlow()

    private val _favorites = MutableStateFlow<List<AppItem>>(emptyList())
    override val favorites: StateFlow<List<AppItem>> = _favorites.asStateFlow()

    private val _folders = MutableStateFlow<List<FolderItem>>(emptyList())
    override val folders: StateFlow<List<FolderItem>> = _folders.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    override val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _weather = MutableStateFlow(WeatherInfo())
    val weather: StateFlow<WeatherInfo> = _weather.asStateFlow()

    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    override val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    private val _mediaPlayer = MutableStateFlow(MediaPlaybackState())
    override val mediaPlayer: StateFlow<MediaPlaybackState> = _mediaPlayer.asStateFlow()

    private val _activeNotifications = MutableStateFlow<Map<String, NotificationItem>>(emptyMap())
    override val activeNotifications: StateFlow<Map<String, NotificationItem>> = _activeNotifications.asStateFlow()

    private val _allActiveNotifications = MutableStateFlow<Map<String, List<NotificationItem>>>(emptyMap())
    override val allActiveNotifications: StateFlow<Map<String, List<NotificationItem>>> = _allActiveNotifications.asStateFlow()

    private val _leftWidgets = MutableStateFlow<List<CustomWidgetConfig>>(emptyList())
    override val leftWidgets: StateFlow<List<CustomWidgetConfig>> = _leftWidgets.asStateFlow()

    private val _carouselWidgets = MutableStateFlow<List<WidgetType>>(emptyList())
    override val carouselWidgets: StateFlow<List<WidgetType>> = _carouselWidgets.asStateFlow()

    private val _dockPackageNames = MutableStateFlow<List<String>>(emptyList())
    override val dockPackageNames: StateFlow<List<String>> = _dockPackageNames.asStateFlow()

    val widgetIntegrations = WidgetIntegrationsManager(context)
    val autoWallpaperManager = com.example.util.AutoWallpaperManager(context)

    private val _autoWallpaperBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    override val autoWallpaperBitmap: StateFlow<android.graphics.Bitmap?> = _autoWallpaperBitmap.asStateFlow()

    private val _isWallpaperSwitching = MutableStateFlow(false)
    override val isWallpaperSwitching: StateFlow<Boolean> = _isWallpaperSwitching.asStateFlow()

    init {
        observeLiveNotifications()
        registerPackageListeners()
        loadLeftWidgets()
        loadCarouselWidgets()
        loadDockPackages()
        loadFolders()
        refreshCalendarEvents()
        reloadApps()
        checkAutoWallpaperSchedule()
    }

    override fun refreshAutoWallpaperNow(overrideConfig: AutoWallpaperConfig?) {
        val cfg = overrideConfig ?: _settings.value.autoWallpaperConfig
        repositoryScope.launch(Dispatchers.IO) {
            _isWallpaperSwitching.value = true
            try {
                val result = autoWallpaperManager.fetchNextWallpaper(cfg)
                if (result is com.example.util.AutoWallpaperResult.Success) {
                    _autoWallpaperBitmap.value = result.bitmap
                    val updatedSettings = _settings.value.copy(
                        wallpaperStyle = WallpaperStyle.AUTO_ALBUM,
                        autoWallpaperConfig = result.config
                    )
                    updateSettings(updatedSettings)
                }
            } catch (_: Exception) {
            } finally {
                _isWallpaperSwitching.value = false
            }
        }
    }

    fun checkAutoWallpaperSchedule() {
        repositoryScope.launch(Dispatchers.IO) {
            val cfg = _settings.value.autoWallpaperConfig
            if (cfg.isEnabled && autoWallpaperManager.shouldSwitchWallpaper(cfg)) {
                refreshAutoWallpaperNow(cfg)
            } else if (cfg.isEnabled && _autoWallpaperBitmap.value == null) {
                val cachedFile = java.io.File(context.cacheDir, "auto_wallpapers/active_wallpaper.jpg")
                if (cachedFile.exists() && cachedFile.length() > 0L) {
                    try {
                        val bmp = android.graphics.BitmapFactory.decodeFile(cachedFile.absolutePath)
                        if (bmp != null) {
                            _autoWallpaperBitmap.value = bmp
                        } else {
                            refreshAutoWallpaperNow(cfg)
                        }
                    } catch (_: Exception) {
                        refreshAutoWallpaperNow(cfg)
                    }
                } else {
                    refreshAutoWallpaperNow(cfg)
                }
            }
        }
    }

    private fun debouncedReloadApps() {
        reloadDebounceJob?.cancel()
        reloadDebounceJob = repositoryScope.launch {
            kotlinx.coroutines.delay(300L)
            reloadApps()
        }
    }

    private fun registerPackageListeners() {
        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            launcherApps?.registerCallback(object : LauncherApps.Callback() {
                override fun onPackageAdded(packageName: String, user: android.os.UserHandle) {
                    debouncedReloadApps()
                }

                override fun onPackageChanged(packageName: String, user: android.os.UserHandle) {
                    debouncedReloadApps()
                }

                override fun onPackageRemoved(packageName: String, user: android.os.UserHandle) {
                    debouncedReloadApps()
                }

                override fun onPackagesAvailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) {
                    debouncedReloadApps()
                }

                override fun onPackagesUnavailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) {
                    debouncedReloadApps()
                }
            })
        } catch (e: Throwable) {
            android.util.Log.w("FluxLauncher", "Package listener registration failed", e)
        }

        try {
            val packageFilter = android.content.IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    debouncedReloadApps()
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, packageFilter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, packageFilter)
            }
        } catch (e: Throwable) {
            android.util.Log.w("FluxLauncher", "Broadcast receiver registration failed", e)
        }
    }

    private fun observeLiveNotifications() {
        repositoryScope.launch {
            com.example.service.FluxNotificationListenerService.liveNotifications.collect { liveMap ->
                _activeNotifications.value = liveMap
                val currentApps = _allApps.value
                if (currentApps.isNotEmpty()) {
                    var changed = false
                    val updated = currentApps.map { app ->
                        val notif = liveMap[app.instanceKey] ?: if (!app.isDualApp) liveMap[app.packageName] else null
                        if (app.notification != notif) {
                            changed = true
                            app.copy(notification = notif)
                        } else app
                    }
                    if (changed) {
                        _allApps.value = updated
                    }
                }
                val currentFavs = _favorites.value
                if (currentFavs.isNotEmpty()) {
                    var favChanged = false
                    val updatedFavs = currentFavs.map { app ->
                        val notif = liveMap[app.instanceKey] ?: if (!app.isDualApp) liveMap[app.packageName] else null
                        if (app.notification != notif) {
                            favChanged = true
                            app.copy(notification = notif)
                        } else app
                    }
                    if (favChanged) {
                        _favorites.value = updatedFavs
                    }
                }
            }
        }
        repositoryScope.launch {
            com.example.service.FluxNotificationListenerService.allLiveNotifications.collect { multiMap ->
                _allActiveNotifications.value = multiMap
            }
        }
        repositoryScope.launch {
            com.example.service.FluxNotificationListenerService.liveMediaPlayback.collect { mediaState ->
                if (mediaState != null) {
                    _mediaPlayer.value = mediaState
                }
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

        val autoWpConfig = AutoWallpaperConfig(
            isEnabled = prefs.getBoolean("auto_wp_enabled", false),
            source = AutoWallpaperSource.entries.getOrElse(prefs.getInt("auto_wp_source", AutoWallpaperSource.GIT_REPO.ordinal)) { AutoWallpaperSource.GIT_REPO },
            interval = AutoWallpaperInterval.entries.getOrElse(prefs.getInt("auto_wp_interval", AutoWallpaperInterval.DAILY.ordinal)) { AutoWallpaperInterval.DAILY },
            localFolderPath = prefs.getString("auto_wp_local_path", "") ?: "",
            localFolderUri = prefs.getString("auto_wp_local_uri", "") ?: "",
            gitRepoUrl = prefs.getString("auto_wp_git_url", com.example.domain.RemoteConfig.DEFAULT_GIT_WALLPAPER_REPO) ?: com.example.domain.RemoteConfig.DEFAULT_GIT_WALLPAPER_REPO,
            gitBranch = prefs.getString("auto_wp_git_branch", "main") ?: "main",
            gitSubfolder = prefs.getString("auto_wp_git_subfolder", "") ?: "",
            googlePhotosAlbumUrl = prefs.getString("auto_wp_gphoto_url", "") ?: "",
            currentWallpaperUrlOrPath = prefs.getString("auto_wp_current_url", "") ?: "",
            currentWallpaperTitle = prefs.getString("auto_wp_current_title", "Emerald Abstract #04") ?: "Emerald Abstract #04",
            currentWallpaperSourceInfo = prefs.getString("auto_wp_current_source", "Git: dharmx/walls") ?: "Git: dharmx/walls",
            lastSwitchTimeMs = prefs.getLong("auto_wp_last_time", 0L),
            blurStrengthDp = prefs.getFloat("auto_wp_blur", 0f),
            dimOpacity = prefs.getFloat("auto_wp_dim", 0.15f)
        )

        return LauncherSettings(
            leftHandedMode = prefs.getBoolean("left_handed_mode", false),
            hideStatusBar = prefs.getBoolean("hide_status_bar", false),
            isLayoutLocked = prefs.getBoolean("is_layout_locked", false),
            clockStyle = ClockStyle.entries.getOrElse(prefs.getInt("clock_style", ClockStyle.FLUX_INLINE.ordinal)) { ClockStyle.FLUX_INLINE },
            showSeconds = prefs.getBoolean("show_seconds", false),
            showDate = prefs.getBoolean("show_date", true),
            showClockHeader = prefs.getBoolean("show_clock_header", true),
            clockScale = prefs.getFloat("clock_scale", 1.0f),
            clockColorTintHex = prefs.getString("clock_tint_hex", "#FFFFFF") ?: "#FFFFFF",
            iconPackType = IconPackType.entries.getOrElse(prefs.getInt("icon_pack", 0)) { IconPackType.MATERIAL },
            themePreset = LauncherThemePreset.entries.getOrElse(prefs.getInt("theme_preset", LauncherThemePreset.FLUX_EMERALD.ordinal)) { LauncherThemePreset.FLUX_EMERALD },
            wallpaperStyle = if (prefs.contains("wallpaper_style")) {
                WallpaperStyle.entries.getOrElse(prefs.getInt("wallpaper_style", WallpaperStyle.SYSTEM_DEVICE.ordinal)) { WallpaperStyle.SYSTEM_DEVICE }
            } else {
                WallpaperStyle.SYSTEM_DEVICE
            },
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
            backgroundBlurStrength = prefs.getFloat("bg_blur_strength", 25f),
            frostedTranslucency = prefs.getFloat("frosted_translucency", 0.30f),
            frostedBlurRadius = prefs.getFloat("frosted_blur_radius", 15f),
            frostedGlassEffect = prefs.getFloat("frosted_glass_effect", 0.30f),
            cardCornerRadius = prefs.getFloat("card_corner_radius", 20f),
            showAppNotificationBadges = prefs.getBoolean("app_notif_badges", true),
            compactAppRowSpacing = prefs.getBoolean("compact_row_spacing", false),
            favoritesTopSpacing = FavoritesTopSpacing.entries.getOrElse(prefs.getInt("fav_top_spacing", FavoritesTopSpacing.LOW.ordinal)) { FavoritesTopSpacing.LOW },
            showDock = prefs.getBoolean("show_dock", true),
            dockSlotCount = prefs.getInt("dock_slot_count", 3).coerceIn(3, 5),
            showDockLabels = prefs.getBoolean("show_dock_labels", false),
            dockBlurBackground = prefs.getBoolean("dock_blur_bg", true),
            dockIconScale = prefs.getFloat("dock_icon_scale", 1.0f),
            waveAlphabetHapticIntensity = prefs.getInt("wave_haptic_intensity", 2),
            customIconSettings = customIconSettings,
            autoWallpaperConfig = autoWpConfig,
            hasCompletedOnboarding = prefs.getBoolean("has_completed_onboarding", false)
        )
    }

    override fun updateSettings(newSettings: LauncherSettings) {
        _settings.value = newSettings
        prefs.edit()
            .putBoolean("left_handed_mode", newSettings.leftHandedMode)
            .putBoolean("hide_status_bar", newSettings.hideStatusBar)
            .putBoolean("is_layout_locked", newSettings.isLayoutLocked)
            .putInt("clock_style", newSettings.clockStyle.ordinal)
            .putBoolean("show_seconds", newSettings.showSeconds)
            .putBoolean("show_date", newSettings.showDate)
            .putBoolean("show_clock_header", newSettings.showClockHeader)
            .putFloat("clock_scale", newSettings.clockScale)
            .putString("clock_tint_hex", newSettings.clockColorTintHex)
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
            .putFloat("bg_blur_strength", newSettings.backgroundBlurStrength)
            .putFloat("frosted_translucency", newSettings.frostedTranslucency)
            .putFloat("frosted_blur_radius", newSettings.frostedBlurRadius)
            .putFloat("frosted_glass_effect", newSettings.frostedGlassEffect)
            .putFloat("card_corner_radius", newSettings.cardCornerRadius)
            .putBoolean("app_notif_badges", newSettings.showAppNotificationBadges)
            .putBoolean("compact_row_spacing", newSettings.compactAppRowSpacing)
            .putInt("fav_top_spacing", newSettings.favoritesTopSpacing.ordinal)
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
            .putBoolean("auto_wp_enabled", newSettings.autoWallpaperConfig.isEnabled)
            .putInt("auto_wp_source", newSettings.autoWallpaperConfig.source.ordinal)
            .putInt("auto_wp_interval", newSettings.autoWallpaperConfig.interval.ordinal)
            .putString("auto_wp_local_path", newSettings.autoWallpaperConfig.localFolderPath)
            .putString("auto_wp_local_uri", newSettings.autoWallpaperConfig.localFolderUri)
            .putString("auto_wp_git_url", newSettings.autoWallpaperConfig.gitRepoUrl)
            .putString("auto_wp_git_branch", newSettings.autoWallpaperConfig.gitBranch)
            .putString("auto_wp_git_subfolder", newSettings.autoWallpaperConfig.gitSubfolder)
            .putString("auto_wp_gphoto_url", newSettings.autoWallpaperConfig.googlePhotosAlbumUrl)
            .putString("auto_wp_current_url", newSettings.autoWallpaperConfig.currentWallpaperUrlOrPath)
            .putString("auto_wp_current_title", newSettings.autoWallpaperConfig.currentWallpaperTitle)
            .putString("auto_wp_current_source", newSettings.autoWallpaperConfig.currentWallpaperSourceInfo)
            .putLong("auto_wp_last_time", newSettings.autoWallpaperConfig.lastSwitchTimeMs)
            .putFloat("auto_wp_blur", newSettings.autoWallpaperConfig.blurStrengthDp)
            .putFloat("auto_wp_dim", newSettings.autoWallpaperConfig.dimOpacity)
            .putBoolean("has_completed_onboarding", newSettings.hasCompletedOnboarding)
            .apply()

        checkAutoWallpaperSchedule()
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

    override fun isWidgetAvailable(type: WidgetType): Boolean {
        return when (type) {
            WidgetType.MEDIA_PLAYER -> com.example.ui.components.isYouTubeMusicInstalled(context)
            WidgetType.WEATHER_FORECAST -> true
            WidgetType.NOTIFICATION_SUMMARY -> true
            WidgetType.SYSTEM_WIDGET -> true
            WidgetType.NEWS_FEED -> true
            WidgetType.QUICK_NOTE -> true
            WidgetType.CLOCK -> true
            WidgetType.MINDFULNESS_QUOTE -> true
            WidgetType.DUAL_CLOCK -> true
        }
    }

    override fun refreshCalendarEvents() {
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

    override fun reloadApps() {
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
        val seenKeys = mutableSetOf<String>()
        val myUser = Process.myUserHandle()

        // 1. Primary: query via LauncherApps service (the official Android API for Home Screen apps)
        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            val profiles = userManager?.userProfiles ?: listOf(myUser)

            for (profile in profiles) {
                val isSecondaryProfile = (profile != myUser)
                val activityList = launcherApps?.getActivityList(null, profile) ?: emptyList()
                for (activityInfo in activityList) {
                    val pkg = activityInfo.applicationInfo.packageName
                    val key = "$pkg:${profile.hashCode()}"
                    if (pkg == context.packageName || seenKeys.contains(key)) continue
                    seenKeys.add(key)

                    var label = try {
                        activityInfo.label?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                            ?: pm.getApplicationLabel(activityInfo.applicationInfo).toString()
                    } catch (t: Throwable) {
                        pkg
                    }

                    if (label.contains("Flux Launcher", true) || label.contains("Flux Settings", true) || label.contains("Launcher Settings", true)) continue

                    val isDual = isSecondaryProfile || pkg.contains("dual", true) || pkg.contains("clone", true) || pkg.contains("parallel", true)
                    if (isDual && !label.contains("(Dual)", true)) {
                        label = "$label (Dual)"
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

                    val instanceKey = if (isDual) "$pkg:dual" else pkg
                    val notif = _activeNotifications.value[instanceKey] ?: if (!isDual) _activeNotifications.value[pkg] else null

                    installedAppItems.add(
                        AppItem(
                            packageName = pkg,
                            activityName = activityInfo.componentName.className,
                            label = label,
                            shortcuts = listOf("App info", "Share", "Open"),
                            isDualApp = isDual,
                            userProfileId = profile.hashCode(),
                            iconDrawable = iconDrawable,
                            notification = notif
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
                if (pkg == context.packageName || seenKeys.any { it.startsWith(pkg) }) continue
                seenKeys.add(pkg)

                val actName = try { info.activityInfo?.name ?: "" } catch (t: Throwable) { "" }
                val label = try {
                    info.loadLabel(pm)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                        ?: info.activityInfo?.name
                        ?: pkg
                } catch (t: Throwable) {
                    pkg
                }

                if (label.contains("Flux Launcher", true) || label.contains("Flux Settings", true) || label.contains("Launcher Settings", true)) continue

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

        val favoritePackagesList = getSavedFavoritePackages()
        val favoritePackagesSet = favoritePackagesList.toSet()
        val hiddenPackages = getSavedHiddenPackages()
        val usageBreakerPackages = getSavedUsageBreakerPackages()
        val categoryMap = getCategoryMap()

        val finalAppItems = installedAppItems.map { app ->
            app.copy(
                isFavorite = favoritePackagesSet.contains(app.packageName),
                isHidden = hiddenPackages.contains(app.packageName),
                isUsageBreakerActive = usageBreakerPackages.contains(app.packageName),
                customCategory = categoryMap[app.packageName] ?: ""
            )
        }

        _allApps.value = finalAppItems

        // Set favorites strictly in saved custom order (NOT alphabetically)
        val favList = mutableListOf<AppItem>()
        for (pkg in favoritePackagesList) {
            val app = finalAppItems.find { it.packageName == pkg && !it.isHidden }
            if (app != null && favList.none { it.packageName == pkg }) {
                favList.add(app)
            }
        }

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
            saveFavoritePackages(favList.map { it.packageName })
        }

        if (_settings.value.isHeadphonesConnected && _settings.value.audioAppAutoSurfacing) {
            val musicApp = finalAppItems.find { it.packageName.contains("spotify", true) || it.label.contains("music", true) }
            if (musicApp != null) {
                favList.removeAll { it.packageName == musicApp.packageName }
                favList.add(0, musicApp)
            }
        }

        _favorites.value = favList

        // Synchronize dock icons only on very first launch if dock has not yet been configured
        val hasConfiguredDock = prefs.contains("dock_apps_json")
        if (!hasConfiguredDock && finalAppItems.isNotEmpty()) {
            val phone = finalAppItems.find { it.packageName.contains("dialer", true) || it.packageName.contains("phone", true) || it.label.equals("Phone", true) }?.packageName
            val msg = finalAppItems.find { it.packageName.contains("messaging", true) || it.packageName.contains("message", true) || it.label.equals("Messages", true) }?.packageName
            val camera = finalAppItems.find { it.packageName.contains("camera", true) || it.label.equals("Camera", true) }?.packageName
            val browser = finalAppItems.find { it.packageName.contains("chrome", true) || it.packageName.contains("browser", true) || it.label.contains("Browser", true) }?.packageName

            val initialDock = listOfNotNull(phone, msg, camera, browser).distinct().take(_settings.value.dockSlotCount.coerceIn(3, 5))
            val finalInitial = if (initialDock.isNotEmpty()) initialDock else finalAppItems.take(3).map { it.packageName }
            _dockPackageNames.value = finalInitial
            saveDockPackages(finalInitial)
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

        // Default folders is empty unless user creates folders
        _folders.value = emptyList()
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
        val currentFavs = getSavedFavoritePackages().toMutableList()
        val isNowFav = if (currentFavs.contains(packageName)) {
            currentFavs.remove(packageName)
            false
        } else {
            if (currentFavs.size >= 10) {
                android.widget.Toast.makeText(context, "Maximum 10 favorite apps allowed", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            currentFavs.add(packageName)
            true
        }
        saveFavoritePackages(currentFavs)

        // Instant in-memory state update without full reloading spinner
        val updatedApps = _allApps.value.map {
            if (it.packageName == packageName) it.copy(isFavorite = isNowFav) else it
        }
        _allApps.value = updatedApps
        val updatedFavList = mutableListOf<AppItem>()
        for (pkg in currentFavs) {
            val app = updatedApps.find { it.packageName == pkg }
            if (app != null) {
                updatedFavList.add(app)
            }
        }
        _favorites.value = updatedFavList
    }

    fun reorderFavorites(fromIndex: Int, toIndex: Int) {
        val current = _favorites.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _favorites.value = current
            saveFavoritePackages(current.map { it.packageName })
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

    fun setAppCategory(packageName: String, categoryName: String) {
        val map = getCategoryMap().toMutableMap()
        if (categoryName.isBlank()) {
            map.remove(packageName)
        } else {
            map[packageName] = categoryName
        }
        saveCategoryMap(map)
        _allApps.value = _allApps.value.map {
            if (it.packageName == packageName) it.copy(customCategory = categoryName) else it
        }
        _favorites.value = _favorites.value.map {
            if (it.packageName == packageName) it.copy(customCategory = categoryName) else it
        }
    }

    private val _userCategories = MutableStateFlow<List<String>>(loadUserCategories())
    override val userCategories: StateFlow<List<String>> = _userCategories.asStateFlow()

    private fun loadUserCategories(): List<String> {
        val json = prefs.getString("user_categories_list", null)
        if (json != null) {
            try {
                val arr = org.json.JSONArray(json)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                return list
            } catch (e: Exception) {
                android.util.Log.w("FluxLauncher", "Failed to load user categories", e)
            }
        }
        return emptyList()
    }

    override fun addUserCategory(categoryName: String) {
        val trimmed = categoryName.trim()
        if (trimmed.isBlank()) return
        val current = _userCategories.value.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            _userCategories.value = current
            saveUserCategories(current)
        }
    }

    fun updateUserCategoryName(oldName: String, newName: String) {
        val trimmedNew = newName.trim()
        if (trimmedNew.isBlank() || oldName == trimmedNew) return
        val current = _userCategories.value.toMutableList()
        val index = current.indexOf(oldName)
        if (index != -1) {
            current[index] = trimmedNew
            _userCategories.value = current
            saveUserCategories(current)

            val map = getCategoryMap().toMutableMap()
            val updatedMap = map.mapValues { if (it.value == oldName) trimmedNew else it.value }
            saveCategoryMap(updatedMap)

            _allApps.value = _allApps.value.map {
                if (it.customCategory == oldName) it.copy(customCategory = trimmedNew) else it
            }
        }
    }

    override fun removeUserCategory(categoryName: String) {
        val current = _userCategories.value.toMutableList()
        if (current.contains(categoryName)) {
            current.remove(categoryName)
            _userCategories.value = current
            saveUserCategories(current)

            val map = getCategoryMap().toMutableMap()
            val keysToRemove = map.filterValues { it == categoryName }.keys
            keysToRemove.forEach { map.remove(it) }
            saveCategoryMap(map)

            _allApps.value = _allApps.value.map {
                if (it.customCategory == categoryName) it.copy(customCategory = "") else it
            }
        }
    }

    private fun saveUserCategories(list: List<String>) {
        val arr = org.json.JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString("user_categories_list", arr.toString()).apply()
    }

    private fun getCategoryMap(): Map<String, String> {
        val json = prefs.getString("app_categories_json", null) ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        try {
            val obj = org.json.JSONObject(json)
            obj.keys().forEach { k ->
                result[k] = obj.getString(k)
            }
        } catch (_: Exception) {}
        return result
    }

    private fun saveCategoryMap(map: Map<String, String>) {
        val obj = org.json.JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString("app_categories_json", obj.toString()).apply()
    }

    fun replyToNotification(packageName: String, replyText: String, key: String? = null, isDualApp: Boolean = false) {
        try {
            if (key != null) {
                com.example.service.FluxNotificationListenerService.instance?.replyNotificationByKey(key, replyText)
            } else {
                com.example.service.FluxNotificationListenerService.replyToPackage(packageName, replyText, isDualApp)
            }
        } catch (_: Throwable) {}
        val instanceKey = if (isDualApp) "$packageName:dual" else packageName
        val current = _activeNotifications.value.toMutableMap()
        current.remove(instanceKey)
        if (!isDualApp) current.remove(packageName)
        _activeNotifications.value = current
        val currentApps = _allApps.value
        _allApps.value = currentApps.map {
            if (it.packageName == packageName && it.isDualApp == isDualApp) it.copy(notification = null) else it
        }
        _favorites.value = _favorites.value.map {
            if (it.packageName == packageName && it.isDualApp == isDualApp) it.copy(notification = null) else it
        }
    }

    fun dismissNotification(packageName: String, key: String? = null, isDualApp: Boolean = false) {
        try {
            if (key != null) {
                com.example.service.FluxNotificationListenerService.dismissNotification(key)
            } else {
                com.example.service.FluxNotificationListenerService.dismissAllForPackage(packageName, isDualApp)
            }
        } catch (_: Throwable) {}
        val instanceKey = if (isDualApp) "$packageName:dual" else packageName
        val current = _activeNotifications.value.toMutableMap()
        current.remove(instanceKey)
        if (!isDualApp) current.remove(packageName)
        _activeNotifications.value = current
        if (_allApps.value.any { it.packageName == packageName && it.isDualApp == isDualApp && it.notification != null }) {
            _allApps.value = _allApps.value.map {
                if (it.packageName == packageName && it.isDualApp == isDualApp) it.copy(notification = null) else it
            }
        }
        if (_favorites.value.any { it.packageName == packageName && it.isDualApp == isDualApp && it.notification != null }) {
            _favorites.value = _favorites.value.map {
                if (it.packageName == packageName && it.isDualApp == isDualApp) it.copy(notification = null) else it
            }
        }
    }

    fun clearAllNotifications() {
        try {
            com.example.service.FluxNotificationListenerService.instance?.cancelAllNotifications()
        } catch (_: Throwable) {}
        if (_activeNotifications.value.isNotEmpty()) {
            _activeNotifications.value = emptyMap()
        }
        if (_allActiveNotifications.value.isNotEmpty()) {
            _allActiveNotifications.value = emptyMap()
        }
        if (_allApps.value.any { it.notification != null }) {
            _allApps.value = _allApps.value.map { if (it.notification != null) it.copy(notification = null) else it }
        }
        if (_favorites.value.any { it.notification != null }) {
            _favorites.value = _favorites.value.map { if (it.notification != null) it.copy(notification = null) else it }
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
        val currentlyPlaying = _mediaPlayer.value.isPlaying
        val handled = com.example.service.FluxNotificationListenerService.sendMediaControl("toggle_play")
        if (!handled) {
            val keycode = if (currentlyPlaying) android.view.KeyEvent.KEYCODE_MEDIA_PAUSE else android.view.KeyEvent.KEYCODE_MEDIA_PLAY
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            try {
                val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keycode)
                val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keycode)
                audioManager?.dispatchMediaKeyEvent(downEvent)
                audioManager?.dispatchMediaKeyEvent(upEvent)
            } catch (_: Exception) {}
        }
        _mediaPlayer.value = _mediaPlayer.value.copy(
            isPlaying = !currentlyPlaying
        )
    }

    fun skipNextMedia() {
        val handled = com.example.service.FluxNotificationListenerService.sendMediaControl("next")
        if (!handled) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            try {
                val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                audioManager?.dispatchMediaKeyEvent(downEvent)
                audioManager?.dispatchMediaKeyEvent(upEvent)
            } catch (_: Exception) {}
        }
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

    fun toggleShuffleMedia() {
        val current = _mediaPlayer.value
        _mediaPlayer.value = current.copy(
            isShuffleEnabled = !current.isShuffleEnabled
        )
    }

    fun skipPreviousMedia() {
        val handled = com.example.service.FluxNotificationListenerService.sendMediaControl("prev")
        if (!handled) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            try {
                val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                audioManager?.dispatchMediaKeyEvent(downEvent)
                audioManager?.dispatchMediaKeyEvent(upEvent)
            } catch (_: Exception) {}
        }
    }

    fun seekMedia(progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        val handled = com.example.service.FluxNotificationListenerService.sendMediaControl("seek", p)
        val currentDuration = _mediaPlayer.value.durationMs
        if (currentDuration > 0) {
            val currentPos = (p * currentDuration).toLong()
            _mediaPlayer.value = _mediaPlayer.value.copy(
                progressPercent = p,
                positionMs = currentPos,
                currentTimeText = com.example.service.FluxNotificationListenerService.formatMs(currentPos)
            )
        } else {
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

    private fun getSavedFavoritePackages(): List<String> {
        val jsonStr = prefs.getString("favorite_packages_json", null)
        if (jsonStr != null) {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                return list
            } catch (_: Exception) {}
        }
        val legacySet = prefs.getStringSet("favorite_packages", null)
        if (legacySet != null) {
            return legacySet.toList()
        }
        return emptyList()
    }

    private fun saveFavoritePackages(list: Collection<String>) {
        val array = JSONArray()
        for (pkg in list.distinct()) {
            array.put(pkg)
        }
        prefs.edit().putString("favorite_packages_json", array.toString()).apply()
    }

    private fun getSavedHiddenPackages(): Set<String> {
        return prefs.getStringSet("hidden_packages", null)?.toSet() ?: emptySet()
    }

    private fun saveHiddenPackages(set: Set<String>) {
        prefs.edit().putStringSet("hidden_packages", HashSet(set)).apply()
    }

    private fun getSavedUsageBreakerPackages(): Set<String> {
        return prefs.getStringSet("usage_breaker_packages", null)?.toSet() ?: emptySet()
    }

    private fun saveUsageBreakerPackages(set: Set<String>) {
        prefs.edit().putStringSet("usage_breaker_packages", set).apply()
    }

    private fun getDisguisedMap(): Map<String, String> {
        val str = prefs.getString("disguised_map", null) ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        try {
            val obj = JSONObject(str)
            for (key in obj.keys()) {
                map[key] = obj.optString(key, "")
            }
        } catch (_: Exception) {}
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
                    if (type != WidgetType.MEDIA_PLAYER) {
                        list.add(
                            CustomWidgetConfig(
                                id = obj.optString("id", "w_$i"),
                                type = type,
                                title = obj.optString("title", type.displayName),
                                content = obj.optString("content", ""),
                                isEnabled = obj.optBoolean("isEnabled", true),
                                heightCategory = obj.optString("heightCategory", "4x2"),
                                appWidgetId = obj.optInt("appWidgetId", -1)
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) {
                    _leftWidgets.value = list
                    return
                }
            } catch (e: Exception) {
                // fallback to default widgets
            }
        }

        // Default Left Page widgets (clean standard launcher widgets)
        _leftWidgets.value = listOf(
            CustomWidgetConfig("w_weather", WidgetType.WEATHER_FORECAST, "Weather & Forecast", heightCategory = "4x2"),
            CustomWidgetConfig("w_digest", WidgetType.NOTIFICATION_SUMMARY, "Notification Digest", heightCategory = "4x2"),
            CustomWidgetConfig("w_news", WidgetType.NEWS_FEED, "Top News Highlights", heightCategory = "4x2"),
            CustomWidgetConfig(
                id = "w_note",
                type = WidgetType.QUICK_NOTE,
                title = "Quick Scratchpad",
                content = "Swipe right to All Apps • Long press dock to swap icons • Swipe left to customize widgets.",
                heightCategory = "4x2"
            ),
            CustomWidgetConfig("w_mindfulness", WidgetType.MINDFULNESS_QUOTE, "Daily Reflection", heightCategory = "4x2")
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
            obj.put("heightCategory", w.heightCategory)
            obj.put("appWidgetId", w.appWidgetId)
            arr.put(obj)
        }
        prefs.edit().putString("left_widgets_json", arr.toString()).apply()
    }

    fun addLeftSystemWidget(appWidgetId: Int, label: String = "System Widget", heightCategory: String = "4x2") {
        val newWidget = CustomWidgetConfig(
            id = "sys_${appWidgetId}_${System.currentTimeMillis()}",
            type = WidgetType.SYSTEM_WIDGET,
            title = label.ifBlank { "System Widget" },
            appWidgetId = appWidgetId,
            heightCategory = heightCategory
        )
        val updated = _leftWidgets.value + newWidget
        _leftWidgets.value = updated
        saveLeftWidgets(updated)
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

    fun updateLeftWidget(id: String, newTitle: String = "", newContent: String = "", newHeightCategory: String? = null) {
        val updated = _leftWidgets.value.map {
            if (it.id == id) {
                it.copy(
                    title = if (newTitle.isNotEmpty()) newTitle else it.title,
                    content = if (newContent.isNotEmpty()) newContent else it.content,
                    heightCategory = newHeightCategory ?: it.heightCategory
                )
            } else it
        }
        _leftWidgets.value = updated
        saveLeftWidgets(updated)
    }

    fun deleteLeftWidget(id: String) {
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
                    val type = WidgetType.entries.find { it.name == name }
                    if (type != null && type != WidgetType.CLOCK) {
                        if (type != WidgetType.MEDIA_PLAYER || com.example.ui.components.isYouTubeMusicInstalled(context)) {
                            list.add(type)
                        }
                    }
                }
                _carouselWidgets.value = list.take(5)
                return
            } catch (e: Exception) {
                // fallback
            }
        }

        // Default optional widgets in carousel (e.g. Weather and YouTube Music if available)
        val defaultList = if (com.example.ui.components.isYouTubeMusicInstalled(context)) {
            listOf(WidgetType.WEATHER_FORECAST, WidgetType.MEDIA_PLAYER)
        } else {
            listOf(WidgetType.WEATHER_FORECAST)
        }
        _carouselWidgets.value = defaultList
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
        val updated = current.filter { it != type }
        _carouselWidgets.value = updated
        saveCarouselWidgets(updated)
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
        // Do not force hardcoded packages here if not yet saved;
        // let loadInstalledAppsInternal seed real installed apps on first run.
    }

    private fun saveDockPackages(pkgs: List<String>) {
        val arr = JSONArray()
        for (p in pkgs.take(5)) {
            arr.put(p)
        }
        prefs.edit().putString("dock_apps_json", arr.toString()).apply()
    }

    override fun setDockSlot(slotIndex: Int, packageName: String) {
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

    override fun clearDockSlot(slotIndex: Int) {
        val current = _dockPackageNames.value.toMutableList()
        if (slotIndex in current.indices) {
            current.removeAt(slotIndex)
            _dockPackageNames.value = current
            saveDockPackages(current)
        }
    }

    override fun resetDockToDefaults() {
        val apps = _allApps.value
        val phone = apps.find { it.packageName.contains("dialer", true) || it.packageName.contains("phone", true) || it.label.equals("Phone", true) }?.packageName
        val msg = apps.find { it.packageName.contains("messaging", true) || it.packageName.contains("message", true) || it.label.equals("Messages", true) }?.packageName
        val camera = apps.find { it.packageName.contains("camera", true) || it.label.equals("Camera", true) }?.packageName

        val defaults = listOfNotNull(phone, msg, camera).distinct().ifEmpty {
            listOf(
                "com.google.android.dialer",
                "com.google.android.apps.messaging",
                "com.google.android.GoogleCamera"
            )
        }
        _dockPackageNames.value = defaults
        saveDockPackages(defaults)
    }
}

