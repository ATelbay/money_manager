package com.atelbay.money_manager.data.transactions.di

import com.atelbay.money_manager.data.transactions.repository.TransactionRepositoryImpl
import com.atelbay.money_manager.domain.transactions.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TransactionDataModule {
    @Binds
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl,
    ): TransactionRepository
}
