package com.atelbay.money_manager.feature.importstatement.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionOverride
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParsedTransactionItem(
    transaction: ParsedTransaction,
    override: TransactionOverride?,
    categories: List<CategoryEntity>,
    onAmountChange: (Double) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onDetailsChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onCategoryChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentAmount = override?.amount ?: transaction.amount
    val currentType = override?.type ?: transaction.type
    val currentDetails = override?.details ?: transaction.details
    val currentDate = override?.date ?: transaction.date
    val currentCategoryId = override?.categoryId ?: transaction.categoryId

    val hasCategory = currentCategoryId != null
    val borderColor = when {
        !hasCategory -> Color(0xFFF44336)
        transaction.needsReview -> Color(0xFFFFA000)
        else -> Color.Transparent
    }
    val confidenceColor = when {
        transaction.confidence >= 0.8f -> Color(0xFF4CAF50)
        transaction.confidence >= 0.5f -> Color(0xFFFFA000)
        else -> Color(0xFFF44336)
    }

    val filteredCategories = categories.filter { it.type == currentType.value }
    val selectedCategory = categories.find { it.id == currentCategoryId }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        border = if (!hasCategory || transaction.needsReview) BorderStroke(1.dp, borderColor) else null,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Confidence badge
            Text(
                text = "${(transaction.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = confidenceColor,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Details (editable)
            OutlinedTextField(
                value = currentDetails,
                onValueChange = onDetailsChange,
                label = { Text("Описание") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Amount + Date row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                var amountText by remember(currentAmount) {
                    mutableStateOf(
                        if (currentAmount == currentAmount.toLong().toDouble()) {
                            currentAmount.toLong().toString()
                        } else {
                            currentAmount.toString()
                        },
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { text ->
                        amountText = text
                        text.toDoubleOrNull()?.let(onAmountChange)
                    },
                    label = { Text("Сумма") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )

                var dateText by remember(currentDate) {
                    mutableStateOf(currentDate.toString())
                }
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { text ->
                        dateText = text
                        runCatching { LocalDate.parse(text) }
                            .getOrNull()
                            ?.let(onDateChange)
                    },
                    label = { Text("Дата") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Type toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = currentType == TransactionType.EXPENSE,
                    onClick = { onTypeChange(TransactionType.EXPENSE) },
                    label = { Text("Расход") },
                )
                FilterChip(
                    selected = currentType == TransactionType.INCOME,
                    onClick = { onTypeChange(TransactionType.INCOME) },
                    label = { Text("Доход") },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category dropdown
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name
                        ?: transaction.suggestedCategoryName
                        ?: "Выберите категорию",
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Категория") },
                    isError = !hasCategory,
                    supportingText = if (!hasCategory) {
                        { Text("Выберите категорию для импорта") }
                    } else {
                        null
                    },
                    textStyle = MaterialTheme.typography.bodySmall,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    filteredCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                onCategoryChange(category.id)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
