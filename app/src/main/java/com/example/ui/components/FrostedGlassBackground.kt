package com.example.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassStyle
import kotlin.math.roundToInt

/**
 * Blur radius is quantised to whole dp steps before it reaches the renderer.
 *
 * A slider drag (or the wallpaper pager offset) produces a continuous float; without quantising,
 * every tick would allocate a brand new [RenderEffect] and force a full re-blur of the surface.
 * Quantising means a swipe settles after a handful of blurs instead of dozens per frame.
 */
internal fun quantiseBlurDp(blurDp: Float, stepDp: Int = 1): Float =
    (blurDp / stepDp).roundToInt().toFloat().coerceIn(0f, 30f)

/**
 * Process-wide cache of hardware blur effects, keyed by the *quantised* radius in pixels.
 *
 * `RenderEffect` objects are immutable and cheap to share between layers, so a single dock pill, a
 * sheet and a card that all resolve to the same 12dp radius share one instance instead of allocating
 * a new one per recomposition. The cache is bounded (8 entries) which covers every radius the
 * slider and the wallpaper backdrop can produce.
 */
private val blurEffectCache: LruCache<Int, ComposeRenderEffect> = object : LruCache<Int, ComposeRenderEffect>(8) {
    override fun entryRemoved(
        evicted: Boolean,
        key: Int?,
        oldValue: ComposeRenderEffect?,
        newValue: ComposeRenderEffect?
    ) {
        // Nothing to release: RenderEffect is a native handle owned by the render thread, dropping
        // the last reference is what frees it. Keeping the entry point explicit documents intent.
        super.entryRemoved(evicted, key, oldValue, newValue)
    }
}

/**
 * Remembers the (single, cached) hardware blur effect for a glass surface, or `null` when blur is
 * unavailable (radius 0, glass disabled, or API < 31 — see [nonFrostedPanelModifier] fallback).
 *
 * The effect is created once per distinct quantised radius and never re-created per recomposition.
 */
@Composable
internal fun rememberBlurRenderEffect(glass: GlassStyle): ComposeRenderEffect? {
    val quantisedDp = quantiseBlurDp(glass.blurDp)
    val density = LocalDensity.current
    val radiusPx = with(density) { quantisedDp.dp.roundToPx() }
    val key = radiusPx.coerceAtLeast(0)
    val blurAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && key > 0
    return if (!blurAvailable) {
        null
    } else {
        remember(key) {
            blurEffectCache.get(key) ?: RenderEffect.createBlurEffect(
                key.toFloat(),
                key.toFloat(),
                Shader.TileMode.CLAMP
            ).asComposeRenderEffect().also { blurEffectCache.put(key, it) }
        }
    }
}

/**
 * The single documented non-frosted path.
 *
 * Used when `GlassStyle.enabled == false` (e.g. any preset other than FROSTED_GLASS): no render
 * effect, no clip layer, no specular pass — just one opaque-ish themed surface. Everything else in
 * the file is frost-only work.
 */
@Immutable
internal data class GlassScrimBrushes(
    val baseScrim: Brush,
    val specular: Brush,
    val border: Brush
)

/**
 * Hoists the three gradient brushes out of the per-instance draw block and memoises them on the
 * validated [GlassStyle], so dozens of panels share one scrim + one specular shader instead of
 * rebuilding the colour lists for every instance on every recomposition.
 */
@Composable
internal fun rememberGlassScrimBrushes(
    translucency: Float,
    specular: Float
): GlassScrimBrushes = remember(translucency, specular) {
    GlassScrimBrushes(
        baseScrim = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E293B).copy(alpha = (translucency * 0.70f + 0.15f).coerceIn(0.15f, 0.85f)),
                Color(0xFF0F172A).copy(alpha = (translucency * 0.80f + 0.20f).coerceIn(0.20f, 0.90f)),
                Color(0xFF070B14).copy(alpha = (translucency * 0.90f + 0.25f).coerceIn(0.25f, 0.95f))
            )
        ),
        specular = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = (specular * 0.45f + 0.12f).coerceIn(0.08f, 0.80f)),
                Color.Transparent,
                Color.White.copy(alpha = (specular * 0.18f).coerceIn(0.01f, 0.35f))
            )
        ),
        border = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.65f),
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    )
}

/** Applies the non-frosted fallback surface behind [content]. Must be called inside a BoxScope. */
@Composable
private fun BoxScope.NonFrostedPanelFallback(shape: Shape, translucency: Float) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = (translucency * 0.75f).coerceIn(0.25f, 0.85f)),
                shape
            )
    )
}

/**
 * Reusable Frosted Glass Container Panel conforming to [GlassStyle] with zero halo edge bleed.
 *
 * @param enabled explicit on/off for the frost. Defaults to `glass.enabled`, which is derived from
 *   the selected theme preset — never hardcode `true` at a call site. When `false` the panel takes
 *   the single documented non-frosted fallback path ([NonFrostedPanelFallback]).
 * @param blurRadius / @param translucency / @param glassEffect escape hatches that override the
 *   [glass] token; they default to the token's validated values so callers normally pass nothing.
 */
@Composable
fun FrostedGlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    glass: GlassStyle = GlassStyle(),
    enabled: Boolean = glass.enabled,
    blurRadius: Dp = glass.blurDp.dp,
    translucency: Float = glass.translucency,
    glassEffect: Float = glass.specular,
    borderWidth: Dp = 1.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    content: @Composable BoxScope.() -> Unit
) {
    val effectiveGlass = remember(glass, enabled, blurRadius, translucency, glassEffect) {
        if (!enabled ||
            blurRadius.value != glass.blurDp ||
            translucency != glass.translucency ||
            glassEffect != glass.specular
        ) {
            glass.copy(
                enabled = enabled,
                blurDp = quantiseBlurDp(blurRadius.value),
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
            // One cached blur for the whole surface, never re-created per recomposition.
            val blurEffect = rememberBlurRenderEffect(effectiveGlass)
            val brushes = rememberGlassScrimBrushes(
                translucency = effectiveGlass.translucency,
                specular = effectiveGlass.specular
            )

            // 1. Background Blur & Specular Tint Layer (hardware GPU blur, clipped to shape).
            //    On API < 31 (or radius 0) `blurEffect` is null and the layer degrades to the
            //    scrim-only path: the wallpaper layer beneath supplies the pre-blurred bitmap.
            val layerShape = shape
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        // Removed (null) whenever blur is unavailable, so an off-screen blur layer
                        // is never left allocating an offscreen buffer it does not use.
                        this.renderEffect = blurEffect
                        // Setting `shape` already implies clipping; a second clip flag would
                        // allocate a redundant offscreen pass.
                        this.shape = layerShape
                    }
                    .drawWithCache {
                        val strokePx = borderWidth.toPx()
                        onDrawBehind {
                            drawRect(brush = brushes.baseScrim)
                            if (effectiveGlass.specular > 0f) {
                                drawRect(brush = brushes.specular)
                            }
                            if (strokePx > 0f) {
                                drawRect(
                                    brush = brushes.border,
                                    style = Stroke(width = strokePx)
                                )
                            }
                        }
                    }
            )
        } else {
            NonFrostedPanelFallback(shape = shape, translucency = effectiveGlass.translucency)
        }

        // 2. Foreground Content Layer
        Box(
            modifier = Modifier,
            content = content
        )
    }
}

/**
 * Fullscreen Frosted Glass Screen Backdrop.
 *
 * @param enabled follows `glass.enabled` (derived from the theme preset) — the backdrop is a no-op
 *   on the non-frosted path apart from an optional transparent fill.
 */
@Composable
fun FrostedGlassBackground(
    modifier: Modifier = Modifier,
    glass: GlassStyle = GlassStyle(),
    enabled: Boolean = glass.enabled,
    content: @Composable BoxScope.() -> Unit
) {
    // A full-screen scrim is pure overdraw when the theme is not frosted: skip mounting it.
    if (enabled) {
        Box(modifier = modifier.fillMaxSize()) {
            FrostedGlassPanel(
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0.dp,
                glass = glass,
                enabled = true,
                borderWidth = 0.dp,
                shape = RoundedCornerShape(0.dp)
            ) {
                // Background fill only
            }
            content()
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            content()
        }
    }
}

/**
 * Blur layer visibility helper.
 *
 * Returns `0f` when the layer should be removed from the display list (glass disabled, no blur
 * available) and `1f` otherwise. Kept explicit because an off-screen blur layer that is merely
 * invisible still costs a full offscreen allocation every frame.
 */
internal fun blurLayerAlpha(enabled: Boolean, blurAvailable: Boolean): Float =
    if (enabled && blurAvailable) 1f else 0f

/**
 * Scrim alpha for the fullscreen wallpaper backdrop, derived from the validated [GlassStyle].
 *
 * The frosted path is intentionally heavier than the non-frosted path (the wallpaper sits directly
 * under the icons with no panel in between, so it needs more neutralisation). Both are computed from
 * [GlassStyle.translucency] — no call site ever hardcodes a hex/float — and are clamped so a
 * pathological slider value can never wash the wallpaper out to pure black or leave it raw.
 */
internal fun wallpaperScrimAlpha(isFrosted: Boolean, glass: GlassStyle): Float {
    val translucency = glass.translucency
    val frosted = (0.35f + translucency * 0.85f).coerceIn(0.22f, 0.85f)
    // Non-frosted themes keep the same tonal direction at roughly 0.37x the weight.
    return if (isFrosted) frosted else (frosted * 0.37f).coerceIn(0.10f, 0.40f)
}
