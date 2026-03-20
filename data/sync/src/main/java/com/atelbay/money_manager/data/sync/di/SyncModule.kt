package com.atelbay.money_manager.data.sync.di

import com.atelbay.money_manager.data.sync.SyncRepositoryImpl
import com.atelbay.money_manager.domain.sync.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository
}
