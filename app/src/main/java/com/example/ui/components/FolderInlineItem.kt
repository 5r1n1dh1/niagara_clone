package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppItem
import com.example.data.FolderItem
import com.example.data.IconPackType

@Composable
fun FolderInlineItem(
    folder: FolderItem,
    appsInFolder: List<AppItem>,
    iconPackType: IconPackType,
    isLeftHanded: Boolean,
    fontScale: Float,
    onToggleExpand: () -> Unit,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    onSwipeRightForPopup: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .testTag("folder_${folder.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isLeftHanded) Arrangement.End else Arrangement.Start
        ) {
            if (!isLeftHanded) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = folder.iconEmoji, fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = if (isLeftHanded) Alignment.End else Alignment.Start
            ) {
                Text(
                    text = folder.name,
                    fontSize = (16 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${appsInFolder.size} apps",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLeftHanded) {
                Spacer(modifier = Modifier.width(16.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = folder.iconEmoji, fontSize = 20.sp)
                    }
                }
            } else {
                Icon(
                    imageVector = if (folder.isExpanded) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        // Inline Compact Expanded Apps List
        AnimatedVisibility(
            visible = folder.isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (!isLeftHanded) 28.dp else 0.dp,
                        end = if (isLeftHanded) 28.dp else 0.dp
                    )
            ) {
                appsInFolder.forEach { app ->
                    AppListItem(
                        app = app,
                        iconPackType = iconPackType,
                        isLeftHanded = isLeftHanded,
                        fontScale = fontScale,
                        onAppClick = { onAppClick(app) },
                        onAppLongClick = { onAppLongClick(app) },
                        onSwipeRightForPopup = { onSwipeRightForPopup(app) },
                        onReplyNotification = {},
                        onDismissNotification = {}
                    )
                }
            }
        }
    }
}
