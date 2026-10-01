package com.example.domain

import com.example.data.NotificationItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Notification Repository Interface - Phase 2 Decomposed Domain Layer
 */
interface NotificationRepository {
    val activeNotifications: StateFlow<Map<String, NotificationItem>>
    val allActiveNotifications: StateFlow<Map<String, List<NotificationItem>>>
}
