package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import javax.inject.Inject

class BankDetector @Inject constructor() {

    fun detect(text: String, configs: List<ParserConfig>): ParserConfig? {
        return detectAll(text, configs).firstOrNull()
    }

    fun detectAll(text: String, configs: List<ParserConfig>): List<ParserConfig> {
        return configs.filter { config ->
            config.bankMarkers.any { marker -> text.contains(marker, ignoreCase = true) }
        }
    }
}
