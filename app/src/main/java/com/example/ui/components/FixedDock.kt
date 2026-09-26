package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppItem
import com.example.data.IconPackType

/**
 * Fixed Bottom Dock showing user-customized app icons.
 * Persistent across all 3 screens (Left, Center, Right).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FixedDock(
    dockApps: List<AppItem>,
    iconPackType: IconPackType,
    slotCount: Int = 3,
    showLabels: Boolean = false,
    blurBackground: Boolean = true,
    iconScale: Float = 1.0f,
    isFrostedTheme: Boolean = false,
    blurRadius: Float = 15f,
    translucency: Float = 0.30f,
    glassEffect: Float = 0.30f,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (Int) -> Unit, // Slot index 0..(slotCount-1)
    modifier: Modifier = Modifier
) {
    val maxSlots = slotCount.coerceIn(3, 5)
    val displayApps = dockApps.take(maxSlots)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
            .testTag("fixed_dock_container"),
        contentAlignment = Alignment.Center
    ) {
        FrostedGlassPanel(
            cornerRadius = 28.dp,
            blurRadius = if (blurBackground) blurRadius.dp else 0.dp,
            translucency = if (blurBackground) translucency else 0.15f,
            glassEffect = if (blurBackground) glassEffect else 0f,
            modifier = Modifier
                .wrapContentWidth()
                .height(if (showLabels) (76 * iconScale).dp else (64 * iconScale).dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = (12 * iconScale).dp, vertical = (6 * iconScale).dp),
                horizontalArrangement = Arrangement.spacedBy((14 * iconScale).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (index in 0 until maxSlots) {
                    val app = displayApps.getOrNull(index)
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val slotScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.88f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "dock_slot_scale_$index"
                    )

                    if (app != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .width((50 * iconScale).dp)
                                .graphicsLayer {
                                    scaleX = slotScale
                                    scaleY = slotScale
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = ripple(),
                                    onClick = { onAppClick(app) },
                                    onLongClick = { onAppLongClick(index) }
                                )
                                .testTag("dock_slot_$index")
                        ) {
                            Box(
                                modifier = Modifier.size(if (showLabels) (38 * iconScale).dp else (46 * iconScale).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AppIconRenderer(
                                    app = app,
                                    iconPackType = iconPackType,
                                    size = if (showLabels) (34 * iconScale).dp else (40 * iconScale).dp
                                )
                            }
                            if (showLabels) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = app.label,
                                    fontSize = (9 * iconScale).sp,
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
                                .size(if (showLabels) (38 * iconScale).dp else (46 * iconScale).dp)
                                .graphicsLayer {
                                    scaleX = slotScale
                                    scaleY = slotScale
                                }
                                .clip(CircleShape)
                                .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = ripple(),
                                    onClick = { onAppLongClick(index) },
                                    onLongClick = { onAppLongClick(index) }
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontSize = 18.sp,
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
    onDismiss: () -> Unit,
    blurRadius: Float = 15f,
    translucency: Float = 0.30f,
    glassEffect: Float = 0.30f
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.45f)
    ) {
        FrostedGlassPanel(
            cornerRadius = 28.dp,
            blurRadius = blurRadius.dp,
            translucency = translucency,
            glassEffect = glassEffect,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
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

            var searchQuery by remember { mutableStateOf("") }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search apps for dock...", fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            val filteredApps = remember(allApps, searchQuery) {
                val nonHidden = allApps.filter { !it.isHidden }
                if (searchQuery.isBlank()) {
                    nonHidden
                } else {
                    val q = searchQuery.trim().lowercase()
                    nonHidden.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
                }
            }

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                items(filteredApps.size) { i ->
                    val app = filteredApps[i]
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
}
