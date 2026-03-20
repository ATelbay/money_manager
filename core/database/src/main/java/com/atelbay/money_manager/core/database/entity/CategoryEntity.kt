package com.atelbay.money_manager.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index("type", "isDeleted"),
        Index("remoteId"),
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long,
    val type: String,
    val isDefault: Boolean,
    val remoteId: String? = null,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
)
