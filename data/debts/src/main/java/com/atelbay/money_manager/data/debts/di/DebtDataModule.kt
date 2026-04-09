package com.atelbay.money_manager.data.debts.di

import com.atelbay.money_manager.data.debts.repository.DebtPaymentRepositoryImpl
import com.atelbay.money_manager.data.debts.repository.DebtRepositoryImpl
import com.atelbay.money_manager.domain.debts.repository.DebtPaymentRepository
import com.atelbay.money_manager.domain.debts.repository.DebtRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DebtDataModule {
    @Binds
    abstract fun bindDebtRepository(
        impl: DebtRepositoryImpl,
    ): DebtRepository

    @Binds
    abstract fun bindDebtPaymentRepository(
        impl: DebtPaymentRepositoryImpl,
    ): DebtPaymentRepository
}
