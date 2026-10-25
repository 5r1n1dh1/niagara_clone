package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.WidgetType

@Composable
fun CarouselWidgetPager(
    widgets: List<WidgetType>,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        widgets.forEach { widget ->
            WidgetCard(type = widget, modifier = Modifier.weight(1f))
        }
    }
}
