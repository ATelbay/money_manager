package com.atelbay.money_manager.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS parser_configs (
                id TEXT NOT NULL PRIMARY KEY,
                bankId TEXT NOT NULL,
                configType TEXT NOT NULL,
                configJson TEXT NOT NULL,
                version INTEGER NOT NULL,
                status TEXT NOT NULL,
                source TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_parser_configs_bankId_configType ON parser_configs(bankId, configType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_parser_configs_status ON parser_configs(status)")
    }
}
