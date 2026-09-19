package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Flux Launcher Signature Wallpaper:
 * - Dynamic deep forest emerald to sage mint diagonal gradient
 * - Large translucent orbital intersecting circular arcs and wave rings
 * - 3D Frosted glassmorphism Flux emblem with glowing pearl node and pill capsules
 */
@Composable
fun FluxWallpaper(
    showEmblem: Boolean = true,
    emblemAlpha: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Base Rich Emerald to Sage Gradient
        val bgGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF569484), // Top right sage mint
                Color(0xFF387263),
                Color(0xFF22574A),
                Color(0xFF133C31),
                Color(0xFF0A221B)  // Bottom left deep emerald
            ),
            start = Offset(width * 0.85f, 0f),
            end = Offset(0f, height * 0.95f)
        )
        drawRect(brush = bgGradient)

        // 2. Overlapping Soft Translucent Orbital Circular Arcs
        // Ring 1: Primary upper center-left orbital circle
        drawCircle(
            color = Color(0x35A7F3D0),
            radius = width * 0.44f,
            center = Offset(width * 0.46f, height * 0.28f),
            style = Stroke(width = 2.dp.toPx())
        )
        // Soft diffuse inner fill for Ring 1
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x186EE7B7), Color(0x0534D399), Color.Transparent),
                center = Offset(width * 0.46f, height * 0.28f),
                radius = width * 0.44f
            ),
            radius = width * 0.44f,
            center = Offset(width * 0.46f, height * 0.28f)
        )

        // Ring 2: Intersecting mid-left circle
        drawCircle(
            color = Color(0x226EE7B7),
            radius = width * 0.52f,
            center = Offset(width * 0.24f, height * 0.38f),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Ring 3: Intersecting right circle
        drawCircle(
            color = Color(0x265EEAD4),
            radius = width * 0.48f,
            center = Offset(width * 0.74f, height * 0.42f),
            style = Stroke(width = 1.6.dp.toPx())
        )

        // Ring 4: Large lower gentle sweeping orbital wave arc
        drawCircle(
            color = Color(0x1C6EE7B7),
            radius = width * 0.78f,
            center = Offset(width * 0.50f, height * 0.76f),
            style = Stroke(width = 1.4.dp.toPx())
        )

        // 3. 3D Frosted Glass Flux Emblem
        if (showEmblem && emblemAlpha > 0.01f) {
            drawFluxEmblem(
                center = Offset(width * 0.50f, height * 0.25f),
                scale = (width / 360f).coerceIn(0.85f, 1.4f) * 1.05f,
                alpha = emblemAlpha
            )
        }
    }
}

private fun DrawScope.drawFluxEmblem(
    center: Offset,
    scale: Float,
    alpha: Float
) {
    val s = 1.35f * scale

    // Ambient radial glow behind the emblem
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x4D6EE7B7).copy(alpha = 0.30f * alpha),
                Color(0x205EEAD4).copy(alpha = 0.15f * alpha),
                Color.Transparent
            ),
            center = center,
            radius = 110.dp.toPx() * s
        ),
        radius = 110.dp.toPx() * s,
        center = center
    )

    fun p(x: Float, y: Float): Offset {
        return Offset(center.x + (x - 50f) * s, center.y + (y - 50f) * s)
    }

    val ribbonPath = Path().apply {
        moveTo(p(64f, 41f).x, p(64f, 41f).y)
        cubicTo(
            p(70f, 44f).x, p(70f, 44f).y,
            p(73f, 50f).x, p(73f, 50f).y,
            p(71f, 57f).x, p(71f, 57f).y
        )
        cubicTo(
            p(69f, 65f).x, p(69f, 65f).y,
            p(62f, 71f).x, p(62f, 71f).y,
            p(49f, 71f).x, p(49f, 71f).y
        )
        cubicTo(
            p(38f, 71f).x, p(38f, 71f).y,
            p(33f, 63f).x, p(33f, 63f).y,
            p(33f, 53f).x, p(33f, 53f).y
        )
        cubicTo(
            p(33f, 42f).x, p(33f, 42f).y,
            p(39f, 28f).x, p(39f, 28f).y,
            p(50f, 28f).x, p(50f, 28f).y
        )
        cubicTo(
            p(58f, 28f).x, p(58f, 28f).y,
            p(62f, 31f).x, p(62f, 31f).y,
            p(64f, 34f).x, p(64f, 34f).y
        )
    }

    // Outer glow halo pass
    drawPath(
        path = ribbonPath,
        color = Color(0x3B6EE7B7).copy(alpha = 0.35f * alpha),
        style = Stroke(width = 12.dp.toPx() * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Ribbon main frosted glass rim
    drawPath(
        path = ribbonPath,
        color = Color(0xFFB2F5EA).copy(alpha = 0.85f * alpha),
        style = Stroke(width = 5.2.dp.toPx() * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Inner bright specular highlight
    drawPath(
        path = ribbonPath,
        color = Color.White.copy(alpha = 0.65f * alpha),
        style = Stroke(width = 1.8.dp.toPx() * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Shorter Left Pill Capsule: x=38 to 44, y=47 to 65
    val pill1Left = p(38f, 47f).x
    val pill1Top = p(38f, 47f).y
    val pill1W = 6.5f * s
    val pill1H = 18f * s
    val pill1Radius = pill1W / 2f
    drawRoundRect(
        color = Color(0x55FFFFFF).copy(alpha = 0.35f * alpha),
        topLeft = Offset(pill1Left, pill1Top),
        size = Size(pill1W, pill1H),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(pill1Radius, pill1Radius)
    )
    drawRoundRect(
        color = Color(0xFFC6F6D5).copy(alpha = 0.8f * alpha),
        topLeft = Offset(pill1Left, pill1Top),
        size = Size(pill1W, pill1H),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(pill1Radius, pill1Radius),
        style = Stroke(width = 2.dp.toPx() * s)
    )

    // Taller Center Pill Capsule: x=47.5 to 54.5, y=38 to 65
    val pill2Left = p(47.5f, 38f).x
    val pill2Top = p(47.5f, 38f).y
    val pill2W = 7.5f * s
    val pill2H = 27f * s
    val pill2Radius = pill2W / 2f
    drawRoundRect(
        color = Color(0x66FFFFFF).copy(alpha = 0.40f * alpha),
        topLeft = Offset(pill2Left, pill2Top),
        size = Size(pill2W, pill2H),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(pill2Radius, pill2Radius)
    )
    drawRoundRect(
        color = Color(0xFFE6FFFA).copy(alpha = 0.9f * alpha),
        topLeft = Offset(pill2Left, pill2Top),
        size = Size(pill2W, pill2H),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(pill2Radius, pill2Radius),
        style = Stroke(width = 2.2.dp.toPx() * s)
    )

    // Top Right Pearl Node
    val pearlCenter = p(66.5f, 33.5f)
    val pearlRadius = 7f * s

    // Pearl halo glow
    drawCircle(
        color = Color(0x406EE7B7).copy(alpha = 0.40f * alpha),
        radius = pearlRadius * 1.6f,
        center = pearlCenter
    )

    // Pearl 3D shaded sphere
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.98f * alpha),
                Color(0xFFE6FFFA).copy(alpha = 0.95f * alpha),
                Color(0xFFB2F5EA).copy(alpha = 0.88f * alpha),
                Color(0xFF48BB78).copy(alpha = 0.70f * alpha)
            ),
            center = Offset(pearlCenter.x - pearlRadius * 0.3f, pearlCenter.y - pearlRadius * 0.3f),
            radius = pearlRadius
        ),
        radius = pearlRadius,
        center = pearlCenter
    )

    // Pearl rim
    drawCircle(
        color = Color(0xFFE6FFFA).copy(alpha = 0.95f * alpha),
        radius = pearlRadius,
        center = pearlCenter,
        style = Stroke(width = 1.8.dp.toPx() * s)
    )

    // Pearl bright specular reflection glint
    drawCircle(
        color = Color.White.copy(alpha = 0.95f * alpha),
        radius = pearlRadius * 0.28f,
        center = Offset(pearlCenter.x - pearlRadius * 0.35f, pearlCenter.y - pearlRadius * 0.35f)
    )
}
