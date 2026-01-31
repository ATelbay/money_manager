package com.atelbay.money_manager.feature.onboarding.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.components.MoneyManagerButton
import com.atelbay.money_manager.core.ui.components.MoneyManagerTextField
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(
    state: CreateAccountState,
    onAccountNameChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("createAccount:screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Создайте первый счёт",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Укажите название, валюту и начальный баланс",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        MoneyManagerTextField(
            value = state.accountName,
            onValueChange = onAccountNameChange,
            label = "Название счёта",
            placeholder = "Основной",
            errorMessage = state.accountNameError,
            modifier = Modifier.fillMaxWidth(),
            tag = "createAccount:nameField",
        )

        Spacer(modifier = Modifier.height(16.dp))

        var currencyExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = currencyExpanded,
            onExpandedChange = { currencyExpanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            MoneyManagerTextField(
                value = state.currency,
                onValueChange = {},
                label = "Валюта",
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                tag = "createAccount:currencyField",
            )

            ExposedDropdownMenu(
                expanded = currencyExpanded,
                onDismissRequest = { currencyExpanded = false },
            ) {
                state.availableCurrencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            onCurrencyChange(currency)
                            currencyExpanded = false
                        },
                        modifier = Modifier.testTag("createAccount:currency_$currency"),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        MoneyManagerTextField(
            value = state.initialBalance,
            onValueChange = onBalanceChange,
            label = "Начальный баланс",
            placeholder = "0",
            errorMessage = state.balanceError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            tag = "createAccount:balanceField",
        )

        Spacer(modifier = Modifier.weight(1f))

        MoneyManagerButton(
            onClick = onCreateClick,
            enabled = !state.isCreating,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("createAccount:createButton"),
        ) {
            Text(if (state.isCreating) "Создание..." else "Создать счёт")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateAccountScreenPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        CreateAccountScreen(
            state = CreateAccountState(accountName = "Основной", currency = "KZT"),
            onAccountNameChange = {},
            onCurrencyChange = {},
            onBalanceChange = {},
            onCreateClick = {},
        )
    }
}
