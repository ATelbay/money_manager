package com.atelbay.money_manager.presentation.statistics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import java.time.YearMonth

private val MONTH_ABBREVIATIONS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

@Composable
fun MonthPickerDialog(
    initialYearMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    val now = YearMonth.now()
    var displayedYear by remember { mutableIntStateOf(initialYearMonth.year) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("statistics:monthPicker"),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                // Year selector row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { displayedYear-- }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous year",
                            tint = MoneyManagerTheme.colors.textPrimary,
                        )
                    }
                    Text(
                        text = displayedYear.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MoneyManagerTheme.colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    IconButton(onClick = { displayedYear++ }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next year",
                            tint = MoneyManagerTheme.colors.textPrimary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3×4 month grid
                val rows = MONTH_ABBREVIATIONS.chunked(3)
                rows.forEach { rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        rowMonths.forEach { abbr ->
                            val monthNumber = MONTH_ABBREVIATIONS.indexOf(abbr) + 1
                            val yearMonth = YearMonth.of(displayedYear, monthNumber)
                            val isSelected = yearMonth == initialYearMonth
                            val isCurrent = yearMonth == now

                            val bgColor = when {
                                isSelected -> MoneyManagerTheme.colors.greenAccent
                                isCurrent -> MoneyManagerTheme.colors.divider
                                else -> androidx.compose.ui.graphics.Color.Transparent
                            }
                            val textColor = when {
                                isSelected -> MaterialTheme.colorScheme.surface
                                isCurrent -> MoneyManagerTheme.colors.textPrimary
                                else -> MoneyManagerTheme.colors.textSecondary
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .clickable { onMonthSelected(yearMonth) },
                            ) {
                                Text(
                                    text = abbr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
