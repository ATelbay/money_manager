package com.atelbay.money_manager.core.model

sealed interface ImportState {
    data object Idle : ImportState
    data object SelectingFile : ImportState
    data object Parsing : ImportState
    data class Preview(
        val result: ImportResult,
        val overrides: Map<Int, TransactionOverride>,
    ) : ImportState
    data object Importing : ImportState
    data class Success(val imported: Int) : ImportState
    data class Error(val message: String) : ImportState
}
