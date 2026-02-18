package com.atelbay.money_manager.core.remoteconfig

interface ParserConfigProvider {
    suspend fun getConfigs(): List<ParserConfig>
    suspend fun getConfigForBank(bankId: String): ParserConfig?
}
