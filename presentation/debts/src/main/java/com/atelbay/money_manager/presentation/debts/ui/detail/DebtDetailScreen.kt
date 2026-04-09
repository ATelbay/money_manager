package com.atelbay.money_manager.presentation.debts.ui.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.core.model.DebtDirection
import com.atelbay.money_manager.core.model.DebtPayment
import com.atelbay.money_manager.core.model.DebtStatus
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.components.MoneyManagerButton
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal
import com.atelbay.money_manager.presentation.debts.ui.list.DebtEditBottomSheet
import kotlinx.collections.immutable.ImmutableList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(
    state: DebtDetailState,
    accounts: ImmutableList<Account>,
    onAddPayment: (Double, Long, String?, Boolean) -> Unit,
    onDeletePayment: (Long) -> Unit,
    onDeleteDebt: () -> Unit,
    onSaveDebt: (Debt) -> Unit,
    onTogglePaymentSheet: () -> Unit,
    onToggleEditSheet: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = MoneyManagerTheme.strings
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val debt = state.debt

    Scaffold(
        modifier = modifier.testTag("debtDetail:screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = debt?.contactName ?: "",
                        style = typography.sectionHeader,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("debtDetail:backButton"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = s.back,
                            tint = colors.textPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleEditSheet,
                        modifier = Modifier.testTag("debtDetail:editButton"),
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = colors.textPrimary,
                        )
                    }
                    IconButton(
                        onClick = onDeleteDebt,
                        modifier = Modifier.testTag("debtDetail:deleteButton"),
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = s.delete,
                            tint = colors.expenseForeground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        if (state.isLoading || debt == null) {
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

        val directionColor = if (debt.direction == DebtDirection.LENT) {
            colors.incomeForeground
        } else {
            colors.expenseForeground
        }
        val directionLabel = if (debt.direction == DebtDirection.LENT) s.iLent else s.iBorrowed
        val progress = if (debt.totalAmount > 0) {
            (debt.paidAmount / debt.totalAmount).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .testTag("debtDetail:content"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Debt Info Card
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debtDetail:infoCard"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = directionLabel,
                                style = typography.caption,
                                color = directionColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(directionColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )

                            if (debt.status == DebtStatus.PAID_OFF) {
                                Text(
                                    text = s.debtPaidOff,
                                    style = typography.caption,
                                    color = Teal,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Teal.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("debtDetail:paidOffBadge"),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = formatAmount(debt.totalAmount),
                            style = typography.sectionHeader,
                            color = colors.textPrimary,
                            modifier = Modifier.testTag("debtDetail:totalAmount"),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (debt.accountName.isNotBlank()) {
                            Row {
                                Text(
                                    text = s.selectAccount + ": ",
                                    style = typography.caption,
                                    color = colors.textSecondary,
                                )
                                Text(
                                    text = debt.accountName,
                                    style = typography.caption,
                                    color = colors.textPrimary,
                                )
                            }
                        }

                        Text(
                            text = formatDate(debt.createdAt),
                            style = typography.caption,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 4.dp),
                        )

                        if (!debt.note.isNullOrBlank()) {
                            Text(
                                text = debt.note,
                                style = typography.caption,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }

            // Progress section
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debtDetail:progressCard"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .testTag("debtDetail:progressBar"),
                            color = directionColor,
                            trackColor = colors.borderSubtle,
                            strokeCap = StrokeCap.Round,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = s.paidXOfY(
                                formatAmount(debt.paidAmount),
                                formatAmount(debt.totalAmount),
                            ),
                            style = typography.caption,
                            color = colors.textSecondary,
                            modifier = Modifier.testTag("debtDetail:paidText"),
                        )
                    }
                }
            }

            // Record payment button
            if (debt.status == DebtStatus.ACTIVE) {
                item {
                    MoneyManagerButton(
                        onClick = onTogglePaymentSheet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("debtDetail:recordPaymentButton"),
                    ) {
                        Text(
                            text = s.recordPayment,
                            style = typography.cardTitle,
                        )
                    }
                }
            }

            // Payments header
            if (state.payments.isNotEmpty()) {
                item {
                    Text(
                        text = s.debtPaymentStr,
                        style = typography.sectionHeader,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag("debtDetail:paymentsHeader"),
                    )
                }
            }

            // Payments list
            items(
                items = state.payments,
                key = { it.id },
            ) { payment ->
                PaymentSwipeToDeleteItem(
                    onDelete = { onDeletePayment(payment.id) },
                ) {
                    PaymentListItem(
                        payment = payment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("debtDetail:payment_${payment.id}"),
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Payment Bottom Sheet
    if (state.showPaymentSheet && debt != null) {
        PaymentBottomSheet(
            remainingAmount = debt.remainingAmount,
            onSave = onAddPayment,
            onDismiss = onTogglePaymentSheet,
        )
    }

    // Edit Bottom Sheet
    if (state.showEditSheet && debt != null) {
        DebtEditBottomSheet(
            debt = debt,
            accounts = accounts,
            onSave = { updatedDebt ->
                onSaveDebt(updatedDebt)
                onToggleEditSheet()
            },
            onDismiss = onToggleEditSheet,
        )
    }
}

@Composable
private fun PaymentListItem(
    payment: DebtPayment,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDate(payment.date),
                    style = typography.caption,
                    color = colors.textSecondary,
                )
                if (!payment.note.isNullOrBlank()) {
                    Text(
                        text = payment.note,
                        style = typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatAmount(payment.amount),
                style = typography.cardTitle,
                color = colors.incomeForeground,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentSwipeToDeleteItem(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    var dismissed by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                dismissed = true
                true
            } else {
                false
            }
        },
    )

    LaunchedEffect(dismissed) {
        if (dismissed) onDelete()
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color.Transparent
                },
                label = "swipe_bg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = MoneyManagerTheme.strings.delete,
                    tint = MaterialTheme.colorScheme.onError,
                )
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        content()
    }
}

private fun formatAmount(amount: Double): String {
    return if (amount >= 1_000_000) {
        "${(amount / 1_000_000).toBigDecimal().stripTrailingZeros().toPlainString()}M"
    } else if (amount >= 1_000) {
        "${(amount / 1_000).toBigDecimal().stripTrailingZeros().toPlainString()}K"
    } else {
        amount.toBigDecimal().stripTrailingZeros().toPlainString()
    }
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}
