package com.example.data

import kotlinx.coroutines.flow.StateFlow

/**
 * MediaPlaybackRepository implementation — Agent C split.
 */
class MediaPlaybackRepositoryImpl : MediaPlaybackRepository {
    override val mediaPlayer: StateFlow<MediaPlaybackState> = kotlinx.coroutines.flow.MutableStateFlow(MediaPlaybackState())
}
