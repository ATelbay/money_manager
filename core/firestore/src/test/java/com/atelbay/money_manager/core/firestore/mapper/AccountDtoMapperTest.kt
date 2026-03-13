package com.atelbay.money_manager.core.firestore.mapper

import android.util.Base64
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.atelbay.money_manager.core.database.entity.AccountEntity
import com.atelbay.money_manager.core.firestore.dto.AccountDto
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AccountDtoMapperTest {

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
    fun `toDto encrypts name and balance and sets encryptionVersion=1`() {
        val entity = AccountEntity(
            id = 1,
            remoteId = "remote-1",
            name = "Kaspi Gold",
            currency = "KZT",
            balance = 150000.0,
            createdAt = 1000L,
            updatedAt = 2000L,
        )
        val dto = entity.toDto(holder)
        assertEquals(1, dto.encryptionVersion)
        assertEquals("KZT", dto.currency)
        assert(dto.name != "Kaspi Gold")
        assert(dto.balance != "150000.0")
    }

    @Test
    fun `toEntity decrypts back to original values (round-trip)`() {
        val entity = AccountEntity(
            id = 1,
            remoteId = "remote-1",
            name = "Kaspi Gold",
            currency = "KZT",
            balance = 150000.0,
            createdAt = 1000L,
            updatedAt = 2000L,
        )
        val dto = entity.toDto(holder)
        val result = dto.toEntity(localId = 1, fieldCipherHolder = holder)
        assertNotNull(result)
        assertEquals("Kaspi Gold", result!!.name)
        assertEquals(150000.0, result.balance, 0.0)
        assertEquals("KZT", result.currency)
    }

    @Test
    fun `toEntity with encryptionVersion=0 reads plaintext Double balance`() {
        val dto = AccountDto(
            remoteId = "remote-1",
            name = "My Account",
            currency = "KZT",
            balance = "99999.0",
            createdAt = 1000L,
            updatedAt = 2000L,
            encryptionVersion = 0,
        )
        val result = dto.toEntity(localId = 0, fieldCipherHolder = holder)
        assertNotNull(result)
        assertEquals(99999.0, result!!.balance, 0.0)
        assertEquals("My Account", result.name)
    }

    @Test
    fun `toDto with null cipher skips encryption`() {
        val emptyHolder = FieldCipherHolder()
        val entity = AccountEntity(
            id = 1,
            remoteId = "remote-1",
            name = "Test",
            currency = "USD",
            balance = 100.0,
            createdAt = 1000L,
        )
        val dto = entity.toDto(emptyHolder)
        assertEquals(0, dto.encryptionVersion)
        assertEquals("Test", dto.name)
        assertEquals("100.0", dto.balance)
    }

    @Test
    fun `toEntity with encrypted version but null cipher returns null`() {
        val emptyHolder = FieldCipherHolder()
        val dto = AccountDto(
            remoteId = "remote-1",
            name = "SGVsbG8gV29ybGQ=",
            currency = "KZT",
            balance = "SGVsbG8gV29ybGQ=",
            createdAt = 1000L,
            updatedAt = 2000L,
            encryptionVersion = 1,
        )
        val result = dto.toEntity(localId = 0, fieldCipherHolder = emptyHolder)
        assertNull(result)
    }

    @Test
    fun `toEntity with corrupted ciphertext returns null`() {
        val dto = AccountDto(
            remoteId = "remote-1",
            name = "corrupted-name",
            currency = "KZT",
            balance = "corrupted-balance",
            createdAt = 1000L,
            updatedAt = 2000L,
            encryptionVersion = 1,
        )
        val result = dto.toEntity(localId = 0, fieldCipherHolder = holder)
        assertNull(result)
    }
}
