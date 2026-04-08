package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.remoteconfig.RegexParserProfile
import javax.inject.Inject

class BankDetector @Inject constructor() {

    fun detect(text: String, configs: List<RegexParserProfile>): RegexParserProfile? {
        return detectAll(text, configs).firstOrNull()
    }

    fun detectAll(text: String, configs: List<RegexParserProfile>): List<RegexParserProfile> {
        return configs.filter { config ->
            config.bankMarkers.any { marker -> text.contains(marker, ignoreCase = true) }
        }
    }
}
