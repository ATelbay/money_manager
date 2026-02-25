package com.atelbay.money_manager.presentation.importstatement.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.ImportResult
import com.atelbay.money_manager.core.model.TransactionOverride
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreview(
    result: ImportResult,
    overrides: Map<Int, TransactionOverride>,
    categories: List<Category>,
    accounts: List<Account>,
    selectedAccountId: Long?,
    onAccountSelected: (Long) -> Unit,
    onAmountChange: (Int, Double) -> Unit,
    onTypeChange: (Int, TransactionType) -> Unit,
    onDetailsChange: (Int, String) -> Unit,
    onDateChange: (Int, LocalDate) -> Unit,
    onCategoryChange: (Int, Long) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val confident = result.newTransactions
        .mapIndexed { index, tx -> index to tx }
        .filter { !it.second.needsReview }
    val needsReview = result.newTransactions
        .mapIndexed { index, tx -> index to tx }
        .filter { it.second.needsReview }

    val selectedAccount = accounts.find { it.id == selectedAccountId }

    val readyCount = result.newTransactions.indices.count { index ->
        val override = overrides[index]
        val tx = result.newTransactions[index]
        (override?.categoryId ?: tx.categoryId) != null
    }
    val noCategoryCount = result.newTransactions.size - readyCount

    LazyColumn(
        modifier = modifier.testTag("import:preview"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "summary") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Найдено: ${result.total}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (result.duplicates > 0) {
                        Text(
                            text = "Дубликаты: ${result.duplicates}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("import:duplicateCount"),
                        )
                    }
                    Text(
                        text = "К импорту: $readyCount из ${result.newTransactions.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    if (noCategoryCount > 0) {
                        Text(
                            text = "Без категории: $noCategoryCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Button(
                    onClick = onImport,
                    enabled = selectedAccountId != null,
                    modifier = Modifier.testTag("import:importButton"),
                ) {
                    Text("Импорт ($readyCount)")
                }
            }
        }

        item(key = "accountSelector") {
            AccountSelector(
                accounts = accounts,
                selectedAccount = selectedAccount,
                onAccountSelected = onAccountSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        if (needsReview.isNotEmpty()) {
            item(key = "reviewHeader") {
                Text(
                    text = "Требуют проверки (${needsReview.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            itemsIndexed(
                items = needsReview,
                key = { _, pair -> "review_${pair.first}" },
            ) { _, (index, transaction) ->
                val override = overrides[index]
                ParsedTransactionItem(
                    transaction = transaction,
                    override = override,
                    categories = categories,
                    onAmountChange = { onAmountChange(index, it) },
                    onTypeChange = { onTypeChange(index, it) },
                    onDetailsChange = { onDetailsChange(index, it) },
                    onDateChange = { onDateChange(index, it) },
                    onCategoryChange = { onCategoryChange(index, it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        if (confident.isNotEmpty()) {
            item(key = "confidentHeader") {
                Text(
                    text = "Распознаны (${confident.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            itemsIndexed(
                items = confident,
                key = { _, pair -> "confident_${pair.first}" },
            ) { _, (index, transaction) ->
                val override = overrides[index]
                ParsedTransactionItem(
                    transaction = transaction,
                    override = override,
                    categories = categories,
                    onAmountChange = { onAmountChange(index, it) },
                    onTypeChange = { onTypeChange(index, it) },
                    onDetailsChange = { onDetailsChange(index, it) },
                    onDateChange = { onDateChange(index, it) },
                    onCategoryChange = { onCategoryChange(index, it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        item(key = "bottomSpacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSelector(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Счёт для импорта",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selectedAccount?.let { "${it.name} (${it.currency})" } ?: "Выберите счёт",
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .testTag("import:accountSelector"),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text("${account.name} (${account.currency})") },
                        onClick = {
                            onAccountSelected(account.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
