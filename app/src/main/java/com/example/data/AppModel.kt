package com.example.data

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color

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

enum class WallpaperStyle(val displayName: String) {
    SYSTEM_DEVICE("Device Wallpaper"),
    FLUX_EMBLEM("Flux Signature Wallpaper"),
    FLUX_MINIMAL_GRADIENT("Flux Soft Gradient"),
    SYSTEM_SOLID("Solid Dark Theme")
}

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
    @Transient val iconDrawable: Drawable? = null
)

data class NotificationItem(
    val id: String,
    val packageName: String,
    val sender: String,
    val message: String,
    val timeAgo: String,
    val canReply: Boolean = true,
    val key: String = id
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
    val isPlaying: Boolean = true,
    val trackTitle: String = "Midnight City",
    val artistName: String = "M83",
    val appName: String = "YouTube Music",
    val progressPercent: Float = 0.42f,
    val durationText: String = "4:03",
    val currentTimeText: String = "1:42"
)

data class LauncherSettings(
    val leftHandedMode: Boolean = false,
    val hideStatusBar: Boolean = false,
    val clockStyle: ClockStyle = ClockStyle.FLUX_INLINE,
    val showSeconds: Boolean = false,
    val showDate: Boolean = true,
    val showClockHeader: Boolean = true,
    val clockScale: Float = 1.0f,
    val clockColorTintHex: String = "#FFFFFF",
    val iconPackType: IconPackType = IconPackType.DEFAULT_APP,
    val themePreset: LauncherThemePreset = LauncherThemePreset.FLUX_EMERALD,
    val wallpaperStyle: WallpaperStyle = WallpaperStyle.FLUX_EMBLEM,
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
    val showAppNotificationBadges: Boolean = true,
    val compactAppRowSpacing: Boolean = false,
    val showDock: Boolean = true,
    val dockSlotCount: Int = 3, // 3, 4, or 5
    val showDockLabels: Boolean = false,
    val dockBlurBackground: Boolean = true,
    val dockIconScale: Float = 1.0f,
    val blurBackgrounds: Boolean = true,
    val waveAlphabetHapticIntensity: Int = 2, // 1: subtle, 2: medium, 3: heavy
    val customIconSettings: CustomIconPackSettings = CustomIconPackSettings(),
    val hasCompletedOnboarding: Boolean = false
)

enum class WidgetType(val displayName: String, val description: String, val iconEmoji: String) {
    CLOCK("Clock Widget", "Customizable digital, analog, or word clock with date", "🕒"),
    WEATHER_FORECAST("Weather & Forecast", "Current condition and hourly forecast", "⛅"),
    NEWS_FEED("News Highlights", "Top breaking stories & articles with photo cards", "📰"),
    MEDIA_PLAYER("Media Player", "Live playback and controls", "🎵"),
    CALENDAR_AGENDA("Calendar Agenda", "Upcoming events and schedule", "📅"),
    QUICK_NOTE("Sticky Note", "Editable scratchpad for quick thoughts", "📝"),
    SYSTEM_TOGGLES("System Toggles", "Battery, Wi-Fi, Bluetooth, Torch", "⚡"),
    MINDFULNESS_QUOTE("Daily Reflection", "Mindfulness quotes and wellbeing stats", "🧘"),
    DAILY_HABITS("Habit Tracker", "Check off daily tasks & habits", "✅"),
    DUAL_CLOCK("World Time", "Local time & secondary timezone", "🌍"),
    SYSTEM_APPWIDGET("System Widget", "Native Android system widget from installed apps", "📱")
}

data class CustomWidgetConfig(
    val id: String,
    val type: WidgetType,
    val title: String,
    val content: String = "",
    val isEnabled: Boolean = true,
    val appWidgetId: Int = -1,
    val providerPackage: String? = null,
    val providerClass: String? = null,
    val linkedPackageName: String? = null,
    val heightCategory: String = "4x2" // "4x1", "4x2", "4x3", "Full"
)

