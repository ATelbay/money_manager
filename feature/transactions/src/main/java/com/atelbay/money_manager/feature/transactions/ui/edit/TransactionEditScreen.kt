package com.atelbay.money_manager.feature.transactions.ui.edit

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.components.MoneyManagerButton
import com.atelbay.money_manager.core.ui.components.MoneyManagerTextField
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.TransactionType
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
) {
    Scaffold(
        modifier = modifier.testTag("transactionEdit:screen"),
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Редактирование" else "Новая транзакция") },
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
            // Type selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactionEdit:typeSelector"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { onTypeChange(TransactionType.EXPENSE) },
                    label = { Text("Расход") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("transactionEdit:typeExpense"),
                )
                FilterChip(
                    selected = state.type == TransactionType.INCOME,
                    onClick = { onTypeChange(TransactionType.INCOME) },
                    label = { Text("Доход") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("transactionEdit:typeIncome"),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount
            MoneyManagerTextField(
                value = state.amount,
                onValueChange = onAmountChange,
                label = "Сумма",
                placeholder = "0",
                errorMessage = state.amountError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                tag = "transactionEdit:amountField",
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category
            Text(
                text = "Категория",
                style = MaterialTheme.typography.labelLarge,
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

            // Date
            MoneyManagerTextField(
                value = formatDateFull(state.date),
                onValueChange = {},
                label = "Дата",
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDateClick() },
                tag = "transactionEdit:dateField",
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Note
            MoneyManagerTextField(
                value = state.note,
                onValueChange = onNoteChange,
                label = "Заметка",
                placeholder = "Необязательно",
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                tag = "transactionEdit:noteField",
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            MoneyManagerButton(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactionEdit:saveButton"),
            ) {
                Text(if (state.isSaving) "Сохранение..." else "Сохранить")
            }
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

@Composable
private fun CategorySelector(
    selectedCategory: Category?,
    errorMessage: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.clickable { onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectedCategory != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(selectedCategory.color).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = selectedCategory.name.take(1),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(selectedCategory.color),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = selectedCategory.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    text = "Выберите категорию",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryBottomSheet(
    categories: ImmutableList<Category>,
    selectedCategory: Category?,
    onSelect: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("transactionEdit:categorySheet"),
    ) {
        Text(
            text = "Выберите категорию",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Color(category.color)
                    else Color(category.color).copy(alpha = 0.2f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Text(
                    text = category.name.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(category.color),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

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
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        modifier = Modifier.testTag("transactionEdit:datePicker"),
    ) {
        DatePicker(state = datePickerState)
    }
}

private val fullDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))

private fun formatDateFull(timestamp: Long): String = fullDateFormat.format(Date(timestamp))

@Preview(showBackground = true)
@Composable
private fun TransactionEditScreenPreview() {
    MoneyManagerTheme(dynamicColor = false) {
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
