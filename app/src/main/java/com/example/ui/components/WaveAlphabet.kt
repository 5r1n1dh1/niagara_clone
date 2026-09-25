package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.exp

val ALPHABET_ITEMS = listOf('★') + ('A'..'Z').toList() + listOf('#')

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

    val verticalPaddingPx = with(androidx.compose.ui.platform.LocalDensity.current) { 32.dp.toPx() }
    val itemCount = ALPHABET_ITEMS.size
    val effectiveHeight = (componentHeight - 2 * verticalPaddingPx).coerceAtLeast(1f)
    val itemHeight = effectiveHeight / itemCount

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(54.dp)
            .testTag("wave_alphabet")
            .onGloballyPositioned { coordinates ->
                componentHeight = coordinates.size.height.toFloat()
                componentWidth = coordinates.size.width.toFloat()
            }
            .pointerInput(componentHeight, isLeftHanded, verticalPaddingPx) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentTouchX = offset.x
                        currentTouchY = offset.y
                        val relativeY = (offset.y - verticalPaddingPx).coerceIn(0f, effectiveHeight - 1f)
                        val index = (relativeY / itemHeight).toInt().coerceIn(0, itemCount - 1)
                        val letter = ALPHABET_ITEMS[index]
                        if (letter != selectedLetter) {
                            onLetterSelected(letter)
                            onLetterConfirmed(letter)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentTouchX = change.position.x
                        currentTouchY = change.position.y
                        val relativeY = (change.position.y - verticalPaddingPx).coerceIn(0f, effectiveHeight - 1f)
                        val index = (relativeY / itemHeight).toInt().coerceIn(0, itemCount - 1)
                        val letter = ALPHABET_ITEMS[index]
                        if (letter != selectedLetter) {
                            onLetterSelected(letter)
                            onLetterConfirmed(letter)
                        }
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
                        currentTouchX = offset.x
                        currentTouchY = offset.y
                        val relativeY = (offset.y - verticalPaddingPx).coerceIn(0f, effectiveHeight - 1f)
                        val index = (relativeY / itemHeight).toInt().coerceIn(0, itemCount - 1)
                        val letter = ALPHABET_ITEMS[index]
                        onLetterSelected(letter)
                        onLetterConfirmed(letter)
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
                .padding(vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = if (isLeftHanded) Alignment.Start else Alignment.End
        ) {
            ALPHABET_ITEMS.forEachIndexed { index, letter ->
                val letterCenterY = verticalPaddingPx + (index + 0.5f) * itemHeight
                val touchY = currentTouchY

                // Gaussian Wave calculation for kinetic bulge
                val distance = if (touchY != null) abs(touchY - letterCenterY) else 9999f
                val waveInfluence = if (touchY != null) {
                    exp(-((distance * distance) / (2f * 55f * 55f)))
                } else {
                    0f
                }

                // Bulge offset and font size
                val horizontalDisplacement = waveInfluence * 20f * (if (isLeftHanded) 1f else -1f)
                val scale = 1f + waveInfluence * 0.75f
                val isCurrent = selectedLetter == letter

                Text(
                    text = letter.toString(),
                    fontSize = if (letter == '★') 11.sp else 9.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else if (waveInfluence > 0.3f) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        waveInfluence > 0.4f -> MaterialTheme.colorScheme.onBackground
                        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
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

        // Floating thumb magnifier bubble: displayed with 120px gap to the left of the touched area rather than below the finger
        if (selectedLetter != null && currentTouchY != null) {
            val touchY = currentTouchY ?: 0f
            val density = androidx.compose.ui.platform.LocalDensity.current
            val bubbleSizePx = with(density) { 38.dp.toPx() }
            val bubbleOffsetY = (touchY - (bubbleSizePx / 2f)).coerceIn(8f, (componentHeight - bubbleSizePx - 8f).coerceAtLeast(8f))
            val touchX = currentTouchX ?: (if (isLeftHanded) 0f else componentWidth)
            val bubbleOffsetX = if (isLeftHanded) {
                touchX + 120f
            } else {
                touchX - 120f - bubbleSizePx
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        translationY = bubbleOffsetY
                        translationX = bubbleOffsetX
                    }
                    .size(width = 38.dp, height = 38.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = selectedLetter.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
