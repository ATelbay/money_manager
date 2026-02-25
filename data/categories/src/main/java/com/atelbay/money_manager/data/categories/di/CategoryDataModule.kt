package com.atelbay.money_manager.data.categories.di

import com.atelbay.money_manager.data.categories.repository.CategoryRepositoryImpl
import com.atelbay.money_manager.domain.categories.repository.CategoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CategoryDataModule {
    @Binds
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl,
    ): CategoryRepository
}
