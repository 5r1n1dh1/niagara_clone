package com.example.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * Single source of truth for motion across the launcher.
 *
 * Every sheet, dialog, popup, dock, pager and widget state change reads its
 * animation spec from here so the app moves with one shared rhythm instead of
 * a dozen ad-hoc springs/tweens. The specs are plain immutable values: they are
 * safe to reference directly as default arguments and cost nothing to create.
 */
@Immutable
object MotionTokens {

    /** Sheets / dialogs / popups: settled, slightly under-damped and quick. */
    val sheetSpring: SpringSpec<Float> = spring(dampingRatio = 0.82f, stiffness = 700f)

    /** Dock icon press feedback: softer and slower than a sheet, feels weighted. */
    val dockSpring: SpringSpec<Float> = spring(dampingRatio = 0.70f, stiffness = 500f)

    /** Screen / content swaps that need to be readable, not snappy. */
    val emphasized: TweenSpec<Float> = tween(durationMillis = 320, easing = FastOutSlowInEasing)

    /** Micro-feedback (ticks, specular sheen, badges). */
    val quick: TweenSpec<Float> = tween(durationMillis = 140, easing = LinearOutSlowInEasing)

    /** Pager / page settling. */
    val screenSpring: SpringSpec<Float> =
        spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
}
