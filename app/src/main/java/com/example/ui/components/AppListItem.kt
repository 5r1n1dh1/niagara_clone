package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppItem
import com.example.data.CustomIconPackSettings
import com.example.data.IconPackType
import com.example.data.NotificationItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppItem,
    iconPackType: IconPackType,
    customIconSettings: CustomIconPackSettings = CustomIconPackSettings(),
    isLeftHanded: Boolean,
    fontScale: Float,
    iconScale: Float = 1.0f,
    showBadges: Boolean = true,
    compactSpacing: Boolean = false,
    onAppClick: () -> Unit,
    onAppLongClick: () -> Unit,
    onSwipeRightForPopup: () -> Unit,
    onReplyNotification: (String) -> Unit,
    onDismissNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isReplying by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(app.packageName) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                    },
                    onDragEnd = {
                        val triggerSwipe = if (isLeftHanded) dragAccumulator < -80f else dragAccumulator > 80f
                        if (triggerSwipe) {
                            onSwipeRightForPopup()
                        }
                        dragAccumulator = 0f
                    },
                    onDragCancel = { dragAccumulator = 0f }
                )
            }
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onAppClick,
                onLongClick = onAppLongClick
            )
            .padding(horizontal = 8.dp, vertical = if (compactSpacing) 3.dp else 6.dp)
            .testTag("app_item_${app.packageName}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isLeftHanded) Arrangement.End else Arrangement.Start
        ) {
            if (!isLeftHanded) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = onAppClick,
                            onLongClick = onAppLongClick
                        )
                ) {
                    AppIconRenderer(
                        app = app,
                        iconPackType = iconPackType,
                        customIconSettings = customIconSettings,
                        size = (38 * iconScale).dp,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    if (showBadges && app.notification != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.TopEnd)
                        ) {}
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = if (isLeftHanded) Alignment.End else Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.disguisedName ?: app.label,
                        fontSize = (16 * fontScale).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (app.isUsageBreakerActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                            modifier = Modifier.size(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Spa,
                                    contentDescription = "Usage Breaker Active",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }

                // Inline Notification Preview
                if (app.notification != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${app.notification.sender}: ${app.notification.message}",
                            fontSize = (12 * fontScale).sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = app.notification.timeAgo,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (isLeftHanded) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = onAppClick,
                            onLongClick = onAppLongClick
                        )
                ) {
                    AppIconRenderer(
                        app = app,
                        iconPackType = iconPackType,
                        customIconSettings = customIconSettings,
                        size = (38 * iconScale).dp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    if (showBadges && app.notification != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.TopStart)
                        ) {}
                    }
                }
            } else {
                IconButton(
                    onClick = onAppLongClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "App Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Notification Action & Inline Direct Reply
        if (app.notification != null) {
            AnimatedVisibility(visible = !isReplying) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (!isLeftHanded) 54.dp else 0.dp,
                            end = if (isLeftHanded) 54.dp else 0.dp,
                            top = 4.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (app.notification.canReply) {
                        FilledTonalButton(
                            onClick = { isReplying = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reply", fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onDismissNotification,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dismiss", fontSize = 11.sp)
                    }
                }
            }

            // Expanded Inline Reply Field
            AnimatedVisibility(visible = isReplying) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (!isLeftHanded) 54.dp else 0.dp,
                            end = if (isLeftHanded) 54.dp else 0.dp,
                            top = 6.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Direct reply…", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (replyText.isNotBlank()) {
                                onReplyNotification(replyText)
                                replyText = ""
                                isReplying = false
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onReplyNotification(replyText)
                                replyText = ""
                                isReplying = false
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { isReplying = false },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
