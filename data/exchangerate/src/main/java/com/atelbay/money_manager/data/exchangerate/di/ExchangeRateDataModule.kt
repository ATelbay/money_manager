package com.atelbay.money_manager.data.exchangerate.di

import com.atelbay.money_manager.data.exchangerate.repository.ExchangeRateRepositoryImpl
import com.atelbay.money_manager.domain.exchangerate.repository.ExchangeRateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ExchangeRateDataModule {
    @Binds
    abstract fun bindExchangeRateRepository(
        impl: ExchangeRateRepositoryImpl,
    ): ExchangeRateRepository
}
