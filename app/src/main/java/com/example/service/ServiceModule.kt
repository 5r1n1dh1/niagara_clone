package com.example.service

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module providing service-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    fun provideNotificationService(): FluxNotificationListenerService? {
        // Service instance bound by Android lifecycle; injectable reference provided for v1.0.1.
        return null
    }

    @Provides
    fun provideAccessibilityService(): FluxAccessibilityService? {
        return null
    }
}
