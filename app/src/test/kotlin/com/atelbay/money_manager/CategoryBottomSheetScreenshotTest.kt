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
class CategoryBottomSheetScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testCategories = listOf(
        Category(id = 1, name = "Food", icon = "🍔", color = 0xFFE57373, type = TransactionType.EXPENSE),
        Category(id = 2, name = "Transport", icon = "🚗", color = 0xFF81C784, type = TransactionType.EXPENSE),
        Category(id = 3, name = "Shopping", icon = "🛍️", color = 0xFF64B5F6, type = TransactionType.EXPENSE),
    ).toImmutableList()

    @Test
    fun captureCategorySheetEmpty() {
        composeTestRule.setContent {
            MoneyManagerTheme(dynamicColor = false) {
                TransactionEditScreen(
                    state = TransactionEditState(
                        isLoading = false,
                        showCategorySheet = true,
                        categories = persistentListOf(),
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

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/category_sheet_empty.png")
    }

    @Test
    fun captureCategorySheetWithItems() {
        composeTestRule.setContent {
            MoneyManagerTheme(dynamicColor = false) {
                TransactionEditScreen(
                    state = TransactionEditState(
                        isLoading = false,
                        showCategorySheet = true,
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

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/category_sheet_with_items.png")
    }

    @Test
    fun captureCategorySheetSelected() {
        composeTestRule.setContent {
            MoneyManagerTheme(dynamicColor = false) {
                TransactionEditScreen(
                    state = TransactionEditState(
                        isLoading = false,
                        showCategorySheet = true,
                        categories = testCategories,
                        selectedCategory = testCategories[0],
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

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/category_sheet_selected.png")
    }
}
