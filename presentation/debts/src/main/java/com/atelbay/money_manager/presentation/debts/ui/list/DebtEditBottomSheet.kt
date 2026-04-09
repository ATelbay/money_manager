package com.atelbay.money_manager.presentation.debts.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.core.model.DebtDirection
import com.atelbay.money_manager.core.model.DebtStatus
import com.atelbay.money_manager.core.ui.components.MoneyManagerButton
import com.atelbay.money_manager.core.ui.components.MoneyManagerTextField
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtEditBottomSheet(
    debt: Debt?,
    accounts: ImmutableList<Account>,
    onSave: (Debt) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = MoneyManagerTheme.strings
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isEditing = debt != null

    var contactName by remember { mutableStateOf(debt?.contactName ?: "") }
    var direction by remember { mutableStateOf(debt?.direction ?: DebtDirection.LENT) }
    var amountText by remember {
        mutableStateOf(
            if (debt != null) debt.totalAmount.toBigDecimal().stripTrailingZeros().toPlainString() else "",
        )
    }
    var selectedAccountId by remember { mutableLongStateOf(debt?.accountId ?: 0L) }
    var noteText by remember { mutableStateOf(debt?.note ?: "") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }

    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("debtEdit:sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = if (isEditing) s.debtsTitle else s.addDebt,
                style = typography.sectionHeader,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Contact Name
            MoneyManagerTextField(
                value = contactName,
                onValueChange = {
                    contactName = it
                    nameError = null
                },
                label = s.contactName,
                errorMessage = nameError,
                modifier = Modifier.fillMaxWidth(),
                tag = "debtEdit:contactName",
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Direction selector
            Text(
                text = s.debtDirection,
                style = typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = direction == DebtDirection.LENT,
                    onClick = { direction = DebtDirection.LENT },
                    label = { Text(s.iGaveDebt) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Teal.copy(alpha = 0.15f),
                        selectedLabelColor = Teal,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("debtEdit:directionLent"),
                )
                FilterChip(
                    selected = direction == DebtDirection.BORROWED,
                    onClick = { direction = DebtDirection.BORROWED },
                    label = { Text(s.iTookDebt) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.expenseForeground.copy(alpha = 0.15f),
                        selectedLabelColor = colors.expenseForeground,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("debtEdit:directionBorrowed"),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount
            MoneyManagerTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = null
                },
                label = s.totalAmount,
                placeholder = "0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                errorMessage = amountError,
                modifier = Modifier.fillMaxWidth(),
                tag = "debtEdit:amount",
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Account selector
            AccountDropdown(
                selectedAccount = selectedAccount,
                accounts = accounts,
                onSelect = {
                    selectedAccountId = it.id
                    accountError = null
                },
                errorMessage = accountError,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Note
            MoneyManagerTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = s.note,
                placeholder = s.noteHint,
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                tag = "debtEdit:note",
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            MoneyManagerButton(
                onClick = {
                    var hasError = false
                    if (contactName.isBlank()) {
                        nameError = s.contactName
                        hasError = true
                    }
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        amountError = s.errorEnterValidAmount
                        hasError = true
                    }
                    if (selectedAccountId == 0L) {
                        accountError = s.errorSelectAccount
                        hasError = true
                    }
                    if (hasError) return@MoneyManagerButton

                    val account = accounts.first { it.id == selectedAccountId }
                    onSave(
                        Debt(
                            id = debt?.id ?: 0,
                            contactName = contactName.trim(),
                            direction = direction,
                            totalAmount = amount!!,
                            paidAmount = debt?.paidAmount ?: 0.0,
                            remainingAmount = amount - (debt?.paidAmount ?: 0.0),
                            currency = account.currency,
                            accountId = selectedAccountId,
                            accountName = account.name,
                            note = noteText.takeIf { it.isNotBlank() },
                            createdAt = debt?.createdAt ?: System.currentTimeMillis(),
                            status = debt?.status ?: DebtStatus.ACTIVE,
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debtEdit:saveButton"),
            ) {
                Text(
                    text = s.save,
                    style = typography.cardTitle,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(
    selectedAccount: Account?,
    accounts: ImmutableList<Account>,
    onSelect: (Account) -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val s = MoneyManagerTheme.strings
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            MoneyManagerTextField(
                value = selectedAccount?.name ?: "",
                onValueChange = {},
                label = s.selectAccount,
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                tag = "debtEdit:accountField",
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${account.name} (${account.currency})",
                                color = colors.textPrimary,
                            )
                        },
                        onClick = {
                            onSelect(account)
                            expanded = false
                        },
                        modifier = Modifier.testTag("debtEdit:account_${account.id}"),
                    )
                }
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
