package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LauncherRepository
import com.example.data.NotificationItem
import kotlinx.coroutines.flow.*

data class NotificationCenterUiState(
    val notifications: Map<String, NotificationItem> = emptyMap(),
    val allNotifications: Map<String, List<NotificationItem>> = emptyMap()
)

/**
 * Decomposed NotificationCenterViewModel adhering to Phase 3.
 */
class NotificationCenterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LauncherRepository(application)

    val uiState: StateFlow<NotificationCenterUiState> = combine(
        repository.activeNotifications,
        repository.allActiveNotifications
    ) { active, all ->
        NotificationCenterUiState(notifications = active, allNotifications = all)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationCenterUiState()
    )

    fun dismissNotification(packageName: String, key: String? = null, isDualApp: Boolean = false) {
        repository.dismissNotification(packageName, key, isDualApp)
    }

    fun replyNotification(packageName: String, text: String, isDualApp: Boolean) {
        com.example.service.FluxNotificationListenerService.replyToPackage(packageName, text, isDualApp)
    }
}
