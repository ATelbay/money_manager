package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.remoteconfig.RegexParserProfile
import com.atelbay.money_manager.core.remoteconfig.RegexParserProfileList
import kotlinx.serialization.json.Json

/**
 * Loads reference [RegexParserProfile] instances for the *BankIntegrationTest suites from the
 * test-only resource [test_bank_configs.json].
 *
 * These are TEST FIXTURES, not production data. The app ships an intentionally empty bundled
 * `default_parser_config.json` and resolves real parser configs from Firestore at runtime — it is
 * never hard-wired to a bundled set of banks. This file exists solely so the integration tests can
 * exercise the parser against the checked-in sample PDFs offline; it is not packaged into the APK.
 */
object RegexParserProfileTestFactory {

    private val json = Json { ignoreUnknownKeys = true }

    private val configs: List<RegexParserProfile> by lazy {
        val text = PdfTestHelper.loadResource("test_bank_configs.json")
        json.decodeFromString<RegexParserProfileList>(text).banks
    }

    fun getAllConfigs(): List<RegexParserProfile> = configs

    fun getConfig(bankId: String): RegexParserProfile =
        configs.first { it.bankId == bankId }
}
