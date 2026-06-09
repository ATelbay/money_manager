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

    // 150_000.00 major == 15_000_000 minor units.
    private fun entity() = AccountEntity(
        id = 1,
        remoteId = "remote-1",
        name = "Kaspi Gold",
        currency = "KZT",
        balance = 15_000_000L,
        createdAt = 1000L,
        updatedAt = 2000L,
    )

    @Test
    fun `toDto dual-writes encrypted legacy balance and balanceMinor`() {
        val dto = entity().toDto(holder)
        assertEquals(1, dto.encryptionVersion)
        assertNotNull(dto.balanceMinor)
        assert(dto.name != "Kaspi Gold")
        assert(dto.balance != "150000.0")
        assert(dto.balanceMinor != "15000000")
    }

    @Test
    fun `toEntity round-trips balance via balanceMinor`() {
        val dto = entity().toDto(holder)
        val result = dto.toEntity(localId = 1, fieldCipherHolder = holder)
        assertNotNull(result)
        assertEquals("Kaspi Gold", result!!.name)
        assertEquals(15_000_000L, result.balance)
    }

    @Test
    fun `toEntity encrypted doc without balanceMinor falls back to legacy balance`() {
        val cipher = holder.cipher!!
        val dto = AccountDto(
            remoteId = "remote-1",
            name = cipher.encrypt("My Account"),
            currency = "KZT",
            balance = cipher.encryptDouble(99999.0),
            balanceMinor = null,
            createdAt = 1000L,
            updatedAt = 2000L,
            encryptionVersion = 1,
        )
        val result = dto.toEntity(localId = 0, fieldCipherHolder = holder)
        assertEquals(9_999_900L, result!!.balance)
    }

    @Test
    fun `toEntity plaintext v0 legacy-only doc converts major to minor`() {
        val dto = AccountDto(
            remoteId = "remote-1",
            name = "My Account",
            currency = "KZT",
            balance = "99999.0",
            balanceMinor = null,
            createdAt = 1000L,
            updatedAt = 2000L,
            encryptionVersion = 0,
        )
        val result = dto.toEntity(localId = 0, fieldCipherHolder = holder)
        assertNotNull(result)
        assertEquals(9_999_900L, result!!.balance)
        assertEquals("My Account", result.name)
    }

    @Test
    fun `toDto with null cipher writes plaintext major and minor`() {
        val emptyHolder = FieldCipherHolder()
        val entity = AccountEntity(
            id = 1,
            remoteId = "remote-1",
            name = "Test",
            currency = "USD",
            balance = 10_000L,
            createdAt = 1000L,
        )
        val dto = entity.toDto(emptyHolder)
        assertEquals(0, dto.encryptionVersion)
        assertEquals("Test", dto.name)
        assertEquals("100", dto.balance)
        assertEquals("10000", dto.balanceMinor)
    }

    @Test
    fun `toEntity with encrypted version but null cipher returns null`() {
        val emptyHolder = FieldCipherHolder()
        val dto = AccountDto(
            remoteId = "remote-1",
            name = "SGVsbG8gV29ybGQ=",
            currency = "KZT",
            balance = "SGVsbG8gV29ybGQ=",
            balanceMinor = "SGVsbG8gV29ybGQ=",
            createdAt = 1000L,
            updatedAt = 2000L,
            encryptionVersion = 1,
        )
        assertNull(dto.toEntity(localId = 0, fieldCipherHolder = emptyHolder))
    }

    @Test
    fun `toEntity with corrupted ciphertext returns null`() {
        val dto = AccountDto(
            remoteId = "remote-1",
            name = "corrupted-name",
            currency = "KZT",
            balance = "corrupted-balance",
            balanceMinor = "corrupted-balance",
            createdAt = 1000L,
            updatedAt = 2000L,
            encryptionVersion = 1,
        )
        assertNull(dto.toEntity(localId = 0, fieldCipherHolder = holder))
    }
}
