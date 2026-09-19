package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

fun isMessagingApp(packageName: String, label: String = ""): Boolean {
    val p = packageName.lowercase()
    val l = label.lowercase()
    return p.contains("messaging") || p.contains("mms") || p.contains("whatsapp") ||
            p.contains("telegram") || p.contains("slack") || p.contains("discord") ||
            p.contains("signal") || p.contains("viber") || p.contains("wechat") ||
            p.contains("messenger") || p.contains("skype") || p.contains("sms") ||
            l.contains("message") || l.contains("messaging") || l.contains("chat") ||
            l.contains("whatsapp") || l.contains("telegram") || l.contains("slack") ||
            l.contains("discord") || l.contains("signal")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPopupDrawer(
    app: AppItem,
    iconPackType: IconPackType,
    customIconSettings: CustomIconPackSettings = CustomIconPackSettings(),
    notification: NotificationItem? = app.notification,
    onReplyNotification: ((String) -> Unit)? = null,
    onDismissNotification: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHide: () -> Unit,
    onToggleUsageBreaker: () -> Unit,
    onDisguiseApp: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDisguiseDialog by remember { mutableStateOf(false) }
    var disguiseText by remember { mutableStateOf(app.disguisedName ?: "") }
    var replyInput by remember { mutableStateOf("") }
    var replySentConfirmation by remember { mutableStateOf(false) }

    val isMessaging = remember(app.packageName, app.label) {
        isMessagingApp(app.packageName, app.label)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("app_popup_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // App Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppIconRenderer(
                    app = app,
                    iconPackType = iconPackType,
                    customIconSettings = customIconSettings,
                    size = 50.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.disguisedName ?: app.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (app.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (app.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Notifications Section with Inline Reply for Messaging Apps
            if (notification != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().testTag("popup_notification_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOTIFICATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = notification.timeAgo,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (onDismissNotification != null) {
                                IconButton(
                                    onClick = onDismissNotification,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Dismiss notification",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = notification.sender,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = notification.message,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // If Messaging App or replyable notification, show Reply Options
                        if (isMessaging || notification.canReply) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Quick Reply",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick suggestion chips
                            val quickReplies = listOf("On my way! 🏃", "Sounds good! 👍", "I'll reply soon", "Got it!")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickReplies.take(3).forEach { chipText ->
                                    SuggestionChip(
                                        onClick = {
                                            onReplyNotification?.invoke(chipText)
                                            replySentConfirmation = true
                                        },
                                        label = { Text(chipText, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Inline Reply Input
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = replyInput,
                                    onValueChange = {
                                        replyInput = it
                                        replySentConfirmation = false
                                    },
                                    placeholder = { Text("Type reply...", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                    modifier = Modifier.weight(1f).testTag("popup_reply_input"),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (replyInput.isNotBlank()) {
                                            onReplyNotification?.invoke(replyInput)
                                            replyInput = ""
                                            replySentConfirmation = true
                                        }
                                    })
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = {
                                        if (replyInput.isNotBlank()) {
                                            onReplyNotification?.invoke(replyInput)
                                            replyInput = ""
                                            replySentConfirmation = true
                                        }
                                    },
                                    enabled = replyInput.isNotBlank(),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.size(42.dp).testTag("popup_reply_send_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send Reply",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (replySentConfirmation) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "✓ Reply sent",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            } else if (isMessaging) {
                // If it's a messaging app with no current unread notification
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Messaging App",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "No unread notifications",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Pop-up Dynamic Attached Widget Preview
            AttachedAppWidgetCard(app)

            Spacer(modifier = Modifier.height(18.dp))

            // App Shortcuts
            if (app.shortcuts.isNotEmpty()) {
                Text(
                    text = "App Shortcuts",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    app.shortcuts.forEach { shortcut ->
                        FilledTonalButton(
                            onClick = { onDismiss() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(shortcut, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Actions list
            PopupActionRow(
                icon = if (app.isUsageBreakerActive) Icons.Filled.Spa else Icons.Outlined.Spa,
                label = "Usage Breaker (Mindful Delay)",
                subtitle = if (app.isUsageBreakerActive) "Active • 5s pause before launch" else "Off • Add mindful friction",
                onClick = onToggleUsageBreaker
            )

            PopupActionRow(
                icon = Icons.Outlined.DriveFileRenameOutline,
                label = "Disguise App Name",
                subtitle = if (app.disguisedName != null) "Currently disguised as \"${app.disguisedName}\"" else "Rename to disguise from others",
                onClick = { showDisguiseDialog = true }
            )

            PopupActionRow(
                icon = if (app.isHidden) Icons.Filled.VisibilityOff else Icons.Outlined.Visibility,
                label = if (app.isHidden) "Unhide App" else "Hide App",
                subtitle = "Completely hide from homescreen and lists",
                onClick = onToggleHide
            )

            PopupActionRow(
                icon = Icons.Outlined.Info,
                label = "App Info & System Settings",
                subtitle = "Storage, permissions, notifications",
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", app.packageName, null)
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            )
        }
    }

    if (showDisguiseDialog) {
        AlertDialog(
            onDismissRequest = { showDisguiseDialog = false },
            title = { Text("Disguise App Name") },
            text = {
                Column {
                    Text("Choose a stealth decoy label for ${app.label}:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = disguiseText,
                        onValueChange = { disguiseText = it },
                        placeholder = { Text("e.g. Calculator, Notes, System") },
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
private fun AttachedAppWidgetCard(app: AppItem) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pop-up Widget Slot",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Attached",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Live telemetry & instant actions attached directly to ${app.label}. One swipe brings the widget without cluttering the homescreen.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PopupActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
