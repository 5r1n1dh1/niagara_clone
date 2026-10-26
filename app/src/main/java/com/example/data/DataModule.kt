package com.example.data

import android.content.Context
import com.example.domain.CategoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

/**
 * Hilt module providing data-layer bindings.
 *
 * LauncherRepository (which has an @Inject constructor and implements AppRepository,
 * CategoryRepository, and the other repository interfaces) satisfies those
 * interface bindings directly. This module only provides the Application
 * Context and the standalone SettingsRepositoryImpl that LauncherViewModel
 * requests explicitly.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideSettingsRepositoryImpl(context: Context): SettingsRepositoryImpl = SettingsRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideAppRepository(context: Context): AppRepository = LauncherRepository(context)

    @Provides
    @Singleton
    fun provideCategoryRepository(context: Context): CategoryRepository = LauncherRepository(context)
}