package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.LauncherThemePreset

/**
 * Ambient Wallpaper Backdrop:
 * - On Favourites Screen (Center): blurRadius is 0.dp (completely sharp & unblurred).
 * - On Widgets Screen (Left): blurRadius is 24.dp+ (frosted glass blur effect).
 * - On All Apps Screen (Right): blurRadius is 24.dp+ (frosted glass blur effect).
 */
@Composable
fun LauncherWallpaperBackdrop(
    themePreset: LauncherThemePreset,
    blurRadius: Dp,
    modifier: Modifier = Modifier
) {
    val primaryColor = themePreset.primary
    val accentColor = themePreset.accent
    val bgColor = themePreset.background
    val surfaceColor = themePreset.surface

    val blurModifier = if (blurRadius > 0.5.dp) {
        Modifier.blur(blurRadius)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Wallpaper layer that receives dynamic blur
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(blurModifier)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // 1. Soft atmospheric base gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            bgColor,
                            surfaceColor.copy(alpha = 0.85f),
                            bgColor
                        )
                    )
                )

                // 2. Radiant ambient glowing orbs
                // Top right primary glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.28f),
                            primaryColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.85f, h * 0.22f),
                        radius = w * 0.65f
                    ),
                    center = Offset(w * 0.85f, h * 0.22f),
                    radius = w * 0.65f
                )

                // Bottom left accent glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.22f),
                            accentColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.15f, h * 0.78f),
                        radius = w * 0.7f
                    ),
                    center = Offset(w * 0.15f, h * 0.78f),
                    radius = w * 0.7f
                )

                // Center subtle radiant halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = w * 0.45f
                    ),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.45f
                )

                // 3. Subtle geometric aesthetic lines (sharp when unblurred, diffused bokeh when blurred)
                val strokeColor = primaryColor.copy(alpha = 0.15f)
                drawCircle(
                    color = strokeColor,
                    center = Offset(w * 0.85f, h * 0.22f),
                    radius = w * 0.28f,
                    style = Stroke(width = 1.5f)
                )
                drawCircle(
                    color = accentColor.copy(alpha = 0.12f),
                    center = Offset(w * 0.15f, h * 0.78f),
                    radius = w * 0.35f,
                    style = Stroke(width = 1.2f)
                )

                // Constellation / minimalist nodes
                val starNodes = listOf(
                    Offset(w * 0.25f, h * 0.15f),
                    Offset(w * 0.72f, h * 0.12f),
                    Offset(w * 0.88f, h * 0.38f),
                    Offset(w * 0.12f, h * 0.55f),
                    Offset(w * 0.65f, h * 0.68f),
                    Offset(w * 0.35f, h * 0.88f)
                )
                for (pt in starNodes) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        center = pt,
                        radius = 2.5f
                    )
                }
            }
        }
    }
}
