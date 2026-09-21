package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
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
    userCategories: List<String> = emptyList(),
    onAddCategory: (String) -> Unit = {},
    onRenameCategory: (String, String) -> Unit = { _, _ -> },
    onRemoveCategory: (String) -> Unit = {},
    onAssignCategory: (String, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("settings_studio_sheet")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fullscreen Top Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Flux Settings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Customize launcher layout, gestures & haptics",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

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
                userCategories = userCategories,
                onAddCategory = onAddCategory,
                onRenameCategory = onRenameCategory,
                onRemoveCategory = onRemoveCategory,
                onAssignCategory = onAssignCategory,
                onClose = onDismiss,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            )
        }
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
    userCategories: List<String> = emptyList(),
    onAddCategory: (String) -> Unit = {},
    onRenameCategory: (String, String) -> Unit = { _, _ -> },
    onRemoveCategory: (String) -> Unit = {},
    onAssignCategory: (String, String) -> Unit = { _, _ -> },
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var backupDialogText by remember { mutableStateOf<String?>(null) }
    var importDialogText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var activeCategoryTab by remember { mutableIntStateOf(0) }

    var newCategoryInputText by remember { mutableStateOf("") }
    var selectedCategoryForAppAssignment by remember { mutableStateOf<String?>(null) }

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

    val categories = listOf("Appearance", "Categories", "Dock", "Gestures & Lock", "Widgets & Clock", "Integrations", "System")

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
                                                preset.displayName,
                                                fontSize = 11.sp
                                            )
                                        }
                                    )
                                }
                            }

                            // Frosted Glass Customization Sliders
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Frosted Glass Sliders",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

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
                                Text("Glass Effect Specular", fontSize = 12.sp)
                                Text("${(settings.frostedGlassEffect * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = settings.frostedGlassEffect,
                                onValueChange = { onUpdateSettings(settings.copy(frostedGlassEffect = it)) },
                                valueRange = 0f..1f
                            )

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

                            Spacer(modifier = Modifier.height(8.dp))

                            var showIconPackImportDialog by remember { mutableStateOf(false) }

                            OutlinedButton(
                                onClick = { showIconPackImportDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("import_icon_pack_button")
                            ) {
                                Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
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
                                                "Select an installed third-party icon pack or browse Google Play Store to download icon packs:",
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
                    // --- CATEGORIES MANAGEMENT ---
                    item {
                        SettingsCard(title = "Create New Category") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newCategoryInputText,
                                    onValueChange = { newCategoryInputText = it },
                                    placeholder = { Text("Category Name (e.g. Finance, Travel)", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newCategoryInputText.isNotBlank()) {
                                            onAddCategory(newCategoryInputText.trim())
                                            newCategoryInputText = ""
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
                                val renameCategoryFn = onRenameCategory
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    userCategories.forEach { category ->
                                        val assignedCount = allApps.count { it.customCategory == category }
                                        var isEditingThisCat by remember { mutableStateOf(false) }
                                        var editedNameText by remember { mutableStateOf(category) }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                                                    renameCategoryFn(category, editedNameText.trim())
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

                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(userCategories.size) { idx ->
                                    val cat = userCategories[idx]
                                    val isSelected = selectedCategoryForAppAssignment == cat
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedCategoryForAppAssignment = if (isSelected) null else cat
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
                                                    AppIconRenderer(app = app, iconPackType = settings.iconPackType, size = 24.dp)
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

                2 -> {
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

                3 -> {
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

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            // Haptics Settings Section
                            Text(
                                text = "Haptic Feedback & Tactile Response",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

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

                4 -> {
                    // --- WIDGETS & CLOCK ---
                    item {
                        SettingsCard(title = "Clock Widget Customization") {
                            Text(
                                text = "Clock Style Preset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(ClockStyle.entries.size) { idx ->
                                    val style = ClockStyle.entries[idx]
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

                            Text(
                                text = "Clock Accent Tint",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("#FFFFFF" to "White", "#6EE7B7" to "Emerald", "#FBBF24" to "Gold", "#38BDF8" to "Cyan", "#F472B6" to "Pink", "#A78BFA" to "Purple").forEach { (hex, name) ->
                                    val isSelected = settings.clockColorTintHex.equals(hex, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onUpdateSettings(settings.copy(clockColorTintHex = hex)) },
                                        label = { Text(name, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
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

                        Spacer(modifier = Modifier.height(12.dp))

                        SettingsCard(title = "Home Screen & System Widgets") {
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

                5 -> {
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

                6 -> {
                    // --- SYSTEM & PERMISSIONS & BACKUP ---
                    item {
                        SettingsCard(title = "System Permissions & Defaults") {
                            // 1. Set Default Launcher
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
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
                                border = androidx.compose.foundation.BorderStroke(
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
                                            text = if (isNotificationAccessGranted) "✓ Notification listener enabled" else "Required for live badges & chat heads",
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
                                border = androidx.compose.foundation.BorderStroke(
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

                            Spacer(modifier = Modifier.height(10.dp))

                            // 4. App Info & Other Permissions
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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

                        Spacer(modifier = Modifier.height(14.dp))

                        SettingsCard(title = "Backup & Maintenance") {
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
