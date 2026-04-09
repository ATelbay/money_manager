package com.atelbay.money_manager.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.BudgetDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.DebtDao
import com.atelbay.money_manager.core.database.dao.DebtPaymentDao
import com.atelbay.money_manager.core.database.dao.RegexParserProfileDao
import com.atelbay.money_manager.core.database.dao.RecurringTransactionDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.database.entity.AccountEntity
import com.atelbay.money_manager.core.database.entity.BudgetEntity
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.database.entity.DebtEntity
import com.atelbay.money_manager.core.database.entity.DebtPaymentEntity
import com.atelbay.money_manager.core.database.entity.RegexParserProfileEntity
import com.atelbay.money_manager.core.database.entity.RecurringTransactionEntity
import com.atelbay.money_manager.core.database.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        RecurringTransactionEntity::class,
        BudgetEntity::class,
        RegexParserProfileEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
abstract class MoneyManagerDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun regexParserProfileDao(): RegexParserProfileDao
    abstract fun debtDao(): DebtDao
    abstract fun debtPaymentDao(): DebtPaymentDao
}
