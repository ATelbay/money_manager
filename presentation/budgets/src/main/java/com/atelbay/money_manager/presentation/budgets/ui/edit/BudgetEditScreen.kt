package com.atelbay.money_manager.presentation.budgets.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.components.MoneyManagerButton
import com.atelbay.money_manager.core.ui.components.MoneyManagerTextField
import com.atelbay.money_manager.core.ui.components.categoryIconFromName
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditScreen(
    state: BudgetEditState,
    onBack: () -> Unit,
    onCategoryClick: () -> Unit,
    onCategorySelect: (Category) -> Unit,
    onCategoryDismiss: () -> Unit,
    onLimitChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val s = MoneyManagerTheme.strings

    Scaffold(
        modifier = modifier.testTag("budgetEdit:screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditing) s.budget else s.newBudget,
                        style = typography.sectionHeader,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("budgetEdit:back"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = s.back,
                            tint = colors.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
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
                CircularProgressIndicator(color = Teal)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Category Selector
            Text(
                text = s.category,
                style = typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            BudgetCategorySelector(
                categoryId = state.categoryId,
                categoryName = state.categoryName,
                categoryIcon = state.categoryIcon,
                categoryColor = state.categoryColor,
                errorMessage = state.categoryError,
                onClick = onCategoryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budgetEdit:categorySelector"),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Monthly Limit Field
            MoneyManagerTextField(
                value = state.monthlyLimit,
                onValueChange = onLimitChange,
                label = s.monthlyLimit,
                placeholder = "0",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                tag = "budgetEdit:limitField",
            )

            if (state.limitError != null) {
                Text(
                    text = state.limitError,
                    style = typography.caption,
                    color = colors.expenseForeground,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            MoneyManagerButton(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budgetEdit:saveButton"),
            ) {
                Text(
                    text = if (state.isSaving) s.saving else s.save,
                    style = typography.cardTitle,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (state.showCategoryPicker) {
            BudgetCategoryBottomSheet(
                categories = state.expenseCategories,
                selectedCategoryId = state.categoryId,
                onSelect = onCategorySelect,
                onDismiss = onCategoryDismiss,
            )
        }
    }
}

@Composable
private fun BudgetCategorySelector(
    categoryId: Long,
    categoryName: String,
    categoryIcon: String,
    categoryColor: Long,
    errorMessage: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Column(modifier = modifier) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (categoryId != 0L) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(categoryColor).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = categoryIconFromName(categoryIcon),
                            contentDescription = null,
                            tint = Color(categoryColor),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = categoryName,
                        style = typography.cardTitle,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Text(
                        text = MoneyManagerTheme.strings.chooseCategory,
                        style = typography.cardTitle,
                        color = colors.textTertiary,
                        modifier = Modifier.weight(1f),
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = typography.caption,
                color = colors.expenseForeground,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetCategoryBottomSheet(
    categories: ImmutableList<Category>,
    selectedCategoryId: Long,
    onSelect: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("budgetEdit:categorySheet"),
    ) {
        Text(
            text = MoneyManagerTheme.strings.chooseCategory,
            style = typography.sectionHeader,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categories, key = { it.id }) { category ->
                BudgetCategoryGridItem(
                    category = category,
                    isSelected = category.id == selectedCategoryId,
                    onClick = { onSelect(category) },
                    modifier = Modifier.testTag("budgetEdit:category_${category.id}"),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun BudgetCategoryGridItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val categoryColor = Color(category.color)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, Teal, CircleShape)
                    } else {
                        Modifier
                    },
                )
                .clip(CircleShape)
                .background(
                    if (isSelected) categoryColor.copy(alpha = 0.25f)
                    else categoryColor.copy(alpha = 0.12f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Icon(
                    imageVector = categoryIconFromName(category.icon),
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = category.name,
            style = MoneyManagerTheme.typography.caption,
            color = if (isSelected) colors.textPrimary else colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
