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
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .testTag("usage_details_widget_card")
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Screen time segment (4x1 compact)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "Screen Time",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "3h 42m",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Screen Time",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .height(30.dp)
                    .padding(horizontal = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Steps segment (4x1 compact)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsWalk,
                        contentDescription = "Steps",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "6,420",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "64%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { 0.64f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
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
        val topOffset = maxHeight * 0.25f

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // Top offset spacer
            item(key = "center_top_spacer") {
                Spacer(modifier = Modifier.height(topOffset * 0.4f))
            }

            // Usage Details Widget Above Clock
            item(key = "usage_details_widget_card") {
                UsageDetailsWidgetCard(
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
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
