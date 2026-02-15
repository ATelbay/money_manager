package com.atelbay.money_manager.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal

data class NavBarItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun MoneyManagerBottomNavBar(
    items: List<NavBarItem>,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        items.forEach { item ->
            val tint by animateColorAsState(
                targetValue = if (item.selected) Teal else colors.textSecondary,
                animationSpec = spring(stiffness = 400f),
                label = "navTint",
            )

            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = tint,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = tint,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Teal.copy(alpha = 0.12f),
                    selectedIconColor = Teal,
                    selectedTextColor = Teal,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary,
                ),
            )
        }
    }
}
