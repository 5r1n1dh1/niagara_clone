package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.launch

/**
 * Right-most Page: Lists all apps with strict left and right margins,
 * and features the alphabet-guided quick jump (Wave Alphabet).
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
    onSwipeRightForPopup: (AppItem) -> Unit,
    onReplyNotification: (packageName: String, text: String) -> Unit,
    onDismissNotification: (packageName: String) -> Unit,
    onToggleFolderExpanded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isLeftHanded = settings.leftHandedMode

    var filterText by remember { mutableStateOf("") }

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

    // Group apps by first character for letter-guided quick jumping
    val letterPositionMap = remember(nonHiddenApps, folders, filterText) {
        val map = mutableMapOf<Char, Int>()
        // Index 0: Search & Stats Header
        // Index 1: Folders (if any)
        var currentIndex = 1 + (if (folders.isNotEmpty() && filterText.isBlank()) folders.size + 1 else 0)

        for (app in nonHiddenApps) {
            val firstChar = app.label.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (firstChar in 'A'..'Z') firstChar else '#'
            if (!map.containsKey(key)) {
                map[key] = currentIndex
            }
            currentIndex++
        }
        map
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.52f))
            .statusBarsPadding()
            .testTag("right_all_apps_screen")
    ) {
        // 3. Right most screen lists all apps - LEFT AND RIGHT MARGIN IS A MUST
        // Explicit generous margins: start = 24.dp, end = 58.dp (or mirrored for left-handed)
        val leftMargin = if (isLeftHanded) 58.dp else 24.dp
        val rightMargin = if (isLeftHanded) 24.dp else 58.dp

        if (isAppsLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 160.dp, start = leftMargin, end = rightMargin),
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
            LazyColumn(
                state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = leftMargin, end = rightMargin)
                .testTag("all_apps_lazy_column"),
            contentPadding = PaddingValues(top = 10.dp, bottom = 110.dp)
        ) {
            // Header & Fast Filter Field
            item(key = "all_apps_header") {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ALL APPLICATIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${nonHiddenApps.size} apps",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Minimal Search Bar Field
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            TextField(
                                value = filterText,
                                onValueChange = { filterText = it },
                                placeholder = { Text("Filter apps or categories...", fontSize = 13.sp) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
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
            }

            // Optional Folders section
            if (folders.isNotEmpty() && filterText.isBlank()) {
                item(key = "folders_header") {
                    Text(
                        text = "FOLDERS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                itemsIndexed(folders, key = { _, folder -> folder.id }) { _, folder ->
                    val folderApps = remember(folder.appPackageNames, allApps) {
                        allApps.filter { folder.appPackageNames.contains(it.packageName) }
                    }
                    FolderInlineItem(
                        folder = folder,
                        appsInFolder = folderApps,
                        iconPackType = settings.iconPackType,
                        isLeftHanded = isLeftHanded,
                        fontScale = settings.fontScale,
                        onToggleExpand = { onToggleFolderExpanded(folder.id) },
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                        onSwipeRightForPopup = onSwipeRightForPopup
                    )
                }
            }

            // All App Items with Alphabet Sections
            var currentHeaderChar = ' '
            nonHiddenApps.forEachIndexed { index, app ->
                val firstChar = app.label.firstOrNull()?.uppercaseChar() ?: '#'
                val displayChar = if (firstChar in 'A'..'Z') firstChar else '#'

                if (displayChar != currentHeaderChar) {
                    currentHeaderChar = displayChar
                    item(key = "letter_header_$displayChar") {
                        Text(
                            text = displayChar.toString(),
                            fontSize = 13.sp,
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
                        onSwipeRightForPopup = { onSwipeRightForPopup(app) },
                        onReplyNotification = { text -> onReplyNotification(app.packageName, text) },
                        onDismissNotification = { onDismissNotification(app.packageName) }
                    )
                }
            }
        }

        // 3. Alphabet-guided quick jump feature (Wave Alphabet)
        WaveAlphabet(
            isLeftHanded = isLeftHanded,
            selectedLetter = selectedWaveLetter,
            onLetterSelected = { letter ->
                onWaveLetterSelected(letter)
                if (letter != null) {
                    val targetIndex = letterPositionMap[letter] ?: 0
                    coroutineScope.launch {
                        listState.scrollToItem(targetIndex)
                    }
                }
            },
            onLetterConfirmed = { letter ->
                val targetIndex = letterPositionMap[letter] ?: 0
                coroutineScope.launch {
                    listState.animateScrollToItem(targetIndex)
                }
            },
            modifier = Modifier.align(if (isLeftHanded) Alignment.CenterStart else Alignment.CenterEnd)
        )
    }
}
}
