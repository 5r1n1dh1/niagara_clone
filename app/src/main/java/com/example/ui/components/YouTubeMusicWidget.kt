package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MediaPlaybackState

const val YT_MUSIC_PACKAGE_NAME = "com.google.android.apps.youtube.music"
const val YT_MUSIC_REVANCED_PACKAGE_NAME = "app.revanced.android.apps.youtube.music"

val YT_MUSIC_KNOWN_PACKAGES = listOf(
    "app.revanced.android.apps.youtube.music",
    "com.google.android.apps.youtube.music",
    "app.rvx.android.apps.youtube.music",
    "com.vanced.android.apps.youtube.music",
    "app.revanced.android.apps.youtube.music.extended"
)

/**
 * Returns the package name of YouTube Music, YouTube Music ReVanced, or modded YouTube Music
 * installed on the user device, or null if not installed.
 */
fun getInstalledYouTubeMusicPackage(context: Context): String? {
    val pm = context.packageManager

    // 1. Direct check of known package names
    for (pkg in YT_MUSIC_KNOWN_PACKAGES) {
        try {
            pm.getPackageInfo(pkg, 0)
            return pkg
        } catch (_: Throwable) {}
    }

    // 2. Check launch intents for known packages
    for (pkg in YT_MUSIC_KNOWN_PACKAGES) {
        try {
            if (pm.getLaunchIntentForPackage(pkg) != null) {
                return pkg
            }
        } catch (_: Throwable) {}
    }

    // 3. Scan installed launcher activities for YouTube Music / ReVanced signatures
    try {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            val label = info.loadLabel(pm).toString()
            if (pkg.equals(YT_MUSIC_REVANCED_PACKAGE_NAME, ignoreCase = true) ||
                pkg.equals(YT_MUSIC_PACKAGE_NAME, ignoreCase = true) ||
                pkg.contains("youtube.music", ignoreCase = true) ||
                pkg.contains("revanced.android.apps.youtube.music", ignoreCase = true) ||
                (pkg.contains("revanced", ignoreCase = true) && pkg.contains("music", ignoreCase = true)) ||
                label.contains("YouTube Music", ignoreCase = true) ||
                label.contains("Music ReVanced", ignoreCase = true) ||
                label.contains("ReVanced Music", ignoreCase = true) ||
                label.contains("YT Music", ignoreCase = true)
            ) {
                return pkg
            }
        }
    } catch (_: Throwable) {}

    return null
}

/**
 * Checks if YouTube Music or YouTube Music ReVanced app is installed on the user device.
 */
fun isYouTubeMusicInstalled(context: Context): Boolean {
    return getInstalledYouTubeMusicPackage(context) != null
}

/**
 * Returns the human-friendly app title for the installed YouTube Music / ReVanced client.
 */
fun getYouTubeMusicAppLabel(context: Context): String {
    val pkg = getInstalledYouTubeMusicPackage(context) ?: return "YouTube Music"
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(pkg, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (_: Throwable) {
        "YouTube Music"
    }
}

/**
 * Returns the name/type of connected external audio output device (Bluetooth, headphones, USB),
 * or null if only internal phone speaker/earpiece is active.
 */
fun getConnectedAudioOutputDevice(context: Context): String? {
    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (dev in devices) {
                when (dev.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER,
                    AudioDeviceInfo.TYPE_HEARING_AID -> {
                        val name = dev.productName?.toString()
                        return if (!name.isNullOrBlank()) name else "Bluetooth Audio"
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> return "Headphones"
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_DEVICE -> return "USB Audio"
                    else -> {}
                }
            }
        }
    } catch (_: Throwable) {}
    return null
}

/**
 * Opens YouTube Music or YouTube Music ReVanced main app.
 */
fun openYouTubeMusicApp(context: Context) {
    val pkg = getInstalledYouTubeMusicPackage(context)
    if (pkg != null) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return
            }
        } catch (_: Throwable) {}
    }
    // Fallback: Open in browser or Play Store
    try {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    } catch (_: Throwable) {}
}

/**
 * Opens YouTube Music Library / Playlists tab.
 */
fun openYouTubeMusicLibrary(context: Context) {
    val pkg = getInstalledYouTubeMusicPackage(context)
    if (pkg != null) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/library")).apply {
                `package` = pkg
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Throwable) {
            openYouTubeMusicApp(context)
            return
        }
    }
    openYouTubeMusicApp(context)
}

/**
 * Opens system audio route / output switcher settings.
 */
fun openAudioOutputSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Throwable) {
        try {
            val btIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(btIntent)
        } catch (_: Throwable) {}
    }
}

/**
 * Rebuilt Flux YouTube Music Widget:
 * - Exclusively integrates with YouTube Music
 * - Icon-only media controls (Shuffle, Previous, Play/Pause, Next, Library)
 * - Material You style interactive seekbar with timestamps
 * - Media output selector (only visible when external audio output is connected)
 * - Clickable Album Art opens YouTube Music app
 * - Dynamic UI supporting sizes: 4x2, 4x3, and 4x4
 * - Minimalist frosted glass backdrop with subtle blur
 * - Real-time playback synchronization
 */
@Composable
fun YouTubeMusicFluxWidget(
    media: MediaPlaybackState,
    modifier: Modifier = Modifier,
    size: String = "4x2", // "4x2", "4x3", "4x4"
    isHeadphonesOverride: Boolean = false,
    isEmbeddedInCard: Boolean = false,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val isInstalled = remember(context) { isYouTubeMusicInstalled(context) }
    val detectedDevice = remember(context, isHeadphonesOverride) {
        getConnectedAudioOutputDevice(context) ?: if (isHeadphonesOverride) "Headphones" else null
    }

    val handleOpenApp: () -> Unit = {
        onOpenApp()
        openYouTubeMusicApp(context)
    }

    val handleOpenLibrary: () -> Unit = {
        onOpenLibrary()
        openYouTubeMusicLibrary(context)
    }

    val handleOpenOutput: () -> Unit = {
        openAudioOutputSettings(context)
    }

    val widgetContent = @Composable {
        if (!isInstalled) {
            YouTubeMusicNotInstalledCard(
                onInstallClick = {
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=$YT_MUSIC_PACKAGE_NAME")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(intent)
                    } catch (_: Throwable) {
                        val webIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$YT_MUSIC_PACKAGE_NAME")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(webIntent)
                    }
                },
                modifier = Modifier.padding(10.dp)
            )
        } else {
            when (size) {
                "4x3" -> {
                    YouTubeMusicLayout4x3(
                        media = media,
                        outputDevice = detectedDevice,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onPrev = onPrev,
                        onSeek = onSeek,
                        onToggleShuffle = onToggleShuffle,
                        onOpenLibrary = handleOpenLibrary,
                        onOpenApp = handleOpenApp,
                        onOpenOutput = handleOpenOutput
                    )
                }
                "4x4" -> {
                    YouTubeMusicLayout4x4(
                        media = media,
                        outputDevice = detectedDevice,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onPrev = onPrev,
                        onSeek = onSeek,
                        onToggleShuffle = onToggleShuffle,
                        onOpenLibrary = handleOpenLibrary,
                        onOpenApp = handleOpenApp,
                        onOpenOutput = handleOpenOutput
                    )
                }
                else -> { // Default "4x2"
                    YouTubeMusicLayout4x2(
                        media = media,
                        outputDevice = detectedDevice,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onPrev = onPrev,
                        onSeek = onSeek,
                        onToggleShuffle = onToggleShuffle,
                        onOpenLibrary = handleOpenLibrary,
                        onOpenApp = handleOpenApp,
                        onOpenOutput = handleOpenOutput
                    )
                }
            }
        }
    }

    if (isEmbeddedInCard) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .testTag("youtube_music_flux_widget_$size")
        ) {
            widgetContent()
        }
    } else {
        // Standalone Frosted Minimalist Glass Card Container
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
                    )
                )
            ),
            tonalElevation = 2.dp,
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .testTag("youtube_music_flux_widget_$size")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                widgetContent()
            }
        }
    }
}

/**
 * 4x2 Compact Dynamic Layout
 */
@Composable
private fun YouTubeMusicLayout4x2(
    media: MediaPlaybackState,
    outputDevice: String?,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenOutput: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Top Row: Album Art + Track Info + Optional Output Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            YouTubeMusicAlbumArt(
                bitmap = media.albumArtBitmap,
                isPlaying = media.isPlaying,
                size = 40.dp,
                onClick = onOpenApp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.trackTitle.ifBlank { "YouTube Music" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = media.artistName.ifBlank { "Tap to play music" },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Media Output Selector: ONLY visible when external audio device is connected
            if (!outputDevice.isNullOrBlank()) {
                MediaOutputChip(
                    deviceName = outputDevice,
                    onClick = onOpenOutput
                )
            }
        }

        // Middle Row: Material You Seekbar
        MaterialYouSeekbar(
            progress = media.progressPercent,
            currentTime = media.currentTimeText,
            duration = media.durationText,
            onSeek = onSeek
        )

        // Bottom Row: Icon-Only Media Controls (with fully accessible touch area)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle Button (Icon only)
            IconControlButton(
                icon = Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                isActive = media.isShuffleEnabled,
                size = 36.dp,
                iconSize = 18.dp,
                onClick = onToggleShuffle
            )

            // Previous Button (Icon only)
            IconControlButton(
                icon = Icons.Filled.SkipPrevious,
                contentDescription = "Previous Track",
                size = 38.dp,
                iconSize = 22.dp,
                onClick = onPrev
            )

            // Play / Pause Button (Icon only with smooth animated morph)
            PlayPausePillButton(
                isPlaying = media.isPlaying,
                size = 42.dp,
                iconSize = 22.dp,
                onClick = onTogglePlay
            )

            // Next Button (Icon only)
            IconControlButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = "Next Track",
                size = 38.dp,
                iconSize = 22.dp,
                onClick = onNext
            )

            // Library Button (Icon only)
            IconControlButton(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "YouTube Music Library",
                size = 36.dp,
                iconSize = 18.dp,
                onClick = onOpenLibrary
            )
        }
    }
}

/**
 * 4x3 Medium Dynamic Layout
 */
@Composable
private fun YouTubeMusicLayout4x3(
    media: MediaPlaybackState,
    outputDevice: String?,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenOutput: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Section: Larger Album Art + Detailed Header Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            YouTubeMusicAlbumArt(
                bitmap = media.albumArtBitmap,
                isPlaying = media.isPlaying,
                size = 72.dp,
                onClick = onOpenApp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "YOUTUBE MUSIC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = media.trackTitle.ifBlank { "YouTube Music" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = media.artistName.ifBlank { "Ready to play" },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Output chip if connected
                if (!outputDevice.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    MediaOutputChip(
                        deviceName = outputDevice,
                        onClick = onOpenOutput
                    )
                }
            }
        }

        // Middle Section: Material You Seekbar with timestamps
        MaterialYouSeekbar(
            progress = media.progressPercent,
            currentTime = media.currentTimeText,
            duration = media.durationText,
            onSeek = onSeek
        )

        // Bottom Section: Centered Prominent Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconControlButton(
                icon = Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                isActive = media.isShuffleEnabled,
                size = 38.dp,
                onClick = onToggleShuffle
            )

            IconControlButton(
                icon = Icons.Filled.SkipPrevious,
                contentDescription = "Previous Track",
                size = 42.dp,
                iconSize = 24.dp,
                onClick = onPrev
            )

            PlayPausePillButton(
                isPlaying = media.isPlaying,
                size = 52.dp,
                iconSize = 28.dp,
                onClick = onTogglePlay
            )

            IconControlButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = "Next Track",
                size = 42.dp,
                iconSize = 24.dp,
                onClick = onNext
            )

            IconControlButton(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "YouTube Music Library",
                size = 38.dp,
                onClick = onOpenLibrary
            )
        }
    }
}

/**
 * 4x4 Expanded / Hero Dynamic Layout
 */
@Composable
private fun YouTubeMusicLayout4x4(
    media: MediaPlaybackState,
    outputDevice: String?,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenOutput: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Center Album Art with breathing glow
        Box(contentAlignment = Alignment.Center) {
            YouTubeMusicAlbumArt(
                bitmap = media.albumArtBitmap,
                isPlaying = media.isPlaying,
                size = 110.dp,
                onClick = onOpenApp
            )
        }

        // Track Title & Artist
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = media.trackTitle.ifBlank { "YouTube Music" },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = media.artistName.ifBlank { "Tap to stream your library" },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Output Selector chip if connected
            if (!outputDevice.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                MediaOutputChip(
                    deviceName = outputDevice,
                    onClick = onOpenOutput
                )
            }
        }

        // Material You Seekbar
        MaterialYouSeekbar(
            progress = media.progressPercent,
            currentTime = media.currentTimeText,
            duration = media.durationText,
            onSeek = onSeek
        )

        // Expanded Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconControlButton(
                icon = Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                isActive = media.isShuffleEnabled,
                size = 40.dp,
                onClick = onToggleShuffle
            )

            IconControlButton(
                icon = Icons.Filled.SkipPrevious,
                contentDescription = "Previous Track",
                size = 44.dp,
                iconSize = 26.dp,
                onClick = onPrev
            )

            PlayPausePillButton(
                isPlaying = media.isPlaying,
                size = 56.dp,
                iconSize = 30.dp,
                onClick = onTogglePlay
            )

            IconControlButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = "Next Track",
                size = 44.dp,
                iconSize = 26.dp,
                onClick = onNext
            )

            IconControlButton(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "YouTube Music Library",
                size = 40.dp,
                onClick = onOpenLibrary
            )
        }
    }
}

/**
 * Clickable Album Art displaying bitmap or YouTube Music gradient emblem.
 * Subtle breathing animation when music is actively playing.
 */
@Composable
private fun YouTubeMusicAlbumArt(
    bitmap: Bitmap?,
    isPlaying: Boolean,
    size: Dp,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "album_art_breathing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying) 1.03f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (isPlaying) 4.dp else 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .size(size)
            .scale(if (isPlaying) pulseScale else 1.0f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                RoundedCornerShape(16.dp)
            )
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "YouTube Music Album Art (Tap to open)",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF0000), // YouTube Red
                                Color(0xFF280000),
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayCircleFilled,
                    contentDescription = "YouTube Music",
                    tint = Color.White,
                    modifier = Modifier.size((size.value * 0.45f).dp)
                )
            }
        }
    }
}

/**
 * Material You Seekbar with interactive scrubber and timestamps.
 */
@Composable
private fun MaterialYouSeekbar(
    progress: Float,
    currentTime: String,
    duration: String,
    onSeek: (Float) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(progress) }

    LaunchedEffect(progress) {
        if (!isDragging) {
            dragFraction = progress
        }
    }

    val currentFraction = if (isDragging) dragFraction else progress
    val animatedProgress by animateFloatAsState(
        targetValue = currentFraction,
        animationSpec = if (isDragging) snap() else tween(durationMillis = 300, easing = LinearEasing),
        label = "seekbar_smooth_progress"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            isDragging = true
                            dragFraction = fraction
                            onSeek(fraction)
                            tryAwaitRelease()
                            isDragging = false
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Background Inactive Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            )

            // Active Progress Track
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0.01f, 1f))
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )

            // Scrubber Thumb
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0.02f, 0.98f))
                    .wrapContentWidth(Alignment.End)
                    .size(if (isDragging) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }

        // Timestamps (Material You style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentTime,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = duration,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Media Output Selector Chip: Rendered ONLY when an external audio output device is connected.
 */
@Composable
private fun MediaOutputChip(
    deviceName: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (deviceName.contains("headphone", true) || deviceName.contains("buds", true) || deviceName.contains("ear", true)) {
                    Icons.Filled.Headphones
                } else {
                    Icons.Filled.Speaker
                },
                contentDescription = "Output device: $deviceName",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = deviceName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 110.dp)
            )
        }
    }
}

/**
 * Icon-only button with smooth tactile press feedback.
 */
@Composable
private fun IconControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean = false,
    size: Dp = 36.dp,
    iconSize: Dp = 20.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_btn_scale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = size / 2),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Play/Pause Pill/Circle Button with animated icon transition.
 */
@Composable
private fun PlayPausePillButton(
    isPlaying: Boolean,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "play_pause_scale"
    )

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 3.dp,
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = size / 2),
                onClick = onClick
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "play_pause_icon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

/**
 * Disabled / Not-Installed state card for YouTube Music.
 */
@Composable
private fun YouTubeMusicNotInstalledCard(
    onInstallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF0000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "YouTube Music Required",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Install YouTube Music to enable widget",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onInstallClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Install", fontSize = 12.sp)
        }
    }
}
