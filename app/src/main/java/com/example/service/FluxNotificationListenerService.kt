package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.example.data.NotificationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FluxNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        syncNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
        }
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

                for (sbn in activeSbns) {
                    if (shouldFilter(sbn)) continue
                    val pkg = sbn.packageName ?: continue
                    val notif = sbn.notification ?: continue
                    val extras = notif.extras ?: Bundle()

                    val appLabel = try {
                        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
                    } catch (_: Throwable) {
                        pkg
                    }

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

                    val pictureBitmap: Bitmap? = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            extras.getParcelable(Notification.EXTRA_PICTURE) as? Bitmap
                        } ?: (extras.get(Notification.EXTRA_PICTURE) as? Bitmap)
                          ?: (extras.get(Notification.EXTRA_LARGE_ICON_BIG) as? Bitmap)
                    } catch (_: Throwable) {
                        null
                    }

                    val senderIcon: Bitmap? = try {
                        extras.get(Notification.EXTRA_LARGE_ICON) as? Bitmap
                    } catch (_: Throwable) {
                        null
                    }

                    val myUser = try { android.os.Process.myUserHandle() } catch (_: Throwable) { null }
                    val isSecondary = (myUser != null && sbn.user != null && sbn.user != myUser)
                    val isDual = isSecondary || pkg.contains("dual", true) || pkg.contains("clone", true) || pkg.contains("parallel", true)
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

                    // Store notification mapped by instance key for proper dual app separation
                    map[instanceKey] = item
                    if (!isDual && !map.containsKey(pkg)) {
                        map[pkg] = item
                    }
                    multiMap.getOrPut(instanceKey) { mutableListOf() }.add(item)
                    if (!isDual) {
                        multiMap.getOrPut(pkg) { mutableListOf() }.add(item)
                    }
                }
                _liveNotifications.value = map
                _allLiveNotifications.value = multiMap
            } catch (t: Throwable) {
                // Safeguard against OS security exceptions
            }
        }
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

        fun replyToPackage(packageName: String, replyText: String): Boolean {
            val inst = instance ?: return false
            try {
                val sbns = inst.activeNotifications ?: return false
                val sbn = sbns.firstOrNull { it.packageName == packageName } ?: return false
                return inst.replyNotificationByKey(sbn.key, replyText)
            } catch (_: Throwable) {
                return false
            }
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
