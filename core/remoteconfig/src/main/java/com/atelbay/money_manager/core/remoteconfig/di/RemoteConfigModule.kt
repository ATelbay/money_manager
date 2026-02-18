package com.atelbay.money_manager.core.remoteconfig.di

import com.atelbay.money_manager.core.remoteconfig.FirebaseParserConfigProvider
import com.atelbay.money_manager.core.remoteconfig.ParserConfigProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteConfigModule {

    @Binds
    abstract fun bindParserConfigProvider(
        impl: FirebaseParserConfigProvider,
    ): ParserConfigProvider
}
