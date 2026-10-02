package com.example

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.*
import com.example.ui.LauncherViewModel
import com.example.ui.components.*
import com.example.ui.theme.NiagaraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle shortcut intent if launched via static/dynamic shortcut
        handleShortcutIntent(intent)

        // Publish or refresh dynamic shortcut on app start asynchronously
        lifecycleScope.launch(Dispatchers.IO) {
            com.example.util.AppShortcutHelper.publishRecentChatShortcut(applicationContext)
        }

        setContent {
            val settings by viewModel.settings.collectAsState()

            LaunchedEffect(settings.hideStatusBar) {
                window.decorView.post {
                    try {
                        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        if (settings.hideStatusBar) {
                            windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
                        } else {
                            windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                        }
                    } catch (_: Throwable) {}
                }
            }

            NiagaraTheme(
                preset = settings.themePreset,
                fontFamily = settings.customFont
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    NiagaraLauncherScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
        if (intent.action == Intent.ACTION_MAIN || intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.onHomeGestureTriggered()
        }
    }

    private fun handleShortcutIntent(intent: Intent?) {
        if (intent == null) return
        val chatId = intent.getStringExtra(com.example.util.AppShortcutHelper.EXTRA_CHAT_ID)
        if (!chatId.isNullOrBlank()) {
            android.util.Log.i(TAG, "Intercepted App Shortcut - Recent Chat triggered with EXTRA_CHAT_ID: $chatId")
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_BACKGROUND) {
            com.example.ui.components.clearIconBitmapCache()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.ui.components.clearIconBitmapCache()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NiagaraLauncherScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isAppsLoading by viewModel.isAppsLoading.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val calendarEvents by viewModel.calendarEvents.collectAsState()
    val mediaPlayer by viewModel.mediaPlayer.collectAsState()
    val activeNotifications by viewModel.activeNotifications.collectAsState()
    val allActiveNotifications by viewModel.allActiveNotifications.collectAsState()
    val leftWidgets by viewModel.leftWidgets.collectAsState()
    val carouselWidgets by viewModel.carouselWidgets.collectAsState()
    val dockApps by viewModel.dockApps.collectAsState()
    val userCategories by viewModel.userCategories.collectAsState()
    val systemWallpaperBitmap by viewModel.systemWallpaperBitmap.collectAsState()
    val autoWallpaperBitmap by viewModel.autoWallpaperBitmap.collectAsState()
    val isWallpaperSwitching by viewModel.isWallpaperSwitching.collectAsState()
    val isReorderFavoritesOpen by viewModel.isReorderFavoritesOpen.collectAsState()
    val newsState by viewModel.newsState.collectAsState()
    val newsPreferences by viewModel.newsPreferences.collectAsState()
    val isNewsSetupOpen by viewModel.isNewsSetupOpen.collectAsState()

    val glass = remember(settings.frostedTranslucency, settings.frostedBlurRadius, settings.frostedGlassEffect, settings.themePreset) {
        com.example.ui.theme.GlassStyle.create(
            translucency = settings.frostedTranslucency,
            blurDp = settings.frostedBlurRadius,
            specular = settings.frostedGlassEffect,
            preset = settings.themePreset
        )
    }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshAllData()
    }

    LaunchedEffect(Unit) {
        com.example.service.FluxNotificationListenerService.triggerSync()
        val permissionsToRequest = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    val availableWidgetTypes = remember {
        WidgetType.entries.filter { viewModel.repository.isWidgetAvailable(it) }
    }

    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val appWidgetHost = remember { AppWidgetHost(context, 1024) }
    var pendingAppWidgetId by remember { mutableStateOf<Int?>(null) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, appWidgetHost) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                try {
                    appWidgetHost.startListening()
                } catch (_: Throwable) {}
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadSystemWallpaper()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                try {
                    appWidgetHost.stopListening()
                } catch (_: Throwable) {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
            try {
                appWidgetHost.startListening()
            } catch (_: Throwable) {}
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                appWidgetHost.stopListening()
            } catch (_: Throwable) {}
        }
    }

    var isSystemWidgetPickerOpen by remember { mutableStateOf(false) }
    var pendingWidgetProviderInfo by remember { mutableStateOf<android.appwidget.AppWidgetProviderInfo?>(null) }
    var pendingWidgetHeightCategory by remember { mutableStateOf("4x2") }

    val configureWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val appWidgetId = pendingAppWidgetId
        val providerInfo = pendingWidgetProviderInfo
        val heightCategory = pendingWidgetHeightCategory
        if (result.resultCode == Activity.RESULT_OK && appWidgetId != null && appWidgetId > 0) {
            val info = appWidgetManager.getAppWidgetInfo(appWidgetId) ?: providerInfo
            val label = info?.loadLabel(context.packageManager)?.toString() ?: "System Widget"
            viewModel.addLeftSystemWidget(appWidgetId, label, heightCategory)
        } else if (appWidgetId != null && appWidgetId > 0) {
            try {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            } catch (_: Throwable) {}
        }
        pendingAppWidgetId = null
        pendingWidgetProviderInfo = null
    }

    val bindWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val appWidgetId = pendingAppWidgetId
        val providerInfo = pendingWidgetProviderInfo
        val heightCategory = pendingWidgetHeightCategory
        if (result.resultCode == Activity.RESULT_OK && appWidgetId != null && appWidgetId > 0 && providerInfo != null) {
            if (providerInfo.configure != null) {
                val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = providerInfo.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                configureWidgetLauncher.launch(configureIntent)
            } else {
                val label = providerInfo.loadLabel(context.packageManager)?.toString() ?: "System Widget"
                viewModel.addLeftSystemWidget(appWidgetId, label, heightCategory)
                pendingAppWidgetId = null
                pendingWidgetProviderInfo = null
            }
        } else {
            if (appWidgetId != null && appWidgetId > 0) {
                try {
                    appWidgetHost.deleteAppWidgetId(appWidgetId)
                } catch (_: Throwable) {}
            }
            pendingAppWidgetId = null
            pendingWidgetProviderInfo = null
        }
    }

    val launchAddSystemWidget: () -> Unit = {
        isSystemWidgetPickerOpen = true
    }

    val isSearchOpen by viewModel.isSearchOpen.collectAsState()
    val isNotificationCenterOpen by viewModel.isNotificationCenterOpen.collectAsState()
    val isDiscoverOpen by viewModel.isDiscoverOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isOnboardingOpen by viewModel.isOnboardingOpen.collectAsState()
    val isSecretLabOpen by viewModel.isSecretLabOpen.collectAsState()
    val isDebugOverlayOpen by viewModel.isDebugOverlayOpen.collectAsState()
    val isSleepOverlayVisible by viewModel.isSleepOverlayVisible.collectAsState()
    val activeAppForPopup by viewModel.activeAppForPopup.collectAsState()
    val activeUsageBreakerApp by viewModel.activeUsageBreakerApp.collectAsState()
    val waveSelectedLetter by viewModel.waveSelectedLetter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchFilteredApps by viewModel.searchFilteredApps.collectAsState()
    val mathResult by viewModel.mathResult.collectAsState()

    val isAddWidgetOpen by viewModel.isAddWidgetOpen.collectAsState()
    val editingNoteWidget by viewModel.editingNoteWidget.collectAsState()
    val isCarouselCustomizeOpen by viewModel.isCarouselCustomizeOpen.collectAsState()
    val dockPickerSlot by viewModel.dockPickerSlot.collectAsState()

    // 1. FIXED 3-Page Home Screen Layout:
    // Page 0: Left most screen (Widgets CRUD)
    // Page 1: Center screen (Swipeable 4x2 widgets above Favorites)
    // Page 2: Right most screen (All Apps with margins & Wave Alphabet)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Ensure keyboard is dismissed and focus cleared whenever switching away from All Apps screen
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 2) {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    // Home Gesture / Intent handling: always scroll to page 1 (Favorites) smoothly
    LaunchedEffect(Unit) {
        viewModel.homeGestureEvent.collect {
            if (pagerState.currentPage != 1) {
                pagerState.animateScrollToPage(
                    1,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    // Dismiss keyboard when launcher resumes from opened external app or when paused
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_PAUSE) {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Dynamic Gaussian blur on wallpaper when swiping to Widgets (page 0) or Apps (page 2)
    val pageOffset = (pagerState.currentPage - 1) + pagerState.currentPageOffsetFraction
    val blurFraction = kotlin.math.abs(pageOffset).coerceIn(0f, 1f)
    val wallpaperBlurRadius = if (settings.themePreset == LauncherThemePreset.FROSTED_GLASS) {
        (blurFraction * 28f).dp
    } else {
        (blurFraction * 20f).dp
    }

    val isAnyPanelOpen = isSearchOpen || isSettingsOpen || isNotificationCenterOpen ||
        isAddWidgetOpen || (editingNoteWidget != null) || isReorderFavoritesOpen ||
        isCarouselCustomizeOpen || (activeAppForPopup != null) ||
        (dockPickerSlot != null) || isSecretLabOpen || isDebugOverlayOpen || isDiscoverOpen || (activeUsageBreakerApp != null) || isNewsSetupOpen

    // Handle system back gesture / back button defensively
    BackHandler(enabled = isAnyPanelOpen || isSleepOverlayVisible || isOnboardingOpen || pagerState.currentPage != 1) {
        when {
            isSleepOverlayVisible -> viewModel.dismissSleepOverlay()
            activeAppForPopup != null -> viewModel.closeAppPopup()
            activeUsageBreakerApp != null -> viewModel.cancelUsageBreaker()
            isNewsSetupOpen -> viewModel.closeNewsSetup()
            isReorderFavoritesOpen -> viewModel.closeReorderFavorites()
            isSearchOpen -> viewModel.closeSearch()
            isNotificationCenterOpen -> viewModel.closeNotificationCenter()
            isDiscoverOpen -> viewModel.closeDiscover()
            isSettingsOpen -> viewModel.closeSettings()
            isOnboardingOpen -> viewModel.closeSetupGuide()
            isAddWidgetOpen -> viewModel.closeAddWidgetDialog()
            editingNoteWidget != null -> viewModel.closeEditNoteWidget()
            isCarouselCustomizeOpen -> viewModel.closeCarouselCustomize()
            dockPickerSlot != null -> viewModel.closeDockPicker()
            isSecretLabOpen -> viewModel.closeSecretLab()
            isDebugOverlayOpen -> viewModel.closeDebugOverlay()
            pagerState.currentPage != 1 -> {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(1)
                }
            }
        }
    }

    val panelBackgroundBlurRadius by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isAnyPanelOpen) 22.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 250),
        label = "panelBlur"
    )

    val panelBlurModifier = if (panelBackgroundBlurRadius > 0.5.dp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(panelBackgroundBlurRadius)
    } else {
        Modifier
    }

    val doubleTapToSleepEnabled = settings.doubleTapToSleep
    val triggerSleepState = rememberUpdatedState { viewModel.triggerDoubleTapSleep() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (doubleTapToSleepEnabled) {
                            triggerSleepState.value()
                        }
                    }
                )
            }
            .testTag("niagara_root")
    ) {
        // Live Wallpaper Backdrop supporting Device Wallpaper, Minimal Gradients, and Frosted Glass
        LauncherWallpaperBackdrop(
            themePreset = settings.themePreset,
            wallpaperStyle = settings.wallpaperStyle,
            systemWallpaperBitmap = systemWallpaperBitmap,
            autoWallpaperBitmap = autoWallpaperBitmap,
            blurRadius = wallpaperBlurRadius + panelBackgroundBlurRadius,
            backgroundBlurStrength = settings.frostedBlurRadius,
            modifier = Modifier.fillMaxSize()
        )

        // Launcher Foreground Screens & Dock Container (Blurred behind open panels)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(panelBlurModifier)
        ) {
            // 3-Page Horizontal Pager with Smooth Page Transitions
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_screens_pager")
            ) { page ->
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                val pageScale = (1f - 0.04f * kotlin.math.abs(pageOffset)).coerceIn(0.92f, 1f)
                val pageAlpha = (1f - 0.25f * kotlin.math.abs(pageOffset)).coerceIn(0.65f, 1f)
                val isSwiping = kotlin.math.abs(pageOffset) > 0.001f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = if (isSwiping) pageScale else 1f
                            scaleY = if (isSwiping) pageScale else 1f
                            alpha = pageAlpha
                        }
                ) {
                    when (page) {
                        0 -> {
                            // 2. Left most page houses widgets - user can customize (CRUD)
                            LeftWidgetsScreen(
                                widgets = leftWidgets,
                                newsState = newsState,
                                newsPreferences = newsPreferences,
                                onRefreshNews = { viewModel.refreshNews(force = true) },
                                onOpenNewsSetup = { viewModel.openNewsSetup() },
                                onOpenArticle = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                                weather = weather,
                                mediaPlayer = mediaPlayer,
                                calendarEvents = calendarEvents,
                                isHeadphonesConnected = settings.isHeadphonesConnected,
                                glass = glass,
                                cardCornerRadius = settings.cardCornerRadius,
                                appWidgetHost = appWidgetHost,
                                appWidgetManager = appWidgetManager,
                                onAddWidgetClick = { viewModel.openAddWidgetDialog() },
                                onAddSystemWidgetClick = launchAddSystemWidget,
                                onDeleteWidget = { id ->
                                    val target = leftWidgets.firstOrNull { it.id == id }
                                    if (target != null && target.type == WidgetType.SYSTEM_WIDGET && target.appWidgetId > 0) {
                                        try {
                                            appWidgetHost.deleteAppWidgetId(target.appWidgetId)
                                        } catch (_: Throwable) {}
                                    }
                                    viewModel.deleteLeftWidget(id)
                                },
                                onReorderWidget = { from, to -> viewModel.reorderLeftWidget(from, to) },
                                onEditNoteClick = { widget -> viewModel.openEditNoteWidget(widget) },
                                onToggleMediaPlay = { viewModel.toggleMediaPlayPause() },
                                onMediaNext = { viewModel.skipMediaNext() },
                                onMediaPrev = { viewModel.skipMediaPrev() },
                                onToggleHeadphones = { viewModel.toggleHeadphonesConnected() },
                                onRefreshWeather = { viewModel.showToast("Updated weather for ${weather.location} ⛅") },
                                onOpenWeather = { viewModel.launchIntegration(com.example.util.IntegrationCategory.WEATHER) },
                                onOpenClock = { viewModel.launchIntegration(com.example.util.IntegrationCategory.CLOCK) },
                                onOpenCalendar = { viewModel.launchIntegration(com.example.util.IntegrationCategory.CALENDAR) },
                                onUpdateWidgetSize = { id, size -> viewModel.updateLeftWidgetSize(id, size) }
                            )
                        }
                        1 -> {
                            // 4. Center page: Swipable 4x2 widgets above favorites, NO alphabet scrollbar
                            CenterHomeScreen(
                                carouselWidgets = carouselWidgets,
                                newsState = newsState,
                                newsPreferences = newsPreferences,
                                onRefreshNews = { viewModel.refreshNews(force = true) },
                                onOpenNewsSetup = { viewModel.openNewsSetup() },
                                onOpenArticle = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                                favorites = favorites,
                                isAppsLoading = isAppsLoading,
                                settings = settings,
                                weather = weather,
                                mediaPlayer = mediaPlayer,
                                calendarEvents = calendarEvents,
                                appWidgetHost = appWidgetHost,
                                appWidgetManager = appWidgetManager,
                                onAppClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    viewModel.launchApp(it)
                                },
                                onAppLongClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    viewModel.openAppPopup(it)
                                },
                                onSwipeDownForNotification = { viewModel.openNotificationCenter() },
                                onReplyNotification = { pkg, text, isDualApp -> viewModel.replyToNotification(pkg, text, isDualApp = isDualApp) },
                                onDismissNotification = { pkg, isDualApp -> viewModel.dismissNotification(pkg, isDualApp = isDualApp) },
                                onToggleMediaPlay = { viewModel.toggleMediaPlayPause() },
                                onMediaNext = { viewModel.skipMediaNext() },
                                onMediaPrev = { viewModel.skipMediaPrev() },
                                onSeekMedia = { viewModel.seekMedia(it) },
                                onToggleHeadphones = { viewModel.toggleHeadphonesConnected() },
                                onOpenWeather = { viewModel.launchIntegration(com.example.util.IntegrationCategory.WEATHER) },
                                onOpenClock = { viewModel.launchIntegration(com.example.util.IntegrationCategory.CLOCK) },
                                onOpenCalendar = { viewModel.launchIntegration(com.example.util.IntegrationCategory.CALENDAR) },
                                onOpenCarouselCustomize = { viewModel.openCarouselCustomize() },
                                onOpenReorderFavorites = { viewModel.openReorderFavorites() },
                                onAddSystemWidget = launchAddSystemWidget,
                                onOpenSettings = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    viewModel.openSettings()
                                },
                                onNavigateToAllApps = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            2,
                                            animationSpec = androidx.compose.animation.core.spring(
                                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                                                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                            )
                        }
                        2 -> {
                            // 3. Right most screen: Lists all apps with strict margins
                            RightAllAppsScreen(
                                allApps = allApps,
                                folders = folders,
                                isAppsLoading = isAppsLoading,
                                settings = settings,
                                selectedWaveLetter = waveSelectedLetter,
                                onWaveLetterSelected = { viewModel.setWaveSelectedLetter(it) },
                                onAppClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    viewModel.launchApp(it)
                                },
                                onAppLongClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    viewModel.openAppPopup(it)
                                },
                                onReplyNotification = { pkg, text, isDualApp -> viewModel.replyToNotification(pkg, text, isDualApp = isDualApp) },
                                onDismissNotification = { pkg, isDualApp -> viewModel.dismissNotification(pkg, isDualApp = isDualApp) },
                                onToggleFolderExpanded = { folderId -> viewModel.repository.toggleFolderExpanded(folderId) },
                                onOpenSettings = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    viewModel.openSettings()
                                }
                            )
                        }
                    }
                }
            }

            // Top Screen Indicator (Smooth animated dots indicating Left / Center / Right page)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { idx ->
                    val isSelected = pagerState.currentPage == idx
                    val animatedWidth by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (isSelected) 20.dp else 6.dp,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        ),
                        label = "dot_width_$idx"
                    )

                    Box(
                        modifier = Modifier
                            .size(animatedWidth, 4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        idx,
                                        animationSpec = androidx.compose.animation.core.spring(
                                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                    )
                }
            }

        // 5. Fixed Dock (Visible ONLY on Home Screen per user request)
        if (pagerState.currentPage == 1 && settings.showDock) {
            FixedDock(
                dockApps = dockApps,
                iconPackType = settings.iconPackType,
                showLabels = settings.showDockLabels,
                blurBackground = settings.dockBlurBackground,
                slotCount = settings.dockSlotCount,
                iconScale = settings.dockIconScale,
                cardCornerRadius = settings.cardCornerRadius,
                glass = glass,
                onAppClick = { viewModel.launchApp(it) },
                onAppLongClick = { slotIndex -> viewModel.openDockPicker(slotIndex) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        }

        // ==========================================
        // DIALOGS, SHEETS & OVERLAYS
        // ==========================================

        // Dock slot app picker
        dockPickerSlot?.let { slotIndex ->
            DockAppPickerDialog(
                slotIndex = slotIndex,
                allApps = allApps,
                iconPackType = settings.iconPackType,
                glass = glass,
                onSelectApp = { app -> viewModel.selectDockApp(slotIndex, app) },
                onDismiss = { viewModel.closeDockPicker() }
            )
        }

        // Left widgets Add dialog
        if (isAddWidgetOpen) {
            AddWidgetDialog(
                availableWidgets = availableWidgetTypes,
                glass = glass,
                onAddWidget = { type -> viewModel.addLeftWidget(type) },
                onDismiss = { viewModel.closeAddWidgetDialog() }
            )
        }

        // Left widgets Sticky Note Edit dialog
        editingNoteWidget?.let { noteWidget ->
            EditNoteDialog(
                widget = noteWidget,
                onSave = { title, content ->
                    viewModel.updateLeftWidget(noteWidget.id, title, content)
                },
                onDismiss = { viewModel.closeEditNoteWidget() }
            )
        }

        // Center carousel customize dialog
        if (isCarouselCustomizeOpen) {
            CarouselCustomizeDialog(
                currentWidgets = carouselWidgets,
                availableWidgets = availableWidgetTypes,
                glass = glass,
                onAddWidget = { viewModel.addCarouselWidget(it) },
                onRemoveWidget = { viewModel.removeCarouselWidget(it) },
                onReorder = { from, to -> viewModel.reorderCarouselWidget(from, to) },
                onDismiss = { viewModel.closeCarouselCustomize() }
            )
        }

        // News Setup Sheet
        if (isNewsSetupOpen) {
            NewsWidgetSetupSheet(
                initial = newsPreferences,
                glass = glass,
                onSave = { prefs ->
                    viewModel.saveNewsPreferences(prefs)
                    viewModel.closeNewsSetup()
                },
                onDismiss = { viewModel.closeNewsSetup() }
            )
        }

        // Unified Search Sheet
        if (isSearchOpen) {
            UnifiedSearchSheet(
                query = searchQuery,
                mathResult = mathResult,
                filteredApps = searchFilteredApps,
                iconPackType = settings.iconPackType,
                glass = glass,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                onAppClick = { app ->
                    viewModel.closeSearch()
                    viewModel.launchApp(app)
                },
                onDismiss = { viewModel.closeSearch() }
            )
        }

        // Google Discover Sheet
        if (isDiscoverOpen) {
            GoogleDiscoverSheet(
                glass = glass,
                onDismiss = { viewModel.closeDiscover() }
            )
        }

        // System App Widget Picker Sheet
        if (isSystemWidgetPickerOpen) {
            SystemWidgetPickerSheet(
                glass = glass,
                onSelectWidget = { providerInfo, heightCat ->
                    isSystemWidgetPickerOpen = false
                    try {
                        val newId = appWidgetHost.allocateAppWidgetId()
                        pendingAppWidgetId = newId
                        pendingWidgetProviderInfo = providerInfo
                        pendingWidgetHeightCategory = heightCat

                        val canBind = try {
                            appWidgetManager.bindAppWidgetIdIfAllowed(newId, providerInfo.provider)
                        } catch (_: Throwable) {
                            false
                        }

                        if (canBind) {
                            if (providerInfo.configure != null) {
                                val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                                    component = providerInfo.configure
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
                                }
                                configureWidgetLauncher.launch(configureIntent)
                            } else {
                                val label = providerInfo.loadLabel(context.packageManager)?.toString() ?: "System Widget"
                                viewModel.addLeftSystemWidget(newId, label, heightCat)
                                pendingAppWidgetId = null
                                pendingWidgetProviderInfo = null
                            }
                        } else {
                            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
                            }
                            bindWidgetLauncher.launch(bindIntent)
                        }
                    } catch (e: Throwable) {
                        viewModel.showToast("Could not add widget: ${e.localizedMessage ?: "Unknown error"}")
                        val idToClean = pendingAppWidgetId
                        if (idToClean != null && idToClean > 0) {
                            try { appWidgetHost.deleteAppWidgetId(idToClean) } catch (_: Throwable) {}
                        }
                        pendingAppWidgetId = null
                        pendingWidgetProviderInfo = null
                    }
                },
                onDismiss = { isSystemWidgetPickerOpen = false }
            )
        }

        // Notification Center & Quick Settings Shade
        if (isNotificationCenterOpen) {
            NotificationCenterSheet(
                notifications = activeNotifications,
                glass = glass,
                onDismissNotification = { pkg -> viewModel.dismissNotification(pkg) },
                onReplyNotification = { pkg, text -> viewModel.replyToNotification(pkg, text) },
                onClearAll = { viewModel.clearAllNotifications() },
                onClose = { viewModel.closeNotificationCenter() },
                onOpenSettings = {
                    viewModel.closeNotificationCenter()
                    viewModel.openSettings()
                }
            )
        }

        // Settings Studio Sheet
        if (isSettingsOpen) {
            SettingsStudioSheet(
                settings = settings,
                allApps = allApps,
                dockApps = dockApps,
                carouselWidgets = carouselWidgets,
                availableWidgets = availableWidgetTypes,
                widgetIntegrations = viewModel.widgetIntegrations,
                isWallpaperSwitching = isWallpaperSwitching,
                onRefreshAutoWallpaperNow = { viewModel.refreshAutoWallpaperNow() },
                onOpenReorderFavorites = { viewModel.openReorderFavorites() },
                onUpdateSettings = { viewModel.repository.updateSettings(it) },
                onSelectDockSlot = { slotIndex -> viewModel.openDockPicker(slotIndex) },
                onClearDockSlot = { slotIndex -> viewModel.clearDockSlot(slotIndex) },
                onResetDockDefaults = { viewModel.resetDockToDefaults() },
                onAddCarouselWidget = { viewModel.addCarouselWidget(it) },
                onRemoveCarouselWidget = { viewModel.removeCarouselWidget(it) },
                onReorderCarouselWidget = { from, to -> viewModel.reorderCarouselWidget(from, to) },
                onOpenAddSystemWidget = launchAddSystemWidget,
                onExportBackup = { viewModel.repository.exportBackupJson() },
                onImportBackup = { viewModel.repository.importBackupJson(it) },
                onResetDefaults = { viewModel.repository.resetToDefaults() },
                onOpenSetupGuide = { viewModel.openSetupGuide() },
                userCategories = userCategories,
                onAddCategory = { viewModel.addUserCategory(it) },
                onRenameCategory = { oldName, newName -> viewModel.updateUserCategoryName(oldName, newName) },
                onRemoveCategory = { viewModel.removeUserCategory(it) },
                onAssignCategory = { pkg, cat -> viewModel.setAppCategory(pkg, cat) },
                onDismiss = { viewModel.closeSettings() }
            )
        }

        // Reorder Favorites Sheet
        if (isReorderFavoritesOpen) {
            com.example.ui.components.ReorderFavoritesSheet(
                favorites = favorites,
                allApps = allApps,
                settings = settings,
                onDismiss = { viewModel.closeReorderFavorites() },
                onMoveUp = { index -> viewModel.moveFavoriteUp(index) },
                onMoveDown = { index -> viewModel.moveFavoriteDown(index) },
                onToggleFavorite = { pkg -> viewModel.toggleFavorite(pkg) }
            )
        }

        // Setup / Start Up Guide
        if (isOnboardingOpen) {
            SetupGuideScreen(
                installedApps = allApps,
                isAppsLoading = isAppsLoading,
                availableWidgets = availableWidgetTypes,
                currentSettings = settings,
                initialFavorites = favorites,
                initialCarouselWidgets = carouselWidgets,
                initialLeftWidgets = leftWidgets,
                onCompleteSetup = { favs, carousel, left, theme, leftHanded, doubleTap ->
                    viewModel.completeSetupGuide(favs, carousel, left, theme, leftHanded, doubleTap)
                },
                onSkip = {
                    viewModel.closeSetupGuide()
                }
            )
        }

        // Secret Lab Dialog
        if (isSecretLabOpen) {
            SecretLabDialog(onDismiss = { viewModel.closeSecretLab() })
        }

        // Debug Metrics Overlay
        if (isDebugOverlayOpen) {
            DebugMetricsOverlay(onDismiss = { viewModel.closeDebugOverlay() })
        }

        // App Popup Drawer (Contextual shortcuts, notifications, & options)
        activeAppForPopup?.let { app ->
            val instanceKey = app.instanceKey
            val notif = activeNotifications[instanceKey] ?: if (!app.isDualApp) activeNotifications[app.packageName] else null ?: app.notification
            val appNotifs = allActiveNotifications[instanceKey] ?: if (!app.isDualApp) allActiveNotifications[app.packageName] ?: emptyList() else emptyList()
            AppPopupDrawer(
                app = app,
                iconPackType = settings.iconPackType,
                customIconSettings = settings.customIconSettings,
                notification = notif,
                notifications = appNotifs,
                mediaPlayer = mediaPlayer,
                onReplyNotification = { replyText ->
                    viewModel.replyToNotification(app.packageName, replyText, isDualApp = app.isDualApp)
                },
                onDismissNotification = {
                    viewModel.dismissNotification(app.packageName, isDualApp = app.isDualApp)
                },
                onReplyNotificationKey = { key, replyText ->
                    viewModel.replyToNotification(app.packageName, replyText, key, isDualApp = app.isDualApp)
                },
                onDismissNotificationKey = { key ->
                    viewModel.dismissNotification(app.packageName, key, isDualApp = app.isDualApp)
                },
                onOpenApp = {
                    viewModel.closeAppPopup()
                    viewModel.launchApp(app)
                },
                onDismiss = { viewModel.closeAppPopup() },
                onToggleFavorite = { viewModel.toggleFavorite(app.packageName) },
                onOpenReorderFavorites = { viewModel.openReorderFavorites() },
                onToggleHide = { viewModel.toggleHideApp(app.packageName) },
                onToggleUsageBreaker = { viewModel.toggleUsageBreaker(app.packageName) },
                onDisguiseApp = { newName -> viewModel.repository.disguiseApp(app.packageName, newName) },
                onAssignCategory = { cat -> viewModel.setAppCategory(app.packageName, cat) },
                glass = glass
            )
        }

        // Digital Wellbeing Usage Breaker
        activeUsageBreakerApp?.let { app ->
            UsageBreakerDialog(
                app = app,
                durationSeconds = settings.usageBreakerDurationSeconds,
                onConfirmLaunch = { viewModel.confirmUsageBreakerAndLaunch(app) },
                onCancel = { viewModel.cancelUsageBreaker() }
            )
        }

        // Double tap sleep overlay
        SleepOverlay(
            isVisible = isSleepOverlayVisible,
            onWakeUp = { viewModel.dismissSleepOverlay() }
        )
    }
}
