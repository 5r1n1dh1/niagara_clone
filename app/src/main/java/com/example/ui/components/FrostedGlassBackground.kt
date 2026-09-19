package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FrostedGlassBackground(
    modifier: Modifier = Modifier,
    isFrostedTheme: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Hardware blur scrim on supported Android versions
        val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.fillMaxSize().blur(28.dp)
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = blurModifier.background(
                Brush.verticalGradient(
                    if (isFrostedTheme) {
                        listOf(
                            Color(0x3B18181B),
                            Color(0x4D27272A),
                            Color(0x6618181B)
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.80f)
                        )
                    }
                )
            )
        )

        // Subtle specular frost highlight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                )
        )

        content()
    }
}

/**
 * Reusable Frosted Glass Surface Container with Gaussian blur and translucent alpha.
 */
@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    isFrostedTheme: Boolean = true,
    blurRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.dp) {
        Modifier.blur(blurRadius)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = shape
            )
            .background(
                if (isFrostedTheme) {
                    Color(0x3B27272A)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                },
                shape = shape
            )
    ) {
        content()
    }
}

