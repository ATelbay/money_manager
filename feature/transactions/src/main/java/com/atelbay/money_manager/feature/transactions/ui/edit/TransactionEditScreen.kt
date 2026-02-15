package com.atelbay.money_manager.feature.transactions.ui.edit

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.ui.components.ChipType
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.components.MoneyManagerButton
import com.atelbay.money_manager.core.ui.components.MoneyManagerChip
import com.atelbay.money_manager.core.ui.components.MoneyManagerTextField
import com.atelbay.money_manager.core.ui.components.categoryIconFromName
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal
import kotlinx.collections.immutable.ImmutableList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    state: TransactionEditState,
    onBack: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryClick: () -> Unit,
    onCategorySelect: (Category) -> Unit,
    onCategoryDismiss: () -> Unit,
    onDateClick: () -> Unit,
    onDateSelect: (Long) -> Unit,
    onDateDismiss: () -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Scaffold(
        modifier = modifier.testTag("transactionEdit:screen"),
        containerColor = colors.textPrimary.copy(alpha = 0f), // transparent — uses parent bg
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditing) "Редактирование" else "Новая транзакция",
                        style = typography.sectionHeader,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = colors.textPrimary,
                        )
                    }
                },
                actions = {
                    if (state.isEditing && onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("transactionEdit:deleteButton"),
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = colors.expenseForeground,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MoneyManagerTheme.colors.textPrimary.copy(alpha = 0f))
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Teal)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Hero Amount Input ──
            AmountHeroInput(
                value = state.amount,
                onValueChange = onAmountChange,
                errorMessage = state.amountError,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactionEdit:amountField"),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Type Chips ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactionEdit:typeSelector"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MoneyManagerChip(
                    label = "Расход",
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { onTypeChange(TransactionType.EXPENSE) },
                    type = ChipType.EXPENSE,
                    modifier = Modifier.testTag("transactionEdit:typeExpense"),
                )
                Spacer(modifier = Modifier.width(12.dp))
                MoneyManagerChip(
                    label = "Доход",
                    selected = state.type == TransactionType.INCOME,
                    onClick = { onTypeChange(TransactionType.INCOME) },
                    type = ChipType.INCOME,
                    modifier = Modifier.testTag("transactionEdit:typeIncome"),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Category Selector ──
            Text(
                text = "Категория",
                style = typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            CategorySelector(
                selectedCategory = state.selectedCategory,
                errorMessage = state.categoryError,
                onClick = onCategoryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactionEdit:categorySelector"),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Date Field ──
            Text(
                text = "Дата",
                style = typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactionEdit:dateField"),
                onClick = onDateClick,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Teal,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = formatDateFull(state.date),
                        style = typography.cardTitle,
                        color = colors.textPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Note Field ──
            MoneyManagerTextField(
                value = state.note,
                onValueChange = onNoteChange,
                label = "Заметка",
                placeholder = "Описание транзакции",
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                tag = "transactionEdit:noteField",
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // ── Save Button ──
            MoneyManagerButton(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactionEdit:saveButton"),
            ) {
                Text(
                    text = if (state.isSaving) "Сохранение..." else "Сохранить",
                    style = typography.cardTitle,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Category BottomSheet
        if (state.showCategorySheet) {
            CategoryBottomSheet(
                categories = state.categories,
                selectedCategory = state.selectedCategory,
                onSelect = onCategorySelect,
                onDismiss = onCategoryDismiss,
            )
        }

        // Date Picker Dialog
        if (state.showDatePicker) {
            MoneyManagerDatePickerDialog(
                initialDate = state.date,
                onConfirm = onDateSelect,
                onDismiss = onDateDismiss,
            )
        }
    }
}

// ── Hero Amount Input ──

@Composable
private fun AmountHeroInput(
    value: String,
    onValueChange: (String) -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "\u20B8",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = value,
                onValueChange = { newValue ->
                    val cleaned = newValue.filter { it.isDigit() || it == '.' }
                    val parts = cleaned.split('.')
                    if (parts.size <= 2) {
                        onValueChange(cleaned)
                    }
                },
                textStyle = TextStyle(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(Teal),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = "0",
                            style = TextStyle(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = colors.textTertiary,
                        )
                    }
                    innerTextField()
                },
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MoneyManagerTheme.typography.caption,
                color = colors.expenseForeground,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

// ── Category Selector ──

@Composable
private fun CategorySelector(
    selectedCategory: Category?,
    errorMessage: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Column(modifier = modifier) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectedCategory != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(selectedCategory.color).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = categoryIconFromName(selectedCategory.icon),
                            contentDescription = null,
                            tint = Color(selectedCategory.color),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedCategory.name,
                        style = typography.cardTitle,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Text(
                        text = "Выберите категорию",
                        style = typography.cardTitle,
                        color = colors.textTertiary,
                        modifier = Modifier.weight(1f),
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = typography.caption,
                color = colors.expenseForeground,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

// ── Category Bottom Sheet ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryBottomSheet(
    categories: ImmutableList<Category>,
    selectedCategory: Category?,
    onSelect: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.glassBgStart,
        modifier = Modifier.testTag("transactionEdit:categorySheet"),
    ) {
        Text(
            text = "Выберите категорию",
            style = typography.sectionHeader,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categories, key = { it.id }) { category ->
                CategoryGridItem(
                    category = category,
                    isSelected = category.id == selectedCategory?.id,
                    onClick = { onSelect(category) },
                    modifier = Modifier.testTag("transactionEdit:category_${category.id}"),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CategoryGridItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val categoryColor = Color(category.color)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, Teal, CircleShape)
                    } else {
                        Modifier
                    },
                )
                .clip(CircleShape)
                .background(
                    if (isSelected) categoryColor.copy(alpha = 0.25f)
                    else categoryColor.copy(alpha = 0.12f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Icon(
                    imageVector = categoryIconFromName(category.icon),
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = category.name,
            style = MoneyManagerTheme.typography.caption,
            color = if (isSelected) colors.textPrimary else colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

// ── Date Picker ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoneyManagerDatePickerDialog(
    initialDate: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onConfirm(it) }
                },
                modifier = Modifier.testTag("transactionEdit:dateConfirm"),
            ) {
                Text("OK", color = Teal)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        modifier = Modifier.testTag("transactionEdit:datePicker"),
        colors = DatePickerDefaults.colors(
            containerColor = MoneyManagerTheme.colors.glassBgStart,
        ),
    ) {
        DatePicker(state = datePickerState)
    }
}

private val fullDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))

private fun formatDateFull(timestamp: Long): String = fullDateFormat.format(Date(timestamp))

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun TransactionEditScreenPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        TransactionEditScreen(
            state = TransactionEditState(isLoading = false),
            onBack = {},
            onTypeChange = {},
            onAmountChange = {},
            onCategoryClick = {},
            onCategorySelect = {},
            onCategoryDismiss = {},
            onDateClick = {},
            onDateSelect = {},
            onDateDismiss = {},
            onNoteChange = {},
            onSave = {},
        )
    }
}
