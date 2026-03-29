package com.atelbay.money_manager.core.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ParserConfigSyncTest {

    @Test
    fun `production and test default_parser_config json must be identical`() {
        val testConfig = javaClass.classLoader!!
            .getResource("default_parser_config.json")!!
            .readText()

        val productionConfig = File("../remoteconfig/src/main/res/raw/default_parser_config.json")
            .readText()

        assertEquals(
            "Production and test default_parser_config.json have drifted — sync them",
            productionConfig,
            testConfig
        )
    }
}
