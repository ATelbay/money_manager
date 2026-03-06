package com.atelbay.money_manager.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE accounts ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE accounts ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE accounts ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE categories ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE categories ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
    }
}
