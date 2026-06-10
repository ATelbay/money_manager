package com.atelbay.money_manager.data.transactions.mapper

import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.model.money.majorToMinor
import com.atelbay.money_manager.core.model.money.toMajorDouble
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The entity↔domain money mapping is a pure `Long` minor-unit pass-through. This asserts the
 * amount survives a domain→entity→domain round trip exactly, and that the underlying
 * major→minor→major conversion is lossless under HALF_UP (FR / SC-001).
 */
class TransactionMapperRoundTripTest {

    private fun domain(amount: Long) = Transaction(
        id = 1,
        amount = amount,
        type = TransactionType.EXPENSE,
        categoryId = 1,
        categoryName = "Food",
        categoryIcon = "wallet",
        categoryColor = 0L,
        accountId = 1,
        note = null,
        date = 0,
        createdAt = 0,
    )

    private val category = CategoryEntity(
        id = 1, name = "Food", icon = "wallet", color = 0L, type = "expense", isDefault = false,
    )

    @Test
    fun `amount survives domain to entity to domain round trip`() {
        val amounts = listOf(0L, 10L, 20L, 199_999L, 1_234_567L, 9_999_999_999L)
        for (minor in amounts) {
            val roundTripped = domain(minor).toEntity().toDomain(category)
            assertEquals(minor, roundTripped.amount)
        }
    }

    @Test
    fun `major to minor to major is lossless under HALF_UP`() {
        val majors = listOf(0.0, 0.1, 0.2, 12345.67, 1999.99, 100.0)
        for (major in majors) {
            val minor = majorToMinor(major)
            assertEquals(major, minor.toMajorDouble(), 0.0)
        }
    }
}
