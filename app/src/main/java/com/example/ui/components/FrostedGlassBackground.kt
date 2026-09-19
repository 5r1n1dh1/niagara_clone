package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FrostedGlassBackground(
    modifier: Modifier = Modifier,
    isFrostedTheme: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Hardware blur scrim on supported Android versions
        val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.fillMaxSize().blur(26.dp)
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = blurModifier.background(
                Brush.verticalGradient(
                    if (isFrostedTheme) {
                        listOf(
                            Color(0x990C1322),
                            Color(0xB31E293B),
                            Color(0xDD0C1322)
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.74f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.90f)
                        )
                    }
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
                            Color.White.copy(alpha = 0.07f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.03f)
                        )
                    )
                )
        )

        content()
    }
}
