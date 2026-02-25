package com.atelbay.money_manager.data.accounts.di

import com.atelbay.money_manager.data.accounts.repository.AccountRepositoryImpl
import com.atelbay.money_manager.domain.accounts.repository.AccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountDataModule {
    @Binds
    abstract fun bindAccountRepository(
        impl: AccountRepositoryImpl,
    ): AccountRepository
}
