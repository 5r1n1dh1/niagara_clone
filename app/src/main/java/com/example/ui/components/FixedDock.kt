package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppItem
import com.example.data.IconPackType

/**
 * Fixed Bottom Dock showing exactly MAX 3 icons.
 * Persistent across all 3 screens (Left, Center, Right).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FixedDock(
    dockApps: List<AppItem>,
    iconPackType: IconPackType,
    showLabels: Boolean = false,
    blurBackground: Boolean = true,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (Int) -> Unit, // Slot index 0, 1, or 2
    modifier: Modifier = Modifier
) {
    // Exactly max 3 icons
    val displayApps = dockApps.take(3)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
            .testTag("fixed_dock_container"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = if (blurBackground) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            },
            tonalElevation = if (blurBackground) 8.dp else 2.dp,
            shadowElevation = if (blurBackground) 10.dp else 0.dp,
            modifier = Modifier
                .wrapContentWidth()
                .height(if (showLabels) 76.dp else 64.dp)
                .clip(RoundedCornerShape(28.dp))
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (index in 0..2) {
                    val app = displayApps.getOrNull(index)
                    if (app != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .width(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = { onAppClick(app) },
                                    onLongClick = { onAppLongClick(index) }
                                )
                                .testTag("dock_slot_$index")
                        ) {
                            Box(
                                modifier = Modifier.size(if (showLabels) 40.dp else 50.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AppIconRenderer(
                                    app = app,
                                    iconPackType = iconPackType,
                                    size = if (showLabels) 36.dp else 42.dp
                                )
                            }
                            if (showLabels) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = app.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        // Empty slot indicator
                        Box(
                            modifier = Modifier
                                .size(if (showLabels) 40.dp else 50.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = { onAppLongClick(index) },
                                    onLongClick = { onAppLongClick(index) }
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom sheet to pick an app for a dock slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockAppPickerDialog(
    slotIndex: Int,
    allApps: List<AppItem>,
    iconPackType: IconPackType,
    onSelectApp: (AppItem) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Customize Dock Slot #${slotIndex + 1}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose an app to pin to your bottom dock",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val nonHidden = allApps.filter { !it.isHidden }
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                items(nonHidden.size) { i ->
                    val app = nonHidden[i]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectApp(app) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconRenderer(
                                app = app,
                                iconPackType = iconPackType,
                                size = 36.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = app.label,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.customCategory,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
