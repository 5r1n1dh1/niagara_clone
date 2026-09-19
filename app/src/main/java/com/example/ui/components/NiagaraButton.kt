package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun NiagaraButton(
    isLeftHanded: Boolean,
    onTap: () -> Unit,
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(targetValue = offsetY, label = "fab_offset")

    Box(
        modifier = modifier
            .padding(
                start = if (isLeftHanded) 24.dp else 0.dp,
                end = if (!isLeftHanded) 24.dp else 0.dp,
                bottom = 28.dp
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y < 0) {
                            offsetY = (offsetY + dragAmount.y).coerceIn(-120f, 0f)
                        }
                    },
                    onDragEnd = {
                        if (offsetY < -40f) {
                            onSwipeUp()
                        }
                        offsetY = 0f
                    },
                    onDragCancel = { offsetY = 0f }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() }
                )
            }
            .testTag("niagara_button")
    ) {
        FloatingActionButton(
            onClick = onTap,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(54.dp)
                .graphicsLayer {
                    translationY = animatedOffsetY
                }
                .shadow(8.dp, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Niagara Unified Search",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
