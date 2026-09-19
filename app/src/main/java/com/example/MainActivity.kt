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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()

            LaunchedEffect(settings.hideStatusBar) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
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
                    color = MaterialTheme.colorScheme.background
                ) {
                    NiagaraLauncherScreen(viewModel = viewModel)
                }
            }
        }
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
    val leftWidgets by viewModel.leftWidgets.collectAsState()
    val carouselWidgets by viewModel.carouselWidgets.collectAsState()
    val dockApps by viewModel.dockApps.collectAsState()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshAllData()
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            add(Manifest.permission.READ_CALENDAR)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.CAMERA)
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
    val editingNoteWidget by viewModel.editingNoteWidget.collectAsState()
    val isCarouselCustomizeOpen by viewModel.isCarouselCustomizeOpen.collectAsState()
    val dockPickerSlot by viewModel.dockPickerSlot.collectAsState()

    // 1. FIXED 3-Page Home Screen Layout:
    // Page 0: Left most screen (Widgets CRUD)
    // Page 1: Center screen (Swipeable 4x2 widgets above Favorites)
    // Page 2: Right most screen (All Apps with margins & Wave Alphabet)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(settings.doubleTapToSleep) {
                detectTapGestures(
                    onDoubleTap = {
                        viewModel.triggerDoubleTapSleep()
                    }
                )
            }
            .testTag("niagara_root")
    ) {
        // Flux Signature Dynamic Wallpaper
        when (settings.wallpaperStyle) {
            WallpaperStyle.FLUX_EMBLEM -> {
                FluxWallpaper(
                    showEmblem = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
            WallpaperStyle.FLUX_MINIMAL_GRADIENT -> {
                FluxWallpaper(
                    showEmblem = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
            WallpaperStyle.SYSTEM_SOLID -> {
                // Handled by system surface background
            }
        }

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
                        onAddWidgetClick = { viewModel.openAddWidgetDialog() },
                        onDeleteWidget = { id -> viewModel.deleteLeftWidget(id) },
                        onReorderWidget = { from, to -> viewModel.reorderLeftWidget(from, to) },
                        onEditNoteClick = { widget -> viewModel.openEditNoteWidget(widget) },
                        onToggleMediaPlay = { viewModel.toggleMediaPlayPause() },
                        onMediaNext = { viewModel.skipMediaNext() },
                        onMediaPrev = { viewModel.skipMediaPrev() },
                        onToggleHeadphones = { viewModel.toggleHeadphonesConnected() },
                        onRefreshWeather = { viewModel.showToast("Updated weather for ${weather.location} ⛅") }
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
                        onSwipeRightForPopup = { viewModel.openAppPopup(it) },
                        onReplyNotification = { pkg, text -> viewModel.replyToNotification(pkg, text) },
                        onDismissNotification = { pkg -> viewModel.dismissNotification(pkg) },
                        onToggleMediaPlay = { viewModel.toggleMediaPlayPause() },
                        onMediaNext = { viewModel.skipMediaNext() },
                        onMediaPrev = { viewModel.skipMediaPrev() },
                        onToggleHeadphones = { viewModel.toggleHeadphonesConnected() },
                        onOpenWeather = { viewModel.showToast("Weather for ${weather.location}: ${weather.condition} ${weather.temperatureCelsius}°C") },
                        onOpenCalendar = { viewModel.showToast("Opening calendar...") },
                        onOpenCarouselCustomize = { viewModel.openCarouselCustomize() },
                        onOpenSettings = { viewModel.openSettings() },
                        onNavigateToAllApps = {
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        }
                    )
                }
                2 -> {
                    // 3. Right most screen: Lists all apps with strict margins and alphabet quick jump
                    RightAllAppsScreen(
                        allApps = allApps,
                        folders = folders,
                        isAppsLoading = isAppsLoading,
                        settings = settings,
                        selectedWaveLetter = waveSelectedLetter,
                        onWaveLetterSelected = { viewModel.setWaveSelectedLetter(it) },
                        onAppClick = { viewModel.launchApp(it) },
                        onAppLongClick = { viewModel.openAppPopup(it) },
                        onSwipeRightForPopup = { viewModel.openAppPopup(it) },
                        onReplyNotification = { pkg, text -> viewModel.replyToNotification(pkg, text) },
                        onDismissNotification = { pkg -> viewModel.dismissNotification(pkg) },
                        onToggleFolderExpanded = { folderId -> viewModel.repository.toggleFolderExpanded(folderId) }
                    )
                }
            }
        }

        // Top Screen Indicator (Dots indicating Left / Center / Right page)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 4.dp),
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

        // 5. ALL 3 SCREENS SHOW A FIXED DOCK (MAX 3 ICONS)
        FixedDock(
            dockApps = dockApps,
            iconPackType = settings.iconPackType,
            showLabels = settings.showDockLabels,
            blurBackground = settings.dockBlurBackground,
            onAppClick = { viewModel.launchApp(it) },
            onAppLongClick = { slotIndex -> viewModel.openDockPicker(slotIndex) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Floating Quick Search Action Button
        NiagaraButton(
            isLeftHanded = settings.leftHandedMode,
            onTap = { viewModel.openSearch() },
            onSwipeUp = {
                val camera = allApps.find { it.packageName.contains("camera", true) || it.label.contains("camera", true) }
                if (camera != null) {
                    viewModel.launchApp(camera)
                } else {
                    viewModel.openSettings()
                }
            },
            modifier = Modifier
                .align(if (settings.leftHandedMode) Alignment.BottomStart else Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
        )

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
                onUpdateSettings = { viewModel.repository.updateSettings(it) },
                onExportBackup = { viewModel.repository.exportBackupJson() },
                onImportBackup = { viewModel.repository.importBackupJson(it) },
                onResetDefaults = { viewModel.repository.resetToDefaults() },
                onOpenSetupGuide = { viewModel.openSetupGuide() },
                onDismiss = { viewModel.closeSettings() }
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
            val notif = activeNotifications[app.packageName] ?: app.notification
            AppPopupDrawer(
                app = app,
                iconPackType = settings.iconPackType,
                customIconSettings = settings.customIconSettings,
                notification = notif,
                onReplyNotification = { replyText ->
                    viewModel.replyToNotification(app.packageName, replyText)
                },
                onDismissNotification = {
                    viewModel.dismissNotification(app.packageName)
                },
                onDismiss = { viewModel.closeAppPopup() },
                onToggleFavorite = { viewModel.toggleFavorite(app.packageName) },
                onToggleHide = { viewModel.toggleHideApp(app.packageName) },
                onToggleUsageBreaker = { viewModel.toggleUsageBreaker(app.packageName) },
                onDisguiseApp = { newName -> viewModel.repository.disguiseApp(app.packageName, newName) }
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

        // 2. Swipe down Notification Center & Quick Settings Shade
        if (isNotificationCenterOpen) {
            NotificationCenterSheet(
                notifications = activeNotifications,
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
    }
}
