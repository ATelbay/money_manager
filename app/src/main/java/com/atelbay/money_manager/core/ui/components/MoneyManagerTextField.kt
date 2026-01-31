package com.atelbay.money_manager.core.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier),
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = errorMessage != null,
        supportingText = errorMessage?.let {
            {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
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
