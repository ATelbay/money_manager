package com.atelbay.money_manager.presentation.statistics.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.model.TransactionType as ModelTransactionType
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.domain.accounts.usecase.GetAccountsUseCase
import com.atelbay.money_manager.domain.categories.usecase.GetCategoryByIdUseCase
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.model.TransactionType
import com.atelbay.money_manager.domain.transactions.usecase.GetTransactionsByCategoryAndDateRangeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class CategoryTransactionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getTransactionsByCategoryAndDateRangeUseCase: GetTransactionsByCategoryAndDateRangeUseCase,
    getCategoryByIdUseCase: GetCategoryByIdUseCase,
    getAccountsUseCase: GetAccountsUseCase,
) : ViewModel() {

    private val args = savedStateHandle.toCategoryTransactionsArgs()

    private val _state = MutableStateFlow(
        args?.toInitialState() ?: CategoryTransactionsState(
            isLoading = false,
            error = UiError.LoadFailed,
        ),
    )
    val state: StateFlow<CategoryTransactionsState> = _state.asStateFlow()

    init {
        val routeArgs = args
        if (routeArgs != null) {
            combine(
                getCategoryByIdUseCase(routeArgs.categoryId),
                getTransactionsByCategoryAndDateRangeUseCase(
                    categoryId = routeArgs.categoryId,
                    transactionType = routeArgs.transactionType.toModelTransactionType(),
                    startMillis = routeArgs.startMillis,
                    endMillis = routeArgs.endMillis,
                ),
                getAccountsUseCase(),
            ) { category, transactions, accounts ->
                val currencyByAccountId = accounts.associate { account ->
                    account.id to account.currency
                }
                CategoryTransactionsState(
                    categoryId = routeArgs.categoryId,
                    categoryName = category?.name ?: routeArgs.categoryName,
                    categoryIcon = category?.icon ?: routeArgs.categoryIcon,
                    categoryColor = category?.color ?: routeArgs.categoryColor,
                    transactionType = routeArgs.transactionType,
                    period = routeArgs.period,
                    startMillis = routeArgs.startMillis,
                    endMillis = routeArgs.endMillis,
                    transactions = transactions.map { transaction ->
                        CategoryTransactionItem(
                            transactionId = transaction.id,
                            description = transaction.note?.ifBlank { transaction.categoryName }
                                ?: transaction.categoryName,
                            categoryName = transaction.categoryName,
                            categoryIcon = transaction.categoryIcon,
                            categoryColor = transaction.categoryColor,
                            amount = transaction.amount,
                            moneyDisplay = MoneyDisplayFormatter.resolveAndFormat(
                                currencyByAccountId[transaction.accountId].orEmpty(),
                            ),
                            date = transaction.date,
                            isIncome = transaction.type == ModelTransactionType.INCOME,
                        )
                    }.toImmutableList(),
                    isLoading = false,
                )
            }
                .catch { _ ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = UiError.LoadFailed,
                    )
                }
                .onEach { _state.value = it }
                .launchIn(viewModelScope)
        }
    }
}

private data class CategoryTransactionsArgs(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val transactionType: TransactionType,
    val period: StatsPeriod,
    val startMillis: Long,
    val endMillis: Long,
)

private fun SavedStateHandle.toCategoryTransactionsArgs(): CategoryTransactionsArgs? {
    val categoryId = get<Long>("categoryId") ?: return null
    val categoryName = get<String>("categoryName") ?: return null
    val categoryIcon = get<String>("categoryIcon") ?: ""
    val categoryColor = get<Long>("categoryColor") ?: 0L
    val transactionTypeName = get<String>("transactionType") ?: return null
    val periodName = get<String>("period") ?: return null
    val startMillis = get<Long>("startMillis") ?: return null
    val endMillis = get<Long>("endMillis") ?: return null

    val transactionType = runCatching { TransactionType.valueOf(transactionTypeName) }.getOrNull()
        ?: return null
    val period = runCatching { StatsPeriod.valueOf(periodName) }.getOrNull() ?: return null

    return CategoryTransactionsArgs(
        categoryId = categoryId,
        categoryName = categoryName,
        categoryIcon = categoryIcon,
        categoryColor = categoryColor,
        transactionType = transactionType,
        period = period,
        startMillis = startMillis,
        endMillis = endMillis,
    )
}

private fun CategoryTransactionsArgs.toInitialState(): CategoryTransactionsState =
    CategoryTransactionsState(
        categoryId = categoryId,
        categoryName = categoryName,
        categoryIcon = categoryIcon,
        categoryColor = categoryColor,
        transactionType = transactionType,
        period = period,
        startMillis = startMillis,
        endMillis = endMillis,
        isLoading = true,
    )

private fun TransactionType.toModelTransactionType(): ModelTransactionType =
    when (this) {
        TransactionType.EXPENSE -> ModelTransactionType.EXPENSE
        TransactionType.INCOME -> ModelTransactionType.INCOME
    }
