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
    isFrostedTheme: Boolean = true,
    blurStrength: Float = 25f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        val effectiveBlur = blurStrength.coerceAtLeast(18f).dp
        // Hardware blur scrim forced across all themes
        val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.fillMaxSize().blur(effectiveBlur)
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = blurModifier.background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.60f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.75f)
                    )
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
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.06f)
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
    val effectiveBlur = blurRadius.coerceAtLeast(12.dp)
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(effectiveBlur)
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
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = shape
            )
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(blurModifier)
                .background(Color(0x22FFFFFF))
        )
        content()
    }
}

