package com.atelbay.money_manager.core.ai.di

import com.atelbay.money_manager.core.ai.GeminiService
import com.atelbay.money_manager.core.ai.GeminiServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    abstract fun bindGeminiService(impl: GeminiServiceImpl): GeminiService
}
