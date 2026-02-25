package com.atelbay.money_manager.presentation.accounts.ui.edit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.components.MoneyManagerButton
import com.atelbay.money_manager.core.ui.components.MoneyManagerTextField
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditScreen(
    state: AccountEditState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("accountEdit:screen"),
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Редактирование" else "Новый счёт") },
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
            MoneyManagerTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = "Название",
                placeholder = "Название счёта",
                errorMessage = state.nameError,
                modifier = Modifier.fillMaxWidth(),
                tag = "accountEdit:nameField",
            )

            Spacer(modifier = Modifier.height(16.dp))

            CurrencyDropdown(
                selectedCurrency = state.currency,
                currencies = state.availableCurrencies,
                onSelect = onCurrencyChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("accountEdit:currencyDropdown"),
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            MoneyManagerButton(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("accountEdit:saveButton"),
            ) {
                Text(if (state.isSaving) "Сохранение..." else "Сохранить")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selectedCurrency: String,
    currencies: ImmutableList<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        MoneyManagerTextField(
            value = selectedCurrency,
            onValueChange = {},
            label = "Валюта",
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            tag = "accountEdit:currencyField",
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        onSelect(currency)
                        expanded = false
                    },
                    modifier = Modifier.testTag("accountEdit:currency_$currency"),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountEditScreenPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        AccountEditScreen(
            state = AccountEditState(isLoading = false),
            onBack = {},
            onNameChange = {},
            onCurrencyChange = {},
            onSave = {},
        )
    }
}
