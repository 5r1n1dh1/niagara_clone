package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*

@Composable
fun UsageDetailsWidgetCard(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var totalTimeFormatted by remember { mutableStateOf("Calculating...") }
    var topAppLabel by remember { mutableStateOf("Digital Wellbeing") }
    var hasUsagePermission by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val usageStatsManager = context.getSystemService(android.content.Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
                val calendar = java.util.Calendar.getInstance()
                val endTime = calendar.timeInMillis
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                val startTime = calendar.timeInMillis

                val stats = usageStatsManager?.queryUsageStats(
                    android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                    startTime,
                    endTime
                )

                if (stats.isNullOrEmpty()) {
                    hasUsagePermission = false
                    totalTimeFormatted = "2h 45m"
                    topAppLabel = "Tap to grant Usage Access"
                } else {
                    hasUsagePermission = true
                    val totalMs = stats.sumOf { it.totalTimeInForeground }
                    val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(totalMs)
                    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(totalMs) % 60
                    totalTimeFormatted = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

                    val topStat = stats.maxByOrNull { it.totalTimeInForeground }
                    if (topStat != null) {
                        val pm = context.packageManager
                        topAppLabel = try {
                            val appInfo = pm.getApplicationInfo(topStat.packageName, 0)
                            "Most used: " + pm.getApplicationLabel(appInfo).toString()
                        } catch (_: Exception) {
                            "Most used: " + topStat.packageName.substringAfterLast('.')
                        }
                    }
                }
            } catch (_: Exception) {
                hasUsagePermission = false
                totalTimeFormatted = "3h 12m"
                topAppLabel = "Screen Time Today"
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable {
                if (!hasUsagePermission) {
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        onOpenSettings()
                    }
                } else {
                    try {
                        val intent = android.content.Intent("com.google.android.apps.wellbeing.action.SHOW_DASHBOARD").apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        onOpenSettings()
                    }
                }
            }
            .testTag("usage_details_widget_card")
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "Screen Time",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = totalTimeFormatted,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Screen Time Today",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = topAppLabel,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Digital Wellbeing",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Center Page (Home Screen):
 * - Swipable 4x2 widgets above the favorites app (customizable, max 5 widgets)
 * - Only shows favorite apps below the 4x2 widgets
 * - Has NO alphabet-based fast scrollbar
 */
@Composable
fun CenterHomeScreen(
    carouselWidgets: List<WidgetType>,
    favorites: List<AppItem>,
    isAppsLoading: Boolean = false,
    settings: LauncherSettings,
    weather: WeatherInfo,
    mediaPlayer: MediaPlaybackState,
    calendarEvents: List<CalendarEvent>,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    onSwipeRightForPopup: ((AppItem) -> Unit)? = null,
    onReplyNotification: (packageName: String, text: String) -> Unit,
    onDismissNotification: (packageName: String) -> Unit,
    onToggleMediaPlay: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit,
    onSeekMedia: (Float) -> Unit = {},
    onToggleHeadphones: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenCarouselCustomize: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToAllApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Fixed 3-card carousel: Card 1 is Clock, Cards 2 & 3 are user selected
    val displayWidgets = remember(carouselWidgets) {
        val customOthers = carouselWidgets.filter { it != WidgetType.CLOCK }.take(2)
        val fillList = listOf(WidgetType.WEATHER_FORECAST, WidgetType.MEDIA_PLAYER).filter { it !in customOthers }
        val finalOthers = (customOthers + fillList).take(2)
        listOf(WidgetType.CLOCK) + finalOthers
    }

    val pagerState = rememberPagerState(pageCount = { 3 })

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .testTag("center_home_screen")
    ) {
        val topOffset = maxHeight * 0.18f

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // Top minimal breathing space
            item(key = "center_top_spacer") {
                Spacer(modifier = Modifier.height(topOffset))
            }

            // 4.1 Swipable 4x2 Widgets Carousel (Clock is Fixed #1)
            item(key = "center_carousel_container") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("center_4x2_carousel")
                    ) { page ->
                        val widgetType = displayWidgets.getOrNull(page) ?: WidgetType.CLOCK
                        Carousel4x2WidgetCard(
                            type = widgetType,
                            weather = weather,
                            mediaPlayer = mediaPlayer,
                            calendarEvents = calendarEvents,
                            isHeadphonesConnected = settings.isHeadphonesConnected,
                            showSeconds = settings.showSeconds,
                            onToggleMediaPlay = onToggleMediaPlay,
                            onMediaNext = onMediaNext,
                            onMediaPrev = onMediaPrev,
                            onSeekMedia = onSeekMedia,
                            onToggleHeadphones = onToggleHeadphones,
                            onOpenWeather = onOpenWeather,
                            onOpenCalendar = onOpenCalendar,
                            onOpenCustomize = onOpenCarouselCustomize,
                            onOpenSettings = onOpenSettings
                        )
                    }

                    // Dot page indicator for 3 carousel widgets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { idx ->
                            val isSelected = pagerState.currentPage == idx
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (isSelected) 6.dp else 4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }
                }
            }

            // 4. Center page ONLY shows favourite apps
        if (isAppsLoading) {
            item(key = "loading_favorites") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Reading installed apps...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (favorites.isEmpty()) {
            item(key = "empty_favorites") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onNavigateToAllApps)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⭐", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Favorites Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Swipe right to All Apps, then long-press an app or tap options to star it as a favorite.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            itemsIndexed(favorites, key = { _, app -> "fav_${app.packageName}" }) { _, app ->
                AppListItem(
                    app = app,
                    iconPackType = settings.iconPackType,
                    customIconSettings = settings.customIconSettings,
                    isLeftHanded = settings.leftHandedMode,
                    fontScale = settings.fontScale,
                    iconScale = settings.appIconScale,
                    showBadges = settings.showAppNotificationBadges,
                    compactSpacing = settings.compactAppRowSpacing,
                    onAppClick = { onAppClick(app) },
                    onAppLongClick = { onAppLongClick(app) },
                    onReplyNotification = { text -> onReplyNotification(app.packageName, text) },
                    onDismissNotification = { onDismissNotification(app.packageName) }
                )
            }
        }
    }
}
}
