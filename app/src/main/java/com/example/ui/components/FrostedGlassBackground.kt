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
    blurStrength: Float = 15f,
    translucency: Float = 0.30f,
    glassEffect: Float = 0.30f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        val effectiveBlur = blurStrength.coerceAtLeast(0f).dp
        val blurModifier = if (effectiveBlur > 0.dp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.fillMaxSize().blur(effectiveBlur)
        } else {
            Modifier.fillMaxSize()
        }

        val baseAlpha = translucency.coerceIn(0.05f, 0.95f)

        Box(
            modifier = blurModifier.background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = baseAlpha * 0.8f),
                        MaterialTheme.colorScheme.surface.copy(alpha = baseAlpha),
                        MaterialTheme.colorScheme.background.copy(alpha = baseAlpha * 1.2f)
                    )
                )
            )
        )

        // Glass specular highlight
        if (glassEffect > 0f) {
            val glassAlpha = glassEffect.coerceIn(0f, 1f) * 0.35f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = glassAlpha),
                                Color.Transparent,
                                Color.White.copy(alpha = glassAlpha * 0.5f)
                            )
                        )
                    )
            )
        }

        content()
    }
}

/**
 * Reusable Frosted Glass Surface Container with Gaussian blur, translucency and specular glass effect.
 */
@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    isFrostedTheme: Boolean = true,
    blurRadius: Dp = 15.dp,
    translucency: Float = 0.30f,
    glassEffect: Float = 0.30f,
    content: @Composable BoxScope.() -> Unit
) {
    val effectiveBlur = blurRadius.coerceAtLeast(0.dp)
    val blurModifier = if (effectiveBlur > 0.dp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(effectiveBlur)
    } else {
        Modifier
    }

    val alpha = translucency.coerceIn(0.05f, 0.95f)
    val glassAlpha = glassEffect.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.15f + glassAlpha * 0.35f),
                        Color.White.copy(alpha = 0.05f + glassAlpha * 0.15f)
                    )
                ),
                shape = shape
            )
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
                shape = shape
            )
    ) {
        if (effectiveBlur > 0.dp) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(blurModifier)
                    .background(Color.White.copy(alpha = 0.08f * glassAlpha))
            )
        }
        content()
    }
}


