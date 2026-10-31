package com.example.data

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

enum class IconPackType(val displayName: String) {
    DEFAULT_APP("Default System Icons"),
    MATERIAL("Material Modern"),
    ANYCONS_DOTS("Niagara Dots"),
    ANYCONS_STICKER("Anycons Sticker"),
    ANYCONS_MINIMAL("Anycons Minimalist"),
    ANYCONS_COLORFUL("Anycons Pastel Duotone"),
    CUSTOM("Custom Icon Pack")
}

data class CustomIconPackSettings(
    val packPreset: CustomPackPreset = CustomPackPreset.WHICONS_WHITE,
    val iconShape: IconShape = IconShape.ROUNDED_SQUIRCLE,
    val customColorHex: String = "#FFFFFF",
    val backgroundOpacity: Float = 0.22f,
    val installedPackPackageName: String? = null,
    val installedPackName: String? = null
)

enum class CustomPackPreset(val displayName: String, val subtitle: String) {
    FLUX_GLASS("Flux Frosted Glass", "Translucent frosted badge with glowing mint rim"),
    WHICONS_WHITE("Whicons Pure White", "Clean white monochrome silhouette glyphs"),
    DELTA_PASTEL("Delta Vintage Pastel", "Muted retro pastel geometry and warm tones"),
    LINEBIT_NEON("Linebit Cyber Glow", "Vibrant glowing neon outline vectors"),
    CHARCOAL_STEALTH("Dark Void Charcoal", "Stealth dark monochrome icons for deep blacks"),
    CANDY_VIVID("CandyCons Vivid", "Juicy saturated material shapes with accent badges"),
    CUSTOM_STUDIO("Custom Pack Engine", "Configure shape, tint, and opacity manually")
}

enum class IconShape(val displayName: String, val cornerPercent: Int) {
    CIRCLE("Circle", 50),
    ROUNDED_SQUIRCLE("Rounded Squircle", 28),
    SMOOTH_SQUARE("Soft Square", 16),
    TEARDROP("Teardrop", 38),
    HEXAGON("Hexagon", 25)
}

data class InstalledIconPackInfo(
    val packageName: String,
    val label: String,
    @Transient val icon: Drawable? = null
)

enum class ClockStyle(val displayName: String) {
    FLUX_INLINE("Flux Minimal (10:09 22°C)"),
    MINIMAL_DIGITAL("Clean Digital"),
    BOLD_MODERN("Bold Headline"),
    TEXT_BASED("Word Clock"),
    ANALOG("Minimal Analog"),
    COMPACT("Inline Minimal")
}

enum class WallpaperStyle(val displayName: String, val usesDeviceOrAutoBitmap: Boolean) {
    SYSTEM_DEVICE("Device Wallpaper", true),
    FLUX_EMBLEM("Flux Signature Wallpaper", false),
    FLUX_MINIMAL_GRADIENT("Flux Soft Gradient", false),
    SOLID_DARK("Solid Dark Theme", false),
    AUTO_ALBUM("Daily Auto Wallpaper Album", true)
}

enum class AutoWallpaperSource(val displayName: String, val subtitle: String, val iconEmoji: String) {
    LOCAL_FOLDER("Local Folder", "Images from local device directory or gallery album", "📁"),
    GIT_REPO("Git Repository", "Raw wallpaper collection from GitHub/Git repository", "🐙"),
    GOOGLE_PHOTOS("Google Photos Album", "Shared album link from Google Photos account", "🖼️")
}

enum class AutoWallpaperInterval(val displayName: String, val hours: Int) {
    HOURLY("Every Hour", 1),
    EVERY_6_HOURS("Every 6 Hours", 6),
    EVERY_12_HOURS("Every 12 Hours", 12),
    DAILY("Daily (Every 24 Hours)", 24)
}

data class WallpaperItem(
    val id: String,
    val title: String,
    val imageUrlOrPath: String,
    val authorOrSource: String = "",
    val sourceKind: AutoWallpaperSource = AutoWallpaperSource.GIT_REPO
)

data class AutoWallpaperConfig(
    val isEnabled: Boolean = false,
    val source: AutoWallpaperSource = AutoWallpaperSource.GIT_REPO,
    val interval: AutoWallpaperInterval = AutoWallpaperInterval.DAILY,
    val localFolderPath: String = "",
    val localFolderUri: String = "",
    val gitRepoUrl: String = com.example.domain.RemoteConfig.DEFAULT_GIT_WALLPAPER_REPO,
    val gitBranch: String = "main",
    val gitSubfolder: String = "",
    val googlePhotosAlbumUrl: String = "",
    val currentWallpaperUrlOrPath: String = "",
    val currentWallpaperTitle: String = "Emerald Abstract #04",
    val currentWallpaperSourceInfo: String = "Git: dharmx/walls",
    val lastSwitchTimeMs: Long = 0L,
    val blurStrengthDp: Float = 0f,
    val dimOpacity: Float = 0.15f
)

enum class LauncherThemePreset(val displayName: String, val primary: Color, val background: Color, val surface: Color, val accent: Color) {
    FROSTED_GLASS("Frosted Glass", Color(0xFFFFFFFF), Color(0x3B18181B), Color(0x4D27272A), Color(0xFFE4E4E7)),
    FLUX_EMERALD("Flux Emerald", Color(0xFF6EE7B7), Color(0xFF0B211A), Color(0xFF163E33), Color(0xFFA7F3D0))
}

enum class AppFontFamily(val displayName: String) {
    MODERN_SANS("Modern Sans"),
    GEOMETRIC("Geometric Clean"),
    EDITORIAL_SERIF("Editorial Serif"),
    MONOSPACE("Developer Mono"),
    CONDENSED("Compact Bold")
}

enum class FavoritesTopSpacing(val displayName: String, val rowCount: Int, val heightDp: Int) {
    HIGH("High - 3 empty rows", 3, 180),
    MEDIUM("Medium - 2 empty rows", 2, 120),
    LOW("Low - 1 empty row", 1, 60)
}

data class AppItem(
    val packageName: String,
    val activityName: String = "",
    val label: String,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isUsageBreakerActive: Boolean = false,
    val disguisedName: String? = null,
    val customCategory: String = "",
    val customIconType: String? = null,
    val customIconColorHex: String? = null,
    val notification: NotificationItem? = null,
    val shortcuts: List<String> = emptyList(),
    val isDualApp: Boolean = false,
    val userProfileId: Int = 0,
    @Transient val iconDrawable: Drawable? = null
) {
    val instanceKey: String get() = if (isDualApp) "$packageName:dual" else packageName
    val uniqueKey: String get() = "app_${packageName}_${isDualApp}_${userProfileId}_${activityName}"
}

data class NotificationItem(
    val id: String,
    val packageName: String,
    val sender: String,
    val message: String,
    val timeAgo: String,
    val canReply: Boolean = true,
    val key: String = id,
    val subText: String? = null,
    val richText: String? = null,
    val isDualApp: Boolean = false,
    val instanceKey: String = if (isDualApp) "$packageName:dual" else packageName,
    @Transient val pictureBitmap: Bitmap? = null,
    @Transient val senderIcon: Bitmap? = null
)

data class FolderItem(
    val id: String,
    val name: String,
    val iconEmoji: String = "📁",
    val appPackageNames: List<String> = emptyList(),
    val isExpanded: Boolean = false
)

data class WeatherInfo(
    val temperatureCelsius: Int = 23,
    val condition: String = "Partly Cloudy",
    val conditionIcon: String = "⛅",
    val location: String = "San Francisco",
    val rainProbability: Int = 15,
    val highTemp: Int = 26,
    val lowTemp: Int = 17,
    val hourlyForecast: List<HourlyWeather> = listOf(
        HourlyWeather("12 PM", 22, "☀️"),
        HourlyWeather("3 PM", 25, "⛅"),
        HourlyWeather("6 PM", 23, "🌤️"),
        HourlyWeather("9 PM", 19, "🌙")
    )
)

data class HourlyWeather(
    val time: String,
    val temp: Int,
    val icon: String
)

data class CalendarEvent(
    val id: String,
    val title: String,
    val time: String,
    val location: String = "Google Meet",
    val isSoon: Boolean = true
)

data class MediaPlaybackState(
    val isPlaying: Boolean = false,
    val trackTitle: String = "YouTube Music",
    val artistName: String = "Tap to play music",
    val appName: String = "YouTube Music",
    val progressPercent: Float = 0f,
    val durationText: String = "--:--",
    val currentTimeText: String = "0:00",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false,
    val albumArtBitmap: Bitmap? = null,
    val packageName: String = com.example.domain.KnownPackages.YOUTUBE_MUSIC_REVANCED,
    val isExternalOutputConnected: Boolean = false,
    val outputDeviceName: String? = null
)

@Serializable
data class LauncherSettings(
    val leftHandedMode: Boolean = false,
    val hideStatusBar: Boolean = false,
    val isLayoutLocked: Boolean = false,
    val clockStyle: ClockStyle = ClockStyle.FLUX_INLINE,
    val showSeconds: Boolean = false,
    val showDate: Boolean = true,
    val showClockHeader: Boolean = true,
    val clockScale: Float = 1.0f,
    val clockColorTintHex: String = "#FFFFFF",
    val iconPackType: IconPackType = IconPackType.DEFAULT_APP,
    val themePreset: LauncherThemePreset = LauncherThemePreset.FLUX_EMERALD,
    val wallpaperStyle: WallpaperStyle = WallpaperStyle.SYSTEM_DEVICE,
    val customFont: AppFontFamily = AppFontFamily.MODERN_SANS,
    val fontScale: Float = 1.0f,
    val hapticFeedback: Boolean = true,
    val doubleTapToSleep: Boolean = true,
    val swipeDownForNotificationCenter: Boolean = true,
    val klwpCompatMode: Boolean = true,
    val audioAppAutoSurfacing: Boolean = true,
    val usageBreakerDurationSeconds: Int = 5,
    val isHeadphonesConnected: Boolean = false,
    val isGoogleDiscoverEnabled: Boolean = true,
    // Advanced Customizations
    val appIconScale: Float = 1.0f,
    val appSpacingScale: Float = 1.0f,
    val backgroundBlurStrength: Float = 25f,
    val frostedTranslucency: Float = 0.30f,
    val frostedBlurRadius: Float = 15f,
    val frostedGlassEffect: Float = 0.30f,
    val cardCornerRadius: Float = 20f,
    val showAppNotificationBadges: Boolean = true,
    val compactAppRowSpacing: Boolean = false,
    val favoritesTopSpacing: FavoritesTopSpacing = FavoritesTopSpacing.LOW,
    val showDock: Boolean = true,
    val dockSlotCount: Int = 3, // 3, 4, or 5
    val showDockLabels: Boolean = false,
    val dockBlurBackground: Boolean = true,
    val dockIconScale: Float = 1.0f,
    val blurBackgrounds: Boolean = true,
    val waveAlphabetHapticIntensity: Int = 2, // 1: subtle, 2: medium, 3: heavy
    val batteryOptimizationMode: Boolean = true, // Note: wired to real policy in Agent I (v1.0.1)
    @Contextual
    val customIconSettings: CustomIconPackSettings = CustomIconPackSettings(),
    @Contextual
    val autoWallpaperConfig: AutoWallpaperConfig = AutoWallpaperConfig(),
    val hasCompletedOnboarding: Boolean = false
)

enum class WidgetType(val displayName: String, val description: String, val iconEmoji: String) {
    CLOCK("Clock Widget", "Customizable digital, analog, or word clock with date", "🕒"),
    WEATHER_FORECAST("Weather & Forecast", "Current condition and hourly forecast", "⛅"),
    NOTIFICATION_SUMMARY("Notification Digest", "Unified summary of low-priority & promotional alerts", "🔕"),
    MEDIA_PLAYER("YouTube Music", "Live playback, controls and album art", "🎵"),
    SYSTEM_WIDGET("System Widget", "Android AppWidget from installed applications", "🧩"),
    NEWS_FEED("News Highlights", "Top breaking stories & articles with photo cards", "📰"),
    QUICK_NOTE("Sticky Note", "Editable scratchpad for quick thoughts", "📝"),
    MINDFULNESS_QUOTE("Daily Reflection", "Mindfulness quotes and wellbeing stats", "🧘"),
    DUAL_CLOCK("World Time", "Local time & secondary timezone", "🌍")
}

enum class WidgetChromeMode {
    LAUNCHER,
    NONE
}

data class CustomWidgetConfig(
    val id: String,
    val type: WidgetType,
    val title: String,
    val content: String = "",
    val isEnabled: Boolean = true,
    val heightCategory: String = "4x2", // "4x1", "4x2", "4x3", "4x4"
    val appWidgetId: Int = -1,
    val chromeMode: WidgetChromeMode = if (type == WidgetType.SYSTEM_WIDGET) WidgetChromeMode.NONE else WidgetChromeMode.LAUNCHER
)

data class NewsStory(
    val id: String,
    val title: String,
    val description: String,
    val url: String,
    val publisher: String,
    val imageUrl: String?,
    val category: String,
    val publishedAtEpochMillis: Long?
)

sealed interface NewsFeedState {
    data object NeedsApiKey : NewsFeedState
    data class Loading(val cached: List<NewsStory>) : NewsFeedState
    data class Ready(val stories: List<NewsStory>, val lastUpdated: Long, val remainingQuota: Int) : NewsFeedState
    data class Stale(val stories: List<NewsStory>, val lastError: NewsError) : NewsFeedState
    data class Failed(val error: NewsError) : NewsFeedState
}

sealed interface NewsError {
    data object Offline : NewsError
    data class InvalidKey(val msg: String) : NewsError
    data class QuotaExceeded(val retryAfterSeconds: Long) : NewsError
    data class BadRequest(val field: String?, val msg: String) : NewsError
    data class Server(val code: Int) : NewsError
    data class Unknown(val msg: String) : NewsError
}

data class NewsPreferencesState(
    val apiKey: String = "",
    val configured: Boolean = false,
    val language: String = "en",
    val country: String = "",
    val categories: Set<String> = setOf("general"),
    val keywords: String = "",
    val query: String = "",
    val useDateWindow: Boolean = false,
    val windowHours: Int = 24,
    val requireImages: Boolean = false,
    val pageSize: Int = 20,
    val minRefreshMinutes: Int = 60,
    val lastSuccessEpoch: Long = 0L,
    val rateLimitRemaining: Int = 100,
    val rateLimitResetEpoch: Long = 0L,
    val backoffUntilEpoch: Long = 0L,
    /**
     * Opt-in flag for the launcher scheduling background news refreshes.
     *
     * Defaults to **false**: a user's news API quota is a finite, paid-for resource, so the
     * launcher never spends it on the user's behalf without an explicit opt-in. When false, cached
     * stories stay fully visible and only a user-initiated (forced) refresh may hit the network.
     */
    val autoRefreshEnabled: Boolean = false
)

data class NewsCategory(val code: String, val label: String)

class NewsApiException(val code: Int, val newsError: NewsError) : Exception(newsError.toString())


