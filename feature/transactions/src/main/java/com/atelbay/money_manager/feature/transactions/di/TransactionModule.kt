package com.atelbay.money_manager.feature.transactions.di

import com.atelbay.money_manager.feature.transactions.data.repository.TransactionRepositoryImpl
import com.atelbay.money_manager.feature.transactions.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TransactionModule {

    @Binds
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl,
    ): TransactionRepository
}
