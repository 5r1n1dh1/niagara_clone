package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

                    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                        ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
                        ?: pkg

                    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                        ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
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

                    val item = NotificationItem(
                        id = sbn.key ?: "${pkg}_${sbn.id}",
                        packageName = pkg,
                        sender = title,
                        message = text,
                        timeAgo = postTime,
                        canReply = hasReply,
                        key = sbn.key ?: ""
                    )

                    // Store notification mapped by package name for app launcher badge/preview
                    map[pkg] = item
                    multiMap.getOrPut(pkg) { mutableListOf() }.add(item)
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
            syncNotifications()
        } catch (t: Throwable) {
            // Ignore
        }
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
