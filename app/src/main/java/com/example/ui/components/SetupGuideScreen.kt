package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupGuideScreen(
    installedApps: List<AppItem>,
    isAppsLoading: Boolean,
    availableWidgets: List<WidgetType>,
    currentSettings: LauncherSettings,
    initialFavorites: List<AppItem>,
    initialCarouselWidgets: List<WidgetType>,
    initialLeftWidgets: List<CustomWidgetConfig>,
    onCompleteSetup: (
        favorites: List<String>,
        carouselWidgets: List<WidgetType>,
        leftWidgets: List<CustomWidgetConfig>,
        themePreset: LauncherThemePreset,
        leftHanded: Boolean,
        doubleTapToSleep: Boolean
    ) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 6 // 0 to 5

    // Step state
    var selectedCarousel by remember(initialCarouselWidgets) {
        mutableStateOf(
            if (initialCarouselWidgets.isNotEmpty()) initialCarouselWidgets
            else listOf(WidgetType.WEATHER_FORECAST)
        )
    }

    var selectedFavoritePackages by remember(initialFavorites) {
        mutableStateOf(
            if (initialFavorites.isNotEmpty()) initialFavorites.map { it.packageName }.toSet()
            else emptySet()
        )
    }

    var selectedLeftWidgetTypes by remember(initialLeftWidgets) {
        mutableStateOf(
            if (initialLeftWidgets.isNotEmpty()) initialLeftWidgets.map { it.type }.toSet()
            else setOf(WidgetType.WEATHER_FORECAST, WidgetType.QUICK_NOTE, WidgetType.NEWS_FEED)
        )
    }

    var selectedTheme by remember(currentSettings.themePreset) {
        mutableStateOf(currentSettings.themePreset)
    }

    var isLeftHanded by remember(currentSettings.leftHandedMode) {
        mutableStateOf(currentSettings.leftHandedMode)
    }

    var doubleTapSleep by remember(currentSettings.doubleTapToSleep) {
        mutableStateOf(currentSettings.doubleTapToSleep)
    }

    var appFilterQuery by remember { mutableStateOf("") }

    // Auto-prepopulate favorites from installed apps once available if user hasn't selected any yet
    LaunchedEffect(installedApps) {
        if (selectedFavoritePackages.isEmpty() && installedApps.isNotEmpty()) {
            val essentials = installedApps.filter { app ->
                val lower = (app.packageName + " " + app.label).lowercase()
                lower.contains("dialer") || lower.contains("phone") ||
                lower.contains("message") || lower.contains("sms") ||
                lower.contains("chrome") || lower.contains("browser") ||
                lower.contains("camera") || lower.contains("maps") ||
                lower.contains("whatsapp") || lower.contains("settings")
            }.take(6).map { it.packageName }.toSet()

            if (essentials.isNotEmpty()) {
                selectedFavoritePackages = essentials
            } else {
                selectedFavoritePackages = installedApps.take(5).map { it.packageName }.toSet()
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("setup_guide_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Navigation & Step Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 0) {
                    IconButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.testTag("setup_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Step dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until totalSteps) {
                        Box(
                            modifier = Modifier
                                .size(if (i == currentStep) 18.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == currentStep) MaterialTheme.colorScheme.primary
                                    else if (i < currentStep) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.testTag("setup_skip_button")
                ) {
                    Text(
                        text = if (currentStep == totalSteps - 1) "Close" else "Skip",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Animated Step Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentStep) {
                    0 -> StepWelcome(
                        installedAppsCount = installedApps.size,
                        isAppsLoading = isAppsLoading,
                        onNext = { currentStep = 1 }
                    )
                    1 -> StepCarouselWidgets(
                        availableWidgets = availableWidgets,
                        selectedWidgets = selectedCarousel,
                        onToggleWidget = { widget ->
                            selectedCarousel = if (selectedCarousel.contains(widget)) {
                                if (selectedCarousel.size > 1) selectedCarousel - widget else selectedCarousel
                            } else {
                                if (selectedCarousel.size < 5) selectedCarousel + widget else selectedCarousel
                            }
                        },
                        onMoveUp = { index ->
                            if (index > 0) {
                                val list = selectedCarousel.toMutableList()
                                val item = list.removeAt(index)
                                list.add(index - 1, item)
                                selectedCarousel = list
                            }
                        },
                        onMoveDown = { index ->
                            if (index < selectedCarousel.size - 1) {
                                val list = selectedCarousel.toMutableList()
                                val item = list.removeAt(index)
                                list.add(index + 1, item)
                                selectedCarousel = list
                            }
                        },
                        onNext = { currentStep = 2 }
                    )
                    2 -> StepFavoritesSelection(
                        installedApps = installedApps,
                        isAppsLoading = isAppsLoading,
                        selectedPackageNames = selectedFavoritePackages,
                        filterQuery = appFilterQuery,
                        onFilterChange = { appFilterQuery = it },
                        iconPackType = currentSettings.iconPackType,
                        customIconSettings = currentSettings.customIconSettings,
                        onToggleFavorite = { pkg ->
                            selectedFavoritePackages = if (selectedFavoritePackages.contains(pkg)) {
                                selectedFavoritePackages - pkg
                            } else {
                                selectedFavoritePackages + pkg
                            }
                        },
                        onAutoSelectEssentials = {
                            val essentials = installedApps.filter { app ->
                                val lower = (app.packageName + " " + app.label).lowercase()
                                lower.contains("dialer") || lower.contains("phone") ||
                                lower.contains("message") || lower.contains("sms") ||
                                lower.contains("chrome") || lower.contains("browser") ||
                                lower.contains("camera") || lower.contains("maps")
                            }.map { it.packageName }.toSet()
                            if (essentials.isNotEmpty()) {
                                selectedFavoritePackages = essentials
                            }
                        },
                        onNext = { currentStep = 3 }
                    )
                    3 -> StepLeftWidgetsFeed(
                        availableWidgets = availableWidgets,
                        selectedTypes = selectedLeftWidgetTypes,
                        onToggleType = { type ->
                            selectedLeftWidgetTypes = if (selectedLeftWidgetTypes.contains(type)) {
                                if (selectedLeftWidgetTypes.size > 1) selectedLeftWidgetTypes - type else selectedLeftWidgetTypes
                            } else {
                                selectedLeftWidgetTypes + type
                            }
                        },
                        onNext = { currentStep = 4 }
                    )
                    4 -> StepPersonalization(
                        selectedTheme = selectedTheme,
                        onSelectTheme = { selectedTheme = it },
                        isLeftHanded = isLeftHanded,
                        onToggleLeftHanded = { isLeftHanded = it },
                        doubleTapSleep = doubleTapSleep,
                        onToggleDoubleTap = { doubleTapSleep = it },
                        onNext = { currentStep = 5 }
                    )
                    5 -> StepSummaryComplete(
                        carouselCount = selectedCarousel.size,
                        favoritesCount = selectedFavoritePackages.size,
                        leftWidgetsCount = selectedLeftWidgetTypes.size,
                        selectedTheme = selectedTheme,
                        isLeftHanded = isLeftHanded,
                        onFinish = {
                            val leftConfigs = selectedLeftWidgetTypes.mapIndexed { idx, type ->
                                CustomWidgetConfig(
                                    id = "w_onboard_${type.name.lowercase()}_$idx",
                                    type = type,
                                    title = type.displayName,
                                    content = when (type) {
                                        WidgetType.QUICK_NOTE -> "Tap edit to write quick thoughts, reminders, or grocery items."
                                        else -> ""
                                    }
                                )
                            }
                            onCompleteSetup(
                                selectedFavoritePackages.toList(),
                                selectedCarousel,
                                leftConfigs,
                                selectedTheme,
                                isLeftHanded,
                                doubleTapSleep
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepWelcome(
    installedAppsCount: Int,
    isAppsLoading: Boolean,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Welcome to Flux Launcher",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "A calm, fluid, and distraction-free Android launcher designed for rapid thumb reach and clarity.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Layout Highlights Cards
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LayoutFeatureCard(
                    icon = Icons.Outlined.Widgets,
                    title = "Interactive 4x2 Carousel",
                    subtitle = "Swipe through weather, calendar, and music widgets right above your favorites."
                )
                LayoutFeatureCard(
                    icon = Icons.Outlined.Star,
                    title = "Curated Home Favorites",
                    subtitle = "Fast thumb access to your essential apps without home screen clutter."
                )
                LayoutFeatureCard(
                    icon = Icons.Outlined.SortByAlpha,
                    title = "Wave Alphabet Drawer",
                    subtitle = "Fluid alphabet scrollbar on the right to jump directly to any app in an instant."
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Device Status
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isAppsLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Scanning installed applications...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$installedAppsCount apps detected & ready",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("setup_start_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Begin Setup",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LayoutFeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun StepCarouselWidgets(
    availableWidgets: List<WidgetType>,
    selectedWidgets: List<WidgetType>,
    onToggleWidget: (WidgetType) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
    ) {
        Text(
            text = "Home Screen Widgets",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Select up to 5 widgets for the swipeable 4x2 carousel above your favorites. Selected: ${selectedWidgets.size}/5",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        // Selected Carousel Order Preview
        if (selectedWidgets.isNotEmpty()) {
            Text(
                text = "Carousel Order (Swipe Sequence)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                items(selectedWidgets) { widget ->
                    val index = selectedWidgets.indexOf(widget)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${index + 1}. ${widget.iconEmoji} ${widget.displayName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(availableWidgets) { widget ->
                val isSelected = selectedWidgets.contains(widget)
                val index = selectedWidgets.indexOf(widget)

                Card(
                    onClick = { onToggleWidget(widget) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            if (isSelected) listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
                            else listOf(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        )
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
                        Text(
                            text = widget.iconEmoji,
                            fontSize = 24.sp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = widget.displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                            Text(
                                text = widget.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isSelected) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onMoveUp(index) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move Up",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onMoveDown(index) },
                                    enabled = index < selectedWidgets.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move Down",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            Checkbox(
                                checked = false,
                                onCheckedChange = { onToggleWidget(widget) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("setup_carousel_next_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Next: Choose Favorites (${selectedWidgets.size} widgets)",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun StepFavoritesSelection(
    installedApps: List<AppItem>,
    isAppsLoading: Boolean,
    selectedPackageNames: Set<String>,
    filterQuery: String,
    onFilterChange: (String) -> Unit,
    iconPackType: IconPackType,
    customIconSettings: CustomIconPackSettings,
    onToggleFavorite: (String) -> Unit,
    onAutoSelectEssentials: () -> Unit,
    onNext: () -> Unit
) {
    val filteredApps = remember(installedApps, filterQuery) {
        if (filterQuery.isBlank()) installedApps
        else {
            val q = filterQuery.trim().lowercase()
            installedApps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
    ) {
        Text(
            text = "Curate Home Favorites",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Pick the apps you open most often. They appear directly on your Center Home screen. (${selectedPackageNames.size} chosen)",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )

        // Filter and Auto-Pick Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filterQuery,
                onValueChange = onFilterChange,
                placeholder = { Text("Search installed apps...", fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { onFilterChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            )

            FilledTonalButton(
                onClick = onAutoSelectEssentials,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Essentials", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isAppsLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loading installed apps...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredApps, key = { it.uniqueKey }) { app ->
                    val isFav = selectedPackageNames.contains(app.packageName)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isFav) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onToggleFavorite(app.packageName) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppIconRenderer(
                            app = app,
                            iconPackType = iconPackType,
                            customIconSettings = customIconSettings,
                            size = 36.dp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                fontSize = 14.sp,
                                fontWeight = if (isFav) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = app.packageName,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { onToggleFavorite(app.packageName) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (isFav) "Remove Favorite" else "Add Favorite",
                                tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("setup_favorites_next_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Next: Left Page Widgets (${selectedPackageNames.size} favorites)",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun StepLeftWidgetsFeed(
    availableWidgets: List<WidgetType>,
    selectedTypes: Set<WidgetType>,
    onToggleType: (WidgetType) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
    ) {
        Text(
            text = "Left Widgets Feed",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Select utility widgets to pin on your left dashboard screen. You can scroll through these cards anytime.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(availableWidgets) { widget ->
                val isSelected = selectedTypes.contains(widget)

                Card(
                    onClick = { onToggleType(widget) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            if (isSelected) listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
                            else listOf(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        )
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
                        Text(
                            text = widget.iconEmoji,
                            fontSize = 24.sp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = widget.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = widget.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isSelected,
                            onCheckedChange = { onToggleType(widget) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("setup_left_widgets_next_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Next: Personalize Theme (${selectedTypes.size} widgets)",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun StepPersonalization(
    selectedTheme: LauncherThemePreset,
    onSelectTheme: (LauncherThemePreset) -> Unit,
    isLeftHanded: Boolean,
    onToggleLeftHanded: (Boolean) -> Unit,
    doubleTapSleep: Boolean,
    onToggleDoubleTap: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
    ) {
        Text(
            text = "Theme & Ergonomics",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Personalize colors and layout ergonomics for effortless single-handed reach.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Color Theme Preset",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(LauncherThemePreset.entries) { preset ->
                val isSelected = preset == selectedTheme
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectTheme(preset) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(preset.primary))
                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(preset.background))
                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(preset.surface))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectTheme(preset) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ergonomics & Gestures",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PanTool,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Left-Handed Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Places the Wave Alphabet scrollbar on the left edge for left thumb reach.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isLeftHanded,
                        onCheckedChange = onToggleLeftHanded
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Double Tap to Sleep",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Double tap on empty home screen space to turn off screen instantly.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = doubleTapSleep,
                        onCheckedChange = { isChecked ->
                            onToggleDoubleTap(isChecked)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("setup_personalize_next_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Next: Review Summary",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun StepSummaryComplete(
    carouselCount: Int,
    favoritesCount: Int,
    leftWidgetsCount: Int,
    selectedTheme: LauncherThemePreset,
    isLeftHanded: Boolean,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You're All Set!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Your customized launcher layout is ready to go.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Setup summary card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SummaryRow(
                        icon = Icons.Outlined.ViewCarousel,
                        title = "Home 4x2 Carousel",
                        value = "$carouselCount widgets configured"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SummaryRow(
                        icon = Icons.Outlined.Star,
                        title = "Center Favorites",
                        value = "$favoritesCount apps pinned"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SummaryRow(
                        icon = Icons.Outlined.Widgets,
                        title = "Left Utility Feed",
                        value = "$leftWidgetsCount widgets active"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SummaryRow(
                        icon = Icons.Outlined.Palette,
                        title = "Visual Theme",
                        value = selectedTheme.displayName
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SummaryRow(
                        icon = Icons.Outlined.PanTool,
                        title = "Ergonomics",
                        value = if (isLeftHanded) "Left-Handed" else "Right-Handed"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tip: You can change any of these settings anytime by swiping up on the home screen or tapping the gear icon in All Apps.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("setup_finish_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Launch Flux Launcher ✨",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun SummaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
