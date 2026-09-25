package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R

/**
 * Helper object providing production-ready App Shortcut management
 * using androidx.core ShortcutManagerCompat for maximum backward compatibility.
 */
object AppShortcutHelper {

    const val SHORTCUT_ID_RECENT_CHAT = "dynamic_recent_chat"
    const val EXTRA_CHAT_ID = "EXTRA_CHAT_ID"
    const val DEFAULT_CHAT_ID = "chat_flux_main"

    /**
     * Publishes or updates the dynamic "Recent Chat" shortcut.
     *
     * @param context Application or Activity context.
     * @param chatId Unique identifier for the recent chat session passed in Intent extras.
     */
    fun publishRecentChatShortcut(context: Context, chatId: String = DEFAULT_CHAT_ID) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CHAT_ID, chatId)
        }

        val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID_RECENT_CHAT)
            .setShortLabel(context.getString(R.string.shortcut_chat_short_label))
            .setLongLabel(context.getString(R.string.shortcut_chat_long_label))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_chat))
            .setIntent(launchIntent)
            .setRank(1)
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    /**
     * Removes the dynamic "Recent Chat" shortcut.
     */
    fun removeRecentChatShortcut(context: Context) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(SHORTCUT_ID_RECENT_CHAT))
    }

    /**
     * Disables the dynamic "Recent Chat" shortcut with a user-facing explanation.
     */
    fun disableRecentChatShortcut(context: Context, disabledMessage: String) {
        ShortcutManagerCompat.disableShortcuts(context, listOf(SHORTCUT_ID_RECENT_CHAT), disabledMessage)
    }
}
