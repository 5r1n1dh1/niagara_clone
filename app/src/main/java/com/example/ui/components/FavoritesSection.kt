package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.AppItem
import com.example.data.IconPackType

/**
 * Stateless presentational FavoritesSection component adhering to Phase 4.
 */
@Composable
fun FavoritesSection(
    favorites: List<AppItem>,
    iconPackType: IconPackType,
    isLeftHanded: Boolean = false,
    fontScale: Float = 1.0f,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        favorites.forEach { app ->
            AppListItem(
                app = app,
                iconPackType = iconPackType,
                isLeftHanded = isLeftHanded,
                fontScale = fontScale,
                onAppClick = { onAppClick(app) },
                onAppLongClick = { onAppLongClick(app) },
                onReplyNotification = {},
                onDismissNotification = {}
            )
        }
    }
}
