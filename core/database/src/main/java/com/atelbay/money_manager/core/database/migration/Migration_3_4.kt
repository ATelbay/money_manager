package com.atelbay.money_manager.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_isDeleted ON accounts (isDeleted)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_remoteId ON accounts (remoteId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_type_isDeleted ON categories (type, isDeleted)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_remoteId ON categories (remoteId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId_type_date ON transactions (categoryId, type, date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isDeleted ON transactions (isDeleted)")
    }
}
