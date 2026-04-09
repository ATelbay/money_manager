package com.atelbay.money_manager.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS debts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                contactName TEXT NOT NULL,
                direction TEXT NOT NULL,
                totalAmount REAL NOT NULL,
                currency TEXT NOT NULL,
                accountId INTEGER NOT NULL,
                note TEXT,
                createdAt INTEGER NOT NULL,
                remoteId TEXT,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                isDeleted INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_debts_accountId ON debts(accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_debts_remoteId ON debts(remoteId)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS debt_payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                debtId INTEGER NOT NULL,
                amount REAL NOT NULL,
                date INTEGER NOT NULL,
                note TEXT,
                transactionId INTEGER,
                createdAt INTEGER NOT NULL,
                remoteId TEXT,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                isDeleted INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(debtId) REFERENCES debts(id) ON DELETE CASCADE,
                FOREIGN KEY(transactionId) REFERENCES transactions(id) ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_payments_debtId ON debt_payments(debtId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_payments_remoteId ON debt_payments(remoteId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_payments_transactionId ON debt_payments(transactionId)")

        // Default categories for debts feature
        db.execSQL(
            """
            INSERT INTO categories (name, icon, color, type, isDefault, updatedAt, isDeleted)
            VALUES ('Долги', 'money_off', ${0xFFEF4444}, 'expense', 1, 0, 0)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO categories (name, icon, color, type, isDefault, updatedAt, isDeleted)
            VALUES ('Возврат долга', 'payments', ${0xFF22C55E}, 'income', 1, 0, 0)
            """.trimIndent(),
        )
    }
}
