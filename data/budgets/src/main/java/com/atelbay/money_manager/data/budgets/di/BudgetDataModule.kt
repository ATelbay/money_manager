package com.atelbay.money_manager.data.budgets.di

import com.atelbay.money_manager.data.budgets.repository.BudgetRepositoryImpl
import com.atelbay.money_manager.domain.budgets.repository.BudgetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BudgetDataModule {
    @Binds
    abstract fun bindBudgetRepository(
        impl: BudgetRepositoryImpl,
    ): BudgetRepository
}
