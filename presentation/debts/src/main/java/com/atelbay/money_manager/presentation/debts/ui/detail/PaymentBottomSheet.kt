package com.atelbay.money_manager.presentation.debts.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.money.parseToMinorUnitsOrNull
import com.atelbay.money_manager.core.model.money.toMajorPlainString
import com.atelbay.money_manager.core.ui.components.MoneyManagerButton
import com.atelbay.money_manager.core.ui.components.MoneyManagerTextField
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentBottomSheet(
    remainingAmount: Long,
    onSave: (amount: Long, date: Long, note: String?, createTransaction: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = MoneyManagerTheme.strings
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = remember(s.locale) { SimpleDateFormat("dd.MM.yyyy", s.locale) }

    var amountText by remember {
        mutableStateOf(remainingAmount.toMajorPlainString())
    }
    var noteText by remember { mutableStateOf("") }
    var createTransaction by remember { mutableStateOf(true) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var paymentDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amount = amountText.parseToMinorUnitsOrNull()
    val showOverpayment = amount != null && amount > remainingAmount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("paymentSheet:sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = s.recordPayment,
                style = typography.sectionHeader,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Amount
            MoneyManagerTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = null
                },
                label = s.amount,
                placeholder = "0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                errorMessage = amountError,
                modifier = Modifier.fillMaxWidth(),
                tag = "paymentSheet:amount",
            )

            if (showOverpayment) {
                Text(
                    text = s.overpaymentNotice,
                    style = typography.caption,
                    color = colors.expenseForeground,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 4.dp)
                        .testTag("paymentSheet:overpayment"),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .padding(vertical = 8.dp)
                    .testTag("paymentSheet:dateRow"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = s.date,
                    style = typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.width(100.dp),
                )
                Text(
                    text = dateFormat.format(Date(paymentDate)),
                    style = typography.cardTitle,
                    color = colors.textPrimary,
                )
            }

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
                tag = "paymentSheet:note",
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Create transaction checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = createTransaction,
                    onCheckedChange = { createTransaction = it },
                    colors = CheckboxDefaults.colors(checkedColor = Teal),
                    modifier = Modifier.testTag("paymentSheet:createTransaction"),
                )
                Text(
                    text = s.createTransaction,
                    style = typography.cardTitle,
                    color = colors.textPrimary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            MoneyManagerButton(
                onClick = {
                    val parsedAmount = amountText.parseToMinorUnitsOrNull()
                    if (parsedAmount == null || parsedAmount <= 0) {
                        amountError = s.errorEnterValidAmount
                        return@MoneyManagerButton
                    }
                    onSave(
                        parsedAmount,
                        paymentDate,
                        noteText.takeIf { it.isNotBlank() },
                        createTransaction,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("paymentSheet:saveButton"),
            ) {
                Text(
                    text = s.save,
                    style = typography.cardTitle,
                )
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = paymentDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { utcMillis ->
                            // DatePicker emits UTC midnight; re-anchor to the same calendar day in the
                            // system zone so the stored date matches what the user picked.
                            paymentDate = Instant.ofEpochMilli(utcMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("paymentSheet:dateConfirm"),
                ) {
                    Text(s.ok)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(s.cancel)
                }
            },
        ) {
            DatePicker(state = pickerState, modifier = Modifier.testTag("paymentSheet:datePicker"))
        }
    }
}
