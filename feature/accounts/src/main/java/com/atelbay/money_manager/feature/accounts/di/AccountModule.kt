package com.atelbay.money_manager.feature.accounts.di

import com.atelbay.money_manager.feature.accounts.data.repository.AccountRepositoryImpl
import com.atelbay.money_manager.feature.accounts.domain.repository.AccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    @Binds
    abstract fun bindAccountRepository(
        impl: AccountRepositoryImpl,
    ): AccountRepository
}
