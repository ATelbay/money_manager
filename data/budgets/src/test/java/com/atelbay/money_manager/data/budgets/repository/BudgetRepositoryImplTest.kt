package com.atelbay.money_manager.data.budgets.repository

import com.atelbay.money_manager.core.database.dao.BudgetDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.entity.BudgetEntity
import com.atelbay.money_manager.core.model.Budget
import com.atelbay.money_manager.data.sync.SyncManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BudgetRepositoryImplTest {

    private val budgetDao = mockk<BudgetDao>(relaxed = true)
    private val categoryDao = mockk<CategoryDao>(relaxed = true)
    private val syncManager = mockk<SyncManager>(relaxed = true)
    private val repository = BudgetRepositoryImpl(budgetDao, categoryDao, syncManager)

    @Test
    fun `creating a budget revives a soft-deleted row for the same category`() = runTest {
        val softDeleted = BudgetEntity(
            id = 7,
            categoryId = 3,
            monthlyLimit = 100_00L,
            createdAt = 111L,
            remoteId = "remote-1",
            updatedAt = 50L,
            isDeleted = true,
        )
        coEvery { budgetDao.getByCategoryIdAnyState(3) } returns softDeleted
        val updated = slot<BudgetEntity>()
        coEvery { budgetDao.update(capture(updated)) } returns Unit

        val savedId = repository.save(newBudget(categoryId = 3, monthlyLimit = 200_00L))

        assertEquals(7, savedId)
        assertEquals(7, updated.captured.id)
        assertEquals("remote-1", updated.captured.remoteId) // preserved → no orphaned Firestore doc
        assertEquals(200_00L, updated.captured.monthlyLimit)
        assertFalse(updated.captured.isDeleted)
        coVerify(exactly = 0) { budgetDao.insert(any()) }
    }

    @Test
    fun `creating a budget inserts when no row exists for the category`() = runTest {
        coEvery { budgetDao.getByCategoryIdAnyState(3) } returns null
        coEvery { budgetDao.insert(any()) } returns 42L

        val savedId = repository.save(newBudget(categoryId = 3, monthlyLimit = 200_00L))

        assertEquals(42L, savedId)
        coVerify(exactly = 0) { budgetDao.update(any()) }
    }

    private fun newBudget(categoryId: Long, monthlyLimit: Long) = Budget(
        id = 0,
        categoryId = categoryId,
        categoryName = "",
        categoryIcon = "",
        categoryColor = 0L,
        monthlyLimit = monthlyLimit,
        spent = 0L,
        remaining = monthlyLimit,
        percentage = 0f,
    )
}
