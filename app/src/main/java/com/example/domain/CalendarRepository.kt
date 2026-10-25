package com.example.domain

import kotlinx.coroutines.flow.StateFlow
import com.example.data.CalendarEvent

interface CalendarRepository {
    val calendarEvents: StateFlow<List<CalendarEvent>>
    fun refreshCalendarEvents()
}
