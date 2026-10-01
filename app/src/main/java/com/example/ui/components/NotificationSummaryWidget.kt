package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotificationItem

enum class DigestCategory(val label: String, val emoji: String) {
    ALL("All Digest", "🔕"),
    SOCIAL("Social", "💬"),
    PROMOS("Promos & Deals", "🏷️"),
    UPDATES("App Updates", "⚡"),
    GAMES("Games & Media", "🎮")
}

fun isLowPriorityNotification(packageName: String, sender: String, message: String): Boolean {
    val pkg = packageName.lowercase()
    val send = sender.lowercase()
    val msg = message.lowercase()

    val socialPackages = listOf("instagram", "facebook", "twitter", "x.app", "tiktok", "reddit", "linkedin", "pinterest", "snapchat")
    val promoKeywords = listOf("sale", "off", "discount", "deal", "promo", "coupon", "limited time", "cashback", "free shipping", "order update")
    val socialKeywords = listOf("liked your", "started following", "commented", "retweeted", "mentioned you", "shared a post", "trending in")

    if (socialPackages.any { pkg.contains(it) }) return true
    if (promoKeywords.any { send.contains(it) || msg.contains(it) }) return true
    if (socialKeywords.any { send.contains(it) || msg.contains(it) }) return true

    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSummaryWidget(
    activeNotifications: Map<String, NotificationItem> = emptyMap(),
    size: String = "4x2",
    onDismissNotification: (packageName: String, isDualApp: Boolean) -> Unit = { _, _ -> },
    onReplyNotification: (packageName: String, text: String, isDualApp: Boolean) -> Unit = { _, _, _ -> },
    onClearAllDigest: () -> Unit = {},
    onSimulateNotification: (pkg: String, sender: String, msg: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(DigestCategory.ALL) }
    var isExpanded by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val liveDigestList = remember(activeNotifications) {
        activeNotifications.values.filter {
            isLowPriorityNotification(it.packageName, it.sender, it.message)
        }
    }

    // Hide widget entirely if there are no real notifications summary to display
    if (liveDigestList.isEmpty()) {
        Box(modifier = Modifier.size(0.dp))
        return
    }

    val displayList = remember(liveDigestList, selectedCategory) {
        when (selectedCategory) {
            DigestCategory.ALL -> liveDigestList
            DigestCategory.SOCIAL -> liveDigestList.filter { it.packageName.contains("instagram") || it.packageName.contains("reddit") || it.sender.contains("Instagram") || it.sender.contains("Social") }
            DigestCategory.PROMOS -> liveDigestList.filter { it.packageName.contains("amazon") || it.message.contains("Deal") || it.message.contains("off") || it.sender.contains("Deals") }
            DigestCategory.UPDATES -> liveDigestList.filter { it.packageName.contains("android") || it.message.contains("update") }
            DigestCategory.GAMES -> liveDigestList.filter { it.packageName.contains("game") || it.sender.contains("Game") }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("notification_summary_widget")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🔕", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Notification Digest",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${liveDigestList.size} quiet alerts bundled",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFilterSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Digest Settings", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Filter", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onClearAllDigest,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Clear Digest",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DigestCategory.entries.take(3).forEach { cat ->
                val isSelected = cat == selectedCategory
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = "${cat.emoji} ${cat.label}",
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Digest Notification List
        if (displayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsOff,
                        contentDescription = "All Quiet",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Primary Feed Quiet ✨",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Social media & promo alerts are grouped here quietly",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val maxItems = when (size) {
                "4x1" -> 1
                "4x2" -> 2
                "4x3" -> 4
                "4x4" -> 6
                else -> 2
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(displayList.take(maxItems), key = { it.id }) { notif ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = notif.sender,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = notif.timeAgo,
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = notif.message,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { onDismissNotification(notif.packageName, notif.isDualApp) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter Options Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = Color.Transparent,
            scrimColor = Color.Black.copy(alpha = 0.45f)
        ) {
            FrostedGlassPanel(
                cornerRadius = 24.dp,
                blurRadius = 15.dp,
                translucency = 0.35f,
                glassEffect = 0.30f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Notification Digest Preferences",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select alert types to bundle into quiet summary digest:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val categories = listOf(
                        "Social Media Likes & Comments" to true,
                        "Shopping Discounts & Promotions" to true,
                        "News Headlines & Media Recommendations" to true,
                        "Game Invites & Community Highlights" to true
                    )

                    categories.forEach { (catName, initialCheck) ->
                        var isChecked by remember { mutableStateOf(initialCheck) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(catName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { isChecked = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            showFilterSheet = false
                            onSimulateNotification("com.instagram.android", "Instagram", "alex_dev started following you")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simulate Digest Alert & Save")
                    }
                }
            }
        }
    }
}
