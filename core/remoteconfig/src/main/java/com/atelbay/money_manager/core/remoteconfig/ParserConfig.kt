@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.atelbay.money_manager.core.remoteconfig

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParserConfigList(
    @SerialName("banks") val banks: List<ParserConfig>,
)

@Serializable
data class ParserConfig(
    @SerialName("bank_id") val bankId: String,
    @SerialName("bank_markers") val bankMarkers: List<String>,
    @SerialName("transaction_pattern") val transactionPattern: String,
    @SerialName("date_format") val dateFormat: String,
    @SerialName("operation_type_map") val operationTypeMap: Map<String, String>,
    @SerialName("skip_patterns") val skipPatterns: List<String> = emptyList(),
    @SerialName("join_lines") val joinLines: Boolean = false,
    @SerialName("amount_format") val amountFormat: String = "space_comma",
    @SerialName("use_sign_for_type") val useSignForType: Boolean = false,
)
