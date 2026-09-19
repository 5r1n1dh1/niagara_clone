package com.example.ui.components

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
    onOpenCalendar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isEditMode by remember { mutableStateOf(false) }

    FrostedGlassBackground(
        isFrostedTheme = isFrostedTheme,
        modifier = modifier.testTag("left_widgets_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "WIDGETS DASHBOARD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Personalized Cards",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Edit / Done toggle button
                FilterChip(
                    selected = isEditMode,
                    onClick = { isEditMode = !isEditMode },
                    label = { Text(if (isEditMode) "Done" else "Customize") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isEditMode) Icons.Filled.Check else Icons.Filled.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                FilledTonalIconButton(
                    onClick = onAddSystemWidgetClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.Widgets, contentDescription = "Add System Widget", modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                FilledTonalIconButton(
                    onClick = onAddWidgetClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Widget", modifier = Modifier.size(20.dp))
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
                        text = "Tap the + button to add customized widgets to your left screen",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onAddWidgetClick) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add First Widget")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(widgets, key = { _, w -> w.id }) { index, item ->
                    LeftWidgetCardWrapper(
                        widget = item,
                        index = index,
                        totalCount = widgets.size,
                        isEditMode = isEditMode,
                        weather = weather,
                        mediaPlayer = mediaPlayer,
                        calendarEvents = calendarEvents,
                        isHeadphonesConnected = isHeadphonesConnected,
                        onDelete = { onDeleteWidget(item.id) },
                        onMoveUp = { onReorderWidget(index, index - 1) },
                        onMoveDown = { onReorderWidget(index, index + 1) },
                        onEditNote = { onEditNoteClick(item) },
                        onToggleMediaPlay = onToggleMediaPlay,
                        onMediaNext = onMediaNext,
                        onMediaPrev = onMediaPrev,
                        onToggleHeadphones = onToggleHeadphones,
                        onRefreshWeather = onRefreshWeather
                    )
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
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditNote: () -> Unit,
    onToggleMediaPlay: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrev: () -> Unit,
    onToggleHeadphones: () -> Unit,
    onRefreshWeather: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 2.dp,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = widget.type.iconEmoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = widget.title.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Edit controls
                if (isEditMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (index > 0) {
                            IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                            }
                        }
                        if (index < totalCount - 1) {
                            IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                            }
                        }
                        if (widget.type == WidgetType.QUICK_NOTE) {
                            IconButton(onClick = onEditNote, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit note", modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete widget", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body depending on widget type
            when (widget.type) {
                WidgetType.CLOCK -> {
                    ClockWidgetBody()
                }
                WidgetType.WEATHER_FORECAST -> {
                    WeatherWidgetBody(weather = weather, onRefresh = onRefreshWeather)
                }
                WidgetType.MEDIA_PLAYER -> {
                    MediaWidgetBody(
                        media = mediaPlayer,
                        isHeadphones = isHeadphonesConnected,
                        onTogglePlay = onToggleMediaPlay,
                        onNext = onMediaNext,
                        onPrev = onMediaPrev,
                        onToggleHeadphones = onToggleHeadphones
                    )
                }
                WidgetType.CALENDAR_AGENDA -> {
                    CalendarAgendaWidgetBody(events = calendarEvents)
                }
                WidgetType.QUICK_NOTE -> {
                    QuickNoteWidgetBody(content = widget.content, onEdit = onEditNote)
                }
                WidgetType.SYSTEM_TOGGLES -> {
                    SystemTogglesWidgetBody(isHeadphones = isHeadphonesConnected, onToggleHeadphones = onToggleHeadphones)
                }
                WidgetType.MINDFULNESS_QUOTE -> {
                    MindfulnessWidgetBody()
                }
                WidgetType.DAILY_HABITS -> {
                    DailyHabitsWidgetBody(initialHabits = widget.content)
                }
                WidgetType.DUAL_CLOCK -> {
                    DualClockWidgetBody()
                }
                WidgetType.SYSTEM_APPWIDGET -> {
                    HostedSystemWidgetView(
                        appWidgetId = widget.appWidgetId,
                        providerPackage = widget.providerPackage,
                        providerClass = widget.providerClass,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherWidgetBody(weather: WeatherInfo, onRefresh: () -> Unit) {
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
    }
}

@Composable
private fun CalendarAgendaWidgetBody(events: List<CalendarEvent>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (events.isEmpty()) {
            Text("No events scheduled today", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            events.take(3).forEach { ev ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = ev.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = ev.location, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(text = ev.time, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickNoteWidgetBody(content: String, onEdit: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onEdit)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (content.isBlank()) "Tap here to write your quick thoughts or to-do notes..." else content,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = if (content.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tap to edit", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SystemTogglesWidgetBody(isHeadphones: Boolean, onToggleHeadphones: () -> Unit) {
    var isWifiOn by remember { mutableStateOf(true) }
    var isBluetoothOn by remember { mutableStateOf(true) }
    var isFlashlightOn by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ToggleItemPill(icon = Icons.Filled.Wifi, label = "Wi-Fi", isActive = isWifiOn) { isWifiOn = !isWifiOn }
        ToggleItemPill(icon = Icons.Filled.Bluetooth, label = "Bluetooth", isActive = isBluetoothOn) { isBluetoothOn = !isBluetoothOn }
        ToggleItemPill(icon = Icons.Filled.Headphones, label = "Headset", isActive = isHeadphones, onClick = onToggleHeadphones)
        ToggleItemPill(icon = Icons.Filled.FlashlightOn, label = "Torch", isActive = isFlashlightOn) { isFlashlightOn = !isFlashlightOn }
    }
}

@Composable
private fun ToggleItemPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .width(72.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MindfulnessWidgetBody() {
    Column {
        Text(
            text = "\"Simplicity is about subtracting the obvious and adding the meaningful.\"",
            fontSize = 13.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Screen Time: 1h 42m", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            Text("Mindful Interventions: 3", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DailyHabitsWidgetBody(initialHabits: String) {
    val items = remember(initialHabits) {
        if (initialHabits.isNotBlank()) {
            initialHabits.lines().filter { it.isNotBlank() }
        } else {
            listOf("Morning walk", "Review agenda", "Drink water")
        }
    }
    val checkedStates = remember { mutableStateMapOf<Int, Boolean>() }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { i, habitText ->
            val checked = checkedStates[i] ?: false
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { checkedStates[i] = !checked }
            ) {
                Checkbox(checked = checked, onCheckedChange = { checkedStates[i] = it }, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = habitText,
                    fontSize = 13.sp,
                    color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ClockWidgetBody() {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            kotlinx.coroutines.delay(1000)
        }
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }

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

@Composable
private fun DualClockWidgetBody() {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val now = Date()
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

/**
 * Dialog to add a new widget to the left dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWidgetDialog(
    availableWidgets: List<WidgetType> = WidgetType.entries,
    onAddWidget: (WidgetType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
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
