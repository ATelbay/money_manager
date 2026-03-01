package com.atelbay.money_manager

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.presentation.transactions.ui.edit.TransactionEditScreen
import com.atelbay.money_manager.presentation.transactions.ui.edit.TransactionEditState
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "xxhdpi")
class TransactionEditScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testCategories = listOf(
        Category(id = 1, name = "Food", icon = "🍔", color = 0xFFE57373, type = TransactionType.EXPENSE),
        Category(id = 2, name = "Transport", icon = "🚗", color = 0xFF81C784, type = TransactionType.EXPENSE),
        Category(id = 3, name = "Shopping", icon = "🛍️", color = 0xFF64B5F6, type = TransactionType.EXPENSE),
    ).toImmutableList()

    @Test
    fun captureTransactionEditEmpty() {
        composeTestRule.setContent {
            MoneyManagerTheme(dynamicColor = false) {
                TransactionEditScreen(
                    state = TransactionEditState(
                        isLoading = false,
                        categories = testCategories,
                    ),
                    onBack = {},
                    onTypeChange = {},
                    onAmountChange = {},
                    onCategoryClick = {},
                    onCategorySelect = {},
                    onCategoryDismiss = {},
                    onDateClick = {},
                    onDateSelect = {},
                    onDateDismiss = {},
                    onNoteChange = {},
                    onSave = {},
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/transaction_edit_empty.png")
    }

    @Test
    fun captureTransactionEditFilled() {
        composeTestRule.setContent {
            MoneyManagerTheme(dynamicColor = false) {
                TransactionEditScreen(
                    state = TransactionEditState(
                        isLoading = false,
                        type = TransactionType.EXPENSE,
                        amount = "15000",
                        selectedCategory = testCategories[0],
                        categories = testCategories,
                        note = "Lunch with colleagues",
                    ),
                    onBack = {},
                    onTypeChange = {},
                    onAmountChange = {},
                    onCategoryClick = {},
                    onCategorySelect = {},
                    onCategoryDismiss = {},
                    onDateClick = {},
                    onDateSelect = {},
                    onDateDismiss = {},
                    onNoteChange = {},
                    onSave = {},
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/transaction_edit_filled.png")
    }
}
