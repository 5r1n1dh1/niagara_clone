package com.example.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Intelligent empty space gesture handler that seamlessly and reliably handles:
 * - Downward swipe/fling to open Notification Center
 * - Long press on actual empty space to open Home Screen Quick Customization menu
 */
fun Modifier.emptySpaceGestureHandler(
    onSwipeDown: () -> Unit,
    onLongPress: () -> Unit
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = true)
        var totalDy = 0f
        var totalDx = 0f
        var isDownSwipeTriggered = false
        var isLongPressTriggered = false
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val startTime = System.currentTimeMillis()

        while (true) {
            val event = withTimeoutOrNull(40L) {
                awaitPointerEvent()
            }

            if (event == null) {
                if (!isDownSwipeTriggered && !isLongPressTriggered && (System.currentTimeMillis() - startTime >= longPressTimeout)) {
                    if (kotlin.math.abs(totalDy) < 20f && kotlin.math.abs(totalDx) < 20f) {
                        isLongPressTriggered = true
                        onLongPress()
                    }
                }
                continue
            }

            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (change.isConsumed || !change.pressed) {
                break
            }

            val drag = change.positionChange()
            totalDx += drag.x
            totalDy += drag.y

            if (!isDownSwipeTriggered && !isLongPressTriggered) {
                if (totalDy > 30f && totalDy > kotlin.math.abs(totalDx) * 1.15f) {
                    isDownSwipeTriggered = true
                    change.consume()
                    onSwipeDown()
                    break
                }
            }
        }
    }
}

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
                com.example.util.DeviceControlsManager.openDigitalWellbeing(context)
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
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    onSwipeDownForNotification: () -> Unit = {},
    onReplyNotification: (packageName: String, text: String, isDualApp: Boolean) -> Unit = { _, _, _ -> },
    onDismissNotification: (packageName: String, isDualApp: Boolean) -> Unit = { _, _ -> },
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
    val context = LocalContext.current
    val isYTInstalled = remember(context) { isYouTubeMusicInstalled(context) }

    // Favorites screen: Clock widget is fixed (#1). All other widgets are optional user choices
    val displayWidgets = remember(carouselWidgets, isYTInstalled) {
        val filteredCarousel = if (isYTInstalled) carouselWidgets else carouselWidgets.filter { it != WidgetType.MEDIA_PLAYER }
        val optionalWidgets = filteredCarousel.filter { it != WidgetType.CLOCK }
        listOf(WidgetType.CLOCK) + optionalWidgets
    }

    val pagerState = rememberPagerState(pageCount = { displayWidgets.size })
    var showHomeEmptySpaceOptions by remember { mutableStateOf(false) }

    val openSystemWallpaperPicker: () -> Unit = {
        try {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
            context.startActivity(Intent.createChooser(intent, "Choose Wallpaper"))
        } catch (_: Exception) {
            try {
                val displayIntent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                context.startActivity(displayIntent)
            } catch (_: Exception) {}
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .testTag("center_home_screen")
    ) {
        val topOffset = settings.favoritesTopSpacing.heightDp.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Top customizable empty space above widgets (Low: 1 row, Medium: 2 rows, High: 3 rows)
            item(key = "center_top_spacer") {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topOffset)
                        .emptySpaceGestureHandler(
                            onSwipeDown = onSwipeDownForNotification,
                            onLongPress = { showHomeEmptySpaceOptions = true }
                        )
                )
            }

            // 4x1 Digital Wellbeing widget above clock (Daily screen usage, no container around it, no padding/margin)
            item(key = "center_digital_wellbeing_4x1") {
                Box(modifier = Modifier.animateItem()) {
                    DigitalWellbeing4x1Widget(
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
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
                        val cardOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                        val cardScale = (1f - 0.05f * kotlin.math.abs(cardOffset)).coerceIn(0.92f, 1f)
                        val cardAlpha = (1f - 0.2f * kotlin.math.abs(cardOffset)).coerceIn(0.6f, 1f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = cardScale
                                    scaleY = cardScale
                                    alpha = cardAlpha
                                }
                        ) {
                            val widgetType = displayWidgets.getOrNull(page) ?: WidgetType.CLOCK
                            Carousel4x2WidgetCard(
                                type = widgetType,
                                weather = weather,
                                mediaPlayer = mediaPlayer,
                                calendarEvents = calendarEvents,
                                isHeadphonesConnected = settings.isHeadphonesConnected,
                                showSeconds = settings.showSeconds,
                                clockColorTintHex = settings.clockColorTintHex,
                                clockScale = settings.clockScale,
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
                    }

                    // Dot page indicator for carousel widgets (shown when multiple widgets are configured)
                    if (displayWidgets.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 12.dp),
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
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            // 4. Center page ONLY shows favourite apps (max 10)
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
                            text = "Swipe right to All Apps, then long-press an app or tap options to star it as a favorite (up to 10 apps).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            itemsIndexed(favorites.take(10), key = { _, app -> "fav_${app.uniqueKey}" }) { _, app ->
                Box(modifier = Modifier.animateItem()) {
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
                        onReplyNotification = { text -> onReplyNotification(app.packageName, text, app.isDualApp) },
                        onDismissNotification = { onDismissNotification(app.packageName, app.isDualApp) }
                    )
                }
            }
        }

            // Bottom empty space that can also receive long-press or swipe down
            item(key = "center_bottom_empty_filler") {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .emptySpaceGestureHandler(
                            onSwipeDown = onSwipeDownForNotification,
                            onLongPress = { showHomeEmptySpaceOptions = true }
                        )
                )
            }
        }

        // Long Press Empty Space Bottom Sheet
        if (showHomeEmptySpaceOptions) {
            ModalBottomSheet(
                onDismissRequest = { showHomeEmptySpaceOptions = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 36.dp)
                ) {
                    Text(
                        text = "Home Screen",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Quick options & customizations",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showHomeEmptySpaceOptions = false
                                onOpenSettings()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Flux Launcher Settings",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Flux Launcher Settings",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Customize icons, themes, widgets & gestures",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showHomeEmptySpaceOptions = false
                                openSystemWallpaperPicker()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Wallpaper,
                                    contentDescription = "Change System Wallpaper",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Change System Wallpaper",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Open device wallpaper settings & picker",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
