package com.atelbay.money_manager.core.remoteconfig.di

import com.atelbay.money_manager.core.remoteconfig.FirebaseRegexParserProfileProvider
import com.atelbay.money_manager.core.remoteconfig.RegexParserProfileProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteConfigModule {

    @Binds
    abstract fun bindRegexParserProfileProvider(
        impl: FirebaseRegexParserProfileProvider,
    ): RegexParserProfileProvider
}
