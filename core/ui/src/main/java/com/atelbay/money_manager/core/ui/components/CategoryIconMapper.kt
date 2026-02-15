package com.atelbay.money_manager.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

fun categoryIconFromName(iconName: String): ImageVector = when (iconName) {
    "restaurant" -> Icons.Default.Restaurant
    "directions_car" -> Icons.Default.DirectionsCar
    "sports_esports" -> Icons.Default.SportsEsports
    "shopping_bag" -> Icons.Default.ShoppingBag
    "medical_services" -> Icons.Default.LocalHospital
    "home" -> Icons.Default.Home
    "phone_android" -> Icons.Default.PhoneAndroid
    "school" -> Icons.Default.School
    "subscriptions" -> Icons.Default.Subscriptions
    "more_horiz" -> Icons.Default.MoreHoriz
    "payments" -> Icons.Default.Payments
    "work" -> Icons.Default.Work
    "card_giftcard" -> Icons.Default.CardGiftcard
    "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
    else -> Icons.Default.MoreHoriz
}
