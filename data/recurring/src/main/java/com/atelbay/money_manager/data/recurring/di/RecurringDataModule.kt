package com.atelbay.money_manager.data.recurring.di

import com.atelbay.money_manager.data.recurring.repository.RecurringTransactionRepositoryImpl
import com.atelbay.money_manager.domain.recurring.repository.RecurringTransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RecurringDataModule {
    @Binds
    abstract fun bindRecurringTransactionRepository(
        impl: RecurringTransactionRepositoryImpl,
    ): RecurringTransactionRepository
}
