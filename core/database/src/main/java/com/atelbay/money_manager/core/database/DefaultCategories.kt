package com.atelbay.money_manager.core.database

import com.atelbay.money_manager.core.database.entity.CategoryEntity

object DefaultCategories {

    fun all(): List<CategoryEntity> = expenses() + income()

    private fun expenses(): List<CategoryEntity> = listOf(
        category("Еда", "restaurant", 0xFFE57373, "expense"),
        category("Транспорт", "directions_car", 0xFF64B5F6, "expense"),
        category("Развлечения", "sports_esports", 0xFFBA68C8, "expense"),
        category("Покупки", "shopping_bag", 0xFFFFB74D, "expense"),
        category("Здоровье", "medical_services", 0xFFEF5350, "expense"),
        category("Жильё", "home", 0xFF78909C, "expense"),
        category("Связь", "phone_android", 0xFF4DD0E1, "expense"),
        category("Образование", "school", 0xFF7986CB, "expense"),
        category("Подписки", "subscriptions", 0xFF9575CD, "expense"),
        category("Перевод", "swap_horiz", 0xFF64B5F6, "expense"),
        category("Другое", "more_horiz", 0xFF90A4AE, "expense"),
    )

    private fun income(): List<CategoryEntity> = listOf(
        category("Зарплата", "payments", 0xFF81C784, "income"),
        category("Фриланс", "work", 0xFF4DB6AC, "income"),
        category("Подарки", "card_giftcard", 0xFFF06292, "income"),
        category("Инвестиции", "trending_up", 0xFF4FC3F7, "income"),
        category("Перевод", "swap_horiz", 0xFF64B5F6, "income"),
        category("Пополнение", "account_balance_wallet", 0xFF81C784, "income"),
        category("Другое", "more_horiz", 0xFFA5D6A7, "income"),
    )

    private fun category(name: String, icon: String, color: Long, type: String) =
        CategoryEntity(
            name = name,
            icon = icon,
            color = color,
            type = type,
            isDefault = true,
        )
}
