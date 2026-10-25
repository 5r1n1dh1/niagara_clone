package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LauncherRepository
import com.example.data.NotificationItem
import com.example.ui.state.NotificationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    application: Application,
    private val repository: LauncherRepository
) : AndroidViewModel(application) {

    val uiState: StateFlow<NotificationUiState> = combine(
        repository.activeNotifications,
        repository.allActiveNotifications
    ) { active, all ->
        NotificationUiState.Content(
            notifications = active,
            allNotifications = all
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationUiState.Loading
    )

    fun dismissNotification(packageName: String, key: String? = null, isDualApp: Boolean = false) {
        repository.dismissNotification(packageName, key, isDualApp)
    }

    fun replyNotification(packageName: String, text: String, isDualApp: Boolean) {
        com.example.service.FluxNotificationListenerService.replyToPackage(packageName, text, isDualApp)
    }
}
