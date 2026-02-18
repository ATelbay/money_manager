package com.atelbay.money_manager.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal

@Composable
fun MoneyManagerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    errorMessage: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    tag: String? = null,
) {
    val colors = MoneyManagerTheme.colors

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier),
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let {
            { Text(it, color = colors.textTertiary) }
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = errorMessage != null,
        supportingText = errorMessage?.let {
            {
                Text(
                    text = it,
                    color = colors.expenseForeground,
                )
            }
        },
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        enabled = enabled,
        readOnly = readOnly,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Teal,
            focusedLabelColor = Teal,
            cursorColor = Teal,
            unfocusedBorderColor = colors.surfaceBorder,
            errorBorderColor = colors.expense,
        ),
    )
}

@Composable
fun MoneyManagerAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    currency: String = "\u20B8",
    tag: String? = null,
) {
    val colors = MoneyManagerTheme.colors

    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            val cleaned = newValue.filter { it.isDigit() || it == '.' }
            val parts = cleaned.split('.')
            if (parts.size <= 2) {
                onValueChange(cleaned)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        placeholder = {
            Text(
                text = "0.00",
                style = TextStyle(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = colors.textTertiary,
            )
        },
        prefix = {
            Text(
                text = currency,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.textSecondary,
            )
        },
        textStyle = TextStyle(
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Teal,
            cursorColor = Teal,
            unfocusedBorderColor = colors.surfaceBorder,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun MoneyManagerTextFieldPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        MoneyManagerTextField(
            value = "1500",
            onValueChange = {},
            label = "Сумма",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MoneyManagerTextFieldErrorPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        MoneyManagerTextField(
            value = "",
            onValueChange = {},
            label = "Сумма",
            errorMessage = "Обязательное поле",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MoneyManagerAmountFieldPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        MoneyManagerAmountField(
            value = "15000",
            onValueChange = {},
        )
    }
}
