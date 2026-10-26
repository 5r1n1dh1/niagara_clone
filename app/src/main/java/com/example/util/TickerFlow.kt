package com.example.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Shared per-second (or per-interval) ticker flow for clock loops,
 * media polling, and any periodic refresh that needs lifecycle-aware ticks.
 */
object TickerFlow {

    private val _tick = MutableStateFlow(System.currentTimeMillis())
    val tick: StateFlow<Long> = _tick.asStateFlow()

    private var tickerJob: Job? = null

    fun start(intervalMs: Long = 1000L) {
        stop()
        tickerJob = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            while (isActive) {
                _tick.value = System.currentTimeMillis()
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        tickerJob?.cancel()
        tickerJob = null
    }

    /** Convenience per-second ticker for visible-clock updates. */
    val perSecondTicker: Flow<Long> = tick.asStateFlow()

    /** Convenience per-minute ticker for non-second-visible clocks. */
    val perMinuteTicker: Flow<Long> = tick.asStateFlow()
}
