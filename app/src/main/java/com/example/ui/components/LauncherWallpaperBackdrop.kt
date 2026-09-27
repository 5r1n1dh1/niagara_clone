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
import androidx.compose.ui.graphics.asAndroidBitmap
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
    autoWallpaperBitmap: android.graphics.Bitmap? = null,
    blurRadius: Dp = 0.dp,
    backgroundBlurStrength: Float = 25f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeWallpaperBitmap by remember(systemWallpaperBitmap, autoWallpaperBitmap, wallpaperStyle) {
        mutableStateOf(
            if (wallpaperStyle == WallpaperStyle.AUTO_ALBUM && autoWallpaperBitmap != null) {
                autoWallpaperBitmap.asImageBitmap()
            } else {
                systemWallpaperBitmap
            }
        )
    }

    LaunchedEffect(systemWallpaperBitmap, autoWallpaperBitmap, wallpaperStyle) {
        if (wallpaperStyle == WallpaperStyle.AUTO_ALBUM && autoWallpaperBitmap != null) {
            activeWallpaperBitmap = autoWallpaperBitmap.asImageBitmap()
        } else if (systemWallpaperBitmap != null) {
            activeWallpaperBitmap = systemWallpaperBitmap
        } else if (wallpaperStyle == WallpaperStyle.SYSTEM_DEVICE) {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("MissingPermission")
                    val wpManager = android.app.WallpaperManager.getInstance(context)
                    @Suppress("MissingPermission")
                    val drawable = try { wpManager.drawable ?: wpManager.peekDrawable() } catch (_: Throwable) { null }
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

    val blurRadiusPx = effectiveBlur.value.toInt().coerceIn(0, 50)

    var softwareBlurredBitmap by remember(activeWallpaperBitmap, blurRadiusPx) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(activeWallpaperBitmap, blurRadiusPx) {
        val sourceBitmap = activeWallpaperBitmap ?: systemWallpaperBitmap
        if (sourceBitmap != null && blurRadiusPx > 0 && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            withContext(Dispatchers.Default) {
                try {
                    val androidBmp = sourceBitmap.asAndroidBitmap()
                    val blurred = fastBlurBitmap(androidBmp, blurRadiusPx)
                    softwareBlurredBitmap = blurred.asImageBitmap()
                } catch (_: Exception) {
                    softwareBlurredBitmap = null
                }
            }
        } else {
            softwareBlurredBitmap = null
        }
    }

    val blurModifier = if (effectiveBlur > 0.5.dp && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        Modifier.blur(effectiveBlur)
    } else {
        Modifier
    }

    val isSystemWallpaper = wallpaperStyle == WallpaperStyle.SYSTEM_DEVICE
    val isAutoWallpaper = wallpaperStyle == WallpaperStyle.AUTO_ALBUM

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isFrosted || isSystemWallpaper || isAutoWallpaper) Color.Transparent else bgColor)
    ) {
        // Wallpaper base layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(blurModifier)
        ) {
            val wpToUse = softwareBlurredBitmap ?: activeWallpaperBitmap ?: systemWallpaperBitmap
            if (wpToUse != null && (isSystemWallpaper || isAutoWallpaper || isFrosted)) {
                Image(
                    bitmap = wpToUse,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isSystemWallpaper && wpToUse == null) {
                // High quality fallback gradient matching theme while system wallpaper resolves
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

/**
 * Fast StackBlur algorithm on Bitmap for guaranteed Android compatibility on all API levels.
 */
private fun fastBlurBitmap(sentBitmap: android.graphics.Bitmap, radius: Int): android.graphics.Bitmap {
    if (radius < 1) return sentBitmap
    val scale = 0.35f
    val width = (sentBitmap.width * scale).toInt().coerceAtLeast(1)
    val height = (sentBitmap.height * scale).toInt().coerceAtLeast(1)
    val input = android.graphics.Bitmap.createScaledBitmap(sentBitmap, width, height, true)
    val output = android.graphics.Bitmap.createBitmap(input.width, input.height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(input, 0f, 0f, paint)

    stackBlurInPlace(output, radius.coerceIn(1, 25))
    return output
}

private fun stackBlurInPlace(bitmap: android.graphics.Bitmap, radius: Int) {
    val w = bitmap.width
    val h = bitmap.height
    val pix = IntArray(w * h)
    bitmap.getPixels(pix, 0, w, 0, 0, w, h)

    val wm = w - 1
    val hm = h - 1
    val wh = w * h
    val div = radius + radius + 1

    val r = IntArray(wh)
    val g = IntArray(wh)
    val b = IntArray(wh)
    var rsum: Int
    var gsum: Int
    var bsum: Int
    var p: Int
    var yp: Int
    var yi: Int
    var yw: Int
    val vmin = IntArray(maxOf(w, h))

    var divsum = (div + 1) shr 1
    divsum *= divsum
    val dv = IntArray(256 * divsum)
    for (i in 0 until 256 * divsum) {
        dv[i] = i / divsum
    }

    yw = 0
    yi = 0

    val stack = Array(div) { IntArray(3) }
    var stackpointer: Int
    var stackstart: Int
    var rbs: Int
    val r1 = radius + 1
    var routsum: Int
    var goutsum: Int
    var boutsum: Int
    var rinsum: Int
    var ginsum: Int
    var binsum: Int

    for (y in 0 until h) {
        bsum = 0
        gsum = 0
        rsum = 0
        boutsum = 0
        goutsum = 0
        routsum = 0
        binsum = 0
        ginsum = 0
        rinsum = 0
        for (i in -radius..radius) {
            p = pix[yi + minOf(wm, maxOf(i, 0))]
            val sir = stack[i + radius]
            sir[0] = (p and 0xff0000) shr 16
            sir[1] = (p and 0x00ff00) shr 8
            sir[2] = (p and 0x0000ff)
            rbs = r1 - Math.abs(i)
            rsum += sir[0] * rbs
            gsum += sir[1] * rbs
            bsum += sir[2] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
        }
        stackpointer = radius

        for (x in 0 until w) {
            r[yi] = dv[rsum]
            g[yi] = dv[gsum]
            b[yi] = dv[bsum]

            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum

            stackstart = stackpointer - radius + div
            val sir = stack[stackstart % div]

            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]

            if (y == 0) {
                vmin[x] = minOf(x + radius + 1, wm)
            }
            p = pix[yw + vmin[x]]

            sir[0] = (p and 0xff0000) shr 16
            sir[1] = (p and 0x00ff00) shr 8
            sir[2] = (p and 0x0000ff)

            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]

            rsum += rinsum
            gsum += ginsum
            bsum += binsum

            stackpointer = (stackpointer + 1) % div
            val sir2 = stack[stackpointer % div]

            routsum += sir2[0]
            goutsum += sir2[1]
            boutsum += sir2[2]

            rinsum -= sir2[0]
            ginsum -= sir2[1]
            binsum -= sir2[2]

            yi++
        }
        yw += w
    }

    for (x in 0 until w) {
        bsum = 0
        gsum = 0
        rsum = 0
        boutsum = 0
        goutsum = 0
        routsum = 0
        binsum = 0
        ginsum = 0
        rinsum = 0
        yp = -radius * w
        for (i in -radius..radius) {
            yi = maxOf(0, yp) + x
            val sir = stack[i + radius]
            sir[0] = r[yi]
            sir[1] = g[yi]
            sir[2] = b[yi]
            rbs = r1 - Math.abs(i)
            rsum += r[yi] * rbs
            gsum += g[yi] * rbs
            bsum += b[yi] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            if (i < hm) {
                yp += w
            }
        }
        yi = x
        stackpointer = radius
        for (y in 0 until h) {
            pix[yi] = (0xff000000.toInt()) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum

            stackstart = stackpointer - radius + div
            val sir = stack[stackstart % div]

            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]

            if (x == 0) {
                vmin[y] = minOf(y + r1, hm) * w
            }
            p = x + vmin[y]

            sir[0] = r[p]
            sir[1] = g[p]
            sir[2] = b[p]

            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]

            rsum += rinsum
            gsum += ginsum
            bsum += binsum

            stackpointer = (stackpointer + 1) % div
            val sir2 = stack[stackpointer % div]

            routsum += sir2[0]
            goutsum += sir2[1]
            boutsum += sir2[2]

            rinsum -= sir2[0]
            ginsum -= sir2[1]
            binsum -= sir2[2]

            yi += w
        }
    }

    bitmap.setPixels(pix, 0, w, 0, 0, w, h)
}
