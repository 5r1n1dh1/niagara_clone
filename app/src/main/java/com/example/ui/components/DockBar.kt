package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.AppItem
import com.example.data.IconPackType

/**
 * Stateless presentational DockBar component adhering to Phase 4.
 */
@Composable
fun DockBar(
    dockApps: List<AppItem>,
    iconPackType: IconPackType,
    onAppClick: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        dockApps.forEach { app ->
            AppIconRenderer(
                app = app,
                iconPackType = iconPackType
            )
        }
    }
}
