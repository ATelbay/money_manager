package com.atelbay.money_manager.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

/**
 * A bento grid cell that wraps content in a GlassCard.
 */
@Composable
fun BentoCell(
    modifier: Modifier = Modifier,
    height: Dp = Dp.Unspecified,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassCard(
        modifier = modifier.then(
            if (height != Dp.Unspecified) Modifier.height(height) else Modifier,
        ),
        onClick = onClick,
        content = content,
    )
}

/**
 * 2-column asymmetric bento grid layout.
 *
 * Usage:
 * ```
 * BentoGrid(gap = 8.dp) {
 *     fullWidth { BentoCell(height = 200.dp) { ... } }
 *     halfRow(
 *         left = { BentoCell(height = 180.dp) { ... } },
 *         right = { BentoCell(height = 180.dp) { ... } },
 *     )
 *     fullWidth { BentoCell(height = 100.dp) { ... } }
 * }
 * ```
 */
@Composable
fun BentoGrid(
    modifier: Modifier = Modifier,
    gap: Dp = 8.dp,
    content: BentoGridScope.() -> Unit,
) {
    val scope = BentoGridScopeImpl().apply(content)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        scope.rows.forEach { row ->
            when (row) {
                is BentoRow.FullWidth -> {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        row.content()
                    }
                }
                is BentoRow.HalfRow -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            row.left()
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            row.right()
                        }
                    }
                }
            }
        }
    }
}

interface BentoGridScope {
    fun fullWidth(content: @Composable () -> Unit)
    fun halfRow(left: @Composable () -> Unit, right: @Composable () -> Unit)
}

private sealed class BentoRow {
    data class FullWidth(val content: @Composable () -> Unit) : BentoRow()
    data class HalfRow(
        val left: @Composable () -> Unit,
        val right: @Composable () -> Unit,
    ) : BentoRow()
}

private class BentoGridScopeImpl : BentoGridScope {
    val rows = mutableListOf<BentoRow>()

    override fun fullWidth(content: @Composable () -> Unit) {
        rows.add(BentoRow.FullWidth(content))
    }

    override fun halfRow(left: @Composable () -> Unit, right: @Composable () -> Unit) {
        rows.add(BentoRow.HalfRow(left, right))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun BentoGridPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        BentoGrid(modifier = Modifier.padding(16.dp)) {
            fullWidth {
                BentoCell(
                    modifier = Modifier.fillMaxWidth(),
                    height = 200.dp,
                ) {
                    Text(
                        text = "Spending Trend",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            halfRow(
                left = {
                    BentoCell(
                        modifier = Modifier.fillMaxWidth(),
                        height = 180.dp,
                    ) {
                        Text(
                            text = "Categories",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                },
                right = {
                    BentoCell(
                        modifier = Modifier.fillMaxWidth(),
                        height = 180.dp,
                    ) {
                        Text(
                            text = "Top Category",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                },
            )
            halfRow(
                left = {
                    BentoCell(
                        modifier = Modifier.fillMaxWidth(),
                        height = 120.dp,
                    ) {
                        Text(
                            text = "Income",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                },
                right = {
                    BentoCell(
                        modifier = Modifier.fillMaxWidth(),
                        height = 120.dp,
                    ) {
                        Text(
                            text = "Expenses",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                },
            )
        }
    }
}
