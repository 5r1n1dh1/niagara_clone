package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.launch

sealed class AppDrawerItem {
    abstract val key: String

    data class CategoryCard(val name: String, val apps: List<AppItem>) : AppDrawerItem() {
        override val key: String = "cat_card_$name"
    }

    data class FolderWrapper(val folder: FolderItem) : AppDrawerItem() {
        override val key: String = "folder_${folder.id}"
    }

    data class LetterHeader(val letter: Char) : AppDrawerItem() {
        override val key: String = "header_$letter"
    }

    data class AppRow(val app: AppItem) : AppDrawerItem() {
        override val key: String = app.uniqueKey
    }
}

/**
 * Right-most Page: Lists all apps with strict left and right margins,
 * Wave Alphabet, and an adaptive search bar that smoothly slides to the top when focused/keyboard opened.
 */
@Composable
fun RightAllAppsScreen(
    allApps: List<AppItem>,
    folders: List<FolderItem>,
    isAppsLoading: Boolean = false,
    settings: LauncherSettings,
    selectedWaveLetter: Char?,
    onWaveLetterSelected: (Char?) -> Unit,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    onSwipeRightForPopup: ((AppItem) -> Unit)? = null,
    onReplyNotification: (packageName: String, text: String, isDualApp: Boolean) -> Unit = { _, _, _ -> },
    onDismissNotification: (packageName: String, isDualApp: Boolean) -> Unit = { _, _ -> },
    onToggleFolderExpanded: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val isLeftHanded = settings.leftHandedMode
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var filterText by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    val handleAppClick: (AppItem) -> Unit = { app ->
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        filterText = ""
        onAppClick(app)
    }

    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    val isKeyboardOpen = remember(imeInsets, density) {
        imeInsets.getBottom(density) > 0
    }

    val isSearchAtTop = isSearchFocused || filterText.isNotEmpty() || isKeyboardOpen

    val categorizedGroupMap = remember(allApps) {
        allApps.filter { !it.isHidden && it.customCategory.isNotBlank() }
            .groupBy { it.customCategory }
    }

    val activeWaveLetters = remember(selectedWaveLetter) {
        if (selectedWaveLetter == null) null
        else {
            val char = selectedWaveLetter
            when {
                char in 'A'..'Z' -> {
                    val prev = if (char > 'A') (char - 1) else null
                    val next = if (char < 'Z') (char + 1) else null
                    setOfNotNull(prev, char, next)
                }
                char == '★' -> {
                    setOf('★', 'A', 'B')
                }
                char == '#' -> {
                    setOf('Y', 'Z', '#')
                }
                else -> setOf(char)
            }
        }
    }

    val displayAppsList = remember(allApps, filterText) {
        if (filterText.isBlank()) {
            allApps.filter { !it.isHidden && it.customCategory.isBlank() }
                .sortedBy { it.label.lowercase() }
        } else {
            val q = filterText.trim().lowercase()
            allApps.filter { !it.isHidden && (it.label.lowercase().contains(q) || it.customCategory.lowercase().contains(q)) }
                .sortedBy { it.label.lowercase() }
        }
    }

    val flatList = remember(displayAppsList, folders, categorizedGroupMap, filterText) {
        val items = mutableListOf<AppDrawerItem>()
        if (filterText.isBlank()) {
            categorizedGroupMap.keys.sorted().forEach { cat ->
                val catApps = categorizedGroupMap[cat] ?: emptyList()
                items.add(AppDrawerItem.CategoryCard(cat, catApps))
            }
            folders.forEach { folder ->
                items.add(AppDrawerItem.FolderWrapper(folder))
            }
        }
        var lastHeader: Char? = null
        displayAppsList.forEach { app ->
            val initial = app.label.firstOrNull()?.uppercaseChar() ?: '#'
            val currentInitial = if (initial in 'A'..'Z') initial else '#'
            if (currentInitial != lastHeader) {
                lastHeader = currentInitial
                items.add(AppDrawerItem.LetterHeader(currentInitial))
            }
            items.add(AppDrawerItem.AppRow(app))
        }
        items
    }

    val letterToHeaderIndexMap = remember(flatList) {
        val map = mutableMapOf<Char, Int>()
        flatList.forEachIndexed { index, item ->
            if (item is AppDrawerItem.LetterHeader) {
                map[item.letter] = index
            }
        }
        map
    }

    FrostedGlassBackground(
        isFrostedTheme = (settings.themePreset == LauncherThemePreset.FROSTED_GLASS),
        blurStrength = settings.frostedBlurRadius,
        translucency = settings.frostedTranslucency,
        glassEffect = settings.frostedGlassEffect,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("right_all_apps_screen")
    ) {
        val sideMargin = 20.dp

        if (isAppsLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 160.dp, start = sideMargin, end = sideMargin),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Reading installed applications...",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Preparing alphabetical launcher list",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Search Bar Container
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isSearchAtTop) 0.88f else 0.55f),
                    tonalElevation = if (isSearchAtTop) 6.dp else 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = if (isSearchAtTop) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = filterText,
                            onValueChange = { filterText = it },
                            placeholder = { Text("Search apps or categories...", fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    val firstMatch = displayAppsList.firstOrNull()
                                    if (firstMatch != null) {
                                        handleAppClick(firstMatch)
                                    }
                                }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { isSearchFocused = it.isFocused }
                        )
                        if (filterText.isNotEmpty() || isSearchFocused) {
                            IconButton(
                                onClick = {
                                    filterText = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                onOpenSettings()
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("all_apps_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Flux Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Main App List & Fast Scroll
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = if (isLeftHanded) 36.dp else sideMargin,
                                end = if (isLeftHanded) sideMargin else 36.dp
                            )
                            .testTag("all_apps_list"),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
                    ) {
                        itemsIndexed(
                            items = flatList,
                            key = { _, item -> item.key },
                            contentType = { _, drawerItem -> drawerItem::class.java }
                        ) { _, drawerItem ->
                            Box(modifier = Modifier.animateItem()) {
                                when (drawerItem) {
                                    is AppDrawerItem.CategoryCard -> {
                                    val catApps = drawerItem.apps
                                    var isExpanded by remember { mutableStateOf(false) }

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { isExpanded = !isExpanded },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = drawerItem.name.uppercase(),
                                                        fontSize = (12 * settings.fontScale).sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.8.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer
                                                    ) {
                                                        Text(
                                                            text = "${catApps.size}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = if (isExpanded) "Hide" else "Show All",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            val appsToShow = if (isExpanded) catApps else catApps.take(4)
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                appsToShow.forEach { app ->
                                                    AppListItem(
                                                        app = app,
                                                        iconPackType = settings.iconPackType,
                                                        customIconSettings = settings.customIconSettings,
                                                        isLeftHanded = isLeftHanded,
                                                        fontScale = settings.fontScale,
                                                        iconScale = settings.appIconScale,
                                                        showBadges = settings.showAppNotificationBadges,
                                                        compactSpacing = settings.compactAppRowSpacing,
                                                        onAppClick = { handleAppClick(app) },
                                                        onAppLongClick = {
                                                            keyboardController?.hide()
                                                            focusManager.clearFocus(force = true)
                                                            onAppLongClick(app)
                                                        },
                                                        onReplyNotification = { text -> onReplyNotification(app.packageName, text, app.isDualApp) },
                                                        onDismissNotification = { onDismissNotification(app.packageName, app.isDualApp) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                is AppDrawerItem.FolderWrapper -> {
                                    val folder = drawerItem.folder
                                    val appsInFolder = allApps.filter { it.packageName in folder.appPackageNames }
                                    FolderInlineItem(
                                        folder = folder,
                                        appsInFolder = appsInFolder,
                                        iconPackType = settings.iconPackType,
                                        isLeftHanded = isLeftHanded,
                                        fontScale = settings.fontScale,
                                        onToggleExpand = { onToggleFolderExpanded(folder.id) },
                                        onAppClick = handleAppClick,
                                        onAppLongClick = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus(force = true)
                                            onAppLongClick(it)
                                        },
                                        onSwipeRightForPopup = onSwipeRightForPopup
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                is AppDrawerItem.LetterHeader -> {
                                    Text(
                                        text = drawerItem.letter.toString(),
                                        fontSize = (13 * settings.fontScale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp, start = 4.dp)
                                    )
                                }

                                is AppDrawerItem.AppRow -> {
                                    val app = drawerItem.app
                                    AppListItem(
                                        app = app,
                                        iconPackType = settings.iconPackType,
                                        customIconSettings = settings.customIconSettings,
                                        isLeftHanded = isLeftHanded,
                                        fontScale = settings.fontScale,
                                        iconScale = settings.appIconScale,
                                        showBadges = settings.showAppNotificationBadges,
                                        compactSpacing = settings.compactAppRowSpacing,
                                        onAppClick = { handleAppClick(app) },
                                        onAppLongClick = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus(force = true)
                                            onAppLongClick(app)
                                        },
                                        onReplyNotification = { text -> onReplyNotification(app.packageName, text, app.isDualApp) },
                                        onDismissNotification = { onDismissNotification(app.packageName, app.isDualApp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Niagara Wave Alphabet Scrollbar
                if (filterText.isBlank()) {
                        val scope = rememberCoroutineScope()
                        val alignModifier = if (isLeftHanded) Modifier.align(Alignment.CenterStart) else Modifier.align(Alignment.CenterEnd)

                        WaveAlphabet(
                            isLeftHanded = isLeftHanded,
                            selectedLetter = selectedWaveLetter,
                            onLetterSelected = { letter ->
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                onWaveLetterSelected(letter)
                            },
                            onLetterConfirmed = { letter ->
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                val targetIndex = when (letter) {
                                    '★' -> 0
                                    '#' -> (flatList.indices.lastOrNull() ?: 0)
                                    else -> {
                                        letterToHeaderIndexMap[letter] ?: run {
                                            val headerLetters = letterToHeaderIndexMap.keys.toList()
                                            if (headerLetters.isNotEmpty()) {
                                                val closestChar = headerLetters.minByOrNull { kotlin.math.abs(it.code - letter.code) }
                                                if (closestChar != null) letterToHeaderIndexMap[closestChar] else null
                                            } else null
                                        }
                                    }
                                }
                                if (targetIndex != null && targetIndex in flatList.indices) {
                                    scope.launch {
                                        listState.scrollToItem(targetIndex)
                                    }
                                }
                            },
                            modifier = alignModifier
                                .fillMaxHeight()
                                .padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
