package com.atelbay.money_manager.presentation.onboarding.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    onBack: (() -> Unit)? = null,
) {
    val s = MoneyManagerTheme.strings
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("createAccount:screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (onBack != null) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(if (onBack != null) 16.dp else 48.dp))

        Text(
            text = s.createFirstAccount,
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = s.createFirstAccountSubtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        MoneyManagerTextField(
            value = state.accountName,
            onValueChange = onAccountNameChange,
            label = s.accountNameHint,
            placeholder = s.accountMain,
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
                label = s.currency,
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
            label = s.initialBalance,
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
            Text(if (state.isCreating) s.creating else s.createAccount)
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
