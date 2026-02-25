package com.atelbay.money_manager.data.categories.repository

import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.data.categories.mapper.toDomain
import com.atelbay.money_manager.data.categories.mapper.toEntity
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.domain.categories.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {

    override fun observeByType(type: TransactionType): Flow<List<Category>> =
        categoryDao.observeByType(type.value).map { list ->
            list.map { it.toDomain() }
        }

    override fun observeById(id: Long): Flow<Category?> =
        categoryDao.observeById(id).map { it?.toDomain() }

    override suspend fun save(category: Category): Long {
        val entity = category.toEntity()
        return if (entity.id == 0L) {
            categoryDao.insert(entity)
        } else {
            categoryDao.update(entity)
            entity.id
        }
    }

    override suspend fun delete(id: Long) {
        val entity = categoryDao.getById(id) ?: return
        categoryDao.delete(entity)
    }
}
