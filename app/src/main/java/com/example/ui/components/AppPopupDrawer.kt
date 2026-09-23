package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    mediaPlayer: com.example.data.MediaPlaybackState = com.example.data.MediaPlaybackState(),
    onReplyNotification: ((String) -> Unit)? = null,
    onDismissNotification: (() -> Unit)? = null,
    onDismissNotificationKey: ((String) -> Unit)? = null,
    onReplyNotificationKey: ((String, String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHide: () -> Unit,
    onToggleUsageBreaker: () -> Unit,
    onDisguiseApp: (String?) -> Unit,
    onAssignCategory: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFavoriteState by remember(app.isFavorite, app.packageName) { mutableStateOf(app.isFavorite) }
    var showDisguiseDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var customCategoryText by remember { mutableStateOf(app.customCategory) }
    var disguiseText by remember { mutableStateOf(app.disguisedName ?: "") }
    var replyInput by remember { mutableStateOf("") }
    var replySentConfirmation by remember { mutableStateOf(false) }

    val category = remember(app.packageName, app.label) {
        detectAppCategory(app.packageName, app.label)
    }

    // Consolidated list of notifications for this app, deduplicated
    val effectiveNotifications = remember(notifications, notification, app.packageName, app.isDualApp) {
        val list = mutableListOf<NotificationItem>()
        if (notifications.isNotEmpty()) {
            list.addAll(notifications)
        }
        if (notification != null && list.none { it.key == notification.key || (it.sender == notification.sender && it.message == notification.message) }) {
            list.add(notification)
        }
        list.distinctBy { it.key }
    }

    // Active selected chat head for detailed message preview popup window
    var activePreviewNotification by remember {
        mutableStateOf<NotificationItem?>(null)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        modifier = modifier.testTag("app_popup_drawer")
    ) {
        FrostedGlassPanel(
            cornerRadius = 28.dp,
            blurRadius = 22.dp,
            translucency = 0.35f,
            glassEffect = 0.35f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
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
                        text = if (app.isDualApp) "${app.packageName} • Dual App" else app.packageName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        onToggleFavorite()
                    },
                    modifier = Modifier.size(36.dp).testTag("app_popup_favorite_toggle")
                ) {
                    Icon(
                        imageVector = if (app.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (app.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                mediaPlayer = mediaPlayer,
                onDismiss = onDismiss
            )

            // 3. Messaging Chat Heads or Standard Notification Section
            if (effectiveNotifications.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                if (category == AppCategoryType.MESSAGING) {
                    // Chat Heads ONLY for Messaging Apps (Compact, scrollable horizontally, tap to preview popup)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
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

                            Spacer(modifier = Modifier.height(10.dp))

                            // Compact horizontally scrollable Chat Heads (No individual dismiss buttons, opens rich popup on touch)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .testTag("chat_heads_horizontal_list"),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                effectiveNotifications.forEach { notifItem ->
                                    val initial = notifItem.sender.firstOrNull()?.uppercaseChar()?.toString() ?: "💬"

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                activePreviewNotification = notifItem
                                            }
                                            .padding(horizontal = 2.dp, vertical = 2.dp)
                                            .testTag("chat_head_${notifItem.id}")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .border(
                                                    width = 1.5.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                        ) {
                                            if (notifItem.senderIcon != null) {
                                                Image(
                                                    bitmap = notifItem.senderIcon.asImageBitmap(),
                                                    contentDescription = notifItem.sender,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    text = initial,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = notifItem.sender.take(8),
                                            fontSize = 10.sp,
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
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
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

                            // Compact list of all notifications (Clickable to open full rich preview popup)
                            effectiveNotifications.take(4).forEachIndexed { index, notifItem ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            activePreviewNotification = notifItem
                                        }
                                        .padding(vertical = 2.dp),
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

                // Category Tag button
                ActionPillButton(
                    icon = Icons.Outlined.Folder,
                    label = if (app.customCategory.isNotBlank()) app.customCategory else "Category",
                    isActive = app.customCategory.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    onClick = { showCategoryDialog = true }
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
}

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Assign App Category", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select or enter custom category for ${app.label}:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val presetCategories = listOf("Social", "Work", "Games", "Media", "Utilities", "Tools", "Productivity")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetCategories.take(4).forEach { cat ->
                            SuggestionChip(
                                onClick = { customCategoryText = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customCategoryText,
                        onValueChange = { customCategoryText = it },
                        placeholder = { Text("e.g. Finance, Games, Travel") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val finalCat = customCategoryText.trim()
                    onAssignCategory?.invoke(finalCat)
                    showCategoryDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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

    // 5. Rich Detailed Message Preview Popup Window
    activePreviewNotification?.let { previewNotif ->
        DetailedMessagePreviewPopupDialog(
            notification = previewNotif,
            appLabel = app.label,
            onDismissRequest = { activePreviewNotification = null },
            onDismissNotification = { notifKey ->
                if (onDismissNotificationKey != null) {
                    onDismissNotificationKey(notifKey)
                } else {
                    onDismissNotification?.invoke()
                }
                activePreviewNotification = null
            },
            onReplyNotification = { notifKey, replyText ->
                if (onReplyNotificationKey != null) {
                    onReplyNotificationKey(notifKey, replyText)
                } else {
                    onReplyNotification?.invoke(replyText)
                }
            }
        )
    }
}

/**
 * Detailed Message Preview Popup Dialog:
 * - Opens on touch of a chat head or notification item
 * - Supports images (pictureBitmap) and rich text formats (HTML / Markdown styling)
 * - Has Reply text input + quick reply chips and Dismiss options
 * - Hides smoothly on click / touch outside or on dismiss request
 */
@Composable
fun DetailedMessagePreviewPopupDialog(
    notification: NotificationItem,
    appLabel: String,
    onDismissRequest: () -> Unit,
    onDismissNotification: (String) -> Unit,
    onReplyNotification: (String, String) -> Unit
) {
    var replyText by remember { mutableStateOf("") }
    var replySentConfirmation by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Intercept clicks inside card to prevent backdrop dismiss */ }
                    )
                    .testTag("chat_head_preview_popup")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header: Avatar, Sender Name, Category/Subtext, Time, Close button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            if (notification.senderIcon != null) {
                                Image(
                                    bitmap = notification.senderIcon.asImageBitmap(),
                                    contentDescription = notification.sender,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = notification.sender.firstOrNull()?.uppercaseChar()?.toString() ?: "💬",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notification.sender,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            val subtitle = buildString {
                                if (!notification.subText.isNullOrBlank()) {
                                    append(notification.subText)
                                    append(" • ")
                                } else if (appLabel.isNotBlank()) {
                                    append(appLabel)
                                    append(" • ")
                                }
                                append(notification.timeAgo)
                            }
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.size(32.dp).testTag("close_chat_head_preview")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Body: Message content (Supporting image attachments and rich text formatting)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(weight = 1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Attached Image if available
                        if (notification.pictureBitmap != null) {
                            Image(
                                bitmap = notification.pictureBitmap.asImageBitmap(),
                                contentDescription = "Message image attachment",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Rich Text Formatted Message
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RichNotificationMessageText(
                                text = notification.richText ?: notification.message,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Reply suggestion chips
                        val quickReplies = listOf("👍 Sounds good!", "On my way 🚗", "Got it, thanks!", "Call you in a bit 📞")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickReplies.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = {
                                        onReplyNotification(notification.key, suggestion)
                                        replySentConfirmation = true
                                    },
                                    label = { Text(suggestion, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Footer: Reply Input & Dismiss / Close Actions
                    if (replySentConfirmation) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reply sent successfully!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else if (notification.canReply) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                placeholder = { Text("Type a reply...", fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("preview_popup_reply_input"),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (replyText.isNotBlank()) {
                                        onReplyNotification(notification.key, replyText.trim())
                                        replySentConfirmation = true
                                    }
                                })
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (replyText.isNotBlank()) {
                                        onReplyNotification(notification.key, replyText.trim())
                                        replySentConfirmation = true
                                    }
                                },
                                enabled = replyText.isNotBlank(),
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        color = if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    )
                                    .testTag("preview_popup_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Reply",
                                    tint = if (replyText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Action Row: Dismiss Option & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDismissNotification(notification.key)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("preview_popup_dismiss_button")
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dismiss", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onDismissRequest,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("preview_popup_close_button")
                        ) {
                            Text("Done", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSpecificOperationsBar(
    category: AppCategoryType,
    packageName: String,
    context: Context,
    mediaPlayer: com.example.data.MediaPlaybackState = com.example.data.MediaPlaybackState(),
    onDismiss: () -> Unit
) {
    when (category) {
        AppCategoryType.MUSIC -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().testTag("media_controls_panel")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header with Album Art and Track Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Album Art Image or Styled Vinyl Placeholder
                        if (mediaPlayer.albumArtBitmap != null) {
                            Image(
                                bitmap = mediaPlayer.albumArtBitmap.asImageBitmap(),
                                contentDescription = "Album Art",
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = "Album Art Placeholder",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mediaPlayer.trackTitle.ifBlank { "Now Playing" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mediaPlayer.artistName.ifBlank { mediaPlayer.appName },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mediaPlayer.appName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Playback Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                                try {
                                    val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                                    val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                                    audioManager?.dispatchMediaKeyEvent(downEvent)
                                    audioManager?.dispatchMediaKeyEvent(upEvent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous Track", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                                try {
                                    val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                                    val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                                    audioManager?.dispatchMediaKeyEvent(downEvent)
                                    audioManager?.dispatchMediaKeyEvent(upEvent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (mediaPlayer.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (mediaPlayer.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (mediaPlayer.isPlaying) "Pause" else "Play", fontSize = 11.sp, maxLines = 1)
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
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next Track", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Next", fontSize = 11.sp)
                        }
                    }
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
