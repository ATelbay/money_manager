package com.atelbay.money_manager.presentation.importstatement.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionOverride
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.model.money.parseToMinorUnitsOrNull
import com.atelbay.money_manager.core.model.money.toMajorPlainString
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParsedTransactionItem(
    index: Int,
    transaction: ParsedTransaction,
    override: TransactionOverride?,
    categories: List<Category>,
    onAmountChange: (Long) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onDetailsChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onCategoryChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = MoneyManagerTheme.strings
    val currentAmount = override?.amount ?: transaction.amount
    val currentType = override?.type ?: transaction.type
    val currentDetails = override?.details ?: transaction.details
    val currentDate = override?.date ?: transaction.date
    val currentCategoryId = override?.categoryId ?: transaction.categoryId

    // A category is resolved if the user/parser set an id, or a name is pending creation at import.
    val hasCategory = currentCategoryId != null || transaction.pendingCategoryName != null
    val borderColor = when {
        !hasCategory -> MaterialTheme.colorScheme.error
        transaction.needsReview -> MaterialTheme.colorScheme.tertiary
        else -> Color.Transparent
    }
    val confidenceColor = when {
        transaction.confidence >= 0.8f -> MaterialTheme.colorScheme.primary
        transaction.confidence >= 0.5f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    val filteredCategories = categories.filter { it.type == currentType }
    val selectedCategory = categories.find { it.id == currentCategoryId }
    val tag = "import:item:$index"

    Surface(
        modifier = modifier.testTag(tag),
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
                label = { Text(strings.importDescriptionLabel) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("$tag:description"),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Amount + Date row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                var amountText by remember(currentAmount) {
                    mutableStateOf(currentAmount.toMajorPlainString())
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { text ->
                        amountText = text
                        text.parseToMinorUnitsOrNull()?.let(onAmountChange)
                    },
                    label = { Text(strings.amount) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("$tag:amount"),
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
                    label = { Text(strings.date) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("$tag:date"),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Type toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = currentType == TransactionType.EXPENSE,
                    onClick = { onTypeChange(TransactionType.EXPENSE) },
                    label = { Text(strings.expense) },
                    modifier = Modifier.testTag("$tag:expense"),
                )
                FilterChip(
                    selected = currentType == TransactionType.INCOME,
                    onClick = { onTypeChange(TransactionType.INCOME) },
                    label = { Text(strings.income) },
                    modifier = Modifier.testTag("$tag:income"),
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
                        ?: transaction.pendingCategoryName
                        ?: transaction.suggestedCategoryName
                        ?: strings.chooseCategory,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text(strings.category) },
                    isError = !hasCategory,
                    supportingText = if (!hasCategory) {
                        { Text(strings.importCategoryRequired) }
                    } else {
                        null
                    },
                    textStyle = MaterialTheme.typography.bodySmall,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("$tag:category")
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
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
