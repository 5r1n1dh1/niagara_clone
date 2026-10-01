package com.example.domain

import com.example.data.CustomWidgetConfig
import com.example.data.WidgetType
import kotlinx.coroutines.flow.StateFlow

/**
 * Widget Repository Interface - Phase 2 Decomposed Domain Layer
 */
interface WidgetRepository {
    val leftWidgets: StateFlow<List<CustomWidgetConfig>>
    val carouselWidgets: StateFlow<List<WidgetType>>
    fun isWidgetAvailable(type: WidgetType): Boolean
}
