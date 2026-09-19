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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
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
    onSwipeRightForPopup: (AppItem) -> Unit,
    onReplyNotification: (packageName: String, text: String) -> Unit,
    onDismissNotification: (packageName: String) -> Unit,
    onToggleMediaPlay: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit,
    onToggleHeadphones: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenCarouselCustomize: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToAllApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { carouselWidgets.size.coerceAtMost(5) })

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .testTag("center_home_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp)
    ) {
        // Flux Emblem breathing room
        if (settings.wallpaperStyle == WallpaperStyle.FLUX_EMBLEM) {
            item(key = "flux_wallpaper_emblem_spacer") {
                Spacer(modifier = Modifier.height(130.dp))
            }
        }

        // Clock Header
        if (settings.showClockHeader) {
            item(key = "center_clock_header") {
                ClockHeader(
                    clockStyle = settings.clockStyle,
                    showSeconds = settings.showSeconds,
                    showDate = settings.showDate,
                    weather = weather,
                    upcomingEvent = calendarEvents.firstOrNull(),
                    fontScale = settings.fontScale,
                    isLeftHanded = settings.leftHandedMode,
                    onClockClick = onOpenSettings,
                    onDateClick = onOpenCalendar,
                    onWeatherClick = onOpenWeather
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 4.1 Swipable 4x2 Widgets Carousel (Max 5 widgets)
        item(key = "center_carousel_container") {
            Column(modifier = Modifier.fillMaxWidth()) {
                val displayWidgets = carouselWidgets.take(5)

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("center_4x2_carousel")
                ) { page ->
                    val widgetType = displayWidgets.getOrNull(page) ?: WidgetType.WEATHER_FORECAST
                    Carousel4x2WidgetCard(
                        type = widgetType,
                        weather = weather,
                        mediaPlayer = mediaPlayer,
                        calendarEvents = calendarEvents,
                        isHeadphonesConnected = settings.isHeadphonesConnected,
                        onToggleMediaPlay = onToggleMediaPlay,
                        onMediaNext = onMediaNext,
                        onMediaPrev = onMediaPrev,
                        onToggleHeadphones = onToggleHeadphones,
                        onOpenWeather = onOpenWeather,
                        onOpenCalendar = onOpenCalendar,
                        onOpenCustomize = onOpenCarouselCustomize
                    )
                }

                // Dot page indicator for carousel
                if (displayWidgets.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(displayWidgets.size) { idx ->
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
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Section Title: Favorite Apps
        item(key = "center_fav_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FAVORITES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "${favorites.size} apps",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                            text = "Swipe right to All Apps, then swipe right or long-press an app to star it as a favorite.",
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
                    onSwipeRightForPopup = { onSwipeRightForPopup(app) },
                    onReplyNotification = { text -> onReplyNotification(app.packageName, text) },
                    onDismissNotification = { onDismissNotification(app.packageName) }
                )
            }
        }
    }
}
