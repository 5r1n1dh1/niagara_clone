package com.example.data

import kotlinx.coroutines.flow.StateFlow

/**
 * WidgetRepository implementation — Agent C split.
 */
class WidgetRepositoryImpl : WidgetRepository {
    override val leftWidgets: StateFlow<List<CustomWidgetConfig>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    override val carouselWidgets: StateFlow<List<WidgetType>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    override fun isWidgetAvailable(type: WidgetType): Boolean = true
}
