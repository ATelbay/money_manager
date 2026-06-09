package com.atelbay.money_manager.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Money columns REAL → INTEGER (minor units, scale 2).
 *
 * SQLite has no `ALTER COLUMN`, so each money-bearing table is rebuilt with the SQLite-recommended
 * create-new → copy → drop-old → rename procedure, recreating every index and foreign key. The
 * money column is converted in flight with `CAST(ROUND(<col> * 100.0) AS INTEGER)` — SQLite's
 * `ROUND` rounds halves away from zero, which matches `RoundingMode.HALF_UP` used by the Kotlin
 * `majorToMinor` helper (amounts are stored as positive magnitudes; `balance` may be negative but
 * away-from-zero == HALF_UP there too).
 *
 * `uniqueHash` is copied verbatim (NOT recomputed) so cross-version dedup digests stay valid — see
 * research R1: the hash reconstructs the legacy `amount/100.0` string internally. All sync columns
 * (`remoteId`, `updatedAt`, `isDeleted`) are preserved unchanged so sync state is not disturbed.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No-op inside Room's migration transaction, but documents intent; the framework runs
        // onUpgrade with FK enforcement disabled, so dropping referenced parent tables below does
        // not fire cascade/set-null actions.
        db.execSQL("PRAGMA foreign_keys = OFF")

        // ── transactions ──
        db.execSQL(
            """
            CREATE TABLE `transactions_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `amount` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `accountId` INTEGER NOT NULL,
                `note` TEXT,
                `date` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `uniqueHash` TEXT,
                `remoteId` TEXT,
                `updatedAt` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `transactions_new` (id, amount, type, categoryId, accountId, note, date, createdAt, uniqueHash, remoteId, updatedAt, isDeleted)
            SELECT id, CAST(ROUND(amount * 100.0) AS INTEGER), type, categoryId, accountId, note, date, createdAt, uniqueHash, remoteId, updatedAt, isDeleted
            FROM `transactions`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `transactions`")
        db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_uniqueHash` ON `transactions` (`uniqueHash`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId_type_date` ON `transactions` (`categoryId`, `type`, `date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_isDeleted` ON `transactions` (`isDeleted`)")

        // ── accounts ──
        db.execSQL(
            """
            CREATE TABLE `accounts_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `currency` TEXT NOT NULL,
                `balance` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `remoteId` TEXT,
                `updatedAt` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `accounts_new` (id, name, currency, balance, createdAt, remoteId, updatedAt, isDeleted)
            SELECT id, name, currency, CAST(ROUND(balance * 100.0) AS INTEGER), createdAt, remoteId, updatedAt, isDeleted
            FROM `accounts`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `accounts`")
        db.execSQL("ALTER TABLE `accounts_new` RENAME TO `accounts`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_isDeleted` ON `accounts` (`isDeleted`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_remoteId` ON `accounts` (`remoteId`)")

        // ── budgets ──
        db.execSQL(
            """
            CREATE TABLE `budgets_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `monthlyLimit` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `remoteId` TEXT,
                `updatedAt` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `budgets_new` (id, categoryId, monthlyLimit, createdAt, remoteId, updatedAt, isDeleted)
            SELECT id, categoryId, CAST(ROUND(monthlyLimit * 100.0) AS INTEGER), createdAt, remoteId, updatedAt, isDeleted
            FROM `budgets`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `budgets`")
        db.execSQL("ALTER TABLE `budgets_new` RENAME TO `budgets`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_remoteId` ON `budgets` (`remoteId`)")

        // ── debts ──
        db.execSQL(
            """
            CREATE TABLE `debts_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `contactName` TEXT NOT NULL,
                `direction` TEXT NOT NULL,
                `totalAmount` INTEGER NOT NULL,
                `currency` TEXT NOT NULL,
                `accountId` INTEGER NOT NULL,
                `note` TEXT,
                `createdAt` INTEGER NOT NULL,
                `remoteId` TEXT,
                `updatedAt` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `debts_new` (id, contactName, direction, totalAmount, currency, accountId, note, createdAt, remoteId, updatedAt, isDeleted)
            SELECT id, contactName, direction, CAST(ROUND(totalAmount * 100.0) AS INTEGER), currency, accountId, note, createdAt, remoteId, updatedAt, isDeleted
            FROM `debts`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `debts`")
        db.execSQL("ALTER TABLE `debts_new` RENAME TO `debts`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_accountId` ON `debts` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_remoteId` ON `debts` (`remoteId`)")

        // ── debt_payments ──
        db.execSQL(
            """
            CREATE TABLE `debt_payments_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `debtId` INTEGER NOT NULL,
                `amount` INTEGER NOT NULL,
                `date` INTEGER NOT NULL,
                `note` TEXT,
                `transactionId` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `remoteId` TEXT,
                `updatedAt` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                FOREIGN KEY(`debtId`) REFERENCES `debts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `debt_payments_new` (id, debtId, amount, date, note, transactionId, createdAt, remoteId, updatedAt, isDeleted)
            SELECT id, debtId, CAST(ROUND(amount * 100.0) AS INTEGER), date, note, transactionId, createdAt, remoteId, updatedAt, isDeleted
            FROM `debt_payments`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `debt_payments`")
        db.execSQL("ALTER TABLE `debt_payments_new` RENAME TO `debt_payments`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_debtId` ON `debt_payments` (`debtId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_remoteId` ON `debt_payments` (`remoteId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_transactionId` ON `debt_payments` (`transactionId`)")

        // ── recurring_transactions ──
        db.execSQL(
            """
            CREATE TABLE `recurring_transactions_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `amount` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `accountId` INTEGER NOT NULL,
                `note` TEXT,
                `frequency` TEXT NOT NULL,
                `startDate` INTEGER NOT NULL,
                `endDate` INTEGER,
                `dayOfMonth` INTEGER,
                `dayOfWeek` INTEGER,
                `lastGeneratedDate` INTEGER,
                `isActive` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `remoteId` TEXT,
                `updatedAt` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `recurring_transactions_new` (id, amount, type, categoryId, accountId, note, frequency, startDate, endDate, dayOfMonth, dayOfWeek, lastGeneratedDate, isActive, createdAt, remoteId, updatedAt, isDeleted)
            SELECT id, CAST(ROUND(amount * 100.0) AS INTEGER), type, categoryId, accountId, note, frequency, startDate, endDate, dayOfMonth, dayOfWeek, lastGeneratedDate, isActive, createdAt, remoteId, updatedAt, isDeleted
            FROM `recurring_transactions`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `recurring_transactions`")
        db.execSQL("ALTER TABLE `recurring_transactions_new` RENAME TO `recurring_transactions`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_accountId` ON `recurring_transactions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_categoryId` ON `recurring_transactions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_remoteId` ON `recurring_transactions` (`remoteId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_isActive_isDeleted` ON `recurring_transactions` (`isActive`, `isDeleted`)")
    }
}
