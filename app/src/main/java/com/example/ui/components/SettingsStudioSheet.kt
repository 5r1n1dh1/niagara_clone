package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.util.DefaultLauncherHelper
import com.example.util.IntegrationCategory
import com.example.util.WidgetIntegrationsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsStudioSheet(
    settings: LauncherSettings,
    allApps: List<AppItem> = emptyList(),
    widgetIntegrations: WidgetIntegrationsManager? = null,
    onUpdateSettings: (LauncherSettings) -> Unit,
    onExportBackup: () -> String,
    onImportBackup: (String) -> Boolean,
    onResetDefaults: () -> Unit,
    onOpenSetupGuide: () -> Unit = {},
    onOpenSystemWidgetPicker: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var backupDialogText by remember { mutableStateOf<String?>(null) }
    var importDialogText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

    // State for app integration picker dialog
    var selectedIntegrationForPicker by remember { mutableStateOf<IntegrationCategory?>(null) }
    var integrationSearchFilter by remember { mutableStateOf("") }

    // Check system status
    val isDefaultLauncher = remember(context) {
        DefaultLauncherHelper.isDefaultLauncher(context)
    }
    val isNotificationAccessGranted = remember(context) {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        flat != null && flat.contains(context.packageName)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("settings_studio_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Title Header
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
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Flux Launcher Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Customization, widgets & system controls",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text("Done", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 0.1: Default Launcher Configuration
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDefaultLauncher)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDefaultLauncher) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                            else MaterialTheme.colorScheme.primary
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isDefaultLauncher) Icons.Filled.CheckCircle else Icons.Filled.Home,
                                        contentDescription = null,
                                        tint = if (isDefaultLauncher) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isDefaultLauncher) "Default Home App Active" else "Set Flux as Default Launcher",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isDefaultLauncher)
                                            "Flux is set as your default Android launcher"
                                        else
                                            "Make Flux handle your home button & navigation",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (!isDefaultLauncher) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { DefaultLauncherHelper.openSetDefaultLauncher(context) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Set as Default Launcher", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Section 0.2: Live Notifications Access Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isNotificationAccessGranted) "Live Notifications Active" else "Enable Live Notifications",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isNotificationAccessGranted)
                                        "Real-time app notification dots & direct quick replies"
                                    else
                                        "Grant access so app dots & notification shade update live",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!isNotificationAccessGranted) {
                                FilledTonalButton(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Grant", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Section 0.3: Widget App Integrations
                item {
                    SettingsSectionTitle("Widget App Integrations")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Choose which installed applications launch when clicking widget cards:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            IntegrationCategory.entries.forEach { category ->
                                val currentAppLabel = remember(category, allApps) {
                                    widgetIntegrations?.getAppLabel(category, context.packageManager)
                                        ?: "Auto-Detect Default"
                                }
                                val customPkg = widgetIntegrations?.getPackage(category)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            integrationSearchFilter = ""
                                            selectedIntegrationForPicker = category
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = when (category) {
                                                IntegrationCategory.WEATHER -> Icons.Filled.WbSunny
                                                IntegrationCategory.CALENDAR -> Icons.Filled.CalendarMonth
                                                IntegrationCategory.MUSIC -> Icons.Filled.MusicNote
                                                IntegrationCategory.NOTES -> Icons.Filled.EditNote
                                                IntegrationCategory.CLOCK -> Icons.Filled.Schedule
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = category.displayName,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = currentAppLabel,
                                                fontSize = 12.sp,
                                                color = if (customPkg != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (category != IntegrationCategory.entries.last()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = {
                                    onDismiss()
                                    onOpenSystemWidgetPicker()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Widgets, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add System Android Widget")
                            }
                        }
                    }
                }

                // Section 0.4: Setup / Start Up Guide Banner
                item {
                    Card(
                        onClick = {
                            onDismiss()
                            onOpenSetupGuide()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Launcher Setup Guide",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Reconfigure widgets, favorites, and theme anytime",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                // Section 1: Gestures & Navigation
                item {
                    SettingsSectionTitle("Gestures & Interaction")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            SettingsToggleRow(
                                title = "Swipe Down for Notification Center",
                                subtitle = "Swipe downward anywhere on home screens to pull shade",
                                checked = settings.swipeDownForNotificationCenter,
                                onCheckedChange = { onUpdateSettings(settings.copy(swipeDownForNotificationCenter = it)) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            SettingsToggleRow(
                                title = "Left-Handed Mode",
                                subtitle = "Mirrors Wave Alphabet & gestures to the left edge",
                                checked = settings.leftHandedMode,
                                onCheckedChange = { onUpdateSettings(settings.copy(leftHandedMode = it)) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            SettingsToggleRow(
                                title = "Double-Tap to Sleep",
                                subtitle = "Double-tap empty wallpaper to turn off and lock display",
                                checked = settings.doubleTapToSleep,
                                onCheckedChange = { isChecked ->
                                    onUpdateSettings(settings.copy(doubleTapToSleep = isChecked))
                                    if (isChecked && !com.example.service.FluxAccessibilityService.isServiceRunning()) {
                                        com.example.service.FluxAccessibilityService.openAccessibilitySettings(context)
                                    }
                                }
                            )
                            if (settings.doubleTapToSleep && !com.example.service.FluxAccessibilityService.isServiceRunning()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = {
                                        com.example.service.FluxAccessibilityService.openAccessibilitySettings(context)
                                    },
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
                                title = "Hide Status Bar",
                                subtitle = "Expands screen real-estate for clean immersion",
                                checked = settings.hideStatusBar,
                                onCheckedChange = { onUpdateSettings(settings.copy(hideStatusBar = it)) }
                            )
                        }
                    }
                }

                // Section 2: Haptic Engine
                item {
                    SettingsSectionTitle("Haptic Feedback Engine")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            SettingsToggleRow(
                                title = "Haptic Wave Vibrations",
                                subtitle = "Tactile kinetic feedback when dragging Wave Alphabet or tapping tiles",
                                checked = settings.hapticFeedback,
                                onCheckedChange = { onUpdateSettings(settings.copy(hapticFeedback = it)) }
                            )
                            if (settings.hapticFeedback) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Vibration Intensity: ${when (settings.waveAlphabetHapticIntensity) {
                                        1 -> "Subtle (Soft Tick)"
                                        2 -> "Standard (Firm Click)"
                                        else -> "Strong (Tactile Pulse)"
                                    }}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Slider(
                                    value = settings.waveAlphabetHapticIntensity.toFloat(),
                                    onValueChange = { onUpdateSettings(settings.copy(waveAlphabetHapticIntensity = it.toInt())) },
                                    valueRange = 1f..3f,
                                    steps = 1
                                )
                            }
                        }
                    }
                }

                // Section 3: App List & Dock Customization
                item {
                    SettingsSectionTitle("App Grid & Dock Sizing")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "App Icon Size: ${(settings.appIconScale * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = settings.appIconScale,
                                onValueChange = { onUpdateSettings(settings.copy(appIconScale = it)) },
                                valueRange = 0.8f..1.3f,
                                steps = 4
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            SettingsToggleRow(
                                title = "Show Notification Badges",
                                subtitle = "Display active badge count on app icons",
                                checked = settings.showAppNotificationBadges,
                                onCheckedChange = { onUpdateSettings(settings.copy(showAppNotificationBadges = it)) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            SettingsToggleRow(
                                title = "Compact List Spacing",
                                subtitle = "Dense spacing to fit more apps on screen",
                                checked = settings.compactAppRowSpacing,
                                onCheckedChange = { onUpdateSettings(settings.copy(compactAppRowSpacing = it)) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            SettingsToggleRow(
                                title = "Show Dock Labels",
                                subtitle = "Display text label under dock icons",
                                checked = settings.showDockLabels,
                                onCheckedChange = { onUpdateSettings(settings.copy(showDockLabels = it)) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            SettingsToggleRow(
                                title = "Frosted Glass Dock Bar",
                                subtitle = "Translucent blurred container background for dock",
                                checked = settings.dockBlurBackground,
                                onCheckedChange = { onUpdateSettings(settings.copy(dockBlurBackground = it)) }
                            )
                        }
                    }
                }

                // Section 2: Anycons & Iconography
                item {
                    SettingsSectionTitle("Anycons Icon Family")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            IconPackType.entries.forEach { pack ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onUpdateSettings(settings.copy(iconPackType = pack)) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = pack.displayName,
                                            fontWeight = if (settings.iconPackType == pack) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = when (pack) {
                                                IconPackType.ANYCONS_DOTS -> "Monochrome minimal dots • zero dopamine branding"
                                                IconPackType.ANYCONS_STICKER -> "High-contrast clean badge sticker"
                                                IconPackType.ANYCONS_MINIMAL -> "Pure outline geometric vector glyphs"
                                                IconPackType.ANYCONS_COLORFUL -> "Pastel duotone gradient badges"
                                                IconPackType.MATERIAL -> "Modern squircle adaptive styling"
                                                IconPackType.DEFAULT_APP -> "Stock Android system package icons"
                                                IconPackType.CUSTOM -> "Custom Icon Pack • Presets, shapes, and 3rd-party packs"
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    RadioButton(
                                        selected = settings.iconPackType == pack,
                                        onClick = { onUpdateSettings(settings.copy(iconPackType = pack)) }
                                    )
                                }
                            }

                            // 4. Custom Icon Pack Settings Panel
                            if (settings.iconPackType == IconPackType.CUSTOM) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Custom Pack Presets",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val customPresets = listOf(
                                    CustomPackPreset.WHICONS_WHITE to "Whicons (White)",
                                    CustomPackPreset.DELTA_PASTEL to "Delta (Pastel)",
                                    CustomPackPreset.LINEBIT_NEON to "Linebit (Neon)",
                                    CustomPackPreset.CHARCOAL_STEALTH to "Charcoal (Stealth)",
                                    CustomPackPreset.CANDY_VIVID to "CandyCons (Vivid)",
                                    CustomPackPreset.CUSTOM_STUDIO to "Studio Tint"
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    customPresets.chunked(2).forEach { rowPresets ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            rowPresets.forEach { (preset, label) ->
                                                val isSelected = settings.customIconSettings.packPreset == preset
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        onUpdateSettings(
                                                            settings.copy(
                                                                customIconSettings = settings.customIconSettings.copy(packPreset = preset)
                                                            )
                                                        )
                                                    },
                                                    label = { Text(label, fontSize = 11.sp) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Icon Shape Mask",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val shapes = listOf(
                                    IconShape.CIRCLE to "Circle",
                                    IconShape.ROUNDED_SQUIRCLE to "Squircle",
                                    IconShape.SMOOTH_SQUARE to "Square",
                                    IconShape.TEARDROP to "Teardrop",
                                    IconShape.HEXAGON to "Hexagon"
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    shapes.take(3).forEach { (shape, name) ->
                                        FilterChip(
                                            selected = settings.customIconSettings.iconShape == shape,
                                            onClick = {
                                                onUpdateSettings(
                                                    settings.copy(
                                                        customIconSettings = settings.customIconSettings.copy(iconShape = shape)
                                                    )
                                                )
                                            },
                                            label = { Text(name, fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    shapes.drop(3).forEach { (shape, name) ->
                                        FilterChip(
                                            selected = settings.customIconSettings.iconShape == shape,
                                            onClick = {
                                                onUpdateSettings(
                                                    settings.copy(
                                                        customIconSettings = settings.customIconSettings.copy(iconShape = shape)
                                                    )
                                                )
                                            },
                                            label = { Text(name, fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // If Custom Studio preset is selected, show Color & Opacity
                                if (settings.customIconSettings.packPreset == CustomPackPreset.CUSTOM_STUDIO) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Studio Color Tint",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val colors = listOf(
                                        "#FFFFFF" to Color.White,
                                        "#64B5F6" to Color(0xFF64B5F6),
                                        "#81C784" to Color(0xFF81C784),
                                        "#FFB74D" to Color(0xFFFFB74D),
                                        "#BA68C8" to Color(0xFFBA68C8),
                                        "#FF5252" to Color(0xFFFF5252),
                                        "#00E5FF" to Color(0xFF00E5FF)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        colors.forEach { (hex, color) ->
                                            val isChosen = settings.customIconSettings.customColorHex.equals(hex, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .clickable {
                                                        onUpdateSettings(
                                                            settings.copy(
                                                                customIconSettings = settings.customIconSettings.copy(customColorHex = hex)
                                                            )
                                                        )
                                                    }
                                                    .then(
                                                        if (isChosen) androidx.compose.ui.Modifier.padding(2.dp) else androidx.compose.ui.Modifier
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isChosen) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = if (hex == "#FFFFFF") Color.Black else Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Background Opacity", fontSize = 12.sp)
                                        Text(text = "${(settings.customIconSettings.backgroundOpacity * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = settings.customIconSettings.backgroundOpacity,
                                        onValueChange = {
                                            onUpdateSettings(
                                                settings.copy(
                                                    customIconSettings = settings.customIconSettings.copy(backgroundOpacity = it)
                                                )
                                            )
                                        },
                                        valueRange = 0.0f..1.0f
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Theme Studio
                item {
                    SettingsSectionTitle("Theme Studio")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            LauncherThemePreset.entries.forEach { preset ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onUpdateSettings(settings.copy(themePreset = preset)) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(preset.primary)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = preset.displayName,
                                            fontWeight = if (settings.themePreset == preset) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp
                                        )
                                    }
                                    RadioButton(
                                        selected = settings.themePreset == preset,
                                        onClick = { onUpdateSettings(settings.copy(themePreset = preset)) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3.1: Wallpaper & Dynamic Canvas
                item {
                    SettingsSectionTitle("Wallpaper & Dynamic Canvas")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            WallpaperStyle.entries.forEach { style ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onUpdateSettings(settings.copy(wallpaperStyle = style)) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = style.displayName,
                                        fontWeight = if (settings.wallpaperStyle == style) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                    RadioButton(
                                        selected = settings.wallpaperStyle == style,
                                        onClick = { onUpdateSettings(settings.copy(wallpaperStyle = style)) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: Typography Engine & Scaling
                item {
                    SettingsSectionTitle("Custom Typography Engine")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            AppFontFamily.entries.forEach { font ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onUpdateSettings(settings.copy(customFont = font)) }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = font.displayName,
                                        fontWeight = if (settings.customFont == font) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                    RadioButton(
                                        selected = settings.customFont == font,
                                        onClick = { onUpdateSettings(settings.copy(customFont = font)) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Text Scale: ${(settings.fontScale * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = settings.fontScale,
                                onValueChange = { onUpdateSettings(settings.copy(fontScale = it)) },
                                valueRange = 0.8f..1.3f,
                                steps = 4
                            )
                        }
                    }
                }

                // Section 5: Clock Personalization
                item {
                    SettingsSectionTitle("Personalized Clock Styles")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            ClockStyle.entries.forEach { style ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onUpdateSettings(settings.copy(clockStyle = style)) }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = style.displayName,
                                        fontWeight = if (settings.clockStyle == style) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                    RadioButton(
                                        selected = settings.clockStyle == style,
                                        onClick = { onUpdateSettings(settings.copy(clockStyle = style)) }
                                    )
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            SettingsToggleRow(
                                title = "Show Clock Header",
                                subtitle = "Display top clock header on home screen (or use Clock Widget)",
                                checked = settings.showClockHeader,
                                onCheckedChange = { onUpdateSettings(settings.copy(showClockHeader = it)) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            SettingsToggleRow(
                                title = "Display Date & Agenda",
                                subtitle = "Show active date and upcoming schedule in header",
                                checked = settings.showDate,
                                onCheckedChange = { onUpdateSettings(settings.copy(showDate = it)) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            SettingsToggleRow(
                                title = "Display Seconds",
                                subtitle = "Tick seconds live in digital/analog header",
                                checked = settings.showSeconds,
                                onCheckedChange = { onUpdateSettings(settings.copy(showSeconds = it)) }
                            )
                        }
                    }
                }

                // Section 6: Audio Trigger & Google Discover
                item {
                    SettingsSectionTitle("Smart System Integrations")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            SettingsToggleRow(
                                title = "Plug-and-Play Audio Trigger",
                                subtitle = "Automatically float music & podcast apps when headphones connect",
                                checked = settings.audioAppAutoSurfacing,
                                onCheckedChange = { onUpdateSettings(settings.copy(audioAppAutoSurfacing = it)) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            SettingsToggleRow(
                                title = "Google Discover Swipe",
                                subtitle = "Swipe right to open Google News & Discover overlay",
                                checked = settings.isGoogleDiscoverEnabled,
                                onCheckedChange = { onUpdateSettings(settings.copy(isGoogleDiscoverEnabled = it)) }
                            )
                        }
                    }
                }

                // Section 7: Backup & Restore
                item {
                    SettingsSectionTitle("Backup & Restore")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val json = onExportBackup()
                                        backupDialogText = json
                                        clipboard.setText(AnnotatedString(json))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export JSON")
                                }

                                OutlinedButton(
                                    onClick = { showImportDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import JSON")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            TextButton(
                                onClick = onResetDefaults,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reset Launcher to Defaults")
                            }
                        }
                    }
                }
            }
        }
    }

    if (backupDialogText != null) {
        AlertDialog(
            onDismissRequest = { backupDialogText = null },
            title = { Text("Backup Exported") },
            text = { Text("Complete Niagara configuration exported and copied to clipboard successfully!") },
            confirmButton = {
                Button(onClick = { backupDialogText = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Backup") },
            text = {
                Column {
                    Text("Paste your exported Niagara JSON config below:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importDialogText,
                        onValueChange = { importDialogText = it },
                        placeholder = { Text("{ \"favorites\": [...] }") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (onImportBackup(importDialogText)) {
                        showImportDialog = false
                        importDialogText = ""
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

    if (selectedIntegrationForPicker != null) {
        val category = selectedIntegrationForPicker!!
        AlertDialog(
            onDismissRequest = { selectedIntegrationForPicker = null },
            title = {
                Text("Select ${category.displayName} App")
            },
            text = {
                val filteredApps = remember(allApps, integrationSearchFilter) {
                    if (integrationSearchFilter.isBlank()) allApps
                    else allApps.filter { it.label.contains(integrationSearchFilter, ignoreCase = true) }
                }

                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                    OutlinedTextField(
                        value = integrationSearchFilter,
                        onValueChange = { integrationSearchFilter = it },
                        placeholder = { Text("Search installed apps...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        widgetIntegrations?.setPackage(category, null)
                                        selectedIntegrationForPicker = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Auto-Detect System Default", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Uses system default or recommended handler", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        items(filteredApps.size) { idx ->
                            val app = filteredApps[idx]
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        widgetIntegrations?.setPackage(category, app.packageName)
                                        selectedIntegrationForPicker = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIconRenderer(
                                        app = app,
                                        iconPackType = settings.iconPackType,
                                        size = 28.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(app.packageName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedIntegrationForPicker = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
