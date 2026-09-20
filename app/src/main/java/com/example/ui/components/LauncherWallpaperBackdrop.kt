package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.LauncherThemePreset
import com.example.data.WallpaperStyle

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ambient Wallpaper Backdrop:
 * - Supports Live Device Wallpaper, Flux Emblem, Soft Gradient, and Solid Themes.
 * - On Frosted Glass Theme, applies real-time Gaussian Blur and Translucent Scrim.
 */
@Composable
fun LauncherWallpaperBackdrop(
    themePreset: LauncherThemePreset,
    wallpaperStyle: WallpaperStyle = WallpaperStyle.SYSTEM_DEVICE,
    systemWallpaperBitmap: ImageBitmap? = null,
    blurRadius: Dp = 0.dp,
    backgroundBlurStrength: Float = 25f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeWallpaperBitmap by remember(systemWallpaperBitmap) { mutableStateOf(systemWallpaperBitmap) }

    LaunchedEffect(systemWallpaperBitmap) {
        if (systemWallpaperBitmap == null) {
            withContext(Dispatchers.IO) {
                try {
                    val wpManager = android.app.WallpaperManager.getInstance(context)
                    val drawable = wpManager.drawable ?: wpManager.peekDrawable()
                    if (drawable != null) {
                        val bitmap = drawable.toBitmap(720, 1280, android.graphics.Bitmap.Config.ARGB_8888)
                        activeWallpaperBitmap = bitmap.asImageBitmap()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val primaryColor = themePreset.primary
    val accentColor = themePreset.accent
    val bgColor = themePreset.background
    val surfaceColor = themePreset.surface

    val isFrosted = themePreset == LauncherThemePreset.FROSTED_GLASS
    val baseBlur = if (isFrosted) backgroundBlurStrength.dp else (backgroundBlurStrength * 0.4f).dp
    val effectiveBlur = baseBlur + blurRadius

    val blurModifier = if (effectiveBlur > 0.5.dp && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        Modifier.blur(effectiveBlur)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isFrosted) Color.Transparent else bgColor)
    ) {
        // Wallpaper base layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(blurModifier)
        ) {
            val wpToUse = activeWallpaperBitmap ?: systemWallpaperBitmap
            if (wpToUse != null && (wallpaperStyle == WallpaperStyle.SYSTEM_DEVICE || isFrosted)) {
                Image(
                    bitmap = wpToUse,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (wallpaperStyle == WallpaperStyle.FLUX_MINIMAL_GRADIENT) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                bgColor,
                                surfaceColor,
                                bgColor
                            )
                        )
                    )
                }
            } else if (wallpaperStyle == WallpaperStyle.SYSTEM_SOLID) {
                Box(modifier = Modifier.fillMaxSize().background(bgColor))
            } else {
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

        // Translucent Frosted Glass neutral scrim overlay
        if (isFrosted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x3B18181B),
                                Color(0x4D27272A),
                                Color(0x6618181B)
                            )
                        )
                    )
            )
            // Specular frost rim
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
        }
    }
}
