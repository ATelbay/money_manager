package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.remoteconfig.RegexParserProfile
import com.atelbay.money_manager.core.remoteconfig.RegexParserProfileList
import kotlinx.serialization.json.Json

/**
 * Loads [RegexParserProfile] instances from the real [default_parser_config.json] test resource,
 * so integration tests always use the same configuration as production.
 */
object RegexParserProfileTestFactory {

    private val json = Json { ignoreUnknownKeys = true }

    private val configs: List<RegexParserProfile> by lazy {
        val text = PdfTestHelper.loadResource("default_parser_config.json")
        json.decodeFromString<RegexParserProfileList>(text).banks
    }

    fun getAllConfigs(): List<RegexParserProfile> = configs

    fun getConfig(bankId: String): RegexParserProfile =
        configs.first { it.bankId == bankId }
}
