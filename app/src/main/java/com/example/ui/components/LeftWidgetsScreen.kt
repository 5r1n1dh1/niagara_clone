package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.appwidget.AppWidgetManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Left-most Page: Houses user-customizable widgets with full CRUD
 * (Create, Read, Update, Delete).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeftWidgetsScreen(
    widgets: List<CustomWidgetConfig>,
    weather: WeatherInfo,
    mediaPlayer: MediaPlaybackState,
    calendarEvents: List<CalendarEvent>,
    isHeadphonesConnected: Boolean,
    isFrostedTheme: Boolean = false,
    blurRadius: Float = 15f,
    translucency: Float = 0.30f,
    glassEffect: Float = 0.30f,
    appWidgetHost: android.appwidget.AppWidgetHost? = null,
    appWidgetManager: android.appwidget.AppWidgetManager? = null,
    onAddWidgetClick: () -> Unit,
    onAddSystemWidgetClick: () -> Unit = {},
    onDeleteWidget: (String) -> Unit,
    onReorderWidget: (from: Int, to: Int) -> Unit,
    onEditNoteClick: (CustomWidgetConfig) -> Unit,
    onToggleMediaPlay: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit,
    onToggleHeadphones: () -> Unit,
    onRefreshWeather: () -> Unit,
    onOpenWeather: () -> Unit = {},
    onOpenClock: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onUpdateWidgetSize: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var isEditMode by remember { mutableStateOf(false) }
    var selectedWidgetForOptions by remember { mutableStateOf<CustomWidgetConfig?>(null) }

    FrostedGlassBackground(
        isFrostedTheme = isFrostedTheme,
        blurStrength = blurRadius,
        translucency = translucency,
        glassEffect = glassEffect,
        modifier = modifier.testTag("left_widgets_screen")
    ) {
        if (selectedWidgetForOptions != null) {
            val targetWidget = selectedWidgetForOptions!!
            val widgetIndex = widgets.indexOfFirst { it.id == targetWidget.id }
            WidgetResizeAndOptionsSheet(
                widget = targetWidget,
                canMoveUp = widgetIndex > 0,
                canMoveDown = widgetIndex >= 0 && widgetIndex < widgets.size - 1,
                onUpdateSize = { newSz ->
                    onUpdateWidgetSize(targetWidget.id, newSz)
                    selectedWidgetForOptions = targetWidget.copy(heightCategory = newSz)
                },
                onMoveUp = {
                    if (widgetIndex > 0) {
                        onReorderWidget(widgetIndex, widgetIndex - 1)
                    }
                },
                onMoveDown = {
                    if (widgetIndex >= 0 && widgetIndex < widgets.size - 1) {
                        onReorderWidget(widgetIndex, widgetIndex + 1)
                    }
                },
                onDelete = {
                    onDeleteWidget(targetWidget.id)
                    selectedWidgetForOptions = null
                },
                onDismiss = { selectedWidgetForOptions = null }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        // Combined 1 Top Action Button with Dropdown Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var menuExpanded by remember { mutableStateOf(false) }

            Box {
                FilledTonalButton(
                    onClick = { menuExpanded = true },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("manage_widgets_button")
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Filled.Check else Icons.Filled.Widgets,
                        contentDescription = "Manage Widgets",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEditMode) "Done Editing" else "Manage Widgets",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Launcher Widget") },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddWidgetClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add System App Widget") },
                        leadingIcon = { Icon(Icons.Filled.Extension, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddSystemWidgetClick()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(if (isEditMode) "Exit Edit Mode" else "Customize & Resize") },
                        leadingIcon = { Icon(if (isEditMode) Icons.Filled.Done else Icons.Filled.Tune, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            isEditMode = !isEditMode
                        }
                    )
                }
            }
        }

        if (widgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🧩", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Widgets Added",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap Manage Widgets to add customized or system widgets",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onAddWidgetClick) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Widget")
                        }
                        OutlinedButton(onClick = onAddSystemWidgetClick) {
                            Icon(Icons.Filled.Extension, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("System Widget")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                itemsIndexed(widgets, key = { index, w -> "widget_${w.id}_$index" }) { index, item ->
                    Box(modifier = Modifier.animateItem()) {
                        LeftWidgetCardWrapper(
                            widget = item,
                            index = index,
                            totalCount = widgets.size,
                            isEditMode = isEditMode,
                            weather = weather,
                            mediaPlayer = mediaPlayer,
                            calendarEvents = calendarEvents,
                            isHeadphonesConnected = isHeadphonesConnected,
                            blurRadius = blurRadius,
                            translucency = translucency,
                            glassEffect = glassEffect,
                            appWidgetHost = appWidgetHost,
                            appWidgetManager = appWidgetManager,
                            onOptionsClick = { selectedWidgetForOptions = item },
                            onDelete = { onDeleteWidget(item.id) },
                            onMoveUp = { onReorderWidget(index, index - 1) },
                            onMoveDown = { onReorderWidget(index, index + 1) },
                            onEditNote = { onEditNoteClick(item) },
                            onToggleMediaPlay = onToggleMediaPlay,
                            onMediaNext = onMediaNext,
                            onMediaPrev = onMediaPrev,
                            onToggleHeadphones = onToggleHeadphones,
                            onRefreshWeather = onRefreshWeather,
                            onOpenWeather = onOpenWeather,
                            onOpenClock = onOpenClock,
                            onUpdateSize = { newSize -> onUpdateWidgetSize(item.id, newSize) }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun LeftWidgetCardWrapper(
    widget: CustomWidgetConfig,
    index: Int,
    totalCount: Int,
    isEditMode: Boolean,
    weather: WeatherInfo,
    mediaPlayer: MediaPlaybackState,
    calendarEvents: List<CalendarEvent>,
    isHeadphonesConnected: Boolean,
    blurRadius: Float = 15f,
    translucency: Float = 0.30f,
    glassEffect: Float = 0.30f,
    appWidgetHost: android.appwidget.AppWidgetHost? = null,
    appWidgetManager: android.appwidget.AppWidgetManager? = null,
    onOptionsClick: () -> Unit = {},
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditNote: () -> Unit,
    onToggleMediaPlay: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit,
    onToggleHeadphones: () -> Unit,
    onRefreshWeather: () -> Unit,
    onOpenWeather: () -> Unit = {},
    onOpenClock: () -> Unit = {},
    onUpdateSize: (String) -> Unit = {}
) {
    FrostedGlassPanel(
        cornerRadius = 20.dp,
        blurRadius = blurRadius.dp,
        translucency = translucency,
        glassEffect = glassEffect,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("left_widget_${widget.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Card Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = widget.type.iconEmoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = widget.title.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Size Badge (Static indicator)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.testTag("widget_size_badge_${widget.id}")
                    ) {
                        Text(
                            text = widget.heightCategory,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Edit controls & size picker chips
                if (isEditMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val sizes = listOf("4x1", "4x2", "4x3", "4x4")
                        sizes.forEach { sz ->
                            val isSelected = widget.heightCategory == sz
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .clickable { onUpdateSize(sz) }
                                    .testTag("resize_chip_${widget.id}_$sz")
                            ) {
                                Text(
                                    text = sz,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        if (index > 0) {
                            IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(16.dp))
                            }
                        }
                        if (index < totalCount - 1) {
                            IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(16.dp))
                            }
                        }
                        if (widget.type == WidgetType.QUICK_NOTE) {
                            IconButton(onClick = onEditNote, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit note", modifier = Modifier.size(15.dp))
                            }
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete widget", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    IconButton(
                        onClick = onOptionsClick,
                        modifier = Modifier.size(28.dp).testTag("widget_options_btn_${widget.id}")
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Widget options and resize",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Container size constraints based on heightCategory
            val bodyModifier = when (widget.heightCategory) {
                "4x1" -> Modifier.fillMaxWidth().heightIn(min = 70.dp, max = 100.dp)
                "4x2" -> Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 185.dp)
                "4x3" -> Modifier.fillMaxWidth().heightIn(min = 230.dp, max = 275.dp)
                "4x4" -> Modifier.fillMaxWidth().heightIn(min = 310.dp, max = 365.dp)
                else -> Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 185.dp)
            }

            Box(modifier = bodyModifier) {
                // Body depending on widget type
                when (widget.type) {
                    WidgetType.CLOCK -> {
                        ClockWidgetBody(size = widget.heightCategory, onOpenClock = onOpenClock)
                    }
                    WidgetType.WEATHER_FORECAST -> {
                        WeatherWidgetBody(weather = weather, size = widget.heightCategory, onRefresh = onRefreshWeather, onOpenWeather = onOpenWeather)
                    }
                    WidgetType.NEWS_FEED -> {
                        NewsWidgetBody(size = widget.heightCategory)
                    }
                    WidgetType.MEDIA_PLAYER -> {
                        YouTubeMusicFluxWidget(
                            media = mediaPlayer,
                            size = widget.heightCategory,
                            isHeadphonesOverride = isHeadphonesConnected,
                            onTogglePlay = onToggleMediaPlay,
                            onNext = onMediaNext,
                            onPrev = onMediaPrev
                        )
                    }
                    WidgetType.SYSTEM_WIDGET -> {
                        SystemAppWidgetView(
                            appWidgetId = widget.appWidgetId,
                            heightCategory = widget.heightCategory,
                            appWidgetHost = appWidgetHost,
                            appWidgetManager = appWidgetManager
                        )
                    }
                    WidgetType.QUICK_NOTE -> {
                        QuickNoteWidgetBody(content = widget.content, size = widget.heightCategory, onEdit = onEditNote)
                    }
                    WidgetType.MINDFULNESS_QUOTE -> {
                        MindfulnessWidgetBody(size = widget.heightCategory)
                    }
                    WidgetType.DUAL_CLOCK -> {
                        DualClockWidgetBody(size = widget.heightCategory, onOpenClock = onOpenClock)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherWidgetBody(
    weather: WeatherInfo,
    size: String = "4x2",
    onRefresh: () -> Unit,
    onOpenWeather: () -> Unit = {}
) {
    Box(modifier = Modifier.clickable(onClick = onOpenWeather)) {
        when (size) {
        "4x1" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = weather.conditionIcon, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${weather.temperatureCelsius}°C  •  ${weather.condition}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${weather.location} • H:${weather.highTemp}° L:${weather.lowTemp}°",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh weather", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        "4x3" -> {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = weather.conditionIcon, fontSize = 34.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${weather.temperatureCelsius}°C  •  ${weather.condition}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${weather.location} • Rain ${weather.rainProbability}% • H:${weather.highTemp}° L:${weather.lowTemp}°",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh weather", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weather.hourlyForecast.take(5).forEach { item ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = item.time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = item.icon, fontSize = 18.sp, modifier = Modifier.padding(vertical = 2.dp))
                            Text(text = "${item.temp}°", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Humidity", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("58%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Wind", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("14 km/h", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("UV Index", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("4 Moderate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        "4x4" -> {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = weather.conditionIcon, fontSize = 38.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${weather.temperatureCelsius}°C  •  ${weather.condition}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${weather.location} • Rain ${weather.rainProbability}% • H:${weather.highTemp}° L:${weather.lowTemp}°",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh weather", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weather.hourlyForecast.forEach { item ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = item.time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = item.icon, fontSize = 18.sp, modifier = Modifier.padding(vertical = 2.dp))
                            Text(text = "${item.temp}°", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val upcomingDays = listOf(
                    Triple("Tomorrow", "⛅", "25° / 17°"),
                    Triple("Wednesday", "🌧️", "21° / 15°"),
                    Triple("Thursday", "☀️", "26° / 18°")
                )
                upcomingDays.forEach { (day, ic, t) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(day, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ic, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(t, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        else -> { // Default 4x2
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = weather.conditionIcon, fontSize = 34.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${weather.temperatureCelsius}°C  •  ${weather.condition}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${weather.location} • Rain ${weather.rainProbability}% • H:${weather.highTemp}° L:${weather.lowTemp}°",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh weather", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weather.hourlyForecast.forEach { item ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = item.time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = item.icon, fontSize = 18.sp, modifier = Modifier.padding(vertical = 2.dp))
                            Text(text = "${item.temp}°", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun MediaWidgetBody(
    media: MediaPlaybackState,
    isHeadphones: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleHeadphones: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.trackTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${media.artistName} • ${media.appName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onTogglePlay, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = if (media.isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { media.progressPercent },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = media.currentTimeText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (isHeadphones) "🎧 Plugged In" else "🔊 Speaker",
                fontSize = 10.sp,
                color = if (isHeadphones) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onToggleHeadphones)
            )
            Text(text = media.durationText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // YouTube Music Quick Access Buttons (Requirement 12)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://music.youtube.com/library")).apply {
                            setPackage("com.google.android.apps.youtube.music")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://music.youtube.com/library")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f).height(28.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("YT Library", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://music.youtube.com/library/playlists")).apply {
                            setPackage("com.google.android.apps.youtube.music")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://music.youtube.com/library/playlists")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f).height(28.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.PlaylistPlay, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Playlists", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickNoteWidgetBody(content: String, size: String = "4x2", onEdit: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onEdit)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val maxLines = when (size) {
                "4x1" -> 1
                "4x2" -> 3
                "4x3" -> 7
                "4x4" -> 12
                else -> 3
            }
            Text(
                text = if (content.isBlank()) "Tap here to write your quick thoughts or to-do notes..." else content,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = maxLines,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = if (content.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            if (size != "4x1") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (content.isNotBlank()) "${content.length} chars" else "",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tap to edit", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MindfulnessWidgetBody(size: String = "4x2") {
    Column {
        Text(
            text = "\"Simplicity is about subtracting the obvious and adding the meaningful.\"",
            fontSize = if (size == "4x1") 12.sp else 13.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (size == "4x1") 2 else 4
        )
        if (size != "4x1") {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Screen Time: 1h 42m", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                Text("Mindful Breaks: 3", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (size == "4x3" || size == "4x4") {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { 0.38f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Focus Goal: 3h max today (1h 18m remaining)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ClockWidgetBody(size: String = "4x2", onOpenClock: () -> Unit = {}) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            kotlinx.coroutines.delay(1000)
        }
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val shortTimeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    val shortDateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    Box(modifier = Modifier.fillMaxSize().clickable(onClick = onOpenClock)) {
    when (size) {
        "4x1" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeFormat.format(currentTime.time),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${shortDateFormat.format(currentTime.time)} • ${currentTime.timeZone.id.substringAfterLast('/')}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        "4x3" -> {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = timeFormat.format(currentTime.time),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dateFormat.format(currentTime.time),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentTime.timeZone.id.substringAfterLast('/'),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("London (GMT)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("10:45", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tokyo (JST)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("19:45", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("New York (EST)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("05:45", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        "4x4" -> {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = timeFormat.format(currentTime.time),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dateFormat.format(currentTime.time),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentTime.timeZone.id.substringAfterLast('/'),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("London", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("10:45", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tokyo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("19:45", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("New York", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("05:45", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Next Alarm: 07:00 AM", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Icon(Icons.Filled.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        else -> { // Default 4x2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = timeFormat.format(currentTime.time),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateFormat.format(currentTime.time),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentTime.timeZone.id.substringAfterLast('/'),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun DualClockWidgetBody(size: String = "4x2", onOpenClock: () -> Unit = {}) {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val now = Date()
    Box(modifier = Modifier.fillMaxSize().clickable(onClick = onOpenClock)) {
    when (size) {
        "4x1" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LOCAL: ${sdf.format(now)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("•", color = MaterialTheme.colorScheme.outline)
                Text("TOKYO: 23:45", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        "4x3", "4x4" -> {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LOCAL", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(sdf.format(now), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    VerticalDivider(modifier = Modifier.height(36.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOKYO (JST)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("23:45", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEW YORK (EST)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("09:45", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LONDON (GMT)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("14:45", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        else -> { // Default 4x2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LOCAL", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(sdf.format(now), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                VerticalDivider(modifier = Modifier.height(30.dp).width(1.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOKYO (JST)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("23:45", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
}

/**
 * Dialog to add a new widget to the left dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWidgetDialog(
    availableWidgets: List<WidgetType> = WidgetType.entries,
    onAddWidget: (WidgetType) -> Unit,
    onDismiss: () -> Unit,
    blurRadius: Float = 15f,
    translucency: Float = 0.30f,
    glassEffect: Float = 0.30f
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.45f)
    ) {
        FrostedGlassPanel(
            cornerRadius = 28.dp,
            blurRadius = blurRadius.dp,
            translucency = translucency,
            glassEffect = glassEffect,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .navigationBarsPadding()
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add Available Widget",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${availableWidgets.size} available widgets for this device",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableWidgets.size) { i ->
                    val type = availableWidgets[i]
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onAddWidget(type) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = type.iconEmoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(text = type.displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text(text = type.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
}

/**
 * Dialog to edit Sticky Note widget content.
 */
@Composable
fun EditNoteDialog(
    widget: CustomWidgetConfig,
    onSave: (title: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(widget.title) }
    var content by remember { mutableStateOf(widget.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Note Widget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Widget Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note Content") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(title, content)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private data class NewsStory(
    val title: String,
    val source: String,
    val timeAgo: String,
    val category: String,
    val imageUrl: String,
    val linkUrl: String = ""
)

private val sampleNewsStories = listOf(
    NewsStory(
        title = "NASA's James Webb Telescope Discovers Atmospheric Water Vapor on Exoplanet",
        source = "Space & Science Daily",
        timeAgo = "18m ago",
        category = "SPACE",
        imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&q=80",
        linkUrl = "https://news.google.com"
    ),
    NewsStory(
        title = "Next-Gen Quantum Chips Achieve Record 99.9% Gate Fidelity at Room Temp",
        source = "Tech Discovery Wire",
        timeAgo = "42m ago",
        category = "TECH",
        imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&q=80",
        linkUrl = "https://news.google.com"
    ),
    NewsStory(
        title = "Global Clean Energy Power Grid Surpasses 2 Terawatt Milestone Target",
        source = "World Eco Journal",
        timeAgo = "2h ago",
        category = "ENERGY",
        imageUrl = "https://images.unsplash.com/photo-1497435334941-8c899ee9e8e9?w=600&q=80",
        linkUrl = "https://news.google.com"
    )
)

@Composable
private fun NewsWidgetBody(size: String = "4x2") {
    val context = LocalContext.current
    var stories by remember { mutableStateOf(sampleNewsStories) }
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                val inputStream = conn.inputStream
                val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(inputStream, "UTF-8")

                val fetched = mutableListOf<NewsStory>()
                var eventType = parser.eventType
                var curTitle = ""
                var curSource = "Google News"
                var curPubDate = "Recent"
                var curLink = "https://news.google.com"
                var inItem = false

                val cats = listOf("WORLD", "TECH", "BUSINESS", "SCIENCE", "TOP NEWS")
                val imgs = listOf(
                    "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&q=80",
                    "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&q=80",
                    "https://images.unsplash.com/photo-1497435334941-8c899ee9e8e9?w=600&q=80",
                    "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=600&q=80"
                )

                while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT && fetched.size < 8) {
                    val tag = parser.name
                    when (eventType) {
                        org.xmlpull.v1.XmlPullParser.START_TAG -> {
                            if ("item".equals(tag, true)) {
                                inItem = true
                                curTitle = ""
                                curSource = "Google News"
                                curPubDate = "Recent"
                                curLink = "https://news.google.com"
                            } else if (inItem) {
                                when (tag?.lowercase()) {
                                    "title" -> curTitle = parser.nextText().trim()
                                    "source" -> curSource = parser.nextText().trim()
                                    "pubdate" -> {
                                        val pd = parser.nextText().trim()
                                        curPubDate = if (pd.length > 16) pd.substring(0, 16) else pd
                                    }
                                    "link" -> curLink = parser.nextText().trim()
                                }
                            }
                        }
                        org.xmlpull.v1.XmlPullParser.END_TAG -> {
                            if ("item".equals(tag, true)) {
                                inItem = false
                                if (curTitle.isNotBlank()) {
                                    fetched.add(
                                        NewsStory(
                                            title = curTitle,
                                            source = curSource,
                                            timeAgo = curPubDate,
                                            category = cats[fetched.size % cats.size],
                                            imageUrl = imgs[fetched.size % imgs.size],
                                            linkUrl = curLink
                                        )
                                    )
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
                conn.disconnect()
                if (fetched.isNotEmpty()) {
                    stories = fetched
                }
            } catch (_: Exception) {}
        }
    }

    val currentStory = stories[currentIndex % stories.size]

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentStory.linkUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 86.dp, height = 72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                coil.compose.AsyncImage(
                    model = currentStory.imageUrl,
                    contentDescription = currentStory.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = currentStory.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${currentStory.source} • ${currentStory.timeAgo}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = currentStory.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                stories.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentIndex) 16.dp else 6.dp, 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { currentIndex = (currentIndex - 1 + stories.size) % stories.size },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous story", modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { currentIndex = (currentIndex + 1) % stories.size },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next story", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun SystemAppWidgetView(
    appWidgetId: Int,
    heightCategory: String = "4x2",
    appWidgetHost: android.appwidget.AppWidgetHost?,
    appWidgetManager: android.appwidget.AppWidgetManager?,
    modifier: Modifier = Modifier
) {
    val targetHeight = when (heightCategory) {
        "4x1" -> 85.dp
        "4x2" -> 170.dp
        "4x3" -> 255.dp
        "4x4" -> 340.dp
        else -> 170.dp
    }

    if (appWidgetId <= 0 || appWidgetHost == null || appWidgetManager == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(targetHeight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "System widget not configured",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val info = remember(appWidgetId) {
        try {
            appWidgetManager.getAppWidgetInfo(appWidgetId)
        } catch (_: Throwable) {
            null
        }
    }

    if (info == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(targetHeight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Widget not available",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    val minHeightDpInt = targetHeight.value.toInt()
    val minWidthDpInt = 280
    val maxHeightDpInt = targetHeight.value.toInt()
    val maxWidthDpInt = 380

    LaunchedEffect(appWidgetId, heightCategory) {
        try {
            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidthDpInt)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidthDpInt)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeightDpInt)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeightDpInt)
            }
            appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
        } catch (_: Throwable) {}
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            try {
                val hostView = appWidgetHost.createView(ctx, appWidgetId, info)
                hostView.setAppWidget(appWidgetId, info)
                try {
                    val options = Bundle().apply {
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidthDpInt)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidthDpInt)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeightDpInt)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeightDpInt)
                    }
                    hostView.updateAppWidgetSize(options, minWidthDpInt, minHeightDpInt, maxWidthDpInt, maxHeightDpInt)
                } catch (_: Throwable) {}
                hostView
            } catch (e: Throwable) {
                android.widget.TextView(ctx).apply {
                    text = "Widget load error: ${e.message}"
                    textSize = 12f
                }
            }
        },
        update = { view ->
            try {
                val hostView = view as? android.appwidget.AppWidgetHostView
                if (hostView != null) {
                    val options = Bundle().apply {
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidthDpInt)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidthDpInt)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeightDpInt)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeightDpInt)
                    }
                    hostView.updateAppWidgetSize(options, minWidthDpInt, minHeightDpInt, maxWidthDpInt, maxHeightDpInt)
                }
            } catch (_: Throwable) {}
        },
        modifier = modifier
            .fillMaxWidth()
            .height(targetHeight)
    )
}

/**
 * Bottom Sheet modal to resize and configure any widget in the Left Widgets screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetResizeAndOptionsSheet(
    widget: CustomWidgetConfig,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onUpdateSize: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    blurRadius: Float = 15f,
    translucency: Float = 0.30f,
    glassEffect: Float = 0.30f
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        modifier = Modifier.testTag("widget_resize_sheet")
    ) {
        FrostedGlassPanel(
            cornerRadius = 28.dp,
            blurRadius = blurRadius.dp,
            translucency = translucency,
            glassEffect = glassEffect,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .navigationBarsPadding()
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = widget.type.iconEmoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = widget.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Resize & Manage Widget",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Choose Size",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val sizeOptions = listOf(
                Pair("4x1", "Compact"),
                Pair("4x2", "Standard"),
                Pair("4x3", "Large"),
                Pair("4x4", "Full")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sizeOptions.forEach { (sz, title) ->
                    val isSelected = widget.heightCategory == sz
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 0.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onUpdateSize(sz) }
                            .testTag("sheet_resize_option_$sz")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = sz,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Move Up", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Move Down", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
}
