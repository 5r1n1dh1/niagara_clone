package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.exp

val ALPHABET_ITEMS = listOf('★') + ('A'..'Z').toList() + listOf('#')

/**
 * Niagara-style Fluid Wave Alphabet Scrollbar:
 * - Sliding your finger along the side edge of the apps screen creates a fluid, kinetic wave animation.
 * - Acts as an instant scroll index, jumping through your entire app drawer alphabetically in real-time.
 */
@Composable
fun WaveAlphabet(
    isLeftHanded: Boolean,
    selectedLetter: Char?,
    onLetterSelected: (Char?) -> Unit,
    onLetterConfirmed: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    var componentHeight by remember { mutableFloatStateOf(1f) }
    var componentWidth by remember { mutableFloatStateOf(1f) }
    var currentTouchY by remember { mutableStateOf<Float?>(null) }
    var currentTouchX by remember { mutableStateOf<Float?>(null) }

    val density = LocalDensity.current
    val verticalPaddingPx = with(density) { 24.dp.toPx() }
    val itemCount = ALPHABET_ITEMS.size
    val effectiveHeight = (componentHeight - 2 * verticalPaddingPx).coerceAtLeast(1f)
    val itemHeight = effectiveHeight / itemCount

    val processTouchPoint: (Float, Float) -> Unit = { x, y ->
        currentTouchX = x
        currentTouchY = y
        val relativeY = (y - verticalPaddingPx).coerceIn(0f, effectiveHeight - 1f)
        val index = (relativeY / itemHeight).toInt().coerceIn(0, itemCount - 1)
        val letter = ALPHABET_ITEMS[index]
        if (letter != selectedLetter) {
            onLetterSelected(letter)
        }
        onLetterConfirmed(letter)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(58.dp)
            .testTag("wave_alphabet")
            .onGloballyPositioned { coordinates ->
                componentHeight = coordinates.size.height.toFloat()
                componentWidth = coordinates.size.width.toFloat()
            }
            .pointerInput(componentHeight, isLeftHanded, verticalPaddingPx) {
                detectDragGestures(
                    onDragStart = { offset ->
                        processTouchPoint(offset.x, offset.y)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        processTouchPoint(change.position.x, change.position.y)
                    },
                    onDragEnd = {
                        currentTouchX = null
                        currentTouchY = null
                        onLetterSelected(null)
                    },
                    onDragCancel = {
                        currentTouchX = null
                        currentTouchY = null
                        onLetterSelected(null)
                    }
                )
            }
            .pointerInput(componentHeight, verticalPaddingPx) {
                detectTapGestures(
                    onPress = { offset ->
                        processTouchPoint(offset.x, offset.y)
                        tryAwaitRelease()
                        currentTouchX = null
                        currentTouchY = null
                        onLetterSelected(null)
                    }
                )
            },
        contentAlignment = if (isLeftHanded) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = if (isLeftHanded) Alignment.Start else Alignment.End
        ) {
            ALPHABET_ITEMS.forEachIndexed { index, letter ->
                val letterCenterY = verticalPaddingPx + (index + 0.5f) * itemHeight
                val touchY = currentTouchY

                // Kinetic Gaussian wave calculation for smooth outward bulge
                val distance = if (touchY != null) abs(touchY - letterCenterY) else 9999f
                val sigma = with(density) { 48.dp.toPx() }
                val waveInfluence = if (touchY != null) {
                    exp(-((distance * distance) / (2f * sigma * sigma)))
                } else {
                    0f
                }

                // Wave Bulge offsets: pushes items inward towards screen center
                val horizontalDisplacement = waveInfluence * 26f * (if (isLeftHanded) 1f else -1f)
                val scale = 1f + waveInfluence * 0.85f
                val isCurrent = selectedLetter == letter

                Text(
                    text = letter.toString(),
                    fontSize = if (letter == '★') 11.sp else 9.5.sp,
                    fontWeight = when {
                        isCurrent -> FontWeight.ExtraBold
                        waveInfluence > 0.35f -> FontWeight.Bold
                        else -> FontWeight.Medium
                    },
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        waveInfluence > 0.4f -> MaterialTheme.colorScheme.onBackground
                        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f)
                    },
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = horizontalDisplacement
                            scaleX = scale
                            scaleY = scale
                        }
                        .padding(horizontal = 10.dp)
                )
            }
        }

        // Floating Magnifier Badge: Offset from finger preview showing the selected letter
        if (selectedLetter != null && currentTouchY != null) {
            val touchY = currentTouchY ?: 0f
            val bubbleSizePx = with(density) { 42.dp.toPx() }
            val bubbleOffsetY = (touchY - (bubbleSizePx / 2f)).coerceIn(12f, (componentHeight - bubbleSizePx - 12f).coerceAtLeast(12f))
            val touchX = currentTouchX ?: (if (isLeftHanded) 0f else componentWidth)
            val bubbleOffsetX = if (isLeftHanded) {
                touchX + 110f
            } else {
                touchX - 110f - bubbleSizePx
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        translationY = bubbleOffsetY
                        translationX = bubbleOffsetX
                    }
                    .size(width = 42.dp, height = 42.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = selectedLetter.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
