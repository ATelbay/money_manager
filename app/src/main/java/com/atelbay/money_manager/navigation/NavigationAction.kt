package com.atelbay.money_manager.navigation

sealed interface NavigationAction {
    data class OpenImport(val pdfUri: String) : NavigationAction
}
