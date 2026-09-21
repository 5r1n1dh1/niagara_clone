package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.*
import com.example.ui.LauncherViewModel
import com.example.ui.components.*
import com.example.ui.theme.NiagaraTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.util.SystemWidgetManager.init(applicationContext)
        com.example.util.SystemWidgetManager.startListening()
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()

            LaunchedEffect(settings.hideStatusBar) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (settings.hideStatusBar) {
                    windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
                } else {
                    windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
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

    override fun onStart() {
        super.onStart()
        com.example.util.SystemWidgetManager.startListening()
    }

    override fun onStop() {
        super.onStop()
        com.example.util.SystemWidgetManager.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.util.SystemWidgetManager.stopListening()
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
    val availableSystemWidgets by viewModel.availableSystemWidgets.collectAsState()
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

    val availableWidgetTypes = remember(availableSystemWidgets) {
        WidgetType.entries.filter { viewModel.repository.isWidgetAvailable(it) }
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
    val isSystemWidgetPickerOpen by viewModel.isSystemWidgetPickerOpen.collectAsState()
    val editingNoteWidget by viewModel.editingNoteWidget.collectAsState()
    val isCarouselCustomizeOpen by viewModel.isCarouselCustomizeOpen.collectAsState()
    val dockPickerSlot by viewModel.dockPickerSlot.collectAsState()

    // 1. FIXED 3-Page Home Screen Layout:
    // Page 0: Left most screen (Widgets CRUD)
    // Page 1: Center screen (Swipeable 4x2 widgets above Favorites)
    // Page 2: Right most screen (All Apps with margins & Wave Alphabet)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val notificationSwipeDownConnection = remember(settings.swipeDownForNotificationCenter) {
        object : NestedScrollConnection {
            var accumulatedDragY = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f) {
                    accumulatedDragY = 0f
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!settings.swipeDownForNotificationCenter) return Offset.Zero
                // Only trigger if no scrollable component consumed the drag (consumed.y == 0)
                if (source == NestedScrollSource.UserInput && consumed.y == 0f && available.y > 0f) {
                    accumulatedDragY += available.y
                    if (accumulatedDragY > 120f) {
                        accumulatedDragY = 0f
                        viewModel.openNotificationCenter()
                        return Offset(0f, available.y)
                    }
                } else {
                    accumulatedDragY = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val isTopEdgeFling = accumulatedDragY > 0f && consumed.y == 0f
                accumulatedDragY = 0f
                if (!settings.swipeDownForNotificationCenter) return Velocity.Zero
                if (isTopEdgeFling && available.y > 300f) {
                    viewModel.openNotificationCenter()
                    return available
                }
                return Velocity.Zero
            }
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
        isSystemWidgetPickerOpen || isAddWidgetOpen || (editingNoteWidget != null) ||
        isCarouselCustomizeOpen || (activeAppForPopup != null) ||
        (dockPickerSlot != null) || isSecretLabOpen || isDebugOverlayOpen || isDiscoverOpen || (activeUsageBreakerApp != null)

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(notificationSwipeDownConnection)
            .pointerInput(settings.doubleTapToSleep) {
                detectTapGestures(
                    onDoubleTap = {
                        viewModel.triggerDoubleTapSleep()
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
            // 3-Page Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_screens_pager")
            ) { page ->
            when (page) {
                0 -> {
                    // 2. Left most page houses widgets - user can customize (CRUD)
                    LeftWidgetsScreen(
                        widgets = leftWidgets,
                        weather = weather,
                        mediaPlayer = mediaPlayer,
                        calendarEvents = calendarEvents,
                        isHeadphonesConnected = settings.isHeadphonesConnected,
                        isFrostedTheme = settings.themePreset == LauncherThemePreset.FROSTED_GLASS,
                        blurRadius = settings.frostedBlurRadius,
                        translucency = settings.frostedTranslucency,
                        glassEffect = settings.frostedGlassEffect,
                        onAddWidgetClick = { viewModel.openAddWidgetDialog() },
                        onAddSystemWidgetClick = { viewModel.openSystemWidgetPicker() },
                        onDeleteWidget = { id -> viewModel.deleteLeftWidget(id) },
                        onReorderWidget = { from, to -> viewModel.reorderLeftWidget(from, to) },
                        onEditNoteClick = { widget -> viewModel.openEditNoteWidget(widget) },
                        onToggleMediaPlay = { viewModel.toggleMediaPlayPause() },
                        onMediaNext = { viewModel.skipMediaNext() },
                        onMediaPrev = { viewModel.skipMediaPrev() },
                        onToggleHeadphones = { viewModel.toggleHeadphonesConnected() },
                        onRefreshWeather = { viewModel.showToast("Updated weather for ${weather.location} ⛅") },
                        onOpenWeather = { viewModel.launchIntegration(com.example.util.IntegrationCategory.WEATHER) },
                        onOpenCalendar = { viewModel.launchIntegration(com.example.util.IntegrationCategory.CALENDAR) },
                        onUpdateWidgetSize = { id, size -> viewModel.updateLeftWidgetSize(id, size) }
                    )
                }
                1 -> {
                    // 4. Center page: Swipable 4x2 widgets above favorites, NO alphabet scrollbar
                    CenterHomeScreen(
                        carouselWidgets = carouselWidgets,
                        favorites = favorites,
                        isAppsLoading = isAppsLoading,
                        settings = settings,
                        weather = weather,
                        mediaPlayer = mediaPlayer,
                        calendarEvents = calendarEvents,
                        onAppClick = { viewModel.launchApp(it) },
                        onAppLongClick = { viewModel.openAppPopup(it) },
                        onReplyNotification = { pkg, text -> viewModel.replyToNotification(pkg, text) },
                        onDismissNotification = { pkg -> viewModel.dismissNotification(pkg) },
                        onToggleMediaPlay = { viewModel.toggleMediaPlayPause() },
                        onMediaNext = { viewModel.skipMediaNext() },
                        onMediaPrev = { viewModel.skipMediaPrev() },
                        onSeekMedia = { viewModel.seekMedia(it) },
                        onToggleHeadphones = { viewModel.toggleHeadphonesConnected() },
                        onOpenWeather = { viewModel.launchIntegration(com.example.util.IntegrationCategory.WEATHER) },
                        onOpenCalendar = { viewModel.launchIntegration(com.example.util.IntegrationCategory.CALENDAR) },
                        onOpenCarouselCustomize = { viewModel.openCarouselCustomize() },
                        onOpenSettings = { viewModel.openSettings() },
                        onNavigateToAllApps = {
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
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
                        onAppClick = { viewModel.launchApp(it) },
                        onAppLongClick = { viewModel.openAppPopup(it) },
                        onReplyNotification = { pkg, text -> viewModel.replyToNotification(pkg, text) },
                        onDismissNotification = { pkg -> viewModel.dismissNotification(pkg) },
                        onToggleFolderExpanded = { folderId -> viewModel.repository.toggleFolderExpanded(folderId) },
                        onOpenSettings = { viewModel.openSettings() }
                    )
                }
            }
        }

        // Top Screen Indicator (Dots indicating Left / Center / Right page)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 4.dp)
                .pointerInput(settings.swipeDownForNotificationCenter) {
                    if (settings.swipeDownForNotificationCenter) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 15f) {
                                viewModel.openNotificationCenter()
                            }
                        }
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val titles = listOf("Widgets", "Home", "All Apps")
            repeat(3) { idx ->
                val isSelected = pagerState.currentPage == idx
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 18.dp else 6.dp, 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                        )
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(idx) }
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
                isFrostedTheme = settings.themePreset == LauncherThemePreset.FROSTED_GLASS,
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
                onSelectApp = { app -> viewModel.selectDockApp(slotIndex, app) },
                onDismiss = { viewModel.closeDockPicker() }
            )
        }

        // Left widgets Add dialog
        if (isAddWidgetOpen) {
            AddWidgetDialog(
                availableWidgets = availableWidgetTypes,
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
                onAddWidget = { viewModel.addCarouselWidget(it) },
                onRemoveWidget = { viewModel.removeCarouselWidget(it) },
                onReorder = { from, to -> viewModel.reorderCarouselWidget(from, to) },
                onDismiss = { viewModel.closeCarouselCustomize() }
            )
        }

        // Unified Search Sheet
        if (isSearchOpen) {
            UnifiedSearchSheet(
                query = searchQuery,
                mathResult = mathResult,
                filteredApps = searchFilteredApps,
                iconPackType = settings.iconPackType,
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
                onDismiss = { viewModel.closeDiscover() }
            )
        }

        // Settings Studio Sheet
        if (isSettingsOpen) {
            SettingsStudioSheet(
                settings = settings,
                allApps = allApps,
                dockApps = dockApps,
                widgetIntegrations = viewModel.widgetIntegrations,
                onUpdateSettings = { viewModel.repository.updateSettings(it) },
                onSelectDockSlot = { slotIndex -> viewModel.openDockPicker(slotIndex) },
                onClearDockSlot = { slotIndex -> viewModel.clearDockSlot(slotIndex) },
                onResetDockDefaults = { viewModel.resetDockToDefaults() },
                onExportBackup = { viewModel.repository.exportBackupJson() },
                onImportBackup = { viewModel.repository.importBackupJson(it) },
                onResetDefaults = { viewModel.repository.resetToDefaults() },
                onOpenSetupGuide = { viewModel.openSetupGuide() },
                onOpenSystemWidgetPicker = { viewModel.openSystemWidgetPicker() },
                userCategories = userCategories,
                onAddCategory = { viewModel.addUserCategory(it) },
                onRenameCategory = { oldName, newName -> viewModel.updateUserCategoryName(oldName, newName) },
                onRemoveCategory = { viewModel.removeUserCategory(it) },
                onAssignCategory = { pkg, cat -> viewModel.setAppCategory(pkg, cat) },
                onDismiss = { viewModel.closeSettings() }
            )
        }

        // System Widget Picker Sheet
        if (isSystemWidgetPickerOpen) {
            SystemWidgetPickerSheet(
                targetScreenTitle = "Left Widgets Screen",
                onSelectProvider = { providerItem ->
                    viewModel.closeSystemWidgetPicker()
                    val newId = com.example.util.SystemWidgetManager.allocateAppWidgetId(context)
                    if (newId > 0) {
                        val success = com.example.util.SystemWidgetManager.bindWidgetIfAllowed(
                            context,
                            newId,
                            providerItem.providerInfo.provider
                        )
                        if (!success) {
                            val intent = Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
                                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerItem.providerInfo.provider)
                            }
                            (context as? android.app.Activity)?.startActivityForResult(intent, 501)
                        }
                        viewModel.addLeftSystemWidget(providerItem, newId)
                    }
                },
                onDismiss = { viewModel.closeSystemWidgetPicker() }
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
                onDismiss = { viewModel.closeAppPopup() },
                onToggleFavorite = { viewModel.toggleFavorite(app.packageName) },
                onToggleHide = { viewModel.toggleHideApp(app.packageName) },
                onToggleUsageBreaker = { viewModel.toggleUsageBreaker(app.packageName) },
                onDisguiseApp = { newName -> viewModel.repository.disguiseApp(app.packageName, newName) },
                onAssignCategory = { cat -> viewModel.setAppCategory(app.packageName, cat) }
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
