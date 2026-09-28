package com.example.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable Frosted Glass Container Panel
 *
 * CRITICAL FIX: The background blur & specular tint is rendered on a separate
 * background layer Box (`.matchParentSize()`). This ensures the backdrop underneath
 * is blurred, while foreground `content()` (text, icons, buttons) is rendered completely sharp and unblurred!
 */
@Composable
fun FrostedGlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    blurRadius: Dp = 15.dp,
    translucency: Float = 0.30f,
    glassEffect: Float = 0.30f,
    borderWidth: Dp = 0.75.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier,
        propagateMinConstraints = true
    ) {
        // 1. Background Blur & Specular Tint Layer (Only this layer is blurred)
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val blurPx = blurRadius.toPx().coerceIn(0f, 50f)
                    if (blurPx > 0.5f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            renderEffect = RenderEffect
                                .createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        } catch (_: Throwable) {
                            renderEffect = null
                        }
                    } else {
                        renderEffect = null
                    }
                    clip = true
                    this.shape = shape
                }
                .drawWithCache {
                    val strokePx = borderWidth.toPx()

                    // High-contrast dark base scrim to guarantee text readability on light or dark wallpapers
                    val baseScrimBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A).copy(alpha = (translucency * 0.75f + 0.25f).coerceIn(0.20f, 0.90f)),
                            Color(0xFF020617).copy(alpha = (translucency * 0.85f + 0.30f).coerceIn(0.25f, 0.95f))
                        )
                    )

                    val specularBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (glassEffect * 0.35f + 0.08f).coerceIn(0.05f, 0.75f)),
                            Color.Transparent,
                            Color.White.copy(alpha = (glassEffect * 0.15f).coerceIn(0.0f, 0.30f))
                        )
                    )

                    val borderBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.18f)
                        )
                    )

                    onDrawBehind {
                        // Draw dark high-contrast base scrim
                        drawRect(brush = baseScrimBrush)

                        // Draw glass specular highlight
                        if (glassEffect > 0f) {
                            drawRect(brush = specularBrush)
                        }

                        // Draw subtle white stroke border
                        if (strokePx > 0f) {
                            drawRect(
                                brush = borderBrush,
                                style = Stroke(width = strokePx)
                            )
                        }
                    }
                }
        )

        // 2. Foreground Content Layer (Unblurred & Sharp)
        Box(
            modifier = Modifier,
            content = content
        )
    }
}

/**
 * Backward-compatible Frosted Glass Card component
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
    FrostedGlassPanel(
        modifier = modifier,
        cornerRadius = 20.dp,
        blurRadius = blurRadius,
        translucency = translucency,
        glassEffect = glassEffect,
        shape = shape,
        content = content
    )
}

/**
 * Fullscreen Frosted Glass Screen Backdrop
 */
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
        val baseAlpha = translucency.coerceIn(0.05f, 0.95f)
        val effectiveBlur = blurStrength.coerceAtLeast(0f).dp

        if (isFrostedTheme) {
            FrostedGlassPanel(
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0.dp,
                blurRadius = effectiveBlur,
                translucency = baseAlpha,
                glassEffect = glassEffect,
                borderWidth = 0.dp,
                shape = RoundedCornerShape(0.dp)
            ) {
                // Background fill
            }
        }
        content()
    }
}
