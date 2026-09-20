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
    var currentTouchY by remember { mutableStateOf<Float?>(null) }

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
            }
            .pointerInput(componentHeight, isLeftHanded, verticalPaddingPx) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentTouchY = offset.y
                        val relativeY = (offset.y - verticalPaddingPx).coerceIn(0f, effectiveHeight - 1f)
                        val index = (relativeY / itemHeight).toInt().coerceIn(0, itemCount - 1)
                        val letter = ALPHABET_ITEMS[index]
                        onLetterSelected(letter)
                        onLetterConfirmed(letter)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentTouchY = change.position.y
                        val relativeY = (change.position.y - verticalPaddingPx).coerceIn(0f, effectiveHeight - 1f)
                        val index = (relativeY / itemHeight).toInt().coerceIn(0, itemCount - 1)
                        val letter = ALPHABET_ITEMS[index]
                        onLetterSelected(letter)
                        onLetterConfirmed(letter)
                    },
                    onDragEnd = {
                        selectedLetter?.let { letter ->
                            onLetterConfirmed(letter)
                        }
                        currentTouchY = null
                    },
                    onDragCancel = {
                        currentTouchY = null
                    }
                )
            }
            .pointerInput(componentHeight, verticalPaddingPx) {
                detectTapGestures(
                    onPress = { offset ->
                        currentTouchY = offset.y
                        val relativeY = (offset.y - verticalPaddingPx).coerceIn(0f, effectiveHeight - 1f)
                        val index = (relativeY / itemHeight).toInt().coerceIn(0, itemCount - 1)
                        val letter = ALPHABET_ITEMS[index]
                        onLetterSelected(letter)
                        onLetterConfirmed(letter)
                        tryAwaitRelease()
                        currentTouchY = null
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
                val horizontalDisplacement = waveInfluence * 24f * (if (isLeftHanded) 1f else -1f)
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

        // Floating thumb magnifier bubble for the active letter
        if (selectedLetter != null && currentTouchY != null) {
            val touchY = currentTouchY ?: 0f
            val bubbleOffsetY = touchY.coerceIn(70f, (componentHeight - 70f).coerceAtLeast(70f))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .graphicsLayer {
                        translationY = bubbleOffsetY - 36f
                        translationX = if (isLeftHanded) 42f else -42f
                    }
                    .size(48.dp)
                    .clip(CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = selectedLetter.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
