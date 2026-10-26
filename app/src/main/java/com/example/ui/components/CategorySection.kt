package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.AppItem

@Composable
fun CategorySection(
    title: String,
    apps: List<AppItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = title, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        apps.forEach { app ->
            AppListItem(
                app = app,
                iconPackType = com.example.data.IconPackType.MATERIAL,
                isLeftHanded = false,
                fontScale = 1f,
                onAppClick = {},
                onAppLongClick = {},
                onReplyNotification = {},
                onDismissNotification = {}
            )
        }
    }
}
