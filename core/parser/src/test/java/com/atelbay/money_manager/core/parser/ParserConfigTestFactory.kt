package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import com.atelbay.money_manager.core.remoteconfig.ParserConfigList
import kotlinx.serialization.json.Json

/**
 * Loads [ParserConfig] instances from the real [default_parser_config.json] test resource,
 * so integration tests always use the same configuration as production.
 */
object ParserConfigTestFactory {

    private val json = Json { ignoreUnknownKeys = true }

    private val configs: List<ParserConfig> by lazy {
        val text = PdfTestHelper.loadResource("default_parser_config.json")
        json.decodeFromString<ParserConfigList>(text).banks
    }

    fun getAllConfigs(): List<ParserConfig> = configs

    fun getConfig(bankId: String): ParserConfig =
        configs.first { it.bankId == bankId }
}
