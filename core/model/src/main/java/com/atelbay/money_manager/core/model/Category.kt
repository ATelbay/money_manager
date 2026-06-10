package com.atelbay.money_manager.core.model

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long,
    val type: TransactionType,
    val isDefault: Boolean = false,
) {
    companion object {
        /** Fallback ARGB color used when a transaction's category can't be resolved (Blue Grey 400). */
        const val DEFAULT_COLOR: Long = 0xFF90A4AE
    }
}
