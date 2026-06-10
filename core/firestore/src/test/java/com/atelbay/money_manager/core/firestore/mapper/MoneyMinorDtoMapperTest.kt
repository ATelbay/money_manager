package com.atelbay.money_manager.core.firestore.mapper

import android.util.Base64
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.atelbay.money_manager.core.database.entity.BudgetEntity
import com.atelbay.money_manager.core.database.entity.DebtEntity
import com.atelbay.money_manager.core.database.entity.DebtPaymentEntity
import com.atelbay.money_manager.core.database.entity.RecurringTransactionEntity
import com.atelbay.money_manager.core.firestore.dto.BudgetDto
import com.atelbay.money_manager.core.firestore.dto.DebtDto
import com.atelbay.money_manager.core.firestore.dto.DebtPaymentDto
import com.atelbay.money_manager.core.firestore.dto.RecurringTransactionDto
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Dual-write + `*Minor`-first read + legacy fallback coverage for the budget / debt / debt-payment
 * / recurring DTO mappers (T067/T068). Verifies that an old-shaped doc (no `*Minor`) still reads
 * correctly via the legacy major-unit path.
 */
class MoneyMinorDtoMapperTest {

    private lateinit var holder: FieldCipherHolder

    @Before
    fun setUp() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
        }
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }
        holder = FieldCipherHolder()
        holder.init("test-uid")
    }

    @After
    fun tearDown() {
        holder.clear()
        unmockkStatic(Base64::class)
    }

    // ── Budget ──

    @Test
    fun `budget dual-write and minor-first round-trip`() {
        val entity = BudgetEntity(id = 1, remoteId = "r", categoryId = 1, monthlyLimit = 5_000_050L, createdAt = 0)
        val dto = entity.toDto(holder, "cat-remote")
        assertNotNull(dto.monthlyLimitMinor)
        val result = dto.toEntity(localId = 1, fieldCipherHolder = holder, localCategoryId = 1)
        assertEquals(5_000_050L, result!!.monthlyLimit)
    }

    @Test
    fun `budget legacy encrypted doc without minor falls back`() {
        val cipher = holder.cipher!!
        val dto = BudgetDto(
            remoteId = "r",
            categoryRemoteId = "c",
            monthlyLimit = cipher.encryptDouble(50000.50),
            monthlyLimitMinor = null,
            createdAt = 0,
            encryptionVersion = 1,
        )
        val result = dto.toEntity(localId = 0, fieldCipherHolder = holder, localCategoryId = 1)
        assertEquals(5_000_050L, result!!.monthlyLimit)
    }

    @Test
    fun `budget plaintext v0 legacy-only converts major to minor`() {
        val dto = BudgetDto(remoteId = "r", categoryRemoteId = "c", monthlyLimit = "50000.50", createdAt = 0, encryptionVersion = 0)
        val result = dto.toEntity(localId = 0, fieldCipherHolder = holder, localCategoryId = 1)
        assertEquals(5_000_050L, result!!.monthlyLimit)
    }

    // ── Debt ──

    @Test
    fun `debt dual-write and minor-first round-trip`() {
        val entity = DebtEntity(
            id = 1, remoteId = "r", contactName = "Bob", direction = "LENT",
            totalAmount = 100_000L, currency = "KZT", accountId = 1, note = null, createdAt = 0,
        )
        val dto = entity.toDto(holder, "acc-remote")
        assertNotNull(dto.totalAmountMinor)
        val result = dto.toEntity(localId = 1, fieldCipherHolder = holder, localAccountId = 1)
        assertEquals(100_000L, result!!.totalAmount)
    }

    @Test
    fun `debt legacy encrypted doc without minor falls back`() {
        val cipher = holder.cipher!!
        val dto = DebtDto(
            remoteId = "r", contactName = cipher.encrypt("Bob"), direction = "LENT",
            totalAmount = cipher.encryptDouble(1000.00), totalAmountMinor = null,
            currency = "KZT", accountRemoteId = "a", note = cipher.encrypt(""), createdAt = 0, encryptionVersion = 1,
        )
        val result = dto.toEntity(localId = 0, fieldCipherHolder = holder, localAccountId = 1)
        assertEquals(100_000L, result!!.totalAmount)
    }

    @Test
    fun `debt plaintext v0 legacy-only converts major to minor`() {
        val dto = DebtDto(
            remoteId = "r", contactName = "Bob", direction = "LENT",
            totalAmount = "1000.00", currency = "KZT", accountRemoteId = "a", note = "", createdAt = 0, encryptionVersion = 0,
        )
        val result = dto.toEntity(localId = 0, fieldCipherHolder = holder, localAccountId = 1)
        assertEquals(100_000L, result!!.totalAmount)
    }

    // ── Debt payment ──

    @Test
    fun `debt payment dual-write and minor-first round-trip`() {
        val entity = DebtPaymentEntity(id = 1, remoteId = "r", debtId = 1, amount = 25_025L, date = 0, createdAt = 0)
        val dto = entity.toDto(holder, "debt-remote", "tx-remote")
        assertNotNull(dto.amountMinor)
        val result = dto.toEntity(localId = 1, fieldCipherHolder = holder, localDebtId = 1, localTransactionId = null)
        assertEquals(25_025L, result!!.amount)
    }

    @Test
    fun `debt payment plaintext v0 legacy-only converts major to minor`() {
        val dto = DebtPaymentDto(remoteId = "r", debtRemoteId = "d", amount = "250.25", date = 0, createdAt = 0, encryptionVersion = 0)
        val result = dto.toEntity(localId = 0, fieldCipherHolder = holder, localDebtId = 1, localTransactionId = null)
        assertEquals(25_025L, result!!.amount)
    }

    // ── Recurring ──

    @Test
    fun `recurring dual-write and minor-first round-trip`() {
        val entity = RecurringTransactionEntity(
            id = 1, remoteId = "r", amount = 9_999L, type = "expense", categoryId = 1, accountId = 1,
            note = null, frequency = "MONTHLY", startDate = 0, endDate = null, dayOfMonth = 1,
            dayOfWeek = null, lastGeneratedDate = null, isActive = true, createdAt = 0,
        )
        val dto = entity.toDto("cat-remote", "acc-remote", holder)
        assertNotNull(dto.amountMinor)
        val result = dto.toEntity(localId = 1, categoryLocalId = 1, accountLocalId = 1, fieldCipherHolder = holder)
        assertEquals(9_999L, result!!.amount)
    }

    @Test
    fun `recurring plaintext v0 legacy-only converts major to minor`() {
        val dto = RecurringTransactionDto(
            remoteId = "r", amount = "99.99", type = "expense", categoryRemoteId = "c", accountRemoteId = "a",
            frequency = "MONTHLY", startDate = 0, dayOfMonth = 1, isActive = true, createdAt = 0, encryptionVersion = 0,
        )
        val result = dto.toEntity(localId = 0, categoryLocalId = 1, accountLocalId = 1, fieldCipherHolder = holder)
        assertEquals(9_999L, result!!.amount)
    }
}
