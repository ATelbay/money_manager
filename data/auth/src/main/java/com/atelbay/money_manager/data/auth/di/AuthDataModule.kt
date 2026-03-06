package com.atelbay.money_manager.data.auth.di

import com.atelbay.money_manager.data.auth.repository.AuthRepositoryImpl
import com.atelbay.money_manager.domain.auth.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
