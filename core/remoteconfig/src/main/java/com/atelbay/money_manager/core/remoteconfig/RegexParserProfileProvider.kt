package com.atelbay.money_manager.core.remoteconfig

import com.atelbay.money_manager.core.model.TableParserProfile

interface RegexParserProfileProvider {
    suspend fun getConfigs(): List<RegexParserProfile>
    suspend fun getTableConfigs(): List<TableParserProfile>
    suspend fun getConfigForBank(bankId: String): RegexParserProfile?
    fun isAiFullParseEnabled(): Boolean
    fun getGeminiModelName(): String
}
