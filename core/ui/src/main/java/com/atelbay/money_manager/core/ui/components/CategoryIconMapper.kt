package com.atelbay.money_manager.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VolunteerActivism
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
    "flight" -> Icons.Default.Flight
    "local_cafe" -> Icons.Default.LocalCafe
    "fitness_center" -> Icons.Default.FitnessCenter
    "pets" -> Icons.Default.Pets
    "child_care" -> Icons.Default.ChildCare
    "checkroom" -> Icons.Default.Checkroom
    "local_grocery_store" -> Icons.Default.LocalGroceryStore
    "local_gas_station" -> Icons.Default.LocalGasStation
    "build" -> Icons.Default.Build
    "savings" -> Icons.Default.Savings
    "account_balance" -> Icons.Default.AccountBalance
    "attach_money" -> Icons.Default.AttachMoney
    "redeem" -> Icons.Default.Redeem
    "volunteer_activism" -> Icons.Default.VolunteerActivism
    "celebration" -> Icons.Default.Celebration
    "music_note" -> Icons.Default.MusicNote
    "swap_horiz" -> Icons.Default.SwapHoriz
    "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
    else -> Icons.Default.MoreHoriz
}
