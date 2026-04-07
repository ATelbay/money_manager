package com.atelbay.money_manager.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parser_configs",
    indices = [
        Index("bankId", "configType"),
        Index("status"),
    ],
)
data class RegexParserProfileEntity(
    @PrimaryKey val id: String,
    val bankId: String,
    val configType: String,
    val configJson: String,
    val version: Long,
    val status: String,
    val source: String,
    val updatedAt: Long,
) {
    companion object {
        const val CONFIG_TYPE_REGEX = "regex"
        const val CONFIG_TYPE_TABLE = "table"
        const val STATUS_ACTIVE = "active"
        const val SOURCE_SEED = "seed"
        const val SOURCE_AI_CACHED = "ai_cached"
    }
}
