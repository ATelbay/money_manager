package com.atelbay.money_manager.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TableParserProfile(
    @SerialName("bank_id") val bankId: String,
    @SerialName("bank_markers") val bankMarkers: List<String>,
    @SerialName("date_column") val dateColumn: Int,
    @SerialName("amount_column") val amountColumn: Int,
    @SerialName("operation_column") val operationColumn: Int? = null,
    @SerialName("details_column") val detailsColumn: Int? = null,
    @SerialName("sign_column") val signColumn: Int? = null,
    @SerialName("currency_column") val currencyColumn: Int? = null,
    @SerialName("date_format") val dateFormat: String,
    @SerialName("amount_format") val amountFormat: String = "dot",
    @SerialName("negative_sign_means_expense") val negativeSignMeansExpense: Boolean = true,
    @SerialName("skip_header_rows") val skipHeaderRows: Int = 1,
    @SerialName("deduplicate_max_amount") val deduplicateMaxAmount: Boolean = false,
    @SerialName("operation_type_map") val operationTypeMap: Map<String, String> = emptyMap(),
    @SerialName("category_map") val categoryMap: Map<String, String> = emptyMap(),
)

@Serializable
data class TableParserProfileList(
    val configs: List<TableParserProfile>
)
