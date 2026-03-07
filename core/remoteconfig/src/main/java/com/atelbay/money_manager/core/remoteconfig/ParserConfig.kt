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
    /** Maps operation name → "income"|"expense". Only consulted when both [useSignForType] and [negativeSignMeansExpense] are false. */
    @SerialName("operation_type_map") val operationTypeMap: Map<String, String>,
    @SerialName("skip_patterns") val skipPatterns: List<String> = emptyList(),
    @SerialName("join_lines") val joinLines: Boolean = false,
    @SerialName("amount_format") val amountFormat: String = "space_comma",
    @SerialName("use_sign_for_type") val useSignForType: Boolean = false,
    /** When true: captured sign "-" → EXPENSE, anything else (empty or "+") → INCOME. */
    @SerialName("negative_sign_means_expense") val negativeSignMeansExpense: Boolean = false,
    /** When true: regex uses named groups (date, sign, amount, operation, details) instead of positional. */
    @SerialName("use_named_groups") val useNamedGroups: Boolean = false,
    /** When true: after parsing, keep only the transaction with the max amount per (date, details) group. */
    @SerialName("deduplicate_max_amount") val deduplicateMaxAmount: Boolean = false,
)
