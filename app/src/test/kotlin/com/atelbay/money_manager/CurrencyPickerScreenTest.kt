package com.atelbay.money_manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.presentation.settings.ui.CurrencyPickerSide
import com.atelbay.money_manager.presentation.settings.ui.CurrencyPickerScreen
import com.atelbay.money_manager.presentation.settings.ui.SupportedCurrencies
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "xxhdpi")
class CurrencyPickerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun currencyPicker_edits_only_active_side_and_supports_search() {
        composeTestRule.setContent {
            var baseCurrency by remember { mutableStateOf(SupportedCurrencies.fromCode("KZT")) }
            var targetCurrency by remember { mutableStateOf(SupportedCurrencies.fromCode("USD")) }
            var activeSide by remember { mutableStateOf(CurrencyPickerSide.BASE) }

            MoneyManagerTheme(dynamicColor = false) {
                CurrencyPickerScreen(
                    baseCurrency = baseCurrency,
                    targetCurrency = targetCurrency,
                    activeSide = activeSide,
                    onSideChange = { activeSide = it },
                    onSelect = { currency ->
                        if (activeSide == CurrencyPickerSide.BASE) {
                            baseCurrency = currency
                        } else {
                            targetCurrency = currency
                        }
                    },
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Валютная пара").assertIsDisplayed()
        composeTestRule.onNodeWithText("KZT -> USD").assertIsDisplayed()

        composeTestRule.onNodeWithTag("currencyPicker:sideTarget").performClick()
        composeTestRule.onNodeWithTag("currencyPicker:search").performTextInput("евро")
        composeTestRule.onNodeWithText("EUR · Евро").performClick()

        composeTestRule.onNodeWithText("KZT -> EUR").assertIsDisplayed()

        composeTestRule.onNodeWithTag("currencyPicker:sideBase").performClick()
        composeTestRule.onNodeWithTag("currencyPicker:search").performTextClearance()
        composeTestRule.onNodeWithTag("currencyPicker:search").performTextInput("JPY")
        composeTestRule.onNodeWithText("JPY · Японская иена").performClick()

        composeTestRule.onNodeWithText("JPY -> EUR").assertIsDisplayed()
    }
}
