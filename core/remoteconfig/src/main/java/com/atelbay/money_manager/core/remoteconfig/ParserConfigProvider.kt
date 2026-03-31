package com.atelbay.money_manager.core.remoteconfig

import com.atelbay.money_manager.core.model.TableParserConfig

interface ParserConfigProvider {
    suspend fun getConfigs(): List<ParserConfig>
    suspend fun getTableConfigs(): List<TableParserConfig>
    suspend fun getConfigForBank(bankId: String): ParserConfig?
    fun isAiFullParseEnabled(): Boolean
    fun getGeminiModelName(): String
}
