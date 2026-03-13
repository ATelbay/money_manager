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

    @Test
    fun `toDto encrypts amount, note, uniqueHash and sets encryptionVersion=1`() {
        val entity = TransactionEntity(
            id = 1,
            remoteId = "remote-1",
            amount = 50000.0,
            type = "expense",
            categoryId = 1,
            accountId = 1,
            note = "Salary",
            date = 1000L,
            createdAt = 2000L,
            updatedAt = 3000L,
            uniqueHash = "hash-123",
        )
        val dto = entity.toDto("cat-remote", "acc-remote", holder)
        assertEquals(1, dto.encryptionVersion)
        assertEquals("expense", dto.type)
        assertEquals("cat-remote", dto.categoryRemoteId)
        assertEquals("acc-remote", dto.accountRemoteId)
        // Encrypted fields should not equal plaintext
        assert(dto.amount != "50000.0")
        assert(dto.note != "Salary")
        assert(dto.uniqueHash != "hash-123")
    }

    @Test
    fun `toEntity decrypts back to original values (round-trip)`() {
        val entity = TransactionEntity(
            id = 1,
            remoteId = "remote-1",
            amount = 50000.0,
            type = "expense",
            categoryId = 1,
            accountId = 1,
            note = "Salary",
            date = 1000L,
            createdAt = 2000L,
            updatedAt = 3000L,
            uniqueHash = "hash-123",
        )
        val dto = entity.toDto("cat-remote", "acc-remote", holder)
        val result = dto.toEntity(
            localId = 1,
            categoryLocalId = 1,
            accountLocalId = 1,
            fieldCipherHolder = holder,
        )
        assertNotNull(result)
        assertEquals(50000.0, result!!.amount, 0.0)
        assertEquals("Salary", result.note)
        assertEquals("hash-123", result.uniqueHash)
    }

    @Test
    fun `toEntity with encryptionVersion=0 reads plaintext Double amount`() {
        val dto = TransactionDto(
            remoteId = "remote-1",
            amount = "12345.67",
            type = "income",
            categoryRemoteId = "cat",
            accountRemoteId = "acc",
            note = "test",
            date = 1000L,
            createdAt = 2000L,
            updatedAt = 3000L,
            encryptionVersion = 0,
        )
        val result = dto.toEntity(
            localId = 0,
            categoryLocalId = 1,
            accountLocalId = 1,
            fieldCipherHolder = holder,
        )
        assertNotNull(result)
        assertEquals(12345.67, result!!.amount, 0.0)
        assertEquals("test", result.note)
    }

    @Test
    fun `toEntity with null note and uniqueHash preserves nulls`() {
        val entity = TransactionEntity(
            id = 1,
            remoteId = "remote-1",
            amount = 100.0,
            type = "expense",
            categoryId = 1,
            accountId = 1,
            note = null,
            date = 1000L,
            createdAt = 2000L,
            updatedAt = 3000L,
            uniqueHash = null,
        )
        val dto = entity.toDto("cat", "acc", holder)
        assertNull(dto.note)
        assertNull(dto.uniqueHash)

        val result = dto.toEntity(1, 1, 1, holder)
        assertNotNull(result)
        assertNull(result!!.note)
        assertNull(result.uniqueHash)
    }

    @Test
    fun `toEntity with encrypted version but null cipher returns null`() {
        val emptyHolder = FieldCipherHolder()
        val dto = TransactionDto(
            remoteId = "remote-1",
            amount = "SGVsbG8gV29ybGQ=",
            type = "expense",
            categoryRemoteId = "cat",
            accountRemoteId = "acc",
            note = "SGVsbG8gV29ybGQ=",
            date = 1000L,
            createdAt = 2000L,
            updatedAt = 3000L,
            encryptionVersion = 1,
        )
        val result = dto.toEntity(0, 1, 1, emptyHolder)
        assertNull(result)
    }

    @Test
    fun `toDto with null cipher skips encryption`() {
        val emptyHolder = FieldCipherHolder()
        val entity = TransactionEntity(
            id = 1,
            remoteId = "remote-1",
            amount = 50000.0,
            type = "expense",
            categoryId = 1,
            accountId = 1,
            note = "Salary",
            date = 1000L,
            createdAt = 2000L,
            updatedAt = 3000L,
            uniqueHash = "hash-123",
        )
        val dto = entity.toDto("cat-remote", "acc-remote", emptyHolder)
        assertEquals(0, dto.encryptionVersion)
        assertEquals("50000.0", dto.amount)
        assertEquals("Salary", dto.note)
        assertEquals("hash-123", dto.uniqueHash)
    }

    @Test
    fun `toEntity with corrupted ciphertext returns null`() {
        val dto = TransactionDto(
            remoteId = "remote-1",
            amount = "not-valid-ciphertext",
            type = "expense",
            categoryRemoteId = "cat",
            accountRemoteId = "acc",
            date = 1000L,
            createdAt = 2000L,
            updatedAt = 3000L,
            encryptionVersion = 1,
        )
        val result = dto.toEntity(0, 1, 1, holder)
        assertNull(result)
    }
}
