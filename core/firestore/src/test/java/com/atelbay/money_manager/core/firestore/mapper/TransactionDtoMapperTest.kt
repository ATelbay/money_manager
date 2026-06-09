package com.atelbay.money_manager.core.firestore.mapper

import android.util.Base64
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.atelbay.money_manager.core.database.entity.TransactionEntity
import com.atelbay.money_manager.core.firestore.dto.TransactionDto
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TransactionDtoMapperTest {

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

    // 50_000.00 major == 5_000_000 minor units.
    private fun entity() = TransactionEntity(
        id = 1,
        remoteId = "remote-1",
        amount = 5_000_000L,
        type = "expense",
        categoryId = 1,
        accountId = 1,
        note = "Salary",
        date = 1000L,
        createdAt = 2000L,
        updatedAt = 3000L,
        uniqueHash = "hash-123",
    )

    @Test
    fun `toDto dual-writes encrypted legacy amount and amountMinor`() {
        val dto = entity().toDto("cat-remote", "acc-remote", holder)
        assertEquals(1, dto.encryptionVersion)
        // Both money fields are written and encrypted (not plaintext).
        assertNotNull(dto.amountMinor)
        assert(dto.amount != "50000.0")
        assert(dto.amountMinor != "5000000")
        assert(dto.note != "Salary")
    }

    @Test
    fun `toEntity round-trips amount via amountMinor`() {
        val dto = entity().toDto("cat-remote", "acc-remote", holder)
        val result = dto.toEntity(localId = 1, categoryLocalId = 1, accountLocalId = 1, fieldCipherHolder = holder)
        assertNotNull(result)
        assertEquals(5_000_000L, result!!.amount)
        assertEquals("Salary", result.note)
        assertEquals("hash-123", result.uniqueHash)
    }

    @Test
    fun `toEntity prefers amountMinor over legacy amount when both present`() {
        // Encrypted doc whose legacy field disagrees with amountMinor → amountMinor wins.
        val cipher = holder.cipher!!
        val dto = TransactionDto(
            remoteId = "remote-1",
            amount = cipher.encryptDouble(999.99),
            amountMinor = cipher.encryptLong(5_000_000L),
            type = "expense",
            categoryRemoteId = "cat",
            accountRemoteId = "acc",
            date = 1000L,
            createdAt = 2000L,
            updatedAt = 3000L,
            encryptionVersion = 1,
        )
        val result = dto.toEntity(0, 1, 1, holder)
        assertEquals(5_000_000L, result!!.amount)
    }

    @Test
    fun `toEntity encrypted doc without amountMinor falls back to legacy amount`() {
        // A doc written by a prior app version: only the legacy encrypted Double is present.
        val cipher = holder.cipher!!
        val dto = TransactionDto(
            remoteId = "remote-1",
            amount = cipher.encryptDouble(12345.67),
            amountMinor = null,
            type = "expense",
            categoryRemoteId = "cat",
            accountRemoteId = "acc",
            date = 1000L,
            createdAt = 2000L,
            updatedAt = 3000L,
            encryptionVersion = 1,
        )
        val result = dto.toEntity(0, 1, 1, holder)
        assertEquals(1_234_567L, result!!.amount)
    }

    @Test
    fun `toEntity plaintext v0 legacy-only doc converts major to minor`() {
        val dto = TransactionDto(
            remoteId = "remote-1",
            amount = "12345.67",
            amountMinor = null,
            type = "income",
            categoryRemoteId = "cat",
            accountRemoteId = "acc",
            note = "test",
            date = 1000L,
            createdAt = 2000L,
            updatedAt = 3000L,
            encryptionVersion = 0,
        )
        val result = dto.toEntity(0, 1, 1, holder)
        assertNotNull(result)
        assertEquals(1_234_567L, result!!.amount)
        assertEquals("test", result.note)
    }

    @Test
    fun `toEntity plaintext v0 prefers amountMinor when present`() {
        val dto = TransactionDto(
            remoteId = "remote-1",
            amount = "999.99",
            amountMinor = "5000000",
            type = "income",
            categoryRemoteId = "cat",
            accountRemoteId = "acc",
            date = 1000L,
            createdAt = 2000L,
            updatedAt = 3000L,
            encryptionVersion = 0,
        )
        val result = dto.toEntity(0, 1, 1, holder)
        assertEquals(5_000_000L, result!!.amount)
    }

    @Test
    fun `toDto with null cipher writes plaintext major and minor`() {
        val emptyHolder = FieldCipherHolder()
        val dto = entity().toDto("cat-remote", "acc-remote", emptyHolder)
        assertEquals(0, dto.encryptionVersion)
        assertEquals("50000", dto.amount)
        assertEquals("5000000", dto.amountMinor)
        assertEquals("Salary", dto.note)
    }

    @Test
    fun `toEntity with encrypted version but null cipher returns null`() {
        val emptyHolder = FieldCipherHolder()
        val dto = TransactionDto(
            remoteId = "remote-1",
            amount = "SGVsbG8gV29ybGQ=",
            amountMinor = "SGVsbG8gV29ybGQ=",
            type = "expense",
            categoryRemoteId = "cat",
            accountRemoteId = "acc",
            date = 1000L,
            createdAt = 2000L,
            updatedAt = 3000L,
            encryptionVersion = 1,
        )
        assertNull(dto.toEntity(0, 1, 1, emptyHolder))
    }

    @Test
    fun `toEntity with corrupted ciphertext returns null`() {
        val dto = TransactionDto(
            remoteId = "remote-1",
            amount = "not-valid-ciphertext",
            amountMinor = "not-valid-ciphertext",
            type = "expense",
            categoryRemoteId = "cat",
            accountRemoteId = "acc",
            date = 1000L,
            createdAt = 2000L,
            updatedAt = 3000L,
            encryptionVersion = 1,
        )
        assertNull(dto.toEntity(0, 1, 1, holder))
    }
}
