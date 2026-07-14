package com.aistudio.xide.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Foundation Hilt module for Core Services.
 * Will provide concrete implementations in later phases.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    // Example of how a ServiceRegistry would be provided
    // @Provides
    // @Singleton
    // fun provideServiceRegistry(): ServiceRegistry = ServiceRegistryImpl()

}
