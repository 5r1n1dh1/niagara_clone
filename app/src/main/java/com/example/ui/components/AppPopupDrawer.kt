package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppItem
import com.example.data.CustomIconPackSettings
import com.example.data.IconPackType
import com.example.data.NotificationItem

enum class AppCategoryType {
    MUSIC,
    MESSAGING,
    CAMERA,
    BROWSER,
    EMAIL,
    MAPS,
    CALENDAR,
    CLOCK,
    GENERIC
}

fun detectAppCategory(packageName: String, label: String = ""): AppCategoryType {
    val p = packageName.lowercase()
    val l = label.lowercase()

    return when {
        p.contains("spotify") || p.contains("music") || p.contains("audio") || p.contains("podcast") ||
                p.contains("soundcloud") || p.contains("deezer") || p.contains("tidal") ||
                l.contains("music") || l.contains("spotify") || l.contains("podcast") -> AppCategoryType.MUSIC

        p.contains("messaging") || p.contains("mms") || p.contains("whatsapp") ||
                p.contains("telegram") || p.contains("slack") || p.contains("discord") ||
                p.contains("signal") || p.contains("viber") || p.contains("wechat") ||
                p.contains("messenger") || p.contains("skype") || p.contains("sms") ||
                l.contains("message") || l.contains("messaging") || l.contains("chat") ||
                l.contains("whatsapp") || l.contains("telegram") || l.contains("slack") ||
                l.contains("discord") || l.contains("signal") -> AppCategoryType.MESSAGING

        p.contains("camera") || p.contains("gallery") || p.contains("photo") ||
                l.contains("camera") || l.contains("photos") || l.contains("gallery") -> AppCategoryType.CAMERA

        p.contains("chrome") || p.contains("browser") || p.contains("firefox") ||
                p.contains("edge") || p.contains("opera") || p.contains("brave") ||
                l.contains("browser") || l.contains("chrome") || l.contains("firefox") -> AppCategoryType.BROWSER

        p.contains("mail") || p.contains("gmail") || p.contains("outlook") ||
                l.contains("mail") || l.contains("gmail") || l.contains("outlook") -> AppCategoryType.EMAIL

        p.contains("maps") || p.contains("waze") || p.contains("navigation") ||
                l.contains("maps") || l.contains("navigation") || l.contains("waze") -> AppCategoryType.MAPS

        p.contains("calendar") || l.contains("calendar") -> AppCategoryType.CALENDAR
        p.contains("deskclock") || p.contains("alarm") || l.contains("clock") -> AppCategoryType.CLOCK
        else -> AppCategoryType.GENERIC
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppPopupDrawer(
    app: AppItem,
    iconPackType: IconPackType,
    customIconSettings: CustomIconPackSettings = CustomIconPackSettings(),
    notification: NotificationItem? = app.notification,
    notifications: List<NotificationItem> = emptyList(),
    onReplyNotification: ((String) -> Unit)? = null,
    onDismissNotification: (() -> Unit)? = null,
    onDismissNotificationKey: ((String) -> Unit)? = null,
    onReplyNotificationKey: ((String, String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHide: () -> Unit,
    onToggleUsageBreaker: () -> Unit,
    onDisguiseApp: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFavoriteState by remember(app.isFavorite, app.packageName) { mutableStateOf(app.isFavorite) }
    var showDisguiseDialog by remember { mutableStateOf(false) }
    var disguiseText by remember { mutableStateOf(app.disguisedName ?: "") }
    var replyInput by remember { mutableStateOf("") }
    var replySentConfirmation by remember { mutableStateOf(false) }

    val category = remember(app.packageName, app.label) {
        detectAppCategory(app.packageName, app.label)
    }

    // Consolidated list of notifications for this app
    val effectiveNotifications = remember(notifications, notification) {
        if (notifications.isNotEmpty()) {
            notifications
        } else if (notification != null) {
            listOf(notification)
        } else {
            emptyList()
        }
    }

    // Active selected chat head for messaging apps
    var selectedChatHeadKey by remember(effectiveNotifications) {
        mutableStateOf<String?>(null)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("app_popup_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // 1. Compact App Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppIconRenderer(
                    app = app,
                    iconPackType = iconPackType,
                    customIconSettings = customIconSettings,
                    size = 46.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.disguisedName ?: app.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        isFavoriteState = !isFavoriteState
                        onToggleFavorite()
                    },
                    modifier = Modifier.size(36.dp).testTag("app_popup_favorite_toggle")
                ) {
                    Icon(
                        imageVector = if (isFavoriteState) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavoriteState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. App-Specific Operations (Play/Pause, Reply, Compose, Camera, etc.)
            AppSpecificOperationsBar(
                category = category,
                packageName = app.packageName,
                context = context,
                onDismiss = onDismiss
            )

            // 3. Messaging Chat Heads or Standard Notification Section
            if (effectiveNotifications.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                if (category == AppCategoryType.MESSAGING) {
                    // Chat Heads ONLY for Messaging Apps (no message content text shown, individually dismissible)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth().testTag("popup_chat_heads_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Chat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CHAT HEADS (${effectiveNotifications.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )

                                // Clear All Notifications Button for messaging app
                                TextButton(
                                    onClick = { onDismissNotification?.invoke() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp).testTag("clear_all_messaging_notifs")
                                ) {
                                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Horizontal list of Chat Heads with individual dismiss buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                effectiveNotifications.forEach { notifItem ->
                                    val initial = notifItem.sender.firstOrNull()?.uppercaseChar()?.toString() ?: "💬"

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.testTag("chat_head_${notifItem.id}")
                                    ) {
                                        Box(contentAlignment = Alignment.TopEnd) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape
                                                    )
                                            ) {
                                                Text(
                                                    text = initial,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }

                                            // Individual dismiss button on top end of chat head
                                            IconButton(
                                                onClick = {
                                                    if (onDismissNotificationKey != null) {
                                                        onDismissNotificationKey(notifItem.key)
                                                    } else {
                                                        onDismissNotification?.invoke()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.errorContainer)
                                                    .testTag("dismiss_chat_head_${notifItem.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Dismiss",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = notifItem.sender.take(10),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Standard notification card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth().testTag("popup_notification_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (effectiveNotifications.size > 1) "NOTIFICATIONS (${effectiveNotifications.size})" else "NOTIFICATION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )

                                if (onDismissNotification != null) {
                                    TextButton(
                                        onClick = onDismissNotification,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).testTag("clear_all_app_notifs")
                                    ) {
                                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Compact list of all notifications
                            effectiveNotifications.take(4).forEachIndexed { index, notifItem ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = notifItem.sender,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = notifItem.message,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (onDismissNotificationKey != null) {
                                        IconButton(
                                            onClick = { onDismissNotificationKey(notifItem.key) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Quick Action Grid / Minimal Action Rows (Uncluttered)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Usage Breaker button
                ActionPillButton(
                    icon = if (app.isUsageBreakerActive) Icons.Filled.Spa else Icons.Outlined.Spa,
                    label = if (app.isUsageBreakerActive) "Breaker On" else "Breaker",
                    isActive = app.isUsageBreakerActive,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleUsageBreaker
                )

                // Hide App button
                ActionPillButton(
                    icon = if (app.isHidden) Icons.Filled.VisibilityOff else Icons.Outlined.Visibility,
                    label = if (app.isHidden) "Unhide" else "Hide",
                    isActive = app.isHidden,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleHide
                )

                // Disguise Name button
                ActionPillButton(
                    icon = Icons.Outlined.DriveFileRenameOutline,
                    label = if (app.disguisedName != null) "Disguised" else "Rename",
                    isActive = app.disguisedName != null,
                    modifier = Modifier.weight(1f),
                    onClick = { showDisguiseDialog = true }
                )

                // App Info button
                ActionPillButton(
                    icon = Icons.Outlined.Info,
                    label = "Info",
                    isActive = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                            }
                            context.startActivity(intent)
                            onDismiss()
                        } catch (_: Exception) {}
                    }
                )
            }
        }
    }

    if (showDisguiseDialog) {
        AlertDialog(
            onDismissRequest = { showDisguiseDialog = false },
            title = { Text("Disguise App Name", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Decoy label for ${app.label}:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = disguiseText,
                        onValueChange = { disguiseText = it },
                        placeholder = { Text("e.g. Calculator, Notes, Clock") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onDisguiseApp(if (disguiseText.isBlank()) null else disguiseText.trim())
                    showDisguiseDialog = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDisguiseApp(null)
                    showDisguiseDialog = false
                }) {
                    Text("Reset")
                }
            }
        )
    }
}

@Composable
private fun AppSpecificOperationsBar(
    category: AppCategoryType,
    packageName: String,
    context: Context,
    onDismiss: () -> Unit
) {
    when (category) {
        AppCategoryType.MUSIC -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                        try {
                            val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                            val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                            audioManager?.dispatchMediaKeyEvent(downEvent)
                            audioManager?.dispatchMediaKeyEvent(upEvent)
                        } catch (_: Exception) {}
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play / Pause", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                        try {
                            val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                            val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                            audioManager?.dispatchMediaKeyEvent(downEvent)
                            audioManager?.dispatchMediaKeyEvent(upEvent)
                        } catch (_: Exception) {}
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Next Track", fontSize = 12.sp)
                }
            }
        }

        AppCategoryType.CAMERA -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        try {
                            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Take Photo", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = {
                        try {
                            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                                putExtra("android.intent.extras.CAMERA_FACING", 1)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.CameraFront, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Selfie", fontSize = 12.sp)
                }
            }
        }

        AppCategoryType.BROWSER -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                                setPackage(packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                            launchIntent?.let { context.startActivity(it) }
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Tab", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = {
                        try {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                            launchIntent?.let { context.startActivity(it) }
                        } catch (_: Exception) {}
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Private / Open", fontSize = 12.sp)
                }
            }
        }

        AppCategoryType.EMAIL -> {
            FilledTonalButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                            setPackage(packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compose New Email", fontSize = 12.sp)
            }
        }

        AppCategoryType.CLOCK -> {
            FilledTonalButton(
                onClick = {
                    try {
                        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Set Alarm / Timer", fontSize = 12.sp)
            }
        }

        else -> {
            // Generic apps: no redundant buttons, keeps context menu minimal
        }
    }
}

@Composable
private fun ActionPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier.height(52.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
