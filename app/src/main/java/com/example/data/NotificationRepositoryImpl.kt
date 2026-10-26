package com.example.data

import kotlinx.coroutines.flow.StateFlow
import com.example.domain.NotificationRepository

/**
 * NotificationRepository implementation — Agent C split.
 */
class NotificationRepositoryImpl : NotificationRepository {
    override val activeNotifications: StateFlow<Map<String, NotificationItem>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())
    override val allActiveNotifications: StateFlow<Map<String, List<NotificationItem>>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())
}
