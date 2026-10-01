package com.example.domain

import com.example.data.MediaPlaybackState
import kotlinx.coroutines.flow.StateFlow

/**
 * Media Playback Repository Interface - Phase 2 Decomposed Domain Layer
 */
interface MediaPlaybackRepository {
    val mediaPlayer: StateFlow<MediaPlaybackState>
}
