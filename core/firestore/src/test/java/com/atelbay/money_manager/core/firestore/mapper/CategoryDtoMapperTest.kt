package com.atelbay.money_manager.core.firestore.mapper

import android.util.Base64
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.firestore.dto.CategoryDto
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CategoryDtoMapperTest {

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
    fun `toDto encrypts name, icon, color and sets encryptionVersion=1`() {
        val entity = CategoryEntity(
            id = 1,
            remoteId = "remote-1",
            name = "Groceries",
            icon = "shopping_cart",
            color = 0xFF4CAF50L,
            type = "expense",
            isDefault = false,
            updatedAt = 1000L,
        )
        val dto = entity.toDto(holder)
        assertEquals(1, dto.encryptionVersion)
        assertEquals("expense", dto.type)
        assert(dto.name != "Groceries")
        assert(dto.icon != "shopping_cart")
        assert(dto.color != "4283215696")
    }

    @Test
    fun `toEntity decrypts back to original values (round-trip)`() {
        val entity = CategoryEntity(
            id = 1,
            remoteId = "remote-1",
            name = "Groceries",
            icon = "shopping_cart",
            color = 0xFF4CAF50L,
            type = "expense",
            isDefault = false,
            updatedAt = 1000L,
        )
        val dto = entity.toDto(holder)
        val result = dto.toEntity(localId = 1, fieldCipherHolder = holder)
        assertNotNull(result)
        assertEquals("Groceries", result!!.name)
        assertEquals("shopping_cart", result.icon)
        assertEquals(0xFF4CAF50L, result.color)
    }

    @Test
    fun `toEntity with encryptionVersion=0 reads plaintext Long color`() {
        val dto = CategoryDto(
            remoteId = "remote-1",
            name = "Food",
            icon = "restaurant",
            color = "4283215696",
            type = "expense",
            updatedAt = 1000L,
            encryptionVersion = 0,
        )
        val result = dto.toEntity(localId = 0, fieldCipherHolder = holder)
        assertNotNull(result)
        assertEquals(4283215696L, result!!.color)
        assertEquals("Food", result.name)
        assertEquals("restaurant", result.icon)
    }

    @Test
    fun `toDto with null cipher skips encryption`() {
        val emptyHolder = FieldCipherHolder()
        val entity = CategoryEntity(
            id = 1,
            remoteId = "remote-1",
            name = "Test",
            icon = "test_icon",
            color = 123L,
            type = "income",
            isDefault = false,
            updatedAt = 1000L,
        )
        val dto = entity.toDto(emptyHolder)
        assertEquals(0, dto.encryptionVersion)
        assertEquals("Test", dto.name)
        assertEquals("test_icon", dto.icon)
        assertEquals("123", dto.color)
    }

    @Test
    fun `toEntity with encrypted version but null cipher returns null`() {
        val emptyHolder = FieldCipherHolder()
        val dto = CategoryDto(
            remoteId = "remote-1",
            name = "SGVsbG8gV29ybGQ=",
            icon = "SGVsbG8gV29ybGQ=",
            color = "SGVsbG8gV29ybGQ=",
            type = "expense",
            updatedAt = 1000L,
            encryptionVersion = 1,
        )
        val result = dto.toEntity(localId = 0, fieldCipherHolder = emptyHolder)
        assertNull(result)
    }

    @Test
    fun `toEntity with corrupted ciphertext returns null`() {
        val dto = CategoryDto(
            remoteId = "remote-1",
            name = "corrupted",
            icon = "corrupted",
            color = "corrupted",
            type = "expense",
            updatedAt = 1000L,
            encryptionVersion = 1,
        )
        val result = dto.toEntity(localId = 0, fieldCipherHolder = holder)
        assertNull(result)
    }
}
