package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.service.FluxAccessibilityService
import com.example.service.FluxNotificationListenerService
import com.example.util.DefaultLauncherHelper
import com.example.util.IntegrationCategory
import com.example.util.WidgetIntegrationsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsStudioSheet(
    settings: LauncherSettings,
    allApps: List<AppItem> = emptyList(),
    dockApps: List<AppItem> = emptyList(),
    widgetIntegrations: WidgetIntegrationsManager? = null,
    onUpdateSettings: (LauncherSettings) -> Unit,
    onSelectDockSlot: ((Int) -> Unit)? = null,
    onClearDockSlot: ((Int) -> Unit)? = null,
    onResetDockDefaults: (() -> Unit)? = null,
    onExportBackup: () -> String,
    onImportBackup: (String) -> Boolean,
    onResetDefaults: () -> Unit,
    onOpenSetupGuide: () -> Unit = {},
    onOpenSystemWidgetPicker: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("settings_studio_sheet")
    ) {
        SettingsContent(
            settings = settings,
            allApps = allApps,
            dockApps = dockApps,
            widgetIntegrations = widgetIntegrations,
            onUpdateSettings = onUpdateSettings,
            onSelectDockSlot = onSelectDockSlot,
            onClearDockSlot = onClearDockSlot,
            onResetDockDefaults = onResetDockDefaults,
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup,
            onResetDefaults = onResetDefaults,
            onOpenSetupGuide = onOpenSetupGuide,
            onOpenSystemWidgetPicker = onOpenSystemWidgetPicker,
            onClose = onDismiss,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
fun SettingsContent(
    settings: LauncherSettings,
    allApps: List<AppItem> = emptyList(),
    dockApps: List<AppItem> = emptyList(),
    widgetIntegrations: WidgetIntegrationsManager? = null,
    onUpdateSettings: (LauncherSettings) -> Unit,
    onSelectDockSlot: ((Int) -> Unit)? = null,
    onClearDockSlot: ((Int) -> Unit)? = null,
    onResetDockDefaults: (() -> Unit)? = null,
    onExportBackup: () -> String,
    onImportBackup: (String) -> Boolean,
    onResetDefaults: () -> Unit,
    onOpenSetupGuide: () -> Unit = {},
    onOpenSystemWidgetPicker: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var backupDialogText by remember { mutableStateOf<String?>(null) }
    var importDialogText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var activeCategoryTab by remember { mutableIntStateOf(0) }

    // State for app integration picker dialog
    var selectedIntegrationForPicker by remember { mutableStateOf<IntegrationCategory?>(null) }
    var integrationSearchFilter by remember { mutableStateOf("") }

    val isDefaultLauncher = remember(context) {
        DefaultLauncherHelper.isDefaultLauncher(context)
    }
    val isNotificationAccessGranted = remember(context) {
        FluxNotificationListenerService.isNotificationAccessGranted(context)
    }
    val isAccessibilityGranted = remember(context) {
        FluxAccessibilityService.isServiceRunning()
    }

    val categories = listOf("Appearance", "Dock", "Gestures & Lock", "Widgets & Clock", "Integrations", "System")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Flux Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Clean, focused Android experience",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onClose != null) {
                FilledTonalButton(
                    onClick = onClose,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Done", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category Tab Row
        ScrollableTabRow(
            selectedTabIndex = activeCategoryTab,
            edgePadding = 0.dp,
            divider = {},
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEachIndexed { index, title ->
                Tab(
                    selected = activeCategoryTab == index,
                    onClick = { activeCategoryTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (activeCategoryTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeCategoryTab) {
                0 -> {
                    // --- APPEARANCE ---
                    item {
                        SettingsCard(title = "Wallpaper & Themes") {
                            Text("Wallpaper Style", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                WallpaperStyle.entries.forEach { style ->
                                    FilterChip(
                                        selected = settings.wallpaperStyle == style,
                                        onClick = { onUpdateSettings(settings.copy(wallpaperStyle = style)) },
                                        label = {
                                            val name = when (style) {
                                                WallpaperStyle.SYSTEM_DEVICE -> "Device"
                                                WallpaperStyle.FLUX_EMBLEM -> "Emblem"
                                                WallpaperStyle.FLUX_MINIMAL_GRADIENT -> "Gradient"
                                                WallpaperStyle.SYSTEM_SOLID -> "Solid"
                                            }
                                            Text(name, fontSize = 11.sp)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

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
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Device Wallpaper", fontSize = 12.sp)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            Text("Theme Preset", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(LauncherThemePreset.entries.size) { idx ->
                                    val preset = LauncherThemePreset.entries[idx]
                                    FilterChip(
                                        selected = settings.themePreset == preset,
                                        onClick = { onUpdateSettings(settings.copy(themePreset = preset)) },
                                        label = {
                                            Text(
                                                preset.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                                fontSize = 11.sp
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Icon Style", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
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

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            SettingsToggleRow(
                                title = "Show Notification Badges",
                                subtitle = "Show dots on apps with active unread alerts",
                                checked = settings.showAppNotificationBadges,
                                onCheckedChange = { onUpdateSettings(settings.copy(showAppNotificationBadges = it)) }
                            )
                        }
                    }
                }

                1 -> {
                    // --- DOCK CUSTOMIZATION ---
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

                2 -> {
                    // --- GESTURES & LOCK ---
                    item {
                        SettingsCard(title = "Gestures & Locking") {
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
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { FluxAccessibilityService.openAccessibilitySettings(context) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Grant Lock Permission (Accessibility)", fontSize = 12.sp)
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
                }

                3 -> {
                    // --- WIDGETS & CLOCK ---
                    item {
                        SettingsCard(title = "Home Screen Widgets") {
                            Text(
                                text = "Favorites Screen 3-Widget Carousel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Card 1 is the Fixed Clock & Glance Widget. Cards 2 & 3 are customizable widgets.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            SettingsToggleRow(
                                title = "Show Seconds",
                                subtitle = "Display live running seconds in clock widget",
                                checked = settings.showSeconds,
                                onCheckedChange = { onUpdateSettings(settings.copy(showSeconds = it)) }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            OutlinedButton(
                                onClick = onOpenSystemWidgetPicker,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add System Widget to Left Screen", fontSize = 12.sp)
                            }
                        }
                    }
                }

                4 -> {
                    // --- APP INTEGRATIONS ---
                    item {
                        val pm = context.packageManager
                        SettingsCard(title = "Default Integrations") {
                            IntegrationItemRow(
                                title = "Music App",
                                subtitle = widgetIntegrations?.getAppLabel(IntegrationCategory.MUSIC, pm) ?: "System Default (Spotify / YT Music)",
                                icon = Icons.Filled.MusicNote,
                                onClick = {
                                    selectedIntegrationForPicker = IntegrationCategory.MUSIC
                                    integrationSearchFilter = ""
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            IntegrationItemRow(
                                title = "Calendar App",
                                subtitle = widgetIntegrations?.getAppLabel(IntegrationCategory.CALENDAR, pm) ?: "Google / System Calendar",
                                icon = Icons.Filled.CalendarMonth,
                                onClick = {
                                    selectedIntegrationForPicker = IntegrationCategory.CALENDAR
                                    integrationSearchFilter = ""
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            IntegrationItemRow(
                                title = "Weather App",
                                subtitle = widgetIntegrations?.getAppLabel(IntegrationCategory.WEATHER, pm) ?: "Google Weather / Browser",
                                icon = Icons.Filled.Cloud,
                                onClick = {
                                    selectedIntegrationForPicker = IntegrationCategory.WEATHER
                                    integrationSearchFilter = ""
                                }
                            )
                        }
                    }
                }

                5 -> {
                    // --- SYSTEM & BACKUP ---
                    item {
                        SettingsCard(title = "System Setup & Backup") {
                            // Default launcher banner
                            if (!isDefaultLauncher) {
                                Button(
                                    onClick = { DefaultLauncherHelper.openSetDefaultLauncher(context) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Set Flux as Default Home App", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Notification listener permission
                            if (!isNotificationAccessGranted) {
                                OutlinedButton(
                                    onClick = { FluxNotificationListenerService.openNotificationAccessSettings(context) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Grant Notification Listener Permission", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val json = onExportBackup()
                                        backupDialogText = json
                                        clipboard.setText(AnnotatedString(json))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { showImportDialog = true },
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
        }
    }

    // App Integration Selection Dialog
    selectedIntegrationForPicker?.let { cat ->
        val installedApps = allApps.sortedBy { it.label.lowercase() }
        val filtered = if (integrationSearchFilter.isBlank()) {
            installedApps
        } else {
            installedApps.filter { it.label.contains(integrationSearchFilter, ignoreCase = true) }
        }

        AlertDialog(
            onDismissRequest = { selectedIntegrationForPicker = null },
            title = { Text("Choose ${cat.displayName} App", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                    OutlinedTextField(
                        value = integrationSearchFilter,
                        onValueChange = { integrationSearchFilter = it },
                        placeholder = { Text("Filter apps...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        item {
                            Surface(
                                onClick = {
                                    widgetIntegrations?.setPackage(cat, null)
                                    selectedIntegrationForPicker = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text("Use System Default / Auto Detect", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(12.dp))
                            }
                        }
                        items(filtered.size) { idx ->
                            val app = filtered[idx]
                            Surface(
                                onClick = {
                                    widgetIntegrations?.setPackage(cat, app.packageName)
                                    selectedIntegrationForPicker = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    AppIconRenderer(app = app, iconPackType = IconPackType.DEFAULT_APP, size = 32.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(app.label, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedIntegrationForPicker = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Backup Dialog
    backupDialogText?.let { json ->
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

@Composable
private fun IntegrationItemRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
