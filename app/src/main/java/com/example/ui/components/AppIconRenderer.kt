package com.example.ui.components

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.AppItem
import com.example.data.CustomIconPackSettings
import com.example.data.CustomPackPreset
import com.example.data.IconPackType
import com.example.data.IconShape
import kotlin.math.abs

private val iconBitmapCache = LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(120)

@Composable
fun AppIconRenderer(
    app: AppItem,
    iconPackType: IconPackType,
    customIconSettings: CustomIconPackSettings = CustomIconPackSettings(),
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val (primaryColor, accentColor) = remember(app.packageName) {
        getColorsForPackage(app.packageName)
    }
    val iconVector = remember(app.packageName, app.label) {
        getIconVectorForApp(app.packageName, app.label)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when (iconPackType) {
            IconPackType.ANYCONS_DOTS -> {
                // Niagara Dots: Pure minimalist dot to eliminate dopamine branding triggers
                Box(
                    modifier = Modifier
                        .size(size * 0.38f)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
            }

            IconPackType.ANYCONS_STICKER -> {
                // White silhouette icon on gentle tinted circle
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(size * 0.58f)
                    )
                }
            }

            IconPackType.ANYCONS_MINIMAL -> {
                // Pure outline vector glyph, ultra-clean aesthetic
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(size * 0.28f))
                        .border(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), RoundedCornerShape(size * 0.28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(size * 0.54f)
                    )
                }
            }

            IconPackType.ANYCONS_COLORFUL -> {
                // Vibrant pastel duotone gradient badge
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(size * 0.32f))
                        .background(
                            Brush.linearGradient(
                                listOf(primaryColor, accentColor)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
            }

            IconPackType.MATERIAL -> {
                // Material Modern: Sleek rounded squircle with subtle depth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(size * 0.28f))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(size * 0.58f)
                    )
                }
            }

            IconPackType.CUSTOM -> {
                // User-chosen Custom Icon Pack (Presets, 3rd Party, or Custom Engine)
                val shape = when (customIconSettings.iconShape) {
                    IconShape.CIRCLE -> CircleShape
                    IconShape.ROUNDED_SQUIRCLE -> RoundedCornerShape(28)
                    IconShape.SMOOTH_SQUARE -> RoundedCornerShape(16)
                    IconShape.TEARDROP -> RoundedCornerShape(size * 0.45f, size * 0.45f, size * 0.45f, size * 0.12f)
                    IconShape.HEXAGON -> RoundedCornerShape(25)
                }

                when (customIconSettings.packPreset) {
                    CustomPackPreset.FLUX_GLASS -> {
                        // Flux Frosted Glass: Emerald to sage gradient badge with mint glow rim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF2D6154), Color(0xFF14382E))
                                    )
                                )
                                .border(1.2.dp, Color(0xFF6EE7B7).copy(alpha = 0.65f), shape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = Color(0xFFE6FFFA),
                                modifier = Modifier.size(size * 0.58f)
                            )
                        }
                    }

                    CustomPackPreset.WHICONS_WHITE -> {
                        // Whicons Pure White: Crisp white monochrome glyph on subtle dark disc
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape)
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(size * 0.58f)
                            )
                        }
                    }

                    CustomPackPreset.DELTA_PASTEL -> {
                        // Delta Vintage Pastel: Warm muted retro pastel duotone
                        val pastelColors = listOf(
                            Color(0xFF80CBC4), Color(0xFFFFAB91), Color(0xFFA5D6A7),
                            Color(0xFF90CAF9), Color(0xFFCE93D8), Color(0xFFFFF59D),
                            Color(0xFFFFCC80), Color(0xFFB0BEC5)
                        )
                        val pastelBg = pastelColors[abs(app.packageName.hashCode()) % pastelColors.size]
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape)
                                .background(pastelBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = Color(0xFF263238),
                                modifier = Modifier.size(size * 0.54f)
                            )
                        }
                    }

                    CustomPackPreset.LINEBIT_NEON -> {
                        // Linebit Cyber Glow: Neon stroke outlines with glowing vectors
                        val neonPalettes = listOf(
                            Color(0xFF00FFE0), Color(0xFFFF007F), Color(0xFF76FF03),
                            Color(0xFF00E5FF), Color(0xFFFFEA00), Color(0xFFE040FB)
                        )
                        val neonColor = neonPalettes[abs(app.packageName.hashCode()) % neonPalettes.size]
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape)
                                .border(1.6.dp, neonColor.copy(alpha = 0.85f), shape)
                                .background(neonColor.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = neonColor,
                                modifier = Modifier.size(size * 0.54f)
                            )
                        }
                    }

                    CustomPackPreset.CHARCOAL_STEALTH -> {
                        // Dark Void Charcoal: Stealth dark icons for true AMOLED minimalism
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(size * 0.54f)
                            )
                        }
                    }

                    CustomPackPreset.CANDY_VIVID -> {
                        // CandyCons Vivid: Rich saturated gradient badge
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(primaryColor, accentColor)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(size * 0.56f)
                            )
                        }
                    }

                    CustomPackPreset.CUSTOM_STUDIO -> {
                        // Custom Pack Engine: User-configured shape, tint, and opacity
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(customIconSettings.customColorHex))
                        } catch (e: Exception) {
                            primaryColor
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape)
                                .background(parsedColor.copy(alpha = customIconSettings.backgroundOpacity)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = parsedColor,
                                modifier = Modifier.size(size * 0.56f)
                            )
                        }
                    }
                }
            }

            IconPackType.DEFAULT_APP -> {
                val context = LocalContext.current
                val cacheKey = "${app.packageName}_${size.value.toInt()}"
                val cachedBitmap = iconBitmapCache.get(cacheKey)
                val bitmap = remember(app.packageName, size.value, app.iconDrawable) {
                    if (cachedBitmap != null) {
                        cachedBitmap
                    } else {
                        val drawable = app.iconDrawable ?: try {
                            context.packageManager.getApplicationIcon(app.packageName)
                        } catch (e: Exception) {
                            try {
                                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                if (intent?.component != null) {
                                    context.packageManager.getActivityIcon(intent.component!!)
                                } else null
                            } catch (e2: Exception) {
                                null
                            }
                        }
                        try {
                            drawable?.let { d ->
                                val density = context.resources.displayMetrics.density
                                val px = (size.value * density).toInt().coerceAtLeast(64)
                                val converted = d.toBitmap(width = px, height = px).asImageBitmap()
                                iconBitmapCache.put(cacheKey, converted)
                                converted
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.label,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(size * 0.22f))
                    )
                } else {
                    FallbackGlyph(app, size, primaryColor)
                }
            }
        }
    }
}

@Composable
private fun FallbackGlyph(app: AppItem, size: Dp, color: Color) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = app.label.take(1).uppercase(),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.45f).sp
        )
    }
}

private fun getColorsForPackage(pkg: String): Pair<Color, Color> {
    val h = abs(pkg.hashCode())
    val palettes = listOf(
        Pair(Color(0xFF38BDF8), Color(0xFF0284C7)), // Blue
        Pair(Color(0xFF34D399), Color(0xFF059669)), // Emerald
        Pair(Color(0xFFF472B6), Color(0xFFDB2777)), // Pink
        Pair(Color(0xFFFB923C), Color(0xFFEA580C)), // Orange
        Pair(Color(0xFFA78BFA), Color(0xFF7C3AED)), // Violet
        Pair(Color(0xFFFBBF24), Color(0xFFD97706)), // Amber
        Pair(Color(0xFF2DD4BF), Color(0xFF0D9488)), // Teal
        Pair(Color(0xFF818CF8), Color(0xFF4F46E5))  // Indigo
    )
    return palettes[h % palettes.size]
}

private fun getIconVectorForApp(pkg: String, label: String): ImageVector {
    val p = pkg.lowercase()
    val l = label.lowercase()
    return when {
        p.contains("dialer") || p.contains("phone") || l.contains("phone") -> Icons.Filled.Phone
        p.contains("messaging") || p.contains("mms") || l.contains("message") || l.contains("chat") -> Icons.Filled.Mail
        p.contains("chrome") || p.contains("browser") || l.contains("browser") -> Icons.Filled.Public
        p.contains("camera") || l.contains("camera") -> Icons.Filled.CameraAlt
        p.contains("spotify") || p.contains("music") || l.contains("music") || l.contains("podcast") -> Icons.Filled.MusicNote
        p.contains("settings") || l.contains("setting") -> Icons.Filled.Settings
        p.contains("maps") || l.contains("map") || l.contains("navigation") -> Icons.Filled.Place
        p.contains("calendar") || l.contains("calendar") -> Icons.Filled.DateRange
        p.contains("deskclock") || p.contains("clock") || l.contains("clock") || l.contains("alarm") -> Icons.Filled.Schedule
        p.contains("calculator") || l.contains("calc") -> Icons.Filled.Calculate
        p.contains("photos") || p.contains("gallery") || l.contains("photo") || l.contains("gallery") -> Icons.Filled.Image
        p.contains("vending") || p.contains("play") || l.contains("store") -> Icons.Filled.ShoppingCart
        p.contains("docs") || p.contains("drive") || l.contains("drive") || l.contains("file") -> Icons.Filled.Folder
        p.contains("keep") || p.contains("notes") || l.contains("note") -> Icons.Filled.EditNote
        p.contains("youtube") || l.contains("video") -> Icons.Filled.PlayCircle
        p.contains("github") -> Icons.Filled.Code
        p.contains("slack") || p.contains("discord") || p.contains("social") || l.contains("talk") -> Icons.Filled.Forum
        p.contains("instagram") || p.contains("twitter") || p.contains("reddit") -> Icons.Filled.Share
        else -> Icons.Filled.Apps
    }
}
