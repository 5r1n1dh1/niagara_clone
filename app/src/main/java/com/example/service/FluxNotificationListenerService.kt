package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.example.data.NotificationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FluxNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var mediaProgressPollJob: Job? = null
    private var activeMediaController: MediaController? = null

    private val mediaControllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaPlaybackFromController(activeMediaController)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaPlaybackFromController(activeMediaController)
        }

        override fun onSessionDestroyed() {
            activeMediaController = null
            findAndAttachActiveMediaController()
        }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        findAndAttachActiveMediaController(controllers)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        startMediaProgressPoller()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProgressPollJob?.cancel()
        unregisterSessionListener()
        if (instance == this) {
            instance = null
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        registerSessionListener()
        findAndAttachActiveMediaController()
        syncNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        unregisterSessionListener()
        if (instance == this) {
            instance = null
        }
    }

    private fun registerSessionListener() {
        try {
            val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val component = ComponentName(this, FluxNotificationListenerService::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, component)
        } catch (_: Throwable) {}
    }

    private fun unregisterSessionListener() {
        try {
            val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
            activeMediaController?.unregisterCallback(mediaControllerCallback)
            activeMediaController = null
        } catch (_: Throwable) {}
    }

    private fun findAndAttachActiveMediaController(controllers: List<MediaController>? = null) {
        try {
            val list = controllers ?: run {
                val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                val component = ComponentName(this, FluxNotificationListenerService::class.java)
                mediaSessionManager?.getActiveSessions(component) ?: emptyList()
            }

            // Prioritize YouTube Music / ReVanced packages
            val ytController = list.firstOrNull { c ->
                val pkg = c.packageName ?: ""
                pkg.equals("app.revanced.android.apps.youtube.music", ignoreCase = true) ||
                        pkg.equals("com.google.android.apps.youtube.music", ignoreCase = true) ||
                        pkg.equals("app.rvx.android.apps.youtube.music", ignoreCase = true) ||
                        pkg.equals("com.vanced.android.apps.youtube.music", ignoreCase = true) ||
                        pkg.contains("youtube.music", true)
            }

            // Or any currently playing controller
            val playingController = ytController ?: list.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: list.firstOrNull()

            if (playingController != activeMediaController) {
                activeMediaController?.unregisterCallback(mediaControllerCallback)
                activeMediaController = playingController
                activeMediaController?.registerCallback(mediaControllerCallback)
            }

            updateMediaPlaybackFromController(activeMediaController)
        } catch (_: Throwable) {}
    }

    private fun startMediaProgressPoller() {
        mediaProgressPollJob?.cancel()
        mediaProgressPollJob = serviceScope.launch {
            while (isActive) {
                delay(500)
                val current = _liveMediaPlayback.value
                if (current != null && current.isPlaying && current.durationMs > 0) {
                    val controller = activeMediaController
                    if (controller != null) {
                        updateMediaPlaybackFromController(controller)
                    } else {
                        // Smoothly advance time
                        val newPos = (current.positionMs + 500).coerceAtMost(current.durationMs)
                        val newProgress = (newPos.toFloat() / current.durationMs.toFloat()).coerceIn(0f, 1f)
                        _liveMediaPlayback.value = current.copy(
                            positionMs = newPos,
                            progressPercent = newProgress,
                            currentTimeText = formatMs(newPos)
                        )
                    }
                }
            }
        }
    }

    private fun updateMediaPlaybackFromController(controller: MediaController?) {
        if (controller == null) return
        try {
            val metadata = controller.metadata
            val playbackState = controller.playbackState
            val pkg = controller.packageName ?: ""
            val appLabel = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
            } catch (_: Throwable) {
                if (pkg.contains("revanced", true)) "YouTube Music ReVanced" else "YouTube Music"
            }

            val trackTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
            val artistName = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: appLabel

            val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            val rawPosition = playbackState?.position ?: 0L
            val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
            val speed = playbackState?.playbackSpeed ?: 1.0f
            val lastUpdate = playbackState?.lastPositionUpdateTime ?: 0L

            val currentPositionMs = if (isPlaying && lastUpdate > 0) {
                rawPosition + ((SystemClock.elapsedRealtime() - lastUpdate) * (if (speed > 0f) speed else 1.0f)).toLong()
            } else {
                rawPosition
            }.coerceIn(0L, if (durationMs > 0) durationMs else Long.MAX_VALUE)

            val progressPercent = if (durationMs > 0) {
                (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            val durationText = if (durationMs > 0) formatMs(durationMs) else "--:--"
            val currentTimeText = formatMs(currentPositionMs)

            var albumArt: Bitmap? = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

            if (albumArt == null) {
                val artUriStr = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                    ?: metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)
                    ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
                if (!artUriStr.isNullOrBlank()) {
                    albumArt = extractBitmapFromObject(artUriStr, maxDimension = 512)
                }
            }

            if (albumArt == null) {
                val notifs = activeNotifications
                val matchingSbn = notifs?.firstOrNull { it.packageName == pkg }
                if (matchingSbn != null) {
                    val extras = matchingSbn.notification?.extras ?: Bundle()
                    albumArt = extractPictureBitmap(matchingSbn.notification, extras)
                        ?: extractAvatarBitmap(matchingSbn.notification, extras)
                }
            }

            if (trackTitle.isNotBlank()) {
                _liveMediaPlayback.value = com.example.data.MediaPlaybackState(
                    isPlaying = isPlaying,
                    trackTitle = trackTitle,
                    artistName = artistName,
                    appName = appLabel,
                    packageName = pkg,
                    albumArtBitmap = albumArt,
                    progressPercent = progressPercent,
                    durationText = durationText,
                    currentTimeText = currentTimeText,
                    durationMs = durationMs,
                    positionMs = currentPositionMs,
                    isShuffleEnabled = false
                )
            }
        } catch (_: Throwable) {}
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || shouldFilter(sbn)) return
        syncNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        syncNotifications()
    }

    private fun syncNotifications() {
        serviceScope.launch {
            try {
                val activeSbns = activeNotifications ?: return@launch
                val map = mutableMapOf<String, NotificationItem>()
                val multiMap = mutableMapOf<String, MutableList<NotificationItem>>()
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                var detectedMedia: com.example.data.MediaPlaybackState? = null

                for (sbn in activeSbns) {
                    val pkg = sbn.packageName ?: continue
                    val notif = sbn.notification ?: continue
                    val extras = notif.extras ?: Bundle()

                    val appLabel = try {
                        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
                    } catch (_: Throwable) {
                        pkg
                    }

                    // Check for media session / music player notification
                    val isYouTubeMusicPkg = pkg.equals("app.revanced.android.apps.youtube.music", ignoreCase = true) ||
                            pkg.equals("com.google.android.apps.youtube.music", ignoreCase = true) ||
                            pkg.equals("app.rvx.android.apps.youtube.music", ignoreCase = true) ||
                            pkg.equals("com.vanced.android.apps.youtube.music", ignoreCase = true) ||
                            pkg.contains("youtube.music", true) ||
                            (pkg.contains("revanced", true) && (pkg.contains("music", true) || pkg.contains("audio", true)))

                    val isMedia = notif.category == Notification.CATEGORY_TRANSPORT ||
                            extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                            extras.containsKey("android.mediaSession") ||
                            isYouTubeMusicPkg ||
                            pkg.contains("spotify", true) ||
                            pkg.contains("music", true) ||
                            pkg.contains("audio", true) ||
                            pkg.contains("podcast", true) ||
                            pkg.contains("soundcloud", true) ||
                            pkg.contains("deezer", true)

                    // If we haven't found a media notification yet, or if this notification is from YouTube Music / ReVanced
                    if (isMedia && (detectedMedia == null || isYouTubeMusicPkg)) {
                        val trackTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
                            ?: ""
                        val artistName = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                            ?: appLabel

                        val albumArt: Bitmap? = try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                extras.getParcelable(Notification.EXTRA_PICTURE) as? Bitmap
                            } ?: (extras.get(Notification.EXTRA_PICTURE) as? Bitmap)
                              ?: (extras.get(Notification.EXTRA_LARGE_ICON_BIG) as? Bitmap)
                              ?: (extras.get(Notification.EXTRA_LARGE_ICON) as? Bitmap)
                        } catch (_: Throwable) { null }

                        if (trackTitle.isNotBlank()) {
                            val isPlaying = notif.actions?.any {
                                val actionTitle = it.title?.toString()?.lowercase() ?: ""
                                actionTitle.contains("pause")
                            } ?: true

                            val durationMs = extras.getLong("android.duration", 0L).let {
                                if (it > 0) it else extras.getLong("duration", 0L)
                            }
                            val durationText = if (durationMs > 0) formatMs(durationMs) else "--:--"
                            val currentTimeText = "0:00"

                            detectedMedia = com.example.data.MediaPlaybackState(
                                isPlaying = isPlaying,
                                trackTitle = trackTitle,
                                artistName = artistName,
                                appName = appLabel,
                                packageName = pkg,
                                albumArtBitmap = albumArt,
                                progressPercent = 0f,
                                durationText = durationText,
                                currentTimeText = currentTimeText,
                                durationMs = durationMs,
                                positionMs = 0L
                            )
                            // Immediately query MediaController for real-time state
                            findAndAttachActiveMediaController()
                        }
                    }

                    if (shouldFilter(sbn)) continue

                    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                        ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
                        ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
                        ?: appLabel

                    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                        ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                        ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                        ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
                        ?: ""

                    if (title.isBlank() && text.isBlank()) continue

                    val hasReply = notif.actions?.any { action ->
                        action.remoteInputs?.isNotEmpty() == true
                    } ?: false

                    val postTime = if (sbn.postTime > 0) {
                        timeFormat.format(Date(sbn.postTime))
                    } else {
                        "Just now"
                    }

                    val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                        ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()

                    val richText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                        ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

                    val pictureBitmap: Bitmap? = extractPictureBitmap(notif, extras)
                    val senderIcon: Bitmap? = extractAvatarBitmap(notif, extras)

                    val myUser = try { android.os.Process.myUserHandle() } catch (_: Throwable) { null }
                    val isSecondary = (myUser != null && sbn.user != null && sbn.user != myUser)
                    val userStr = sbn.user?.toString() ?: ""
                    val isDual = isSecondary || pkg.contains("dual", true) || pkg.contains("clone", true) ||
                            pkg.contains("parallel", true) || userStr.contains("95") || userStr.contains("96") ||
                            userStr.contains("10") || userStr.contains("11")
                    val instanceKey = if (isDual) "$pkg:dual" else pkg

                    val item = NotificationItem(
                        id = sbn.key ?: "${pkg}_${sbn.id}",
                        packageName = pkg,
                        sender = title,
                        message = text,
                        timeAgo = postTime,
                        canReply = hasReply,
                        key = sbn.key ?: "${pkg}_${sbn.id}",
                        subText = subText,
                        richText = richText,
                        isDualApp = isDual,
                        instanceKey = instanceKey,
                        pictureBitmap = pictureBitmap,
                        senderIcon = senderIcon
                    )

                    // Store latest notification mapped by unique instance key
                    map[instanceKey] = item

                    // Add to multi-notification list for this app instance without duplicates
                    val notifList = multiMap.getOrPut(instanceKey) { mutableListOf() }
                    if (notifList.none { it.key == item.key }) {
                        notifList.add(item)
                    }
                }
                _liveNotifications.value = map
                _allLiveNotifications.value = multiMap
                if (detectedMedia != null) {
                    _liveMediaPlayback.value = detectedMedia
                }
            } catch (t: Throwable) {
                // Safeguard against OS security exceptions
            }
        }
    }

    private fun extractAvatarBitmap(notif: Notification, extras: Bundle): Bitmap? {
        // 1. Direct getLargeIcon on Notification (Android M+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                notif.largeIcon?.let { icon ->
                    extractBitmapFromObject(icon)?.let { return it }
                }
            } catch (_: Throwable) {}
        }

        // 2. EXTRA_LARGE_ICON & EXTRA_LARGE_ICON_BIG
        try {
            extras.get(Notification.EXTRA_LARGE_ICON)?.let { obj ->
                extractBitmapFromObject(obj)?.let { return it }
            }
        } catch (_: Throwable) {}

        try {
            extras.get(Notification.EXTRA_LARGE_ICON_BIG)?.let { obj ->
                extractBitmapFromObject(obj)?.let { return it }
            }
        } catch (_: Throwable) {}

        // 3. Conversation Icon (Android 11+)
        try {
            extras.get("android.conversationIcon")?.let { obj ->
                extractBitmapFromObject(obj)?.let { return it }
            }
        } catch (_: Throwable) {}

        // 4. Messaging Person (Android P+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val person = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable(Notification.EXTRA_MESSAGING_PERSON, android.app.Person::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras.getParcelable(Notification.EXTRA_MESSAGING_PERSON) as? android.app.Person
                }
                person?.icon?.let { icon ->
                    extractBitmapFromObject(icon)?.let { return it }
                }
            } catch (_: Throwable) {}
        }

        // 5. Messages Array in MessagingStyle (check latest message sender avatar)
        try {
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages != null && messages.isNotEmpty()) {
                for (i in messages.indices.reversed()) {
                    val msg = messages[i]
                    if (msg is Bundle) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val person = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                msg.getParcelable("sender_person", android.app.Person::class.java)
                                    ?: msg.getParcelable("person", android.app.Person::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                (msg.getParcelable("sender_person") as? android.app.Person)
                                    ?: (msg.getParcelable("person") as? android.app.Person)
                            }
                            person?.icon?.let { icon ->
                                extractBitmapFromObject(icon)?.let { return it }
                            }
                        }
                        msg.get("sender_icon")?.let { iconObj ->
                            extractBitmapFromObject(iconObj)?.let { return it }
                        }
                    }
                }
            }
        } catch (_: Throwable) {}

        return null
    }

    private fun extractPictureBitmap(notif: Notification, extras: Bundle): Bitmap? {
        // 1. EXTRA_PICTURE
        try {
            extras.get(Notification.EXTRA_PICTURE)?.let { obj ->
                extractBitmapFromObject(obj, maxDimension = 1024)?.let { return it }
            }
        } catch (_: Throwable) {}

        // 2. EXTRA_PICTURE_ICON (Android 12+)
        try {
            extras.get("android.pictureIcon")?.let { obj ->
                extractBitmapFromObject(obj, maxDimension = 1024)?.let { return it }
            }
        } catch (_: Throwable) {}

        // 3. Background Image URI
        try {
            extras.get("android.backgroundImageUri")?.let { obj ->
                extractBitmapFromObject(obj, maxDimension = 1024)?.let { return it }
            }
        } catch (_: Throwable) {}

        // 4. Check MessagingStyle message attachments for images (ignore videos/audio as requested)
        try {
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages != null && messages.isNotEmpty()) {
                for (i in messages.indices.reversed()) {
                    val msg = messages[i]
                    if (msg is Bundle) {
                        val mimeType = msg.getString("data_mime_type") ?: msg.getString("type") ?: ""
                        if (mimeType.startsWith("image/", ignoreCase = true)) {
                            val uriObj = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                msg.getParcelable("data_uri", Uri::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                msg.getParcelable("data_uri")
                            } ?: msg.get("data_uri") ?: msg.get("uri")
                            if (uriObj != null) {
                                extractBitmapFromObject(uriObj, maxDimension = 1024)?.let { return it }
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {}

        return null
    }

    private fun extractBitmapFromObject(obj: Any?, maxDimension: Int = 256): Bitmap? {
        if (obj == null) return null
        if (obj is Bitmap) {
            return scaleBitmapIfNeeded(obj, maxDimension)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && obj is android.graphics.drawable.Icon) {
            try {
                val drawable = obj.loadDrawable(this) ?: return null
                return drawableToBitmap(drawable, maxDimension)
            } catch (_: Throwable) {}
        }
        if (obj is Drawable) {
            return drawableToBitmap(obj, maxDimension)
        }
        if (obj is Uri) {
            try {
                contentResolver.openInputStream(obj)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    return if (bmp != null) scaleBitmapIfNeeded(bmp, maxDimension) else null
                }
            } catch (_: Throwable) {}
        }
        if (obj is String && (obj.startsWith("content://") || obj.startsWith("file://"))) {
            try {
                val uri = Uri.parse(obj)
                contentResolver.openInputStream(uri)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    return if (bmp != null) scaleBitmapIfNeeded(bmp, maxDimension) else null
                }
            } catch (_: Throwable) {}
        }
        return null
    }

    private fun drawableToBitmap(drawable: Drawable, maxDimension: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return scaleBitmapIfNeeded(drawable.bitmap, maxDimension)
        }
        val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceIn(32, maxDimension) else 128
        val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceIn(32, maxDimension) else 128
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) return bitmap
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val newW: Int
        val newH: Int
        if (bitmap.width > bitmap.height) {
            newW = maxDimension
            newH = (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            newH = maxDimension
            newW = (maxDimension * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun shouldFilter(sbn: StatusBarNotification): Boolean {
        val notif = sbn.notification ?: return true
        // Filter out ongoing / foreground service notifications (e.g. download progress, media player, step tracker)
        val isOngoing = (notif.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isGroupSummary = (notif.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        val isSystem = sbn.packageName == "android" || sbn.packageName == packageName
        return isOngoing || isGroupSummary || isSystem
    }

    fun dismissNotificationByKey(key: String) {
        try {
            cancelNotification(key)
        } catch (_: Throwable) {
            try {
                val sbns = activeNotifications
                val match = sbns?.firstOrNull { it.key == key }
                if (match != null) {
                    cancelNotification(match.packageName, match.tag, match.id)
                }
            } catch (_: Throwable) {}
        }
        try {
            syncNotifications()
        } catch (_: Throwable) {}
    }

    fun replyNotificationByKey(key: String, replyText: String): Boolean {
        try {
            val sbns = activeNotifications ?: return false
            val sbn = sbns.firstOrNull { it.key == key } ?: return false
            val notif = sbn.notification ?: return false
            val actions = notif.actions ?: return false

            for (action in actions) {
                val remoteInputs = action.remoteInputs ?: continue
                for (remoteInput in remoteInputs) {
                    val bundle = Bundle()
                    bundle.putCharSequence(remoteInput.resultKey, replyText)
                    val intent = Intent()
                    RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
                    action.actionIntent.send(this, 0, intent)
                    return true
                }
            }
        } catch (t: Throwable) {
            // Fallback
        }
        return false
    }

    companion object {
        var instance: FluxNotificationListenerService? = null
            private set

        private val _liveNotifications = MutableStateFlow<Map<String, NotificationItem>>(emptyMap())
        val liveNotifications: StateFlow<Map<String, NotificationItem>> = _liveNotifications.asStateFlow()

        private val _allLiveNotifications = MutableStateFlow<Map<String, List<NotificationItem>>>(emptyMap())
        val allLiveNotifications: StateFlow<Map<String, List<NotificationItem>>> = _allLiveNotifications.asStateFlow()

        private val _liveMediaPlayback = MutableStateFlow<com.example.data.MediaPlaybackState?>(null)
        val liveMediaPlayback: StateFlow<com.example.data.MediaPlaybackState?> = _liveMediaPlayback.asStateFlow()

        fun triggerSync(): Boolean {
            val inst = instance ?: return false
            try {
                inst.syncNotifications()
                return true
            } catch (_: Throwable) {
                return false
            }
        }

        fun dismissNotification(key: String): Boolean {
            val inst = instance ?: return false
            try {
                inst.dismissNotificationByKey(key)
                return true
            } catch (_: Throwable) {
                return false
            }
        }

        fun dismissAllForPackage(packageName: String, isDualApp: Boolean = false): Boolean {
            val inst = instance ?: return false
            try {
                val myUser = try { android.os.Process.myUserHandle() } catch (_: Throwable) { null }
                val sbns = inst.activeNotifications ?: return false
                for (sbn in sbns) {
                    if (sbn.packageName == packageName) {
                        val sbnIsDual = (myUser != null && sbn.user != null && sbn.user != myUser) ||
                                packageName.contains("dual", true) || packageName.contains("clone", true) || packageName.contains("parallel", true)
                        if (sbnIsDual == isDualApp) {
                            try {
                                inst.cancelNotification(sbn.key)
                            } catch (_: Throwable) {
                                try {
                                    inst.cancelNotification(sbn.packageName, sbn.tag, sbn.id)
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                }
                inst.syncNotifications()
                return true
            } catch (_: Throwable) {
                return false
            }
        }

        fun replyToPackage(packageName: String, replyText: String, isDualApp: Boolean = false): Boolean {
            val inst = instance ?: return false
            try {
                val myUser = try { android.os.Process.myUserHandle() } catch (_: Throwable) { null }
                val sbns = inst.activeNotifications ?: return false
                val sbn = sbns.firstOrNull { sbn ->
                    if (sbn.packageName != packageName) return@firstOrNull false
                    val userStr = sbn.user?.toString() ?: ""
                    val sbnIsDual = (myUser != null && sbn.user != null && sbn.user != myUser) ||
                            packageName.contains("dual", true) || packageName.contains("clone", true) ||
                            packageName.contains("parallel", true) || userStr.contains("95") || userStr.contains("96") ||
                            userStr.contains("10") || userStr.contains("11")
                    sbnIsDual == isDualApp
                } ?: sbns.firstOrNull { it.packageName == packageName } ?: return false
                return inst.replyNotificationByKey(sbn.key, replyText)
            } catch (_: Throwable) {
                return false
            }
        }

        fun sendMediaControl(action: String, value: Float = 0f): Boolean {
            val inst = instance ?: return false
            val controller = inst.activeMediaController ?: run {
                inst.findAndAttachActiveMediaController()
                inst.activeMediaController
            }
            if (controller != null) {
                try {
                    val transport = controller.transportControls
                    when (action) {
                        "toggle_play" -> {
                            val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
                            if (isPlaying) transport.pause() else transport.play()
                        }
                        "play" -> transport.play()
                        "pause" -> transport.pause()
                        "next" -> transport.skipToNext()
                        "prev" -> transport.skipToPrevious()
                        "seek" -> {
                            val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                            if (duration > 0) {
                                transport.seekTo((value * duration).toLong())
                            }
                        }
                    }
                    inst.updateMediaPlaybackFromController(controller)
                    return true
                } catch (_: Throwable) {}
            }
            return false
        }

        fun formatMs(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledPackages.contains(context.packageName)
        }

        fun openNotificationAccessSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    // Ignore
                }
            }
        }
    }
}
