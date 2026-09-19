package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalendarEvent
import com.example.data.ClockStyle
import com.example.data.WeatherInfo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ClockHeader(
    clockStyle: ClockStyle,
    showSeconds: Boolean,
    showDate: Boolean,
    weather: WeatherInfo,
    upcomingEvent: CalendarEvent?,
    fontScale: Float,
    isLeftHanded: Boolean,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            delay(1000)
        }
    }

    val timeFormat = if (showSeconds) "h:mm:ss" else "h:mm"
    val timeString = remember(currentTime, showSeconds) {
        SimpleDateFormat(timeFormat, Locale.getDefault()).format(currentTime.time)
    }
    val amPmString = remember(currentTime) {
        SimpleDateFormat("a", Locale.getDefault()).format(currentTime.time)
    }
    val dateString = remember(currentTime) {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(currentTime.time)
    }

    val alignment = if (isLeftHanded) Alignment.End else Alignment.Start

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("clock_header"),
        horizontalAlignment = alignment
    ) {
        // Clock Display
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClockClick)
                .padding(vertical = 4.dp)
        ) {
            when (clockStyle) {
                ClockStyle.MINIMAL_DIGITAL -> {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = if (isLeftHanded) Arrangement.End else Arrangement.Start
                    ) {
                        Text(
                            text = timeString,
                            fontSize = (46 * fontScale).sp,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = amPmString,
                            fontSize = (16 * fontScale).sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                ClockStyle.BOLD_MODERN -> {
                    Text(
                        text = timeString,
                        fontSize = (54 * fontScale).sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-2).sp
                    )
                }

                ClockStyle.TEXT_BASED -> {
                    val wordTime = remember(currentTime) {
                        toWordClock(currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE))
                    }
                    Text(
                        text = wordTime,
                        fontSize = (26 * fontScale).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 32.sp
                    )
                }

                ClockStyle.ANALOG -> {
                    AnalogClockCanvas(
                        calendar = currentTime,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        textColor = MaterialTheme.colorScheme.onBackground,
                        showSeconds = showSeconds
                    )
                }

                ClockStyle.FLUX_INLINE -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isLeftHanded) Arrangement.End else Arrangement.Start,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onClockClick)
                    ) {
                        Text(
                            text = timeString,
                            fontSize = (24 * fontScale).sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${weather.temperatureCelsius}°C",
                            fontSize = (20 * fontScale).sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onWeatherClick)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🌧️",
                            fontSize = (18 * fontScale).sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onWeatherClick)
                        )
                    }
                }

                ClockStyle.COMPACT -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeString,
                            fontSize = (28 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• $dateString",
                            fontSize = (14 * fontScale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Date and Weather Row
        if (showDate && clockStyle != ClockStyle.COMPACT && clockStyle != ClockStyle.FLUX_INLINE) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isLeftHanded) Arrangement.End else Arrangement.Start,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = dateString,
                    fontSize = (15 * fontScale).sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onDateClick)
                        .padding(end = 12.dp, top = 2.dp, bottom = 2.dp)
                )

                // Weather Capsule
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onWeatherClick)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(text = weather.conditionIcon, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${weather.temperatureCelsius}°C",
                            fontSize = (12 * fontScale).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Upcoming Calendar Agenda Capsule
        if (upcomingEvent != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onDateClick)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${upcomingEvent.time} • ${upcomingEvent.title}",
                        fontSize = (12 * fontScale).sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalogClockCanvas(
    calendar: Calendar,
    primaryColor: Color,
    textColor: Color,
    showSeconds: Boolean
) {
    Canvas(modifier = Modifier.size(88.dp).padding(4.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f

        // Clock Outer Ring
        drawCircle(
            color = textColor.copy(alpha = 0.15f),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )

        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        // Hour Hand
        val hourAngle = Math.toRadians((hour * 30.0) + (minute * 0.5) - 90.0)
        val hourLen = radius * 0.52f
        val hourEnd = Offset(
            (center.x + hourLen * cos(hourAngle)).toFloat(),
            (center.y + hourLen * sin(hourAngle)).toFloat()
        )
        drawLine(
            color = textColor,
            start = center,
            end = hourEnd,
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Minute Hand
        val minAngle = Math.toRadians((minute * 6.0) - 90.0)
        val minLen = radius * 0.76f
        val minEnd = Offset(
            (center.x + minLen * cos(minAngle)).toFloat(),
            (center.y + minLen * sin(minAngle)).toFloat()
        )
        drawLine(
            color = textColor.copy(alpha = 0.9f),
            start = center,
            end = minEnd,
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Second Hand
        if (showSeconds) {
            val secAngle = Math.toRadians((second * 6.0) - 90.0)
            val secLen = radius * 0.85f
            val secEnd = Offset(
                (center.x + secLen * cos(secAngle)).toFloat(),
                (center.y + secLen * sin(secAngle)).toFloat()
            )
            drawLine(
                color = primaryColor,
                start = center,
                end = secEnd,
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Center Pin
        drawCircle(color = primaryColor, radius = 3.dp.toPx(), center = center)
    }
}

private fun toWordClock(hour24: Int, minute: Int): String {
    val hours = listOf(
        "Twelve", "One", "Two", "Three", "Four", "Five",
        "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve"
    )
    val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
    val currentHourName = hours[hour12]
    val nextHourName = hours[(hour12 % 12) + 1]

    return when (minute) {
        0 -> "$currentHourName o'clock"
        15 -> "Quarter past $currentHourName"
        30 -> "Half past $currentHourName"
        45 -> "Quarter to $nextHourName"
        in 1..29 -> "$minute past $currentHourName"
        else -> "${60 - minute} to $nextHourName"
    }
}
