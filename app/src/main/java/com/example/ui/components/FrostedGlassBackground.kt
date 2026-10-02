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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassStyle

/**
 * Reusable Frosted Glass Container Panel conforming to GlassStyle with high-end aesthetic finish and zero halo edge bleed.
 */
@Composable
fun FrostedGlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    glass: GlassStyle = GlassStyle(),
    blurRadius: Dp = glass.blurDp.dp,
    translucency: Float = glass.translucency,
    glassEffect: Float = glass.specular,
    borderWidth: Dp = 1.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    content: @Composable BoxScope.() -> Unit
) {
    val effectiveGlass = remember(glass, blurRadius, translucency, glassEffect) {
        if (blurRadius.value != glass.blurDp || translucency != glass.translucency || glassEffect != glass.specular) {
            glass.copy(
                blurDp = blurRadius.value,
                translucency = translucency,
                specular = glassEffect
            )
        } else {
            glass
        }
    }

    Box(
        modifier = modifier.clip(shape),
        propagateMinConstraints = true
    ) {
        if (effectiveGlass.enabled) {
            // 1. Background Blur & Specular Tint Layer (Hardware-accelerated GPU blur for battery efficiency, clipped to shape)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val blurPx = effectiveGlass.blurDp.dp.toPx().coerceIn(0f, 50f)
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
                        this.shape = shape
                        this.clip = true
                    }
                    .drawWithCache {
                        val strokePx = borderWidth.toPx()

                        // Refined multi-stop glass scrim for organic depth
                        val baseScrimBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E293B).copy(alpha = (effectiveGlass.translucency * 0.70f + 0.15f).coerceIn(0.15f, 0.85f)),
                                Color(0xFF0F172A).copy(alpha = (effectiveGlass.translucency * 0.80f + 0.20f).coerceIn(0.20f, 0.90f)),
                                Color(0xFF070B14).copy(alpha = (effectiveGlass.translucency * 0.90f + 0.25f).coerceIn(0.25f, 0.95f))
                            )
                        )

                        // Elegant diagonal specular light reflection
                        val specularBrush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (effectiveGlass.specular * 0.45f + 0.12f).coerceIn(0.08f, 0.80f)),
                                Color.Transparent,
                                Color.White.copy(alpha = (effectiveGlass.specular * 0.18f).coerceIn(0.01f, 0.35f))
                            )
                        )

                        // Crisp dual-tone glass refraction border
                        val borderBrush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.65f),
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        )

                        onDrawBehind {
                            drawRect(brush = baseScrimBrush)
                            if (effectiveGlass.specular > 0f) {
                                drawRect(brush = specularBrush)
                            }
                            if (strokePx > 0f) {
                                drawRect(
                                    brush = borderBrush,
                                    style = Stroke(width = strokePx)
                                )
                            }
                        }
                    }
            )
        } else {
            // Non-frosted fallback surface
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), shape)
            )
        }

        // 2. Foreground Content Layer
        Box(
            modifier = Modifier,
            content = content
        )
    }
}

/**
 * Fullscreen Frosted Glass Screen Backdrop
 */
@Composable
fun FrostedGlassBackground(
    modifier: Modifier = Modifier,
    glass: GlassStyle = GlassStyle(),
    isFrostedTheme: Boolean = glass.enabled,
    blurStrength: Float = glass.blurDp,
    translucency: Float = glass.translucency,
    glassEffect: Float = glass.specular,
    content: @Composable BoxScope.() -> Unit
) {
    val effectiveGlass = remember(glass, isFrostedTheme, blurStrength, translucency, glassEffect) {
        if (!isFrostedTheme || blurStrength != glass.blurDp || translucency != glass.translucency || glassEffect != glass.specular) {
            glass.copy(
                enabled = isFrostedTheme,
                blurDp = blurStrength,
                translucency = translucency,
                specular = glassEffect
            )
        } else {
            glass
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (effectiveGlass.enabled) {
            FrostedGlassPanel(
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0.dp,
                glass = effectiveGlass,
                borderWidth = 0.dp,
                shape = RoundedCornerShape(0.dp)
            ) {
                // Background fill
            }
        }
        content()
    }
}
