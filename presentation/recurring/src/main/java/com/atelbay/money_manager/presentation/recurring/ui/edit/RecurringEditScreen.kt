package com.atelbay.money_manager.presentation.recurring.ui.edit

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.Frequency
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.ui.components.AccountSelector
import com.atelbay.money_manager.core.ui.components.CategoryItem
import com.atelbay.money_manager.core.ui.components.CategoryPicker
import com.atelbay.money_manager.core.ui.components.ChipType
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.components.MoneyManagerButton
import com.atelbay.money_manager.core.ui.components.MoneyManagerChip
import com.atelbay.money_manager.core.ui.components.MoneyManagerTextField
import com.atelbay.money_manager.core.ui.components.categoryIconFromName
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringEditScreen(
    state: RecurringEditState,
    onBack: () -> Unit,
    onAmountChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onCategoryClick: () -> Unit,
    onCategorySelect: (Long) -> Unit,
    onCategoryDismiss: () -> Unit,
    onAccountSelect: (Long) -> Unit,
    onFrequencySelect: (Frequency) -> Unit,
    onDayOfMonthSelect: (Int) -> Unit,
    onDayOfWeekSelect: (Int) -> Unit,
    onStartDateClick: () -> Unit,
    onStartDateSelect: (Long) -> Unit,
    onStartDateDismiss: () -> Unit,
    onEndDateClick: () -> Unit,
    onEndDateSelect: (Long?) -> Unit,
    onEndDateDismiss: () -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = MoneyManagerTheme.strings
    val typography = MoneyManagerTheme.typography
    val colors = MoneyManagerTheme.colors
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    Scaffold(
        modifier = modifier.testTag("recurringEdit:screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditing) s.editRecurring else s.newRecurring,
                        style = typography.sectionHeader,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("recurringEdit:back"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // Amount
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MoneyManagerTextField(
                        value = state.amount,
                        onValueChange = onAmountChange,
                        label = s.amount,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        errorMessage = state.amountError,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "recurringEdit:amount",
                    )
                }
            }

            // Type toggle
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MoneyManagerChip(
                        label = s.expense,
                        selected = state.type == TransactionType.EXPENSE,
                        type = ChipType.EXPENSE,
                        onClick = { onTypeChange(TransactionType.EXPENSE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recurringEdit:typeExpense"),
                    )
                    MoneyManagerChip(
                        label = s.income,
                        selected = state.type == TransactionType.INCOME,
                        type = ChipType.INCOME,
                        onClick = { onTypeChange(TransactionType.INCOME) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recurringEdit:typeIncome"),
                    )
                }
            }

            // Category
            GlassCard(
                onClick = onCategoryClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("recurringEdit:categoryRow"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = s.category,
                        style = typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.width(80.dp),
                    )
                    if (state.categoryId != 0L) {
                        Icon(
                            imageVector = categoryIconFromName(state.categoryIcon),
                            contentDescription = state.categoryName,
                            tint = Color(state.categoryColor),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.categoryName,
                            style = typography.cardTitle,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Text(
                            text = s.chooseCategory,
                            style = typography.cardTitle,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (state.categoryError != null) {
                Text(
                    text = state.categoryError,
                    style = typography.caption,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("recurringEdit:categoryError"),
                )
            }

            // Account
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    AccountSelector(
                        accounts = state.accounts,
                        selectedAccount = state.accounts.find { it.id == state.accountId },
                        onAccountSelected = onAccountSelect,
                        label = s.selectAccount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recurringEdit:accountSelector"),
                    )
                }
            }

            if (state.accountError != null) {
                Text(
                    text = state.accountError,
                    style = typography.caption,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("recurringEdit:accountError"),
                )
            }

            // Frequency
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = s.frequency,
                        style = typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FrequencyChip(
                            label = s.daily,
                            selected = state.frequency == Frequency.DAILY,
                            onClick = { onFrequencySelect(Frequency.DAILY) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("recurringEdit:freqDaily"),
                        )
                        FrequencyChip(
                            label = s.weekly,
                            selected = state.frequency == Frequency.WEEKLY,
                            onClick = { onFrequencySelect(Frequency.WEEKLY) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("recurringEdit:freqWeekly"),
                        )
                        FrequencyChip(
                            label = s.monthly,
                            selected = state.frequency == Frequency.MONTHLY,
                            onClick = { onFrequencySelect(Frequency.MONTHLY) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("recurringEdit:freqMonthly"),
                        )
                        FrequencyChip(
                            label = s.yearly,
                            selected = state.frequency == Frequency.YEARLY,
                            onClick = { onFrequencySelect(Frequency.YEARLY) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("recurringEdit:freqYearly"),
                        )
                    }
                }
            }

            // Day of month picker (MONTHLY only)
            if (state.frequency == Frequency.MONTHLY) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = s.dayOfMonth,
                            style = typography.caption,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        DayOfMonthGrid(
                            selectedDay = state.dayOfMonth,
                            onDaySelected = onDayOfMonthSelect,
                        )
                    }
                }
            }

            // Day of week picker (WEEKLY only)
            if (state.frequency == Frequency.WEEKLY) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = s.dayOfWeek,
                            style = typography.caption,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        DayOfWeekRow(
                            selectedDay = state.dayOfWeek,
                            onDaySelected = onDayOfWeekSelect,
                        )
                    }
                }
            }

            // Start date
            GlassCard(
                onClick = onStartDateClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("recurringEdit:startDateRow"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = s.startDate,
                        style = typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.width(100.dp),
                    )
                    Text(
                        text = dateFormat.format(Date(state.startDate)),
                        style = typography.cardTitle,
                        color = colors.textPrimary,
                    )
                }
            }

            // End date
            GlassCard(
                onClick = onEndDateClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("recurringEdit:endDateRow"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = s.endDate,
                        style = typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.width(100.dp),
                    )
                    Text(
                        text = if (state.endDate != null) dateFormat.format(Date(state.endDate)) else "-",
                        style = typography.cardTitle,
                        color = if (state.endDate != null) colors.textPrimary else colors.textSecondary,
                    )
                }
            }

            if (state.dateError != null) {
                Text(
                    text = state.dateError,
                    style = typography.caption,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("recurringEdit:dateError"),
                )
            }

            // Note
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    MoneyManagerTextField(
                        value = state.note,
                        onValueChange = onNoteChange,
                        label = s.note,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "recurringEdit:note",
                    )
                }
            }

            // Save button
            MoneyManagerButton(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurringEdit:save"),
            ) {
                Text(if (state.isSaving) s.saving else s.save)
            }

            if (state.saveError != null) {
                Text(
                    text = state.saveError,
                    style = typography.caption,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("recurringEdit:saveError"),
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        // Category bottom sheet
        if (state.showCategoryPicker) {
            CategoryPicker(
                categories = state.categories.map { cat ->
                    CategoryItem(
                        id = cat.id,
                        name = cat.name,
                        icon = categoryIconFromName(cat.icon),
                        color = Color(cat.color),
                    )
                },
                selectedCategoryId = state.categoryId.takeIf { it != 0L },
                onCategorySelected = { item -> onCategorySelect(item.id) },
                onDismiss = onCategoryDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurringEdit:categoryPicker"),
            )
        }

        // Start date picker
        if (state.showStartDatePicker) {
            val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.startDate)
            DatePickerDialog(
                onDismissRequest = onStartDateDismiss,
                confirmButton = {
                    TextButton(
                        onClick = {
                            pickerState.selectedDateMillis?.let { onStartDateSelect(it) }
                                ?: onStartDateDismiss()
                        },
                        modifier = Modifier.testTag("recurringEdit:startDateConfirm"),
                    ) {
                        Text(s.ok)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onStartDateDismiss,
                        modifier = Modifier.testTag("recurringEdit:startDateCancel"),
                    ) {
                        Text(s.cancel)
                    }
                },
            ) {
                DatePicker(state = pickerState)
            }
        }

        // End date picker
        if (state.showEndDatePicker) {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = state.endDate ?: System.currentTimeMillis(),
            )
            DatePickerDialog(
                onDismissRequest = onEndDateDismiss,
                confirmButton = {
                    TextButton(
                        onClick = {
                            onEndDateSelect(pickerState.selectedDateMillis)
                        },
                        modifier = Modifier.testTag("recurringEdit:endDateConfirm"),
                    ) {
                        Text(s.ok)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onEndDateSelect(null) },
                        modifier = Modifier.testTag("recurringEdit:endDateClear"),
                    ) {
                        Text(s.delete)
                    }
                },
            ) {
                DatePicker(state = pickerState)
            }
        }
    }
}

@Composable
private fun FrequencyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) Color(0xFF818CF8).copy(alpha = 0.2f) else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = typography.caption,
            color = if (selected) Color(0xFF818CF8) else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DayOfMonthGrid(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
) {
    val days = (1..31).toList()
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .testTag("recurringEdit:dayOfMonthGrid"),
        userScrollEnabled = false,
    ) {
        items(days) { day ->
            val isSelected = day == selectedDay
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF818CF8) else Color.Transparent)
                    .clickable { onDaySelected(day) }
                    .testTag("recurringEdit:dayOfMonth_$day"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.toString(),
                    style = MoneyManagerTheme.typography.caption,
                    color = if (isSelected) Color.White else MoneyManagerTheme.colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun DayOfWeekRow(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
) {
    val dayLabels = MoneyManagerTheme.strings.dayOfWeekShort
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recurringEdit:dayOfWeekRow"),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        dayLabels.forEachIndexed { index, label ->
            val dayNum = index + 1
            val isSelected = dayNum == selectedDay
            Box(
                modifier = Modifier
                    .weight(1f)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF818CF8) else Color.Transparent)
                    .clickable { onDaySelected(dayNum) }
                    .testTag("recurringEdit:dayOfWeek_$dayNum"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MoneyManagerTheme.typography.caption,
                    color = if (isSelected) Color.White else MoneyManagerTheme.colors.textPrimary,
                )
            }
        }
    }
}
