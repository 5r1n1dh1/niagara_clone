package com.example.ui.components

import android.appwidget.AppWidgetHostView
import android.content.ComponentName
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.util.SystemWidgetManager

@Composable
fun HostedSystemWidgetView(
    appWidgetId: Int,
    providerPackage: String?,
    providerClass: String?,
    heightCategory: String = "4x2",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val providerInfo = remember(appWidgetId, providerPackage, providerClass) {
        SystemWidgetManager.getAppWidgetInfo(context, appWidgetId, providerPackage, providerClass)
    }

    val hostView = remember(appWidgetId, providerInfo) {
        if (appWidgetId > 0 && providerInfo != null) {
            SystemWidgetManager.createWidgetHostView(context, appWidgetId, providerInfo)
        } else null
    }

    val targetHeight = when (heightCategory) {
        "4x1" -> 90.dp
        "4x2" -> 190.dp
        "4x3" -> 290.dp
        "4x4" -> 390.dp
        else -> 190.dp
    }

    if (hostView != null) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = androidx.compose.ui.graphics.Color.Transparent,
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        ) {
            AndroidView(
                factory = { _ ->
                    hostView.apply {
                        setPadding(0, 0, 0, 0)
                    }
                },
                update = { v ->
                    v.setPadding(0, 0, 0, 0)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(targetHeight)
            )
        }
    } else {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "System Widget (${providerPackage ?: "unknown"})",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
