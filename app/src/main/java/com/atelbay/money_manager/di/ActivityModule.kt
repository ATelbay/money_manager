package com.atelbay.money_manager.di

import com.atelbay.money_manager.core.auth.ActivityProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ActivityModule {

    @Binds
    @Singleton
    abstract fun bindActivityProvider(impl: ActivityProviderImpl): ActivityProvider
}
