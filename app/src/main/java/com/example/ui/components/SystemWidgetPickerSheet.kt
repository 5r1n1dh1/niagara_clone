package com.example.ui.components

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SystemWidgetItem(
    val providerInfo: AppWidgetProviderInfo,
    val packageName: String,
    val appName: String,
    val appIcon: Bitmap?,
    val previewBitmap: Bitmap?,
    val label: String,
    val description: String?,
    val minWidth: Int,
    val minHeight: Int,
    val cols: Int,
    val rows: Int,
    val sizeLabel: String,
    val heightCategory: String
)

private fun Drawable.toSafeBitmap(): Bitmap? {
    return try {
        if (this is BitmapDrawable && bitmap != null) {
            bitmap
        } else {
            val width = if (intrinsicWidth > 0) intrinsicWidth else 48
            val height = if (intrinsicHeight > 0) intrinsicHeight else 48
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            bitmap
        }
    } catch (_: Throwable) {
        null
    }
}

/**
 * Production-ready bottom sheet for discovering, searching, and selecting
 * from all available Android System App Widgets across all installed applications and sizes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemWidgetPickerSheet(
    onSelectWidget: (AppWidgetProviderInfo, heightCategory: String) -> Unit,
    onDismiss: () -> Unit,
    glass: GlassStyle = GlassStyle()
) {
    val context = LocalContext.current
    var widgetItems by remember { mutableStateOf<List<SystemWidgetItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSizeFilter by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val pm = context.packageManager
            val providers = try {
                appWidgetManager.installedProviders ?: emptyList()
            } catch (_: Throwable) {
                emptyList()
            }

            val items = providers.mapNotNull { info ->
                try {
                    val pkg = info.provider.packageName
                    val appInfo = try { pm.getApplicationInfo(pkg, 0) } catch (_: Throwable) { null }
                    val appName = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg
                    val appIcon = try {
                        (appInfo?.let { pm.getApplicationIcon(it) } ?: info.loadIcon(context, 0))?.toSafeBitmap()
                    } catch (_: Throwable) { null }

                    val preview = try {
                        info.loadPreviewImage(context, 0)?.toSafeBitmap()
                    } catch (_: Throwable) { null }

                    val label = info.loadLabel(pm).ifBlank { "System Widget" }
                    val desc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try { info.loadDescription(context)?.toString() } catch (_: Throwable) { null }
                    } else null

                    val minW = info.minWidth
                    val minH = info.minHeight
                    val cols = ((minW + 30) / 70).coerceIn(1, 4)
                    val rows = ((minH + 30) / 70).coerceIn(1, 4)
                    val sizeLabel = "${cols}x$rows"
                    val heightCategory = "4x$rows"

                    SystemWidgetItem(
                        providerInfo = info,
                        packageName = pkg,
                        appName = appName,
                        appIcon = appIcon,
                        previewBitmap = preview,
                        label = label,
                        description = desc,
                        minWidth = minW,
                        minHeight = minH,
                        cols = cols,
                        rows = rows,
                        sizeLabel = sizeLabel,
                        heightCategory = heightCategory
                    )
                } catch (_: Throwable) {
                    null
                }
            }.sortedWith(compareBy({ it.appName.lowercase() }, { it.label.lowercase() }))

            withContext(Dispatchers.Main) {
                widgetItems = items
                isLoading = false
            }
        }
    }

    val availableSizeFilters = remember(widgetItems) {
        listOf("All") + widgetItems.map { it.sizeLabel }.distinct().sorted()
    }

    val filteredItems = remember(widgetItems, searchQuery, selectedSizeFilter) {
        widgetItems.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.appName.contains(searchQuery, ignoreCase = true) ||
                    item.label.contains(searchQuery, ignoreCase = true) ||
                    item.packageName.contains(searchQuery, ignoreCase = true) ||
                    (item.description?.contains(searchQuery, ignoreCase = true) == true)

            val matchesSize = selectedSizeFilter == "All" || item.sizeLabel == selectedSizeFilter

            matchesQuery && matchesSize
        }
    }

    val groupedByApp = remember(filteredItems) {
        filteredItems.groupBy { it.appName }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        modifier = Modifier.testTag("system_widget_picker_sheet")
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
                    .fillMaxHeight(0.88f)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
            // Header: Title and Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add System Widget",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isLoading) "Loading installed widgets..."
                        else "${widgetItems.size} widgets available across ${groupedByApp.size} apps",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("system_widget_search_input"),
                placeholder = { Text("Search widgets or apps...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Size Filter Chips Row
            if (availableSizeFilters.size > 2) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(availableSizeFilters) { size ->
                        val isSelected = selectedSizeFilter == size
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSizeFilter = size },
                            label = {
                                Text(
                                    text = if (size == "All") "All Sizes" else size,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Body: Content list or Empty/Loading state
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            } else if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No widgets found for \"$searchQuery\""
                            else "No system widgets available",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Try clearing search or filters",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedByApp.forEach { (appName, appWidgets) ->
                        val pkg = appWidgets.firstOrNull()?.packageName ?: appName
                        item(key = "header_${pkg}_$appName") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val firstIcon = appWidgets.firstOrNull()?.appIcon
                                if (firstIcon != null) {
                                    Image(
                                        bitmap = firstIcon.asImageBitmap(),
                                        contentDescription = appName,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = appName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "${appWidgets.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        itemsIndexed(appWidgets, key = { index, widget -> "sys_widget_${widget.packageName}_${widget.label}_${widget.minWidth}_${widget.minHeight}_$index" }) { _, widget ->
                            SystemWidgetCard(
                                item = widget,
                                onClick = { onSelectWidget(widget.providerInfo, widget.heightCategory) }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun SystemWidgetCard(
    item: SystemWidgetItem,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("system_widget_option_${item.label}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Widget Thumbnail Preview or App Icon
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (item.previewBitmap != null) {
                    Image(
                        bitmap = item.previewBitmap.asImageBitmap(),
                        contentDescription = item.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                } else if (item.appIcon != null) {
                    Image(
                        bitmap = item.appIcon.asImageBitmap(),
                        contentDescription = item.label,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Widget Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!item.description.isNullOrBlank()) {
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Size Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = item.sizeLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "${item.minWidth}×${item.minHeight} dp",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Add Action Button
            FilledTonalButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
