package com.atelbay.money_manager.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

@Composable
fun MoneyManagerBottomBar(
    currentDestination: TopLevelDestination?,
    onNavigate: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = MoneyManagerTheme.strings
    NavigationBar(modifier = modifier.testTag("bottomBar")) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentDestination == destination
            val localizedLabel = when (destination) {
                TopLevelDestination.HOME -> s.navTransactions
                TopLevelDestination.STATISTICS -> s.navStatistics
                TopLevelDestination.ACCOUNTS -> s.navAccounts
                TopLevelDestination.SETTINGS -> s.navSettings
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = localizedLabel,
                    )
                },
                label = { Text(localizedLabel) },
                modifier = Modifier.testTag("bottomBar:${destination.name}"),
            )
        }
    }
}

object MoneyManagerBottomBarDefaults {
    val Height = 80.dp
}
