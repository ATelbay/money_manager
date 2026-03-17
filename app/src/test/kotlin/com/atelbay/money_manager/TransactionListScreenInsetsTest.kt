package com.atelbay.money_manager

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.presentation.transactions.ui.list.TransactionListScreen
import com.atelbay.money_manager.presentation.transactions.ui.list.TransactionListState
import com.atelbay.money_manager.presentation.transactions.ui.list.TransactionRowState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "xxhdpi", application = android.app.Application::class)
class TransactionListScreenInsetsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun transactionList_respects_horizontal_scaffold_insets() {
        val leftInsetPx = with(composeTestRule.density) { 24.dp.roundToPx() }
        val rightInsetPx = with(composeTestRule.density) { 12.dp.roundToPx() }

        composeTestRule.setContent {
            MoneyManagerTheme(dynamicColor = false) {
                TransactionListScreen(
                    state = TransactionListState(
                        transactionRows = persistentListOf(
                            TransactionRowState(
                                transaction = Transaction(
                                    id = 1L,
                                    amount = 100.0,
                                    type = TransactionType.EXPENSE,
                                    categoryId = 1L,
                                    categoryName = "Food",
                                    categoryIcon = "wallet",
                                    categoryColor = 0L,
                                    accountId = 1L,
                                    note = null,
                                    date = 1L,
                                    createdAt = 1L,
                                ),
                                originalAmount = 100.0,
                                originalCurrency = "USD",
                                displayMoneyDisplay = MoneyDisplayFormatter.resolveAndFormat("USD"),
                            ),
                        ),
                        balance = 100.0,
                        isLoading = false,
                        periodIncome = 0.0,
                        periodExpense = 100.0,
                    ),
                    onTransactionClick = {},
                    onAddClick = {},
                    onImportClick = {},
                    onDeleteTransaction = {},
                    onTabSelected = {},
                    onPeriodSelected = {},
                    onSearchQueryChange = {},
                    onAccountPickerClick = {},
                    onAccountSelected = {},
                    onDismissAccountPicker = {},
                    contentWindowInsets = WindowInsets(
                        left = leftInsetPx,
                        top = 0,
                        right = rightInsetPx,
                        bottom = 0,
                    ),
                )
            }
        }

        val rootBounds = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot
        val balanceBounds = composeTestRule.onNodeWithTag("transactionList:balance")
            .fetchSemanticsNode().boundsInRoot
        val expectedStartInset = with(composeTestRule.density) { leftInsetPx.toDp().toPx() + 16.dp.toPx() }
        val expectedEndInset = with(composeTestRule.density) { rightInsetPx.toDp().toPx() + 16.dp.toPx() }

        assertTrue(balanceBounds.left >= expectedStartInset)
        assertTrue(rootBounds.right - balanceBounds.right >= expectedEndInset)
    }
}
