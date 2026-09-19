package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.launch

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
    onReplyNotification: (packageName: String, text: String) -> Unit,
    onDismissNotification: (packageName: String) -> Unit,
    onToggleFolderExpanded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val isLeftHanded = settings.leftHandedMode

    var filterText by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    val isKeyboardOpen = remember(imeInsets, density) {
        imeInsets.getBottom(density) > 0
    }

    val isSearchAtTop = isSearchFocused || filterText.isNotEmpty() || isKeyboardOpen

    val nonHiddenApps = remember(allApps, filterText) {
        val base = allApps.filter { !it.isHidden }
        if (filterText.isBlank()) {
            base.sortedBy { it.label.lowercase() }
        } else {
            val q = filterText.trim().lowercase()
            base.filter { it.label.lowercase().contains(q) || it.customCategory.lowercase().contains(q) }
                .sortedBy { it.label.lowercase() }
        }
    }

    FrostedGlassBackground(
        isFrostedTheme = (settings.themePreset == LauncherThemePreset.FROSTED_GLASS),
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
                // When search is active or keyboard open, the search bar slides/sits gracefully at the TOP
                AnimatedVisibility(
                    visible = isSearchAtTop,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        tonalElevation = 6.dp,
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
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = filterText,
                                onValueChange = { filterText = it },
                                placeholder = { Text("Search apps or categories...", fontSize = 13.sp) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { isSearchFocused = it.isFocused }
                            )
                            if (filterText.isNotEmpty()) {
                                IconButton(
                                    onClick = { filterText = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Main App List & Bottom Bar Container
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
                        contentPadding = PaddingValues(
                            top = if (isSearchAtTop) 4.dp else 12.dp,
                            bottom = if (isSearchAtTop) 24.dp else 80.dp
                        )
                    ) {
                        // Categorized Folders
                        if (filterText.isBlank() && folders.isNotEmpty()) {
                            itemsIndexed(folders, key = { _, f -> "folder_${f.id}" }) { _, folder ->
                                val appsInFolder = allApps.filter { it.packageName in folder.appPackageNames }
                                FolderInlineItem(
                                    folder = folder,
                                    appsInFolder = appsInFolder,
                                    iconPackType = settings.iconPackType,
                                    isLeftHanded = isLeftHanded,
                                    fontScale = settings.fontScale,
                                    onToggleExpand = { onToggleFolderExpanded(folder.id) },
                                    onAppClick = onAppClick,
                                    onAppLongClick = onAppLongClick,
                                    onSwipeRightForPopup = onSwipeRightForPopup
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        // App List Items with Alphabet Section Headers
                        var lastHeader: Char? = null
                        nonHiddenApps.forEach { app ->
                            val currentInitial = app.label.firstOrNull()?.uppercaseChar() ?: '#'
                            val isFirstOfSection = currentInitial != lastHeader

                            if (isFirstOfSection) {
                                lastHeader = currentInitial
                                item(key = "header_$currentInitial") {
                                    Text(
                                        text = currentInitial.toString(),
                                        fontSize = (13 * settings.fontScale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp, start = 4.dp)
                                    )
                                }
                            }

                            item(key = "all_${app.packageName}") {
                                AppListItem(
                                    app = app,
                                    iconPackType = settings.iconPackType,
                                    customIconSettings = settings.customIconSettings,
                                    isLeftHanded = isLeftHanded,
                                    fontScale = settings.fontScale,
                                    iconScale = settings.appIconScale,
                                    showBadges = settings.showAppNotificationBadges,
                                    compactSpacing = settings.compactAppRowSpacing,
                                    onAppClick = { onAppClick(app) },
                                    onAppLongClick = { onAppLongClick(app) },
                                    onReplyNotification = { text -> onReplyNotification(app.packageName, text) },
                                    onDismissNotification = { onDismissNotification(app.packageName) }
                                )
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
                            onLetterSelected = onWaveLetterSelected,
                            onLetterConfirmed = { letter ->
                                val targetChar = if (letter == '★') {
                                    nonHiddenApps.firstOrNull()?.label?.firstOrNull()?.uppercaseChar()
                                } else {
                                    letter
                                }
                                val index = nonHiddenApps.indexOfFirst {
                                    it.label.firstOrNull()?.uppercaseChar() == targetChar
                                }
                                if (index != -1) {
                                    scope.launch {
                                        listState.scrollToItem(index.coerceAtLeast(0))
                                    }
                                }
                            },
                            modifier = alignModifier
                                .fillMaxHeight()
                                .padding(vertical = 16.dp)
                        )
                    }

                    // Bottom-docked minimal search bar when idle (Slides up to top when focused)
                    if (!isSearchAtTop) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                            tonalElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                                .navigationBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = filterText,
                                    onValueChange = { filterText = it },
                                    placeholder = { Text("Search apps or categories...", fontSize = 13.sp) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .onFocusChanged { isSearchFocused = it.isFocused }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
