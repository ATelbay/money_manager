package com.atelbay.money_manager.feature.categories.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.components.MoneyManagerButton
import com.atelbay.money_manager.core.ui.components.MoneyManagerTextField
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScreen(
    state: CategoryEditState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onIconSelect: (String) -> Unit,
    onColorSelect: (Long) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("categoryEdit:screen"),
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Редактирование" else "Новая категория") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Name
            MoneyManagerTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = "Название",
                placeholder = "Название категории",
                errorMessage = state.nameError,
                modifier = Modifier.fillMaxWidth(),
                tag = "categoryEdit:nameField",
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Type selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("categoryEdit:typeSelector"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { onTypeChange(TransactionType.EXPENSE) },
                    label = { Text("Расход") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("categoryEdit:typeExpense"),
                )
                FilterChip(
                    selected = state.type == TransactionType.INCOME,
                    onClick = { onTypeChange(TransactionType.INCOME) },
                    label = { Text("Доход") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("categoryEdit:typeIncome"),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Icon selector
            Text(
                text = "Иконка",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            IconPager(
                icons = state.availableIcons,
                selectedIcon = state.selectedIcon,
                selectedColor = state.selectedColor,
                onSelect = onIconSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("categoryEdit:iconGrid"),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Color selector
            Text(
                text = "Цвет",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            ColorPager(
                colors = state.availableColors,
                selectedColor = state.selectedColor,
                onSelect = onColorSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("categoryEdit:colorGrid"),
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            MoneyManagerButton(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("categoryEdit:saveButton"),
            ) {
                Text(if (state.isSaving) "Сохранение..." else "Сохранить")
            }
        }
    }
}

private const val ICONS_PER_PAGE = 10 // 2 rows x 5 columns
private const val COLORS_PER_PAGE = 8 // 2 rows x 4 columns

@Composable
private fun IconPager(
    icons: ImmutableList<String>,
    selectedIcon: String,
    selectedColor: Long,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = icons.chunked(ICONS_PER_PAGE)
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val pageIcons = pages[page]
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (row in pageIcons.chunked(5)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        for (icon in row) {
                            val isSelected = icon == selectedIcon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color(selectedColor)
                                        else Color(selectedColor).copy(alpha = 0.1f),
                                    )
                                    .clickable { onSelect(icon) }
                                    .testTag("categoryEdit:icon_$icon"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = iconToEmoji(icon),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) Color.White else Color(selectedColor),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        // Fill remaining slots to keep alignment
                        repeat(5 - row.size) {
                            Spacer(modifier = Modifier.size(44.dp))
                        }
                    }
                }
            }
        }

        if (pages.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            PageIndicator(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
            )
        }
    }
}

@Composable
private fun ColorPager(
    colors: ImmutableList<Long>,
    selectedColor: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = colors.chunked(COLORS_PER_PAGE)
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val pageColors = pages[page]
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (row in pageColors.chunked(4)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        for (color in row) {
                            val isSelected = color == selectedColor
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                3.dp,
                                                MaterialTheme.colorScheme.onSurface,
                                                CircleShape,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .clickable { onSelect(color) }
                                    .testTag("categoryEdit:color_$color"),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                        repeat(4 - row.size) {
                            Spacer(modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }
        }

        if (pages.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            PageIndicator(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
            )
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

private fun iconToEmoji(icon: String): String = when (icon) {
    "restaurant" -> "\uD83C\uDF7D"
    "directions_car" -> "\uD83D\uDE97"
    "sports_esports" -> "\uD83C\uDFAE"
    "shopping_bag" -> "\uD83D\uDECD"
    "medical_services" -> "\uD83C\uDFE5"
    "home" -> "\uD83C\uDFE0"
    "phone_android" -> "\uD83D\uDCF1"
    "school" -> "\uD83C\uDF93"
    "subscriptions" -> "\uD83D\uDCE6"
    "more_horiz" -> "\u2022\u2022\u2022"
    "payments" -> "\uD83D\uDCB3"
    "work" -> "\uD83D\uDCBC"
    "card_giftcard" -> "\uD83C\uDF81"
    "trending_up" -> "\uD83D\uDCC8"
    "flight" -> "\u2708\uFE0F"
    "local_cafe" -> "\u2615"
    "fitness_center" -> "\uD83C\uDFCB"
    "pets" -> "\uD83D\uDC3E"
    "child_care" -> "\uD83D\uDC76"
    "checkroom" -> "\uD83D\uDC57"
    "local_grocery_store" -> "\uD83D\uDED2"
    "local_gas_station" -> "\u26FD"
    "build" -> "\uD83D\uDD27"
    "savings" -> "\uD83D\uDC37"
    "account_balance" -> "\uD83C\uDFE6"
    "attach_money" -> "\uD83D\uDCB5"
    "redeem" -> "\uD83C\uDF9F"
    "volunteer_activism" -> "\uD83E\uDD1D"
    "celebration" -> "\uD83C\uDF89"
    "music_note" -> "\uD83C\uDFB5"
    else -> "\u2022"
}

@Preview(showBackground = true)
@Composable
private fun CategoryEditScreenPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        CategoryEditScreen(
            state = CategoryEditState(
                isLoading = false,
                availableIcons = CategoryEditViewModel.AVAILABLE_ICONS.toImmutableList(),
                availableColors = CategoryEditViewModel.AVAILABLE_COLORS.toImmutableList(),
            ),
            onBack = {},
            onNameChange = {},
            onTypeChange = {},
            onIconSelect = {},
            onColorSelect = {},
            onSave = {},
        )
    }
}
