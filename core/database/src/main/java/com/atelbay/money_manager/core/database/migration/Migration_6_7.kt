package com.atelbay.money_manager.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_remoteId ON budgets(remoteId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_remoteId ON recurring_transactions(remoteId)")
    }
}
