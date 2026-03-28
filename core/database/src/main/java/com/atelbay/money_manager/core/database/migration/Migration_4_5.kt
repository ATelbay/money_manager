package com.atelbay.money_manager.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recurring_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                categoryId INTEGER NOT NULL,
                accountId INTEGER NOT NULL,
                note TEXT,
                frequency TEXT NOT NULL,
                startDate INTEGER NOT NULL,
                endDate INTEGER,
                dayOfMonth INTEGER,
                dayOfWeek INTEGER,
                lastGeneratedDate INTEGER,
                isActive INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL,
                remoteId TEXT,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                isDeleted INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                categoryId INTEGER NOT NULL,
                monthlyLimit REAL NOT NULL,
                createdAt INTEGER NOT NULL,
                remoteId TEXT,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                isDeleted INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_recurring_account ON recurring_transactions(accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_recurring_category ON recurring_transactions(categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_recurring_active ON recurring_transactions(isActive, isDeleted)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_budget_category ON budgets(categoryId)")
    }
}
