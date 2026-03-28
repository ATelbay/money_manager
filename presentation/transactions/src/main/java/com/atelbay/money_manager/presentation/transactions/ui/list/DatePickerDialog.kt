package com.atelbay.money_manager.presentation.transactions.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDatePickerDialog(
    onDismiss: () -> Unit,
    onMonthSelected: (year: Int, month: Int) -> Unit,
    onRangeSelected: (startMillis: Long, endMillis: Long) -> Unit,
) {
    val s = MoneyManagerTheme.strings
    val colors = MoneyManagerTheme.colors

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(s.selectMonth, s.selectRange)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .testTag("transactionList:datePickerDialog"),
        ) {
            Column(modifier = Modifier.fillMaxHeight()) {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier.testTag(
                                if (index == 0) "transactionList:tabMonth" else "transactionList:tabRange",
                            ),
                            text = {
                                Text(
                                    text = title,
                                    style = MoneyManagerTheme.typography.caption,
                                )
                            },
                        )
                    }
                }

                when (selectedTabIndex) {
                    0 -> MonthPickerTab(
                        onMonthSelected = { year, month ->
                            onMonthSelected(year, month)
                        },
                        onDismiss = onDismiss,
                    )
                    1 -> RangePickerTab(
                        onRangeSelected = onRangeSelected,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthPickerTab(
    onMonthSelected: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MoneyManagerTheme.colors
    val today = LocalDate.now()
    var displayYear by remember { mutableIntStateOf(today.year) }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM", Locale.getDefault()) }

    Column(modifier = Modifier.padding(16.dp)) {
        // Year navigation row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { displayYear-- },
                modifier = Modifier.testTag("transactionList:yearBack"),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colors.textSecondary,
                )
            }
            Text(
                text = displayYear.toString(),
                style = MoneyManagerTheme.typography.cardTitle,
                color = colors.textPrimary,
            )
            IconButton(
                onClick = { displayYear++ },
                modifier = Modifier.testTag("transactionList:yearForward"),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.textSecondary,
                )
            }
        }

        // 3x4 grid of month buttons
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(12) { index ->
                val monthNumber = index + 1
                val monthDate = LocalDate.of(displayYear, monthNumber, 1)
                val label = monthDate.format(monthFormatter)
                    .replaceFirstChar { it.uppercaseChar() }

                TextButton(
                    onClick = { onMonthSelected(displayYear, monthNumber) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transactionList:monthButton_${displayYear}_$monthNumber"),
                ) {
                    Text(
                        text = label,
                        style = MoneyManagerTheme.typography.caption,
                        textAlign = TextAlign.Center,
                        color = colors.textPrimary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerTab(
    onRangeSelected: (startMillis: Long, endMillis: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = MoneyManagerTheme.strings
    val colors = MoneyManagerTheme.colors

    val dateRangePickerState = rememberDateRangePickerState()

    Column(modifier = Modifier.fillMaxHeight()) {
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            showModeToggle = false,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("transactionList:rangeCancel"),
            ) {
                Text(
                    text = s.cancel,
                    color = colors.textSecondary,
                )
            }
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        onRangeSelected(start, end)
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null &&
                    dateRangePickerState.selectedEndDateMillis != null,
                modifier = Modifier.testTag("transactionList:rangeConfirm"),
            ) {
                Text(
                    text = s.ok,
                    color = colors.chart1,
                )
            }
        }
    }
}
