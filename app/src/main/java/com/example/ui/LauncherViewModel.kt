package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
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

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val repository = LauncherRepository(application)

    val isAppsLoading = repository.isAppsLoading
    val availableSystemWidgets = repository.availableSystemWidgets
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

    private val _systemWallpaperBitmap = MutableStateFlow<ImageBitmap?>(null)
    val systemWallpaperBitmap: StateFlow<ImageBitmap?> = _systemWallpaperBitmap.asStateFlow()

    init {
        loadSystemWallpaper()
    }

    fun loadSystemWallpaper() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wpManager = WallpaperManager.getInstance(getApplication())
                val drawable = wpManager.drawable
                if (drawable != null) {
                    val bitmap = drawable.toBitmap(720, 1280, android.graphics.Bitmap.Config.ARGB_8888)
                    _systemWallpaperBitmap.value = bitmap.asImageBitmap()
                }
            } catch (_: Exception) {
            }
        }
    }

    val widgetIntegrations = com.example.util.WidgetIntegrationsManager(application)

    private val _isSystemWidgetPickerOpen = MutableStateFlow(false)
    val isSystemWidgetPickerOpen: StateFlow<Boolean> = _isSystemWidgetPickerOpen.asStateFlow()

    fun openSystemWidgetPicker() {
        _isSystemWidgetPickerOpen.value = true
    }

    fun closeSystemWidgetPicker() {
        _isSystemWidgetPickerOpen.value = false
    }

    fun addLeftSystemWidget(providerItem: com.example.util.SystemWidgetProviderItem, appWidgetId: Int) {
        repository.addLeftSystemWidget(providerItem, appWidgetId)
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
        for (pkg in pkgs) {
            val found = apps.find { it.packageName == pkg }
            if (found != null) {
                list.add(found)
            }
        }
        if (list.size < maxSlots) {
            val phoneApp = apps.find { it.packageName.contains("dialer", true) || it.packageName.contains("phone", true) || it.label.contains("Phone", true) }
            val msgApp = apps.find { it.packageName.contains("messaging", true) || it.packageName.contains("mms", true) || it.label.contains("Message", true) }
            val camApp = apps.find { it.packageName.contains("camera", true) || it.label.contains("Camera", true) }
            
            val defaults = listOfNotNull(phoneApp, msgApp, camApp)
            for (defApp in defaults) {
                if (list.size >= maxSlots) break
                if (list.none { it.packageName == defApp.packageName }) {
                    list.add(defApp)
                }
            }
            for (app in apps) {
                if (list.size >= maxSlots) break
                if (list.none { it.packageName == app.packageName }) {
                    list.add(app)
                }
            }
        }
        list.take(maxSlots)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _dockPickerSlot = MutableStateFlow<Int?>(null)
    val dockPickerSlot: StateFlow<Int?> = _dockPickerSlot.asStateFlow()

    fun setDockSlot(slotIndex: Int, packageName: String) {
        repository.setDockSlot(slotIndex, packageName)
        _dockPickerSlot.value = null
        showToast("Dock updated")
        vibrateClick()
    }

    fun clearDockSlot(slotIndex: Int) {
        repository.clearDockSlot(slotIndex)
        vibrateClick()
    }

    fun resetDockToDefaults() {
        repository.resetDockToDefaults()
        showToast("Dock reset to default apps")
        vibrateClick()
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
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

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

    val mathResult: StateFlow<String?> = _searchQuery.map { query ->
        evaluateSimpleMath(query.trim())
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val searchFilteredApps: StateFlow<List<AppItem>> = combine(allApps, _searchQuery) { apps, query ->
        if (query.isBlank() || query.startsWith("/")) {
            apps.filter { !it.isHidden }
        } else {
            val q = query.trim().lowercase()
            apps.filter { !it.isHidden && (it.label.lowercase().contains(q) || it.customCategory.lowercase().contains(q)) }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query

        // Check for Niagara Secret Executables
        when (query.trim().lowercase()) {
            "/shrug" -> {
                _searchQuery.value = "¯\\_(ツ)_/¯"
                showToast("Secret executed: Shrug appended")
                vibrateClick()
            }
            "/reset" -> {
                repository.resetToDefaults()
                _searchQuery.value = ""
                _isSearchOpen.value = false
                showToast("Launcher flags reset to factory defaults")
                vibrateClick()
            }
            "/debug" -> {
                _isDebugOverlayOpen.value = true
                _searchQuery.value = ""
                _isSearchOpen.value = false
                showToast("Debug metrics enabled")
                vibrateClick()
            }
            "/secret" -> {
                _isSecretLabOpen.value = true
                _searchQuery.value = ""
                _isSearchOpen.value = false
                showToast("Niagara Developer Laboratory unlocked")
                vibrateClick()
            }
        }
    }

    fun openSearch() {
        _searchQuery.value = ""
        _isSearchOpen.value = true
        vibrateClick()
    }

    fun closeSearch() {
        _isSearchOpen.value = false
        _searchQuery.value = ""
    }

    fun openDiscover() {
        _isDiscoverOpen.value = true
        vibrateClick()
    }

    fun closeDiscover() {
        _isDiscoverOpen.value = false
    }

    fun openSettings() {
        _isSettingsOpen.value = true
        vibrateClick()
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun openNotificationCenter() {
        vibrateClick()
        triggerSystemNotificationShade()
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
        val packages = activeNotifications.value.keys.toList()
        for (pkg in packages) {
            repository.dismissNotification(pkg)
        }
        showToast("Notifications cleared")
        vibrateClick()
    }

    fun closeSecretLab() {
        _isSecretLabOpen.value = false
    }

    fun closeDebugOverlay() {
        _isDebugOverlayOpen.value = false
    }

    fun setWaveSelectedLetter(letter: Char?) {
        if (_waveSelectedLetter.value != letter && letter != null) {
            vibrateTick()
        }
        _waveSelectedLetter.value = letter
    }

    fun openAppPopup(app: AppItem) {
        _activeAppForPopup.value = app
        vibrateClick()
    }

    fun closeAppPopup() {
        _activeAppForPopup.value = null
    }

    fun openFolderPopup(folder: FolderItem) {
        _activeFolderPopup.value = folder
        vibrateClick()
    }

    fun closeFolderPopup() {
        _activeFolderPopup.value = null
    }

    fun launchApp(app: AppItem) {
        if (app.isUsageBreakerActive) {
            // Trigger Mindful Pause
            _activeUsageBreakerApp.value = app
            vibrateClick()
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
        repository.loadAvailableSystemWidgets()
        repository.reloadApps()
    }

    private fun executeAppLaunch(app: AppItem) {
        val context = getApplication<Application>()
        try {
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
                showToast("Could not launch ${app.label}")
            }
        } catch (e: Exception) {
            showToast("Failed to launch ${app.label}")
        }
    }

    fun triggerDoubleTapSleep() {
        if (settings.value.doubleTapToSleep) {
            vibrateTick()
            val locked = com.example.service.FluxAccessibilityService.requestLockScreen(getApplication())
            if (!locked) {
                // If accessibility service is not yet enabled, show visual sleep screen and toast
                _isSleepOverlayVisible.value = true
                showToast("Tip: Enable Accessibility for Flux to turn off the physical screen")
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
        showToast(if (wasFav) "Removed $name from favorites" else "Added $name to favorites ⭐")
        vibrateClick()
        _activeAppForPopup.value?.let { current ->
            if (current.packageName == packageName) {
                _activeAppForPopup.value = current.copy(isFavorite = !wasFav)
            }
        }
    }

    fun toggleHideApp(packageName: String) {
        repository.toggleHideApp(packageName)
        vibrateClick()
    }

    fun toggleUsageBreaker(packageName: String) {
        repository.toggleUsageBreaker(packageName)
        vibrateClick()
    }

    fun replyToNotification(packageName: String, text: String, key: String? = null, isDualApp: Boolean = false) {
        repository.replyToNotification(packageName, text, key, isDualApp)
        showToast("Reply sent: \"$text\"")
        vibrateClick()
    }

    fun dismissNotification(packageName: String, key: String? = null, isDualApp: Boolean = false) {
        repository.dismissNotification(packageName, key, isDualApp)
        showToast("Notification dismissed")
    }

    fun simulateNotification(packageName: String, sender: String, message: String) {
        repository.simulateIncomingNotification(packageName, sender, message)
        showToast("Incoming notification simulated")
    }

    fun toggleMediaPlayPause() {
        repository.togglePlayPauseMedia()
        vibrateTick()
    }

    fun skipMediaNext() {
        repository.skipNextMedia()
        vibrateTick()
    }

    fun skipMediaPrev() {
        repository.skipPreviousMedia()
        vibrateTick()
    }

    fun seekMedia(percent: Float) {
        repository.seekMedia(percent)
        vibrateTick()
    }

    fun toggleHeadphonesConnected() {
        val next = !settings.value.isHeadphonesConnected
        repository.setHeadphonesConnected(next)
        val msg = if (next) "Headphones connected! Audio apps surfaced." else "Headphones disconnected."
        showToast(msg)
        vibrateClick()
    }

    fun showToast(msg: String) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    // Dock slot picker
    fun openDockPicker(slotIndex: Int) {
        _dockPickerSlot.value = slotIndex
        vibrateClick()
    }

    fun closeDockPicker() {
        _dockPickerSlot.value = null
    }

    fun selectDockApp(slotIndex: Int, app: AppItem) {
        repository.setDockSlot(slotIndex, app.packageName)
        _dockPickerSlot.value = null
        showToast("Updated dock slot with ${app.label}")
        vibrateClick()
    }

    // Left screen widget CRUD
    fun openAddWidgetDialog() {
        _isAddWidgetOpen.value = true
        vibrateClick()
    }

    fun closeAddWidgetDialog() {
        _isAddWidgetOpen.value = false
    }

    fun addLeftWidget(type: WidgetType) {
        repository.addLeftWidget(type)
        _isAddWidgetOpen.value = false
        showToast("Added ${type.displayName} widget")
        vibrateClick()
    }

    fun deleteLeftWidget(id: String) {
        repository.deleteLeftWidget(id)
        showToast("Widget removed")
        vibrateClick()
    }

    fun updateLeftWidget(id: String, title: String, content: String) {
        repository.updateLeftWidget(id, title, content)
        showToast("Widget updated")
        vibrateClick()
    }

    fun updateLeftWidgetSize(id: String, newSize: String) {
        repository.updateLeftWidget(id, newHeightCategory = newSize)
        vibrateClick()
    }

    fun reorderLeftWidget(fromIndex: Int, toIndex: Int) {
        repository.reorderLeftWidgets(fromIndex, toIndex)
        vibrateTick()
    }

    fun openEditNoteWidget(widget: CustomWidgetConfig) {
        _editingNoteWidget.value = widget
        vibrateClick()
    }

    fun closeEditNoteWidget() {
        _editingNoteWidget.value = null
    }

    // Center carousel widgets
    fun openCarouselCustomize() {
        _isCarouselCustomizeOpen.value = true
        vibrateClick()
    }

    fun closeCarouselCustomize() {
        _isCarouselCustomizeOpen.value = false
    }

    fun addCarouselWidget(type: WidgetType) {
        if (carouselWidgets.value.size >= 5) {
            showToast("Maximum 5 widgets in carousel")
            return
        }
        repository.addCarouselWidget(type)
        showToast("Added to top carousel")
        vibrateClick()
    }

    fun removeCarouselWidget(type: WidgetType) {
        if (carouselWidgets.value.size <= 1) {
            showToast("Carousel needs at least 1 widget")
            return
        }
        repository.removeCarouselWidget(type)
        showToast("Removed from carousel")
        vibrateClick()
    }

    fun reorderCarouselWidget(fromIndex: Int, toIndex: Int) {
        repository.reorderCarouselWidgets(fromIndex, toIndex)
        vibrateTick()
    }

    fun openSetupGuide() {
        _isOnboardingForcedOpen.value = true
        vibrateClick()
    }

    fun closeSetupGuide() {
        _isOnboardingForcedOpen.value = false
        repository.completeOnboarding()
        vibrateClick()
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
        showToast("Welcome to Flux Launcher! Setup complete ✨")
        vibrateClick()
    }

    private fun vibrateClick() {
        if (!settings.value.hapticFeedback) return
        val vibrator = ContextCompat.getSystemService(getApplication(), Vibrator::class.java) ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    private fun vibrateTick() {
        if (!settings.value.hapticFeedback) return
        val vibrator = ContextCompat.getSystemService(getApplication(), Vibrator::class.java) ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    }

    private fun evaluateSimpleMath(input: String): String? = MathEvaluator.evaluate(input)
}

object MathEvaluator {
    fun evaluate(input: String): String? {
        if (input.length < 2) return null
        try {
            if (input.contains("% of", ignoreCase = true)) {
                val parts = input.split("% of", "% OF", ignoreCase = true)
                if (parts.size == 2) {
                    val p = parts[0].trim().toDoubleOrNull()
                    val total = parts[1].trim().toDoubleOrNull()
                    if (p != null && total != null) {
                        val res = (p / 100.0) * total
                        return formatResult(res)
                    }
                }
            }

            if (input.startsWith("sqrt(", ignoreCase = true) && input.endsWith(")")) {
                val inside = input.substring(5, input.length - 1).trim().toDoubleOrNull()
                if (inside != null && inside >= 0) {
                    return formatResult(kotlin.math.sqrt(inside))
                }
            }

            val sanitized = input.replace("x", "*", ignoreCase = true).replace(" ", "")
            val ops = listOf('+', '-', '*', '/')
            var foundOp: Char? = null
            for (ch in sanitized) {
                if (ch in ops) {
                    foundOp = ch
                    break
                }
            }
            if (foundOp != null) {
                val idx = sanitized.indexOf(foundOp)
                if (idx > 0 && idx < sanitized.length - 1) {
                    val left = sanitized.substring(0, idx).toDoubleOrNull()
                    val right = sanitized.substring(idx + 1).toDoubleOrNull()
                    if (left != null && right != null) {
                        val res = when (foundOp) {
                            '+' -> left + right
                            '-' -> left - right
                            '*' -> left * right
                            '/' -> if (right != 0.0) left / right else null
                            else -> null
                        }
                        if (res != null) return formatResult(res)
                    }
                }
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    private fun formatResult(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", value)
        }
    }
}
