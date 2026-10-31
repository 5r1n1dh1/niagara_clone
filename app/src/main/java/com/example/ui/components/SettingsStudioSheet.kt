@file:Suppress("DEPRECATION")
package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassStyle
import com.example.data.*
import com.example.service.FluxAccessibilityService
import com.example.service.FluxNotificationListenerService
import com.example.util.DefaultLauncherHelper
import com.example.util.IntegrationCategory
import com.example.util.WidgetIntegrationsManager

/**
 * Settings Navigation Pages
 */
enum class SettingsPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    APPEARANCE(
        title = "Appearance & Theming",
        subtitle = "Theme presets, frosted glass blur, icon packs & badges",
        icon = Icons.Filled.Palette
    ),
    CATEGORIES(
        title = "App Categories & Drawer",
        subtitle = "Organize apps, create custom tags & smart folders",
        icon = Icons.Filled.FolderCopy
    ),
    DOCK(
        title = "Dock & Quick Launch",
        subtitle = "Pinned bottom dock slots, blur and scaling",
        icon = Icons.Filled.Dock
    ),
    GESTURES(
        title = "Gestures & Controls",
        subtitle = "Double-tap to sleep, notification shade & haptic feedback",
        icon = Icons.Filled.TouchApp
    ),
    CLOCK_WIDGETS(
        title = "Clock & Widgets",
        subtitle = "Clock styles, accent tint, ticking seconds & layout lock",
        icon = Icons.Filled.Schedule
    ),
    INTEGRATIONS(
        title = "Widget App Integrations",
        subtitle = "Assign default system/installed app for each Flux widget",
        icon = Icons.Filled.Extension
    ),
    SYSTEM(
        title = "System, Permissions & Backup",
        subtitle = "Default launcher, accessibility, restricted settings & restore",
        icon = Icons.Filled.SettingsSuggest
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsStudioSheet(
    settings: LauncherSettings,
    allApps: List<AppItem> = emptyList(),
    dockApps: List<AppItem> = emptyList(),
    carouselWidgets: List<WidgetType> = emptyList(),
    availableWidgets: List<WidgetType> = emptyList(),
    widgetIntegrations: WidgetIntegrationsManager? = null,
    isWallpaperSwitching: Boolean = false,
    onRefreshAutoWallpaperNow: (() -> Unit)? = null,
    onOpenReorderFavorites: (() -> Unit)? = null,
    onUpdateSettings: (LauncherSettings) -> Unit,
    onSelectDockSlot: ((Int) -> Unit)? = null,
    onClearDockSlot: ((Int) -> Unit)? = null,
    onResetDockDefaults: (() -> Unit)? = null,
    onAddCarouselWidget: ((WidgetType) -> Unit)? = null,
    onRemoveCarouselWidget: ((WidgetType) -> Unit)? = null,
    onReorderCarouselWidget: ((Int, Int) -> Unit)? = null,
    onOpenAddSystemWidget: (() -> Unit)? = null,
    onExportBackup: () -> String,
    onImportBackup: (String) -> Boolean,
    onResetDefaults: () -> Unit,
    onOpenSetupGuide: () -> Unit = {},
    userCategories: List<String> = emptyList(),
    onAddCategory: (String) -> Unit = {},
    onRenameCategory: (String, String) -> Unit = { _, _ -> },
    onRemoveCategory: (String) -> Unit = {},
    onAssignCategory: (String, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var currentPage by remember { mutableStateOf<SettingsPage?>(null) }

    // Intercept hardware/gesture back press when deep in a page
    BackHandler(enabled = currentPage != null) {
        currentPage = null
    }

    // Dialog states
    var backupDialogText by remember { mutableStateOf<String?>(null) }
    var importDialogText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var newCategoryInputText by remember { mutableStateOf("") }
    var selectedCategoryForAppAssignment by remember { mutableStateOf<String?>(null) }
    var showRestrictedSettingsGuideDialog by remember { mutableStateOf(false) }

    val isDefaultLauncher = remember(context) { DefaultLauncherHelper.isDefaultLauncher(context) }
    val isNotificationAccessGranted = remember(context) { FluxNotificationListenerService.isNotificationAccessGranted(context) }
    val isAccessibilityGranted = remember(context) { FluxAccessibilityService.isServiceRunning() }

    val glass = remember(settings.frostedTranslucency, settings.frostedBlurRadius, settings.frostedGlassEffect, settings.themePreset) {
        GlassStyle.create(
            translucency = settings.frostedTranslucency,
            blurDp = settings.frostedBlurRadius,
            specular = settings.frostedGlassEffect,
            preset = settings.themePreset
        )
    }

    FrostedGlassBackground(
        glass = glass,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("settings_studio_sheet")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Single Unified Header Bar (No double header)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (currentPage != null) {
                                currentPage = null
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (currentPage != null) "Back to Settings" else "Back to Home",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentPage?.title ?: "Flux Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentPage?.subtitle ?: "Clean, focused Android experience",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("settings_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Animated Page Content
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "settings_page_transition",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            ) { page ->
                when (page) {
                    null -> {
                        // ROOT SETTINGS MENU
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // System Health Status Banner
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDefaultLauncher && isAccessibilityGranted && isNotificationAccessGranted)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isDefaultLauncher) Icons.Filled.Verified else Icons.Filled.Info,
                                                    contentDescription = null,
                                                    tint = if (isDefaultLauncher) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isDefaultLauncher) "Flux is Active Default Home" else "Default Launcher Not Set",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            if (!isDefaultLauncher) {
                                                FilledTonalButton(
                                                    onClick = { DefaultLauncherHelper.openSetDefaultLauncher(context) },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Set Default", fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        if (!isAccessibilityGranted || !isNotificationAccessGranted) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
                                                    .clickable { showRestrictedSettingsGuideDialog = true }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Filled.Security,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Restricted setting blocked? Tap for unlock guide →",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "PREFERENCES & CUSTOMIZATION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                                )
                            }

                            // Category Menu Cards
                            items(SettingsPage.entries.toTypedArray()) { p ->
                                SettingsMenuCard(
                                    page = p,
                                    onClick = { currentPage = p }
                                )
                            }
                        }
                    }

                    SettingsPage.APPEARANCE -> {
                        AppearanceSettingsPage(
                            settings = settings,
                            isWallpaperSwitching = isWallpaperSwitching,
                            onRefreshAutoWallpaperNow = onRefreshAutoWallpaperNow,
                            onOpenReorderFavorites = onOpenReorderFavorites,
                            onUpdateSettings = onUpdateSettings
                        )
                    }

                    SettingsPage.CATEGORIES -> {
                        CategoriesSettingsPage(
                            allApps = allApps,
                            userCategories = userCategories,
                            newCategoryInputText = newCategoryInputText,
                            onNewCategoryInputChange = { newCategoryInputText = it },
                            onAddCategory = onAddCategory,
                            onRenameCategory = onRenameCategory,
                            onRemoveCategory = onRemoveCategory,
                            onAssignCategory = onAssignCategory,
                            selectedCategoryForAppAssignment = selectedCategoryForAppAssignment,
                            onSelectCategoryForAssignment = { selectedCategoryForAppAssignment = it },
                            iconPackType = settings.iconPackType
                        )
                    }

                    SettingsPage.DOCK -> {
                        DockSettingsPage(
                            settings = settings,
                            dockApps = dockApps,
                            onUpdateSettings = onUpdateSettings,
                            onSelectDockSlot = onSelectDockSlot,
                            onClearDockSlot = onClearDockSlot,
                            onResetDockDefaults = onResetDockDefaults
                        )
                    }

                    SettingsPage.GESTURES -> {
                        GesturesSettingsPage(
                            settings = settings,
                            isAccessibilityGranted = isAccessibilityGranted,
                            onUpdateSettings = onUpdateSettings,
                            onOpenRestrictedGuide = { showRestrictedSettingsGuideDialog = true }
                        )
                    }

                    SettingsPage.CLOCK_WIDGETS -> {
                        ClockWidgetsSettingsPage(
                            settings = settings,
                            carouselWidgets = carouselWidgets,
                            availableWidgets = availableWidgets,
                            onUpdateSettings = onUpdateSettings,
                            onAddCarouselWidget = onAddCarouselWidget,
                            onRemoveCarouselWidget = onRemoveCarouselWidget,
                            onReorderCarouselWidget = onReorderCarouselWidget,
                            onOpenAddSystemWidget = onOpenAddSystemWidget
                        )
                    }

                    SettingsPage.INTEGRATIONS -> {
                        IntegrationsSettingsPage(
                            allApps = allApps,
                            widgetIntegrations = widgetIntegrations,
                            glass = glass
                        )
                    }

                    SettingsPage.SYSTEM -> {
                        SystemSettingsPage(
                            settings = settings,
                            onUpdateSettings = onUpdateSettings,
                            isDefaultLauncher = isDefaultLauncher,
                            isNotificationAccessGranted = isNotificationAccessGranted,
                            isAccessibilityGranted = isAccessibilityGranted,
                            onExportBackup = {
                                val json = onExportBackup()
                                backupDialogText = json
                                clipboard.setText(AnnotatedString(json))
                            },
                            onOpenImportDialog = { showImportDialog = true },
                            onResetDefaults = onResetDefaults,
                            onOpenSetupGuide = onOpenSetupGuide,
                            onOpenRestrictedGuide = { showRestrictedSettingsGuideDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Export Backup Dialog
    backupDialogText?.let {
        AlertDialog(
            onDismissRequest = { backupDialogText = null },
            title = { Text("Backup Configuration Copied", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text("Your launcher settings and widgets configuration JSON has been copied to your clipboard.", fontSize = 13.sp)
            },
            confirmButton = {
                Button(onClick = { backupDialogText = null }) {
                    Text("Done")
                }
            }
        )
    }

    // Import Backup Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Backup JSON", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste your exported configuration JSON below:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importDialogText,
                        onValueChange = { importDialogText = it },
                        placeholder = { Text("Paste JSON here...") },
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (importDialogText.isNotBlank()) {
                        onImportBackup(importDialogText.trim())
                        showImportDialog = false
                    }
                }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Android 13+ Restricted Settings Unlock Guide Dialog
    if (showRestrictedSettingsGuideDialog) {
        RestrictedSettingsGuideDialog(
            onDismiss = { showRestrictedSettingsGuideDialog = false },
            onOpenAppInfo = { FluxAccessibilityService.openAppInfoSettings(context) },
            onOpenAccessibility = { FluxAccessibilityService.openAccessibilitySettings(context) },
            onOpenNotifications = { FluxNotificationListenerService.openNotificationAccessSettings(context) }
        )
    }
}

/**
 * Root Navigation Item Card
 */
@Composable
private fun SettingsMenuCard(
    page: SettingsPage,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("settings_menu_${page.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = page.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = page.subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 1. Appearance Settings Page (Wallpaper style removed as requested)
 */
@Composable
private fun AppearanceSettingsPage(
    settings: LauncherSettings,
    isWallpaperSwitching: Boolean = false,
    onRefreshAutoWallpaperNow: (() -> Unit)? = null,
    onOpenReorderFavorites: (() -> Unit)? = null,
    onUpdateSettings: (LauncherSettings) -> Unit
) {
    val context = LocalContext.current
    var showIconPackImportDialog by remember { mutableStateOf(false) }
    val autoWp = settings.autoWallpaperConfig

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Theme Presets & Backdrop Style
        item {
            SettingsCard(title = "Launcher Themes & Backdrop") {
                Text("Theme Preset", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(LauncherThemePreset.entries.toTypedArray()) { preset ->
                        FilterChip(
                            selected = settings.themePreset == preset,
                            onClick = { onUpdateSettings(settings.copy(themePreset = preset)) },
                            label = { Text(preset.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Backdrop Wallpaper Source", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Text("Active Source: ${settings.wallpaperStyle.displayName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(WallpaperStyle.entries.toTypedArray()) { style ->
                        FilterChip(
                            selected = settings.wallpaperStyle == style,
                            onClick = {
                                if (style == WallpaperStyle.AUTO_ALBUM && !autoWp.isEnabled) {
                                    val updatedAuto = autoWp.copy(isEnabled = true)
                                    onUpdateSettings(settings.copy(wallpaperStyle = style, autoWallpaperConfig = updatedAuto))
                                    onRefreshAutoWallpaperNow?.invoke()
                                } else {
                                    onUpdateSettings(settings.copy(wallpaperStyle = style))
                                }
                            },
                            label = { Text(style.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                if (settings.wallpaperStyle == WallpaperStyle.SYSTEM_DEVICE) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                                context.startActivity(Intent.createChooser(intent, "Choose Wallpaper"))
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change System Device Wallpaper", fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. Automatic Wallpaper Album Switcher
        item {
            SettingsCard(title = "Automatic Daily Wallpaper Switch") {
                // Switch Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Automatic Switch", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Switches home wallpaper automatically from selected album", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = autoWp.isEnabled,
                        onCheckedChange = { enabled ->
                            val newStyle = if (enabled) WallpaperStyle.AUTO_ALBUM else settings.wallpaperStyle
                            val updatedAuto = autoWp.copy(isEnabled = enabled)
                            onUpdateSettings(settings.copy(wallpaperStyle = newStyle, autoWallpaperConfig = updatedAuto))
                            if (enabled) {
                                onRefreshAutoWallpaperNow?.invoke()
                            }
                        }
                    )
                }

                if (autoWp.isEnabled || settings.wallpaperStyle == WallpaperStyle.AUTO_ALBUM) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Album Source Selection (Local Folder, Git Repo, Google Photos)
                    Text("Selected Album Source", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AutoWallpaperSource.entries.forEach { src ->
                            val selected = autoWp.source == src
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val updatedAuto = autoWp.copy(source = src)
                                        onUpdateSettings(settings.copy(autoWallpaperConfig = updatedAuto))
                                        onRefreshAutoWallpaperNow?.invoke()
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(src.iconEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        src.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Album Source Specific Input Fields & Presets
                    when (autoWp.source) {
                        AutoWallpaperSource.LOCAL_FOLDER -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Local Storage Folder Path", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Enter device storage directory path or select a preset:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = autoWp.localFolderPath,
                                        onValueChange = { path ->
                                            val updatedAuto = autoWp.copy(localFolderPath = path)
                                            onUpdateSettings(settings.copy(autoWallpaperConfig = updatedAuto))
                                        },
                                        placeholder = { Text("/storage/emulated/0/Pictures/Wallpapers", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("Quick Folder Presets:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val presets = listOf(
                                            "Pictures/Wallpapers",
                                            "Download",
                                            "Pictures/Flux",
                                            "DCIM/Camera"
                                        )
                                        items(presets) { name ->
                                            val fullPath = "${android.os.Environment.getExternalStorageDirectory().absolutePath}/$name"
                                            FilterChip(
                                                selected = autoWp.localFolderPath == fullPath,
                                                onClick = {
                                                    val updatedAuto = autoWp.copy(localFolderPath = fullPath)
                                                    onUpdateSettings(settings.copy(autoWallpaperConfig = updatedAuto))
                                                    onRefreshAutoWallpaperNow?.invoke()
                                                },
                                                label = { Text(name, fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AutoWallpaperSource.GIT_REPO -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Git Repository Configuration", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Enter GitHub/Git repo URL (e.g. dharmx/walls) and branch:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = autoWp.gitRepoUrl,
                                        onValueChange = { url ->
                                            val updatedAuto = autoWp.copy(gitRepoUrl = url)
                                            onUpdateSettings(settings.copy(autoWallpaperConfig = updatedAuto))
                                        },
                                        label = { Text("Repository URL / Name", fontSize = 11.sp) },
                                        placeholder = { Text("https://github.com/dharmx/walls", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = autoWp.gitBranch,
                                            onValueChange = { b ->
                                                val updatedAuto = autoWp.copy(gitBranch = b)
                                                onUpdateSettings(settings.copy(autoWallpaperConfig = updatedAuto))
                                            },
                                            label = { Text("Branch", fontSize = 10.sp) },
                                            placeholder = { Text("main", fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        OutlinedTextField(
                                            value = autoWp.gitSubfolder,
                                            onValueChange = { sub ->
                                                val updatedAuto = autoWp.copy(gitSubfolder = sub)
                                                onUpdateSettings(settings.copy(autoWallpaperConfig = updatedAuto))
                                            },
                                            label = { Text("Subfolder (optional)", fontSize = 10.sp) },
                                            placeholder = { Text("mobile", fontSize = 10.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("Popular Wallpaper Repositories:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val gitPresets = listOf(
                                            "dharmx/walls" to "https://github.com/dharmx/walls",
                                            "linuxdotexe/nordic-wallpapers" to "https://github.com/linuxdotexe/nordic-wallpapers",
                                            "D3vh/Minimal-Wallpapers" to "https://github.com/D3vh/Minimal-Wallpapers"
                                        )
                                        items(gitPresets) { (label, repoUrl) ->
                                            FilterChip(
                                                selected = autoWp.gitRepoUrl == repoUrl,
                                                onClick = {
                                                    val updatedAuto = autoWp.copy(gitRepoUrl = repoUrl, gitBranch = "main", gitSubfolder = "")
                                                    onUpdateSettings(settings.copy(autoWallpaperConfig = updatedAuto))
                                                    onRefreshAutoWallpaperNow?.invoke()
                                                },
                                                label = { Text(label, fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AutoWallpaperSource.GOOGLE_PHOTOS -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Google Photos Album Link", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Paste shared Google Photos Album URL or album link:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = autoWp.googlePhotosAlbumUrl,
                                        onValueChange = { url ->
                                            val updatedAuto = autoWp.copy(googlePhotosAlbumUrl = url)
                                            onUpdateSettings(settings.copy(autoWallpaperConfig = updatedAuto))
                                        },
                                        placeholder = { Text("https://photos.app.goo.gl/...", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("Curated Album Presets:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val photoPresets = listOf(
                                            "Nature 4K Collection",
                                            "Architectural Geometry",
                                            "Serene Landscapes"
                                        )
                                        items(photoPresets) { title ->
                                            FilterChip(
                                                selected = autoWp.googlePhotosAlbumUrl.contains(title, true),
                                                onClick = {
                                                    val updatedAuto = autoWp.copy(googlePhotosAlbumUrl = "https://photos.app.goo.gl/sample_$title")
                                                    onUpdateSettings(settings.copy(autoWallpaperConfig = updatedAuto))
                                                    onRefreshAutoWallpaperNow?.invoke()
                                                },
                                                label = { Text(title, fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Switch Frequency Interval
                    Text("Auto Switch Schedule Interval", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(AutoWallpaperInterval.entries.toTypedArray()) { interval ->
                            FilterChip(
                                selected = autoWp.interval == interval,
                                onClick = {
                                    val updatedAuto = autoWp.copy(interval = interval)
                                    onUpdateSettings(settings.copy(autoWallpaperConfig = updatedAuto))
                                },
                                label = { Text(interval.displayName, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Active Wallpaper Status Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ACTIVE WALLPAPER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.6.sp)
                                    Text(
                                        autoWp.currentWallpaperTitle.ifBlank { "Emerald Abstract #04" },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        autoWp.currentWallpaperSourceInfo.ifBlank { "Git: dharmx/walls" },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { onRefreshAutoWallpaperNow?.invoke() },
                                    enabled = !isWallpaperSwitching,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isWallpaperSwitching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Switch Now", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(title = "Frosted Glass Customization") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Translucency", fontSize = 12.sp)
                    Text("${(settings.frostedTranslucency * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = settings.frostedTranslucency,
                    onValueChange = { onUpdateSettings(settings.copy(frostedTranslucency = it)) },
                    valueRange = 0.10f..0.85f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Blur Radius", fontSize = 12.sp)
                    Text("${settings.frostedBlurRadius.toInt()} px", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = settings.frostedBlurRadius,
                    onValueChange = { onUpdateSettings(settings.copy(frostedBlurRadius = it)) },
                    valueRange = 0f..30f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Glass Specular Highlight", fontSize = 12.sp)
                    Text("${(settings.frostedGlassEffect * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = settings.frostedGlassEffect,
                    onValueChange = { onUpdateSettings(settings.copy(frostedGlassEffect = it)) },
                    valueRange = 0f..1f
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Card Corner Radius", fontSize = 12.sp)
                    Text("${settings.cardCornerRadius.toInt()} dp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = settings.cardCornerRadius,
                    onValueChange = { onUpdateSettings(settings.copy(cardCornerRadius = it)) },
                    valueRange = 0f..40f
                )
            }
        }

        item {
            SettingsCard(title = "Icon Style & Packs") {
                Text("Icon Preset", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconPackType.entries.take(4).forEach { pack ->
                        FilterChip(
                            selected = settings.iconPackType == pack,
                            onClick = { onUpdateSettings(settings.copy(iconPackType = pack)) },
                            label = { Text(pack.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showIconPackImportDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_icon_pack_button")
                ) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (settings.customIconSettings.installedPackName != null) {
                            "Imported Pack: ${settings.customIconSettings.installedPackName}"
                        } else {
                            "Import Custom Icon Pack"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                SettingsToggleRow(
                    title = "Show Notification Badges",
                    subtitle = "Show notification indicators on apps with unread alerts",
                    checked = settings.showAppNotificationBadges,
                    onCheckedChange = { onUpdateSettings(settings.copy(showAppNotificationBadges = it)) }
                )
            }
        }

        item {
            SettingsCard(title = "Favorites Screen & Layout") {
                if (onOpenReorderFavorites != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenReorderFavorites() }
                            .testTag("settings_reorder_favorites_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reorder Favorites Apps List",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Change rank position & custom non-alphabetical order",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Text(
                    text = "Favorites Screen Top Spacing",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Customize the empty row spacing above the widgets and favorites on the home screen",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FavoritesTopSpacing.entries.forEach { spacing ->
                        val isSelected = settings.favoritesTopSpacing == spacing
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpdateSettings(settings.copy(favoritesTopSpacing = spacing)) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = spacing.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when (spacing) {
                                            FavoritesTopSpacing.HIGH -> "3 empty rows at the top (~180dp)"
                                            FavoritesTopSpacing.MEDIUM -> "2 empty rows at the top (~120dp)"
                                            FavoritesTopSpacing.LOW -> "1 empty row at the top (~60dp)"
                                        },
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onUpdateSettings(settings.copy(favoritesTopSpacing = spacing)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showIconPackImportDialog) {
        val installedPacks = remember {
            try {
                val pm = context.packageManager
                val list = mutableListOf<InstalledIconPackInfo>()
                val actions = listOf(
                    "org.adw.launcher.THEMES",
                    "com.novalauncher.THEME",
                    "com.fede.launcher.THEME_ICONPACK",
                    "com.anddoes.launcher.THEME",
                    "com.teslacoilsw.launcher.THEME"
                )
                val seen = mutableSetOf<String>()
                for (action in actions) {
                    val intent = Intent(action)
                    val resolve = pm.queryIntentActivities(intent, 0)
                    for (info in resolve) {
                        val pkg = info.activityInfo.packageName
                        if (seen.add(pkg)) {
                            list.add(
                                InstalledIconPackInfo(
                                    packageName = pkg,
                                    label = info.loadLabel(pm).toString(),
                                    icon = try { info.loadIcon(pm) } catch (_: Exception) { null }
                                )
                            )
                        }
                    }
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }

        AlertDialog(
            onDismissRequest = { showIconPackImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Custom Icon Pack", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Select an installed third-party icon pack or browse Google Play Store:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (installedPacks.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📦 No third-party icon pack apps found.", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Install icon packs like Whicons or Delta from the Play Store.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())
                        ) {
                            installedPacks.forEach { pack ->
                                val isSelected = settings.customIconSettings.installedPackPackageName == pack.packageName
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onUpdateSettings(
                                                settings.copy(
                                                    iconPackType = IconPackType.CUSTOM,
                                                    customIconSettings = settings.customIconSettings.copy(
                                                        installedPackPackageName = pack.packageName,
                                                        installedPackName = pack.label
                                                    )
                                                )
                                            )
                                            showIconPackImportDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(pack.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        if (isSelected) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://search?q=icon%20pack")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/search?q=icon%20pack")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(webIntent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Shop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Browse Play Store for Icon Packs", fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconPackImportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * 2. Categories Settings Page
 */
@Composable
private fun CategoriesSettingsPage(
    allApps: List<AppItem>,
    userCategories: List<String>,
    newCategoryInputText: String,
    onNewCategoryInputChange: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (String, String) -> Unit,
    onRemoveCategory: (String) -> Unit,
    onAssignCategory: (String, String) -> Unit,
    selectedCategoryForAppAssignment: String?,
    onSelectCategoryForAssignment: (String?) -> Unit,
    iconPackType: IconPackType
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsCard(title = "Create New Category") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryInputText,
                        onValueChange = onNewCategoryInputChange,
                        placeholder = { Text("Category Name (e.g. Finance, Games)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newCategoryInputText.isNotBlank()) {
                                onAddCategory(newCategoryInputText.trim())
                                onNewCategoryInputChange("")
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        enabled = newCategoryInputText.isNotBlank()
                    ) {
                        Text("Add", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            SettingsCard(title = "Categories List (${userCategories.size})") {
                if (userCategories.isEmpty()) {
                    Text(
                        text = "No custom categories created yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        userCategories.forEach { category ->
                            val assignedCount = allApps.count { it.customCategory == category }
                            var isEditingThisCat by remember { mutableStateOf(false) }
                            var editedNameText by remember { mutableStateOf(category) }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (isEditingThisCat) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = editedNameText,
                                                onValueChange = { editedNameText = it },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = {
                                                    if (editedNameText.isNotBlank() && editedNameText.trim() != category) {
                                                        onRenameCategory(category, editedNameText.trim())
                                                    }
                                                    isEditingThisCat = false
                                                }
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(
                                                onClick = {
                                                    editedNameText = category
                                                    isEditingThisCat = false
                                                }
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = category,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = "$assignedCount apps",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        editedNameText = category
                                                        isEditingThisCat = true
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Edit,
                                                        contentDescription = "Rename Category",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onRemoveCategory(category) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Delete,
                                                        contentDescription = "Delete Category",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(title = "App Assignment") {
                Text(
                    text = "Select a category to view and assign installed apps.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(userCategories) { cat ->
                        val isSelected = selectedCategoryForAppAssignment == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSelectCategoryForAssignment(if (isSelected) null else cat)
                            },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                val targetCat = selectedCategoryForAppAssignment
                if (targetCat != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    var appSearchText by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = appSearchText,
                        onValueChange = { appSearchText = it },
                        placeholder = { Text("Search apps to assign...", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredApps = remember(allApps, appSearchText) {
                        if (appSearchText.isBlank()) allApps.sortedBy { it.label.lowercase() }
                        else allApps.filter { it.label.contains(appSearchText, ignoreCase = true) }.sortedBy { it.label.lowercase() }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        filteredApps.take(20).forEach { app ->
                            val isAssignedToThis = app.customCategory == targetCat
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAssignedToThis) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isAssignedToThis) {
                                            onAssignCategory(app.packageName, "")
                                        } else {
                                            onAssignCategory(app.packageName, targetCat)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        AppIconRenderer(app = app, iconPackType = iconPackType, size = 24.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = app.label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Text(
                                        text = if (isAssignedToThis) "✓ Assigned" else if (app.customCategory.isNotBlank()) app.customCategory else "+ Assign",
                                        fontSize = 11.sp,
                                        fontWeight = if (isAssignedToThis) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isAssignedToThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Dock Settings Page
 */
@Composable
private fun DockSettingsPage(
    settings: LauncherSettings,
    dockApps: List<AppItem>,
    onUpdateSettings: (LauncherSettings) -> Unit,
    onSelectDockSlot: ((Int) -> Unit)?,
    onClearDockSlot: ((Int) -> Unit)?,
    onResetDockDefaults: (() -> Unit)?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsCard(title = "Dock Layout & Visibility") {
                SettingsToggleRow(
                    title = "Show Bottom Dock",
                    subtitle = "Display pinned quick-launch dock on the home screen",
                    checked = settings.showDock,
                    onCheckedChange = { onUpdateSettings(settings.copy(showDock = it)) }
                )

                if (settings.showDock) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Text(
                        "Number of Dock Slots",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 4, 5).forEach { count ->
                            FilterChip(
                                selected = settings.dockSlotCount == count,
                                onClick = { onUpdateSettings(settings.copy(dockSlotCount = count)) },
                                label = { Text("$count Slots", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Dock Icon Scale",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${(settings.dockIconScale * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = settings.dockIconScale,
                        onValueChange = { onUpdateSettings(settings.copy(dockIconScale = it)) },
                        valueRange = 0.8f..1.3f,
                        steps = 4
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    SettingsToggleRow(
                        title = "Show Dock App Labels",
                        subtitle = "Display app titles beneath dock icons",
                        checked = settings.showDockLabels,
                        onCheckedChange = { onUpdateSettings(settings.copy(showDockLabels = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    SettingsToggleRow(
                        title = "Frosted Glass Blur Background",
                        subtitle = "Apply translucent glass backdrop behind dock icons",
                        checked = settings.dockBlurBackground,
                        onCheckedChange = { onUpdateSettings(settings.copy(dockBlurBackground = it)) }
                    )
                }
            }
        }

        if (settings.showDock) {
            item {
                SettingsCard(title = "Dock App Slots") {
                    val maxSlots = settings.dockSlotCount.coerceIn(3, 5)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (slot in 0 until maxSlots) {
                            val app = dockApps.getOrNull(slot)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (app != null) {
                                            AppIconRenderer(
                                                app = app,
                                                iconPackType = settings.iconPackType,
                                                size = 32.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Slot #${slot + 1}: ${app.label}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = app.packageName,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("+", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Slot #${slot + 1}: (Empty)",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row {
                                        FilledTonalButton(
                                            onClick = { onSelectDockSlot?.invoke(slot) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Change", fontSize = 11.sp)
                                        }
                                        if (app != null && onClearDockSlot != null) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { onClearDockSlot(slot) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Close,
                                                    contentDescription = "Clear Slot",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { onResetDockDefaults?.invoke() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Dock to Defaults (Phone, Messages, Chrome)", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/**
 * 4. Gestures & Controls Page
 */
@Composable
private fun GesturesSettingsPage(
    settings: LauncherSettings,
    isAccessibilityGranted: Boolean,
    onUpdateSettings: (LauncherSettings) -> Unit,
    onOpenRestrictedGuide: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsCard(title = "Gestures & Locking") {
                SettingsToggleRow(
                    title = "Lock Layout (Widgets & Favorites)",
                    subtitle = "Prevent accidental reordering, adding, or deleting of widgets and favorites",
                    checked = settings.isLayoutLocked,
                    onCheckedChange = { onUpdateSettings(settings.copy(isLayoutLocked = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                SettingsToggleRow(
                    title = "Double-Tap to Sleep",
                    subtitle = "Turn off and lock screen on wallpaper double-tap",
                    checked = settings.doubleTapToSleep,
                    onCheckedChange = { isChecked ->
                        onUpdateSettings(settings.copy(doubleTapToSleep = isChecked))
                        if (isChecked && !FluxAccessibilityService.isServiceRunning()) {
                            FluxAccessibilityService.openAccessibilitySettings(context)
                        }
                    }
                )

                if (settings.doubleTapToSleep && !isAccessibilityGranted) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { FluxAccessibilityService.openAccessibilitySettings(context) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Accessibility", fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = onOpenRestrictedGuide,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fix Restricted Setting", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                SettingsToggleRow(
                    title = "Swipe Down for Notifications",
                    subtitle = "Pull down anywhere on home to open system notification shade",
                    checked = settings.swipeDownForNotificationCenter,
                    onCheckedChange = { onUpdateSettings(settings.copy(swipeDownForNotificationCenter = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                SettingsToggleRow(
                    title = "Left-Handed Mode",
                    subtitle = "Flip layout alignment for left hand ergonomics",
                    checked = settings.leftHandedMode,
                    onCheckedChange = { onUpdateSettings(settings.copy(leftHandedMode = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                SettingsToggleRow(
                    title = "Hide Status Bar",
                    subtitle = "Immersive full screen view on homescreen",
                    checked = settings.hideStatusBar,
                    onCheckedChange = { onUpdateSettings(settings.copy(hideStatusBar = it)) }
                )
            }
        }

        item {
            SettingsCard(title = "Haptic Feedback & Tactile Response") {
                SettingsToggleRow(
                    title = "Interaction Haptics",
                    subtitle = "Vibrate on app launch, drawer popout, and button taps",
                    checked = settings.hapticFeedback,
                    onCheckedChange = { onUpdateSettings(settings.copy(hapticFeedback = it)) }
                )

                if (settings.hapticFeedback) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Alphabet Wave Scrollbar Vibration: Level ${settings.waveAlphabetHapticIntensity}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        listOf(0 to "Off", 1 to "Light", 2 to "Medium", 3 to "Heavy").forEach { (level, name) ->
                            val isSelected = settings.waveAlphabetHapticIntensity == level
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateSettings(settings.copy(waveAlphabetHapticIntensity = level)) },
                                label = { Text(name, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. Clock & Widgets Settings Page (Clock Accent Tint UI Fixed & Polished)
 */
@Composable
private fun ClockWidgetsSettingsPage(
    settings: LauncherSettings,
    carouselWidgets: List<WidgetType> = emptyList(),
    availableWidgets: List<WidgetType> = emptyList(),
    onUpdateSettings: (LauncherSettings) -> Unit,
    onAddCarouselWidget: ((WidgetType) -> Unit)? = null,
    onRemoveCarouselWidget: ((WidgetType) -> Unit)? = null,
    onReorderCarouselWidget: ((Int, Int) -> Unit)? = null,
    onOpenAddSystemWidget: (() -> Unit)? = null
) {
    var customHexInput by remember(settings.clockColorTintHex) { mutableStateOf(settings.clockColorTintHex) }

    val presetColors = listOf(
        "#FFFFFF" to "Pure White",
        "#10B981" to "Emerald Green",
        "#38BDF8" to "Sky Blue",
        "#FBBF24" to "Amber Gold",
        "#F43F5E" to "Rose Coral",
        "#A78BFA" to "Cyber Purple",
        "#A3E635" to "Neon Lime",
        "#FB923C" to "Sunset Orange",
        "#6366F1" to "Royal Indigo"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Clock Widget Customization Card
        item {
            SettingsCard(title = "Clock Widget Customization") {
                Text(
                    text = "Clock Style Preset",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(ClockStyle.entries.toTypedArray()) { style ->
                        FilterChip(
                            selected = settings.clockStyle == style,
                            onClick = { onUpdateSettings(settings.copy(clockStyle = style)) },
                            label = { Text(style.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Clock Font Size Scale", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("${(settings.clockScale * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = settings.clockScale,
                    onValueChange = { onUpdateSettings(settings.copy(clockScale = it)) },
                    valueRange = 0.7f..1.5f
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Clock Accent Tint Palette with Visual Color Dots
                Text(
                    text = "Clock Accent Tint Palette",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetColors) { (hex, name) ->
                        val isSelected = settings.clockColorTintHex.equals(hex, ignoreCase = true)
                        val parsedColor = try {
                            Color(AndroidColor.parseColor(hex))
                        } catch (_: Exception) {
                            Color.White
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    customHexInput = hex
                                    onUpdateSettings(settings.copy(clockColorTintHex = hex))
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor)
                                        .then(
                                            if (hex.equals("#FFFFFF", ignoreCase = true))
                                                Modifier.background(Color.White)
                                            else Modifier
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Hex Code Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val previewColor = try {
                        Color(AndroidColor.parseColor(if (customHexInput.startsWith("#")) customHexInput else "#$customHexInput"))
                    } catch (_: Exception) {
                        Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(previewColor)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = customHexInput,
                        onValueChange = { input ->
                            customHexInput = input
                            val formatted = if (input.startsWith("#")) input else "#$input"
                            try {
                                AndroidColor.parseColor(formatted)
                                onUpdateSettings(settings.copy(clockColorTintHex = formatted))
                            } catch (_: Exception) {}
                        },
                        placeholder = { Text("#FFFFFF", fontSize = 12.sp) },
                        singleLine = true,
                        label = { Text("Custom Color Hex", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                SettingsToggleRow(
                    title = "Show Running Seconds",
                    subtitle = "Display live ticking seconds in clock view",
                    checked = settings.showSeconds,
                    onCheckedChange = { onUpdateSettings(settings.copy(showSeconds = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                SettingsToggleRow(
                    title = "Show Calendar Date Header",
                    subtitle = "Display day of week and month below clock time",
                    checked = settings.showDate,
                    onCheckedChange = { onUpdateSettings(settings.copy(showDate = it)) }
                )
            }
        }

        // 2. Favourites Screen Widget Carousel Customization Card
        item {
            SettingsCard(title = "Favourites Screen Widget Carousel") {
                Text(
                    text = "Active Carousel Cards (${carouselWidgets.size + 1}/6)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "The clock is fixed as first card. Swipe left/right on home screen to view your active cards.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Fixed Card #1: Clock
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🕒", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("1. Clock & Date", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Fixed home card #1", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                "Fixed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Dynamic Carousel Widgets
                carouselWidgets.forEachIndexed { index, widgetType ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(widgetType.iconEmoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 2}. ${widgetType.displayName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = widgetType.description,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Reorder Up
                            IconButton(
                                onClick = { onReorderCarouselWidget?.invoke(index, index - 1) },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                            }

                            // Reorder Down
                            IconButton(
                                onClick = { onReorderCarouselWidget?.invoke(index, index + 1) },
                                enabled = index < carouselWidgets.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                            }

                            // Remove
                            IconButton(
                                onClick = { onRemoveCarouselWidget?.invoke(widgetType) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Add More Carousel Widgets
                val availableToAdd = remember(carouselWidgets, availableWidgets) {
                    val fullList = if (availableWidgets.isNotEmpty()) availableWidgets else WidgetType.entries.filter { it != WidgetType.CLOCK }
                    fullList.filter { it !in carouselWidgets }
                }

                if (availableToAdd.isNotEmpty() && carouselWidgets.size < 5) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Add to Carousel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableToAdd) { toAdd ->
                            FilledTonalButton(
                                onClick = { onAddCarouselWidget?.invoke(toAdd) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("${toAdd.iconEmoji} ${toAdd.displayName}", fontSize = 11.5.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Auto-surfacing option
                SettingsToggleRow(
                    title = "Auto-Surfacing Music Widget",
                    subtitle = "Automatically prioritize YouTube Music / media player card when active playback starts",
                    checked = settings.audioAppAutoSurfacing,
                    onCheckedChange = { onUpdateSettings(settings.copy(audioAppAutoSurfacing = it)) }
                )
            }
        }
    }
}

/**
 * 6. System & Backup Settings Page
 */
@Composable
private fun SystemSettingsPage(
    settings: LauncherSettings,
    onUpdateSettings: (LauncherSettings) -> Unit,
    isDefaultLauncher: Boolean,
    isNotificationAccessGranted: Boolean,
    isAccessibilityGranted: Boolean,
    onExportBackup: () -> Unit,
    onOpenImportDialog: () -> Unit,
    onResetDefaults: () -> Unit,
    onOpenSetupGuide: () -> Unit,
    onOpenRestrictedGuide: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsCard(title = "Battery & Performance Optimization") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Battery Saver & Resource Optimization", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            text = "Automatically reduces overdraw and pauses background ticks to save battery",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.batteryOptimizationMode,
                        onCheckedChange = { onUpdateSettings(settings.copy(batteryOptimizationMode = it)) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.fromParts("package", context.packageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BatteryChargingFull,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("System Unrestricted Battery Access", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Ensure system OS does not kill launcher background services", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        item {
            SettingsCard(title = "System Permissions & Defaults") {
                // 1. Set Default Launcher
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isDefaultLauncher) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { DefaultLauncherHelper.openSetDefaultLauncher(context) }
                        .testTag("settings_set_default_launcher_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = null,
                            tint = if (isDefaultLauncher) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Set Default Home App", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = if (isDefaultLauncher) "✓ Flux is your active default launcher" else "Tap to choose Flux as default home",
                                fontSize = 11.sp,
                                color = if (isDefaultLauncher) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Notification Access
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isNotificationAccessGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { FluxNotificationListenerService.openNotificationAccessSettings(context) }
                        .testTag("settings_notification_access_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = if (isNotificationAccessGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Grant Notification Access", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = if (isNotificationAccessGranted) "✓ Notification listener enabled" else "Required for live badges & chat previews",
                                fontSize = 11.sp,
                                color = if (isNotificationAccessGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Accessibility Permission
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isAccessibilityGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { FluxAccessibilityService.openAccessibilitySettings(context) }
                        .testTag("settings_accessibility_access_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Accessibility,
                            contentDescription = null,
                            tint = if (isAccessibilityGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Grant Accessibility Permission", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = if (isAccessibilityGranted) "✓ Service enabled (double tap to lock ready)" else "Required for double tap screen lock gesture",
                                fontSize = 11.sp,
                                color = if (isAccessibilityGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Banner for Restricted Settings (Android 13+)
                if (!isAccessibilityGranted || !isNotificationAccessGranted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenRestrictedGuide)
                            .testTag("settings_restricted_settings_guide_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Controlled by Restricted Setting?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Android 13+ blocks sideloaded APK permissions by default. Tap here for the 3-step unlock guide.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. App Info & Other Permissions
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                        .testTag("settings_app_info_permissions_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("App Permissions & Battery Settings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = "Manage calendar, battery optimization, and system privileges",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        item {
            SettingsCard(title = "Backup & Maintenance") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportBackup,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenImportDialog,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onResetDefaults,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onOpenSetupGuide,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restart Setup Guide", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Android 13+ Restricted Settings Guide Dialog
 */
@Composable
fun RestrictedSettingsGuideDialog(
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Fix 'Restricted Setting'",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Android 13+ protects sideloaded APK apps by greying out Accessibility and Notification settings with 'Controlled by Restricted Setting'.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "HOW TO UNBLOCK IN 3 EASY STEPS:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )

                        Row(verticalAlignment = Alignment.Top) {
                            Text("1️⃣ ", fontSize = 14.sp)
                            Column {
                                Text("Open App Info", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Tap the '1. Open App Info' button below to jump to Flux Launcher's system app settings.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(verticalAlignment = Alignment.Top) {
                            Text("2️⃣ ", fontSize = 14.sp)
                            Column {
                                Text("Tap the 3 dots (⋮) menu", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("In the top-right corner of the App Info screen, tap the ⋮ menu and choose 'Allow restricted settings'.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(verticalAlignment = Alignment.Top) {
                            Text("3️⃣ ", fontSize = 14.sp)
                            Column {
                                Text("Confirm PIN/Biometrics & Enable", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Authenticate with your fingerprint or PIN, then return here to toggle the permission ON!", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Action Buttons
                Button(
                    onClick = onOpenAppInfo,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("1. Open App Info (Tap ⋮ in top right)")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenAccessibility,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("2. Accessibility", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("2. Notifications", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.height(28.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntegrationsSettingsPage(
    allApps: List<AppItem>,
    widgetIntegrations: WidgetIntegrationsManager?,
    glass: GlassStyle
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var selectedCategoryForAppAssign by remember { mutableStateOf<IntegrationCategory?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "WIDGET APP INTEGRATIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        items(IntegrationCategory.entries.toTypedArray()) { cat ->
            val currentLabel = widgetIntegrations?.getAppLabel(cat, pm) ?: "Auto-Detect System Default"

            FrostedGlassPanel(
                cornerRadius = 16.dp,
                glass = glass,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = cat.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = cat.defaultDescription,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalButton(
                            onClick = { selectedCategoryForAppAssign = cat },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Change", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Extension,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Assigned: $currentLabel",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    selectedCategoryForAppAssign?.let { cat ->
        var searchQuery by remember { mutableStateOf("") }
        val filteredApps = remember(allApps, searchQuery) {
            if (searchQuery.isBlank()) allApps.filter { !it.isHidden }
            else allApps.filter { !it.isHidden && it.label.contains(searchQuery, ignoreCase = true) }
        }

        ModalBottomSheet(
            onDismissRequest = { selectedCategoryForAppAssign = null },
            containerColor = Color.Transparent,
            scrimColor = Color.Black.copy(alpha = 0.45f)
        ) {
            FrostedGlassPanel(
                cornerRadius = 28.dp,
                glass = glass,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assign App for ${cat.displayName}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { selectedCategoryForAppAssign = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                widgetIntegrations?.setPackage(cat, null)
                                selectedCategoryForAppAssign = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Auto-Detect System Default", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Uses default system app handler", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search installed apps...", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(filteredApps, key = { idx, app -> "assign_${app.uniqueKey}_$idx" }) { _, app ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        widgetIntegrations?.setPackage(cat, app.packageName)
                                        selectedCategoryForAppAssign = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIconRenderer(app = app, iconPackType = IconPackType.MATERIAL, size = 32.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = app.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

