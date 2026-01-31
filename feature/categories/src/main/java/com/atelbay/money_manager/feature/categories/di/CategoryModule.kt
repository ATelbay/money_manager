package com.atelbay.money_manager.feature.categories.di

import com.atelbay.money_manager.feature.categories.data.repository.CategoryRepositoryImpl
import com.atelbay.money_manager.feature.categories.domain.repository.CategoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CategoryModule {

    @Binds
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl,
    ): CategoryRepository
}
