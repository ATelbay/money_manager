package com.atelbay.money_manager.core.database.migration

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.atelbay.money_manager.core.database.MoneyManagerDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Validates every schema migration against the exported JSON schemas.
 *
 * Coverage starts at v3 because schemas were first exported at v3 — MIGRATION_2_3 predates schema
 * export and cannot be validated by [MigrationTestHelper] (no v2 schema to seed from). All later
 * steps (3→4 … 7→8), the full 3→8 chain, and 7→8 data preservation are covered.
 *
 * Run with: ./gradlew :core:database:connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MoneyManagerDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate3To4() = migrateStep(3, 4, MIGRATION_3_4)

    @Test
    @Throws(IOException::class)
    fun migrate4To5() = migrateStep(4, 5, MIGRATION_4_5)

    @Test
    @Throws(IOException::class)
    fun migrate5To6() = migrateStep(5, 6, MIGRATION_5_6)

    @Test
    @Throws(IOException::class)
    fun migrate6To7() = migrateStep(6, 7, MIGRATION_6_7)

    @Test
    @Throws(IOException::class)
    fun migrate7To8() = migrateStep(7, 8, MIGRATION_7_8)

    @Test
    @Throws(IOException::class)
    fun migrate8To9() = migrateStep(8, 9, MIGRATION_8_9)

    @Test
    @Throws(IOException::class)
    fun migrateAll3To9() {
        helper.createDatabase(TEST_DB, 3).close()
        helper.runMigrationsAndValidate(
            TEST_DB,
            9,
            true,
            MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
        ).close()
    }

    /**
     * 8→9 converts every REAL money column to INTEGER minor units via `round(value * 100)`,
     * losslessly, across all 6 money-bearing tables — including child rows whose FK parents are
     * recreated by the migration.
     */
    @Test
    @Throws(IOException::class)
    fun migrate8To9_convertsMoneyToMinorUnits() {
        helper.createDatabase(TEST_DB, 8).apply {
            // Category parent for FK references.
            execSQL(
                "INSERT INTO categories (id, name, icon, color, type, isDefault, updatedAt, isDeleted) " +
                    "VALUES (1, 'Food', 'food', 0, 'expense', 0, 0, 0)",
            )
            // accounts.balance = 1999.99 -> 199999 ; createdAt preserved.
            execSQL(
                "INSERT INTO accounts (id, name, currency, balance, createdAt, updatedAt, isDeleted) " +
                    "VALUES (1, 'Cash', 'KZT', 1999.99, 111, 0, 0)",
            )
            // transactions.amount = 12345.67 -> 1234567
            execSQL(
                "INSERT INTO transactions (id, amount, type, categoryId, accountId, note, date, createdAt, uniqueHash, updatedAt, isDeleted) " +
                    "VALUES (1, 12345.67, 'expense', 1, 1, NULL, 0, 0, 'hash-1', 0, 0)",
            )
            // budgets.monthlyLimit = 50000.50 -> 5000050
            execSQL(
                "INSERT INTO budgets (id, categoryId, monthlyLimit, createdAt, updatedAt, isDeleted) " +
                    "VALUES (1, 1, 50000.50, 0, 0, 0)",
            )
            // debts.totalAmount = 1000.00 -> 100000
            execSQL(
                "INSERT INTO debts (id, contactName, direction, totalAmount, currency, accountId, note, createdAt, updatedAt, isDeleted) " +
                    "VALUES (1, 'Bob', 'lent', 1000.00, 'KZT', 1, NULL, 0, 0, 0)",
            )
            // debt_payments.amount = 250.25 -> 25025 (child of debts + transactions)
            execSQL(
                "INSERT INTO debt_payments (id, debtId, amount, date, note, transactionId, createdAt, updatedAt, isDeleted) " +
                    "VALUES (1, 1, 250.25, 0, NULL, 1, 0, 0, 0)",
            )
            // recurring_transactions.amount = 99.99 -> 9999
            execSQL(
                "INSERT INTO recurring_transactions (id, amount, type, categoryId, accountId, note, frequency, startDate, endDate, dayOfMonth, dayOfWeek, lastGeneratedDate, isActive, createdAt, updatedAt, isDeleted) " +
                    "VALUES (1, 99.99, 'expense', 1, 1, NULL, 'monthly', 0, NULL, 1, NULL, NULL, 1, 0, 0, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)

        assertLong(db, "SELECT amount FROM transactions WHERE id = 1", 1234567L)
        assertLong(db, "SELECT balance FROM accounts WHERE id = 1", 199999L)
        assertLong(db, "SELECT createdAt FROM accounts WHERE id = 1", 111L) // non-money preserved
        assertLong(db, "SELECT monthlyLimit FROM budgets WHERE id = 1", 5000050L)
        assertLong(db, "SELECT totalAmount FROM debts WHERE id = 1", 100000L)
        assertLong(db, "SELECT amount FROM debt_payments WHERE id = 1", 25025L)
        assertLong(db, "SELECT transactionId FROM debt_payments WHERE id = 1", 1L) // FK child preserved
        assertLong(db, "SELECT amount FROM recurring_transactions WHERE id = 1", 9999L)
        db.close()
    }

    /** 8→9 copies `uniqueHash` verbatim so cross-version dedup digests stay valid (research R1). */
    @Test
    @Throws(IOException::class)
    fun migrate8To9_preservesUniqueHash() {
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL(
                "INSERT INTO accounts (id, name, currency, balance, createdAt, updatedAt, isDeleted) " +
                    "VALUES (1, 'Cash', 'KZT', 100.0, 0, 0, 0)",
            )
            execSQL(
                "INSERT INTO categories (id, name, icon, color, type, isDefault, updatedAt, isDeleted) " +
                    "VALUES (1, 'Food', 'food', 0, 'expense', 0, 0, 0)",
            )
            execSQL(
                "INSERT INTO transactions (id, amount, type, categoryId, accountId, note, date, createdAt, uniqueHash, updatedAt, isDeleted) " +
                    "VALUES (1, 50.0, 'expense', 1, 1, NULL, 0, 0, 'stable-digest-xyz', 0, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)
        db.query("SELECT uniqueHash FROM transactions WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals("uniqueHash must be copied verbatim", "stable-digest-xyz", c.getString(0))
        }
        db.close()
    }

    private fun assertLong(db: androidx.sqlite.db.SupportSQLiteDatabase, query: String, expected: Long) {
        db.query(query).use { c ->
            c.moveToFirst()
            assertEquals(query, expected, c.getLong(0))
        }
    }

    /** 7→8 must preserve existing rows and seed the two default debt categories. */
    @Test
    @Throws(IOException::class)
    fun migrate7To8_preservesDataAndSeedsDebtCategories() {
        helper.createDatabase(TEST_DB, 7).apply {
            execSQL(
                "INSERT INTO accounts (id, name, currency, balance, createdAt, updatedAt, isDeleted) " +
                    "VALUES (1, 'Cash', 'KZT', 100.0, 0, 0, 0)",
            )
            execSQL(
                "INSERT INTO transactions (id, amount, type, categoryId, accountId, note, date, createdAt, updatedAt, isDeleted) " +
                    "VALUES (1, 50.0, 'expense', 1, 1, NULL, 0, 0, 0, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_7_8)

        db.query("SELECT COUNT(*) FROM accounts WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals("Pre-existing account must survive 7→8", 1, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM transactions WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals("Pre-existing transaction must survive 7→8", 1, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM debts").use { c ->
            c.moveToFirst()
            assertTrue("debts table must exist after 7→8", c.getInt(0) >= 0)
        }
        db.query("SELECT COUNT(*) FROM categories WHERE name IN ('Долги', 'Возврат долга') AND isDefault = 1").use { c ->
            c.moveToFirst()
            assertEquals("Both default debt categories must be seeded", 2, c.getInt(0))
        }
        db.close()
    }

    private fun migrateStep(from: Int, to: Int, migration: androidx.room.migration.Migration) {
        helper.createDatabase(TEST_DB, from).close()
        helper.runMigrationsAndValidate(TEST_DB, to, true, migration).close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
