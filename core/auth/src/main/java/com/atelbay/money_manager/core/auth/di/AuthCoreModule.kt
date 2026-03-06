package com.atelbay.money_manager.core.auth.di

import com.atelbay.money_manager.core.auth.AuthManager
import com.atelbay.money_manager.core.auth.FirebaseAuthManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthCoreModule {

    @Binds
    abstract fun bindAuthManager(impl: FirebaseAuthManager): AuthManager
}
