package com.example.domain

import com.example.data.CalendarEvent
import kotlinx.coroutines.flow.StateFlow

/**
 * Calendar Repository Interface - Phase 2 Decomposed Domain Layer
 */
interface CalendarRepository {
    val calendarEvents: StateFlow<List<CalendarEvent>>
    fun refreshCalendarEvents()
}
