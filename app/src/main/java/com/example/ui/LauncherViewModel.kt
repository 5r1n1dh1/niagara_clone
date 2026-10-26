package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.UserManager



import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Deprecated("Decomposed into feature ViewModels; use Home/AllApps/Widgets/Settings/Search/Notification ViewModels")
@HiltViewModel
class LauncherViewModel @Inject constructor(
    application: Application,
    internal val repository: LauncherRepository,
    private val settingsRepo: SettingsRepositoryImpl
) : AndroidViewModel(application) {

    val isAppsLoading = repository.isAppsLoading
    val allApps = repository.allApps
    val favorites = repository.favorites
    val folders = repository.folders
    val settings = repository.settings
    val weather = repository.weather
    val calendarEvents = repository.calendarEvents
    val mediaPlayer = repository.mediaPlayer
    val activeNotifications = repository.activeNotifications
    val allActiveNotifications = repository.allActiveNotifications
    val leftWidgets = repository.leftWidgets
    val carouselWidgets = repository.carouselWidgets

    val autoWallpaperBitmap: StateFlow<android.graphics.Bitmap?> = repository.autoWallpaperBitmap
    val isWallpaperSwitching: StateFlow<Boolean> = repository.isWallpaperSwitching

    private val _systemWallpaperBitmap = MutableStateFlow<ImageBitmap?>(null)
    val systemWallpaperBitmap: StateFlow<ImageBitmap?> = _systemWallpaperBitmap.asStateFlow()

    fun refreshAutoWallpaperNow(overrideConfig: AutoWallpaperConfig? = null) {
        repository.refreshAutoWallpaperNow(overrideConfig)
    }

    init {
        loadSystemWallpaper()
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun loadSystemWallpaper() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wpManager = WallpaperManager.getInstance(getApplication())
                val drawable = try { wpManager.drawable ?: wpManager.peekDrawable() } catch (_: Throwable) { null }
                if (drawable != null) {
                    val config = if (drawable.opacity == android.graphics.PixelFormat.OPAQUE) android.graphics.Bitmap.Config.RGB_565 else android.graphics.Bitmap.Config.ARGB_8888
                    val bitmap = drawable.toBitmap(720, 1280, config)
                    _systemWallpaperBitmap.value = bitmap.asImageBitmap()
                }
            } catch (e: Exception) {
                android.util.Log.e("LauncherViewModel", "Failed to load system wallpaper", e)
            }
        }
    }

    val widgetIntegrations = com.example.util.WidgetIntegrationsManager(application)

    private val newsRepository = com.example.data.NewsRepositoryImpl(application)
    val newsState = newsRepository.newsState
    val newsPreferences = newsRepository.preferences

    fun refreshNews(force: Boolean = false) {
        viewModelScope.launch {
            newsRepository.refresh(force)
        }
    }

    fun saveNewsPreferences(state: com.example.data.NewsPreferencesState) {
        newsRepository.savePreferences(state)
    }

    fun clearNewsApiKey() {
        newsRepository.clearApiKey()
    }

    fun launchIntegration(category: com.example.util.IntegrationCategory) {
        widgetIntegrations.launch(category)
    }

    fun setWidgetIntegration(category: com.example.util.IntegrationCategory, packageName: String?) {
        widgetIntegrations.setPackage(category, packageName)
        repository.syncMusicIntegration()
    }

    val userCategories: StateFlow<List<String>> = repository.userCategories

    fun setAppCategory(packageName: String, categoryName: String) {
        repository.setAppCategory(packageName, categoryName)
    }

    fun addUserCategory(categoryName: String) {
        repository.addUserCategory(categoryName)
    }

    fun updateUserCategoryName(oldName: String, newName: String) {
        repository.updateUserCategoryName(oldName, newName)
    }

    fun removeUserCategory(categoryName: String) {
        repository.removeUserCategory(categoryName)
    }

    val dockApps: StateFlow<List<AppItem>> = combine(allApps, repository.dockPackageNames, settings) { apps, pkgs, sett ->
        val maxSlots = sett.dockSlotCount.coerceIn(3, 5)
        val list = mutableListOf<AppItem>()
        for (pkg in pkgs.take(maxSlots)) {
            if (pkg.isBlank()) continue
            val found = apps.find { it.packageName == pkg }
            if (found != null) {
                list.add(found)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _dockPickerSlot = MutableStateFlow<Int?>(null)
    val dockPickerSlot: StateFlow<Int?> = _dockPickerSlot.asStateFlow()

    fun setDockSlot(slotIndex: Int, packageName: String) {
        repository.setDockSlot(slotIndex, packageName)
        _dockPickerSlot.value = null

    }

    fun clearDockSlot(slotIndex: Int) {
        repository.clearDockSlot(slotIndex)

    }

    fun resetDockToDefaults() {
        repository.resetDockToDefaults()

    }

    private val _isReorderFavoritesOpen = MutableStateFlow(false)
    val isReorderFavoritesOpen: StateFlow<Boolean> = _isReorderFavoritesOpen.asStateFlow()

    fun openReorderFavorites() {
        _isReorderFavoritesOpen.value = true
    }

    fun closeReorderFavorites() {
        _isReorderFavoritesOpen.value = false
    }

    fun reorderFavorites(fromIndex: Int, toIndex: Int) {
        if (settings.value.isLayoutLocked) {
            return
        }
        repository.reorderFavorites(fromIndex, toIndex)

    }

    fun moveFavoriteUp(index: Int) {
        if (index > 0) {
            reorderFavorites(index, index - 1)
        }
    }

    fun moveFavoriteDown(index: Int) {
        val size = favorites.value.size
        if (index < size - 1) {
            reorderFavorites(index, index + 1)
        }
    }

    private val _isNewsSetupOpen = MutableStateFlow(false)
    val isNewsSetupOpen: StateFlow<Boolean> = _isNewsSetupOpen.asStateFlow()

    fun openNewsSetup() {
        _isNewsSetupOpen.value = true

    }

    fun closeNewsSetup() {
        _isNewsSetupOpen.value = false
    }

    private val _isAddWidgetOpen = MutableStateFlow(false)
    val isAddWidgetOpen: StateFlow<Boolean> = _isAddWidgetOpen.asStateFlow()

    private val _editingNoteWidget = MutableStateFlow<CustomWidgetConfig?>(null)
    val editingNoteWidget: StateFlow<CustomWidgetConfig?> = _editingNoteWidget.asStateFlow()

    private val _isCarouselCustomizeOpen = MutableStateFlow(false)
    val isCarouselCustomizeOpen: StateFlow<Boolean> = _isCarouselCustomizeOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    private val _isDiscoverOpen = MutableStateFlow(false)
    val isDiscoverOpen: StateFlow<Boolean> = _isDiscoverOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isOnboardingForcedOpen = MutableStateFlow(false)
    val isOnboardingOpen: StateFlow<Boolean> = combine(
        settings,
        _isOnboardingForcedOpen
    ) { sett, forced ->
        forced || !sett.hasCompletedOnboarding
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isNotificationCenterOpen = MutableStateFlow(false)
    val isNotificationCenterOpen: StateFlow<Boolean> = _isNotificationCenterOpen.asStateFlow()

    private val _isSecretLabOpen = MutableStateFlow(false)
    val isSecretLabOpen: StateFlow<Boolean> = _isSecretLabOpen.asStateFlow()

    private val _isDebugOverlayOpen = MutableStateFlow(false)
    val isDebugOverlayOpen: StateFlow<Boolean> = _isDebugOverlayOpen.asStateFlow()

    private val _isSleepOverlayVisible = MutableStateFlow(false)
    val isSleepOverlayVisible: StateFlow<Boolean> = _isSleepOverlayVisible.asStateFlow()

    private val _activeAppForPopup = MutableStateFlow<AppItem?>(null)
    val activeAppForPopup: StateFlow<AppItem?> = _activeAppForPopup.asStateFlow()

    private val _activeUsageBreakerApp = MutableStateFlow<AppItem?>(null)
    val activeUsageBreakerApp: StateFlow<AppItem?> = _activeUsageBreakerApp.asStateFlow()

    private val _activeFolderPopup = MutableStateFlow<FolderItem?>(null)
    val activeFolderPopup: StateFlow<FolderItem?> = _activeFolderPopup.asStateFlow()

    private val _waveSelectedLetter = MutableStateFlow<Char?>(null)
    val waveSelectedLetter: StateFlow<Char?> = _waveSelectedLetter.asStateFlow()

    val searchFilteredApps: StateFlow<List<AppItem>> = combine(allApps, _searchQuery) { apps, query ->
        if (query.isBlank() || query.startsWith("/")) {
            apps.filter { !it.isHidden }
        } else {
            val q = query.trim().lowercase()
            apps.filter { !it.isHidden && (it.label.lowercase().contains(q) || it.customCategory.lowercase().contains(q)) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query

        // Check for Niagara Secret Executables
        when (query.trim().lowercase()) {
            "/shrug" -> {
                _searchQuery.value = "¯\\_(ツ)_/¯"
        
            }
            "/reset" -> {
                repository.resetToDefaults()
                _searchQuery.value = ""
                _isSearchOpen.value = false
        
            }
            "/debug" -> {
                _isDebugOverlayOpen.value = true
                _searchQuery.value = ""
                _isSearchOpen.value = false
        
            }
            "/secret" -> {
                _isSecretLabOpen.value = true
                _searchQuery.value = ""
                _isSearchOpen.value = false
        
            }
        }
    }

    fun openSearch() {
        _searchQuery.value = ""
        _isSearchOpen.value = true

    }

    fun closeSearch() {
        _isSearchOpen.value = false
        _searchQuery.value = ""
    }

    private val _homeGestureEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homeGestureEvent: SharedFlow<Unit> = _homeGestureEvent.asSharedFlow()

    fun onHomeGestureTriggered() {
        closeAllPanelsAndPopups()
        _homeGestureEvent.tryEmit(Unit)
    }

    fun closeAllPanelsAndPopups() {
        _isSearchOpen.value = false
        _isSettingsOpen.value = false
        _isNotificationCenterOpen.value = false
        _isDiscoverOpen.value = false
        _isNewsSetupOpen.value = false
        _isAddWidgetOpen.value = false
        _editingNoteWidget.value = null
        _isCarouselCustomizeOpen.value = false
        _dockPickerSlot.value = null
        _isSecretLabOpen.value = false
        _isDebugOverlayOpen.value = false
        _isSleepOverlayVisible.value = false
        _activeAppForPopup.value = null
        _activeUsageBreakerApp.value = null
        _activeFolderPopup.value = null
    }

    fun openDiscover() {
        _isDiscoverOpen.value = true

    }

    fun closeDiscover() {
        _isDiscoverOpen.value = false
    }

    fun openSettings() {
        _isSettingsOpen.value = true

    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun openNotificationCenter() {

        val opened = triggerSystemNotificationShade()
        if (!opened) {
            _isNotificationCenterOpen.value = true
        }
    }

    fun triggerSystemNotificationShade(): Boolean {
        val context = getApplication<Application>()
        return try {
            @SuppressLint("WrongConstant")
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
            val method = statusBarManagerClass.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
            true
        } catch (_: Throwable) {
            try {
                @SuppressLint("WrongConstant")
                val statusBarService = context.getSystemService("statusbar")
                val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
                val method = statusBarManagerClass.getMethod("expand")
                method.invoke(statusBarService)
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    fun closeNotificationCenter() {
        _isNotificationCenterOpen.value = false
    }

    fun clearAllNotifications() {
        repository.clearAllNotifications()

    }

    fun closeSecretLab() {
        _isSecretLabOpen.value = false
    }

    fun closeDebugOverlay() {
        _isDebugOverlayOpen.value = false
    }

    fun setWaveSelectedLetter(letter: Char?) {
        if (_waveSelectedLetter.value != letter && letter != null) {
    
        }
        _waveSelectedLetter.value = letter
    }

    fun openAppPopup(app: AppItem) {
        _activeAppForPopup.value = app

    }

    fun closeAppPopup() {
        _activeAppForPopup.value = null
    }

    fun openFolderPopup(folder: FolderItem) {
        _activeFolderPopup.value = folder

    }

    fun closeFolderPopup() {
        _activeFolderPopup.value = null
    }

    fun launchApp(app: AppItem) {
        if (app.isUsageBreakerActive) {
            // Trigger Mindful Pause
            _activeUsageBreakerApp.value = app
    
            return
        }
        executeAppLaunch(app)
    }

    fun confirmUsageBreakerAndLaunch(app: AppItem) {
        _activeUsageBreakerApp.value = null
        executeAppLaunch(app)
    }

    fun cancelUsageBreaker() {
        _activeUsageBreakerApp.value = null
    }

    fun refreshAllData() {
        repository.refreshCalendarEvents()
        repository.reloadApps()
    }

    fun launchAppByPackage(packageName: String, isDualApp: Boolean = false) {
        val targetApp = allApps.value.firstOrNull { it.packageName == packageName && it.isDualApp == isDualApp }
            ?: allApps.value.firstOrNull { it.packageName == packageName }
        if (targetApp != null) {
            launchApp(targetApp)
        } else {
            val context = getApplication<Application>()
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    context.startActivity(intent)
                }
            } catch (_: Throwable) {}
        }
    }

    private fun executeAppLaunch(app: AppItem) {
        val context = getApplication<Application>()
        try {
            // Check if this is a dual app or custom profile managed via LauncherApps
            if (app.isDualApp && app.userProfileId != 0 && app.activityName.isNotBlank()) {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
                val targetProfile = userManager?.userProfiles?.firstOrNull { it.hashCode() == app.userProfileId }
                if (launcherApps != null && targetProfile != null) {
                    launcherApps.startMainActivity(
                        ComponentName(app.packageName, app.activityName),
                        targetProfile,
                        null,
                        null
                    )
                    return
                }
            }

            val pm = context.packageManager
            var intent = pm.getLaunchIntentForPackage(app.packageName)
            if (intent == null && app.activityName.isNotBlank()) {
                intent = Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(app.packageName, app.activityName)
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
            }
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(intent)
            } else {
            }
        } catch (e: Exception) {
        }
    }

    fun triggerDoubleTapSleep() {
        if (settings.value.doubleTapToSleep) {
    
            val locked = com.example.service.FluxAccessibilityService.requestLockScreen(getApplication())
            if (!locked) {
                // If accessibility service is not yet enabled, show visual sleep screen and toast
                _isSleepOverlayVisible.value = true
            }
        }
    }

    fun openAccessibilitySettings() {
        com.example.service.FluxAccessibilityService.openAccessibilitySettings(getApplication())
    }

    fun dismissSleepOverlay() {
        _isSleepOverlayVisible.value = false
    }

    fun toggleFavorite(packageName: String) {
        val wasFav = favorites.value.any { it.packageName == packageName }
        repository.toggleFavorite(packageName)
        val app = allApps.value.find { it.packageName == packageName }
        val name = app?.disguisedName ?: app?.label ?: "App"

        _activeAppForPopup.value?.let { current ->
            if (current.packageName == packageName) {
                _activeAppForPopup.value = current.copy(isFavorite = !wasFav)
            }
        }
    }

    fun toggleHideApp(packageName: String) {
        repository.toggleHideApp(packageName)

    }

    fun toggleUsageBreaker(packageName: String) {
        repository.toggleUsageBreaker(packageName)

    }

    fun replyToNotification(packageName: String, text: String, key: String? = null, isDualApp: Boolean = false) {
        repository.replyToNotification(packageName, text, key, isDualApp)

    }

    fun dismissNotification(packageName: String, key: String? = null, isDualApp: Boolean = false) {
        repository.dismissNotification(packageName, key, isDualApp)
    }

    fun simulateNotification(packageName: String, sender: String, message: String) {
        repository.simulateIncomingNotification(packageName, sender, message)
    }

    fun toggleMediaPlayPause() {
        repository.togglePlayPauseMedia()

    }

    fun skipMediaNext() {
        repository.skipNextMedia()

    }

    fun skipMediaPrev() {
        repository.skipPreviousMedia()

    }

    fun seekMedia(percent: Float) {
        repository.seekMedia(percent)

    }

    fun toggleShuffleMedia() {
        repository.toggleShuffleMedia()

    }

    fun toggleHeadphonesConnected() {
        val next = !settings.value.isHeadphonesConnected
        repository.setHeadphonesConnected(next)
        val msg = if (next) "Headphones connected! Audio apps surfaced." else "Headphones disconnected."
    }

    // Dock slot picker
    fun openDockPicker(slotIndex: Int) {
        _dockPickerSlot.value = slotIndex

    }

    fun closeDockPicker() {
        _dockPickerSlot.value = null
    }

    fun selectDockApp(slotIndex: Int, app: AppItem) {
        repository.setDockSlot(slotIndex, app.packageName)
        _dockPickerSlot.value = null

    }

    // Left screen widget CRUD
    fun toggleLockLayout() {
        val current = settings.value.isLayoutLocked
        val newLocked = !current
        repository.updateSettings(settings.value.copy(isLayoutLocked = newLocked))

    }

    fun setLayoutLocked(locked: Boolean) {
        repository.updateSettings(settings.value.copy(isLayoutLocked = locked))

    }

    fun openAddWidgetDialog() {
        if (settings.value.isLayoutLocked) {
    
            return
        }
        _isAddWidgetOpen.value = true

    }

    fun closeAddWidgetDialog() {
        _isAddWidgetOpen.value = false
    }

    fun addLeftWidget(type: WidgetType) {
        if (settings.value.isLayoutLocked) {
            return
        }
        repository.addLeftWidget(type)
        _isAddWidgetOpen.value = false

    }

    fun addLeftSystemWidget(appWidgetId: Int, label: String = "System Widget", heightCategory: String = "4x2") {
        if (settings.value.isLayoutLocked) {
            return
        }
        repository.addLeftSystemWidget(appWidgetId, label, heightCategory)
        _isAddWidgetOpen.value = false

    }

    fun deleteLeftWidget(id: String) {
        if (settings.value.isLayoutLocked) {
            return
        }
        repository.deleteLeftWidget(id)

    }

    fun updateLeftWidget(id: String, title: String, content: String) {
        repository.updateLeftWidget(id, title, content)

    }

    fun updateLeftWidgetSize(id: String, newSize: String) {
        if (settings.value.isLayoutLocked) {
            return
        }
        repository.updateLeftWidget(id, newHeightCategory = newSize)

    }

    fun reorderLeftWidget(fromIndex: Int, toIndex: Int) {
        if (settings.value.isLayoutLocked) {
            return
        }
        repository.reorderLeftWidgets(fromIndex, toIndex)

    }

    fun openEditNoteWidget(widget: CustomWidgetConfig) {
        _editingNoteWidget.value = widget

    }

    fun closeEditNoteWidget() {
        _editingNoteWidget.value = null
    }

    // Center carousel widgets
    fun openCarouselCustomize() {
        if (settings.value.isLayoutLocked) {
    
            return
        }
        _isCarouselCustomizeOpen.value = true

    }

    fun closeCarouselCustomize() {
        _isCarouselCustomizeOpen.value = false
    }

    fun addCarouselWidget(type: WidgetType) {
        if (settings.value.isLayoutLocked) {
            return
        }
        if (carouselWidgets.value.size >= 5) {
            return
        }
        repository.addCarouselWidget(type)

    }

    fun removeCarouselWidget(type: WidgetType) {
        if (settings.value.isLayoutLocked) {
            return
        }
        if (carouselWidgets.value.size <= 1) {
            return
        }
        repository.removeCarouselWidget(type)

    }

    fun reorderCarouselWidget(fromIndex: Int, toIndex: Int) {
        if (settings.value.isLayoutLocked) {
            return
        }
        repository.reorderCarouselWidgets(fromIndex, toIndex)

    }

    fun openSetupGuide() {
        _isOnboardingForcedOpen.value = true

    }

    fun closeSetupGuide() {
        _isOnboardingForcedOpen.value = false
        repository.completeOnboarding()

    }

    fun showToast(message: String) {
        try {
            android.widget.Toast.makeText(getApplication(), message, android.widget.Toast.LENGTH_SHORT).show()
        } catch (_: Throwable) {}
    }

    fun completeSetupGuide(
        selectedFavorites: List<String>,
        selectedCarouselWidgets: List<WidgetType>,
        selectedLeftWidgets: List<CustomWidgetConfig>,
        themePreset: LauncherThemePreset,
        leftHanded: Boolean,
        doubleTapToSleep: Boolean
    ) {
        if (selectedFavorites.isNotEmpty()) {
            repository.setFavoritePackages(selectedFavorites)
        }
        if (selectedCarouselWidgets.isNotEmpty()) {
            repository.setCarouselWidgets(selectedCarouselWidgets)
        }
        if (selectedLeftWidgets.isNotEmpty()) {
            repository.setLeftWidgetsList(selectedLeftWidgets)
        }
        repository.updateSettings(
            repository.settings.value.copy(
                themePreset = themePreset,
                leftHandedMode = leftHanded,
                doubleTapToSleep = doubleTapToSleep,
                hasCompletedOnboarding = true
            )
        )
        _isOnboardingForcedOpen.value = false
    }
}


